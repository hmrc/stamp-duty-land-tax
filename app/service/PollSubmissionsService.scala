/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package service

import connectors.{ChrisConnector, FilingFormpProxyConnector, FormpProxyConnector}
import models.filing.*
import models.polling.SubmissionForPolling
import models.submission.*
import play.api.Logging
import scheduler.ScheduleStatus.{FailedToPollSubmissions, MongoUnlockException}
import scheduler.{MongoLockKeys, ScheduleStatus, ScheduledService}
import service.filing.ChrisService
import service.submission.{EmailService, SubmissionAuditService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{Clock, LocalDateTime, ZoneOffset, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future, duration}
import scala.util.Try

class PollSubmissionsService @Inject() (
  formpConnector: FormpProxyConnector,
  chrisConnector: ChrisConnector,
  chrisService: ChrisService,
  filingConnector: FilingFormpProxyConnector,
  audit: SubmissionAuditService,
  emailService: EmailService,
  servicesConfig: ServicesConfig,
  lockRepositoryProvider: MongoLockRepository,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends ScheduledService[Either[ScheduleStatus.JobFailed, List[String]]]
    with Logging {

  val jobName: String = "PollSubmissionsJob"

  private implicit val hc: HeaderCarrier =
    HeaderCarrier(extraHeaders = Seq("X-API-Key" -> servicesConfig.getString("internal-service-api-key")))

  lazy val mongoLockTimeoutDuration: duration.Duration =
    duration.Duration(servicesConfig.getString(s"schedules.${MongoLockKeys.pollSubmissionsLock}.mongoLockTimeout"))

  lazy val lockKeeper: LockService = LockService(
    lockRepository = lockRepositoryProvider,
    lockId = s"schedules.${MongoLockKeys.pollSubmissionsLock}",
    ttl = mongoLockTimeoutDuration
  )

  def tryLock(
    f: => Future[Either[ScheduleStatus.JobFailed, List[String]]]
  ): Future[Either[ScheduleStatus.JobFailed, List[String]]] =
    lockKeeper
      .withLock(f)
      .map {
        case Some(result) => result
        case None =>
          logger.info(s"[$jobName] locked because it might be running on another instance")
          Right(Nil)
      }
      .recover { case e: Exception =>
        logger.warn(s"[$jobName] failed with exception: ${Try(e.getMessage).toOption}")
        Left(MongoUnlockException(e))
      }

  override def invoke: Future[Either[ScheduleStatus.JobFailed, List[String]]] =
    tryLock {
      poll().map(Right(_)).recover { case e: Exception =>
        logger.error(s"[$jobName] failed while polling submissions", e)
        Left(FailedToPollSubmissions(e))
      }
    }

  private def poll(): Future[List[String]] = {
    logger.info(s"[$jobName] starting batch poll")
    for {
      selected <- formpConnector.getSubmissionsForPolling().map(_.submissions)
      _         = logger.info(s"[$jobName] selected for polling: ${refList(selected)}")
      outcomes <- pollSequentially(selected)
    } yield {
      val polled = outcomes.filter(_.polled)
      logger.info(s"[$jobName] polled: ${refList(polled.map(_.submission))}")
      logger.info(BatchPollingReport.render(outcomes, LocalDateTime.now(clock)))
      polled.map(_.submission.returnResourceRef)
    }
  }

  private def refList(submissions: List[SubmissionForPolling]): String =
    if (submissions.isEmpty) "none" else submissions.map(s => s"${s.storn}/${s.returnResourceRef}").mkString(", ")

  private def pollSequentially(selected: List[SubmissionForPolling]): Future[List[PollOutcome]] =
    selected.foldLeft(Future.successful(List.empty[PollOutcome])) { (acc, submission) =>
      acc.flatMap(outcomes => pollOne(submission).map(outcomes :+ _))
    }

  private def pollOne(sub: SubmissionForPolling): Future[PollOutcome] =
    chrisService
      .selectGovTalkStatus(SelectGovTalkStatusRequest(sub.storn, sub.returnResourceRef))
      .flatMap { row =>
        resolveCorrelationId(sub, row) match {
          case None                => Future.successful(notPolled(sub))
          case Some(correlationId) => pollWithLock(sub, row, correlationId)
        }
      }
      .recover { case e =>
        logger.warn(s"[$jobName] failed to poll submission ${sub.returnResourceRef}: ${e.getMessage}")
        notPolled(sub)
      }

  private def resolveCorrelationId(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse): Option[String] =
    row.correlationId.map(_.trim).filter(c => c.nonEmpty && !c.equalsIgnoreCase("empty")) match {
      case None =>
        logger.warn(s"[$jobName] no correlation id on GovTalk status, skipping storn=${sub.storn} ref=${sub.returnResourceRef}")
        None
      case Some(_) if !pollAllowed(row) =>
        logger.info(s"[$jobName] poll not allowed yet storn=${sub.storn} ref=${sub.returnResourceRef} protocolStatus=${row.protocolStatus.getOrElse("-")} lastMessage=${row.lastMessageTimestamp.getOrElse("-")} pollInterval=${row.pollInterval.getOrElse("-")}")
        None
      case some => some
    }

  private def pollAllowed(row: SelectGovTalkStatusResponse): Boolean =
    row.protocolStatus.map(_.trim).contains("dataPoll") && intervalElapsed(row)

  private def intervalElapsed(row: SelectGovTalkStatusResponse): Boolean = {
    val interval = row.pollInterval.flatMap(s => Try(s.trim.toLong).toOption).getOrElse(0L)
    row.lastMessageTimestamp.map(_.trim).filter(_.nonEmpty).flatMap(parseTimestamp) match {
      case Some(lastMessage) => !LocalDateTime.now(clock.withZone(ZoneOffset.UTC)).isBefore(lastMessage.plusSeconds(interval))
      case None              => true
    }
  }

  private val TimestampFormatter: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd HH:mm:ss")
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
      .toFormatter

  private def parseTimestamp(value: String): Option[LocalDateTime] =
    Try(LocalDateTime.parse(value.replace('T', ' '), TimestampFormatter)).toOption match {
      case None =>
        logger.warn(s"[$jobName] unparseable GovTalk last message timestamp '$value', treating the poll interval as elapsed")
        None
      case parsed => parsed
    }

  private def pollWithLock(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse, correlationId: String): Future[PollOutcome] =
    acquireGovTalkLock(sub, row).flatMap {
      case false => Future.successful(notPolled(sub))
      case true  =>
        pollLocked(sub, row, correlationId).transformWith { result =>
          releaseGovTalkLock(sub).transformWith(_ => Future.fromTry(result))
        }
    }

  private def acquireGovTalkLock(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse): Future[Boolean] =
    chrisService
      .updateGovTalkStatusLock(buildLockRequest(sub, row, formLockOld = "N", formLockNew = "Y"))
      .map(_ => true)
      .recover { case e =>
        logger.info(s"[$jobName] GovTalk status already locked, skipping storn=${sub.storn} ref=${sub.returnResourceRef}: ${e.getMessage}")
        false
      }

  private def releaseGovTalkLock(sub: SubmissionForPolling): Future[Unit] =
    chrisService
      .selectGovTalkStatus(SelectGovTalkStatusRequest(sub.storn, sub.returnResourceRef))
      .flatMap { fresh =>
        if (fresh.formLock.map(_.trim).contains("Y"))
          chrisService.updateGovTalkStatusLock(buildLockRequest(sub, fresh, formLockOld = "Y", formLockNew = "N")).map { _ =>
            logger.info(s"[$jobName] GovTalk lock released storn=${sub.storn} ref=${sub.returnResourceRef}")
            ()
          }
        else {
          logger.info(s"[$jobName] GovTalk lock already released storn=${sub.storn} ref=${sub.returnResourceRef}")
          Future.unit
        }
      }
      .recover { case e =>
        logger.error(s"[$jobName] GovTalk lock release failed storn=${sub.storn} ref=${sub.returnResourceRef}: ${e.getMessage}")
        ()
      }

  private def buildLockRequest(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse, formLockOld: String, formLockNew: String): UpdateGovTalkStatusLockRequest =
    UpdateGovTalkStatusLockRequest(
      userIdentifier = sub.storn,
      formResultId   = sub.returnResourceRef,
      govTalkStatus  = GovTalkStatusLock(
        formLockOld  = formLockOld,
        formLockNew  = formLockNew,
        pollInterval = row.pollInterval.getOrElse("0"),
        gatewayUrl   = gatewayUrlOf(row).getOrElse(chrisConnector.defaultPath)
      )
    )

  private def gatewayUrlOf(row: SelectGovTalkStatusResponse): Option[String] =
    row.gatewayUrl.map(_.trim.stripSuffix("/")).filter(url => url.nonEmpty && url != servicesConfig.baseUrl("chris").stripSuffix("/"))

  private case class PollContext(
    sub: SubmissionForPolling,
    correlationId: String,
    fullReturn: FullReturn,
    polls: Int,
    rowPollInterval: String,
    gatewayUrl: Option[String]
  )

  private def pollLocked(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse, correlationId: String): Future[PollOutcome] =
    for {
      fullReturn <- filingConnector.getFullReturn(GetReturnByRefRequest(sub.returnResourceRef, sub.storn))
      ctx         = PollContext(
                      sub             = sub,
                      correlationId   = correlationId,
                      fullReturn      = fullReturn,
                      polls           = row.numberOfPolls.flatMap(s => Try(s.trim.toInt).toOption).getOrElse(0) + 1,
                      rowPollInterval = row.pollInterval.getOrElse("0"),
                      gatewayUrl      = gatewayUrlOf(row)
                    )
      _          <- updateGovTalkStatistics(ctx.sub, ctx.polls, ctx.rowPollInterval, ctx.gatewayUrl)
      resp       <- chrisConnector.poll(ctx.gatewayUrl, ctx.correlationId)(HeaderCarrier())
      outcome    <- handlePollResponse(ctx, resp)
    } yield outcome

  private def handlePollResponse(ctx: PollContext, resp: ChrisResponse): Future[PollOutcome] =
    resp match {
      case t: ChrisResponse.TransportError =>
        logger.warn(s"[$jobName] no response from ChRIS (${t.message}), will poll again next cycle storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef}")
        Future.successful(polled(ctx, ctx.sub.submissionStatus, ctx.sub.submissionStatus))

      case a: ChrisResponse.Acknowledged =>
        acknowledgementBranch(ctx, a)

      case c: ChrisResponse.Completed =>
        successBranch(ctx, c, universalStatus(ctx, c))

      case e: ChrisResponse.Errored =>
        errorBranch(ctx, e, universalStatus(ctx, e))
    }

  private def universalStatus(ctx: PollContext, resp: ChrisResponse): UniversalStatus =
    UniversalStatus.fromChrisResponse(resp, ctx.fullReturn.submission.flatMap(_.irmarkSent))

  private def acknowledgementBranch(ctx: PollContext, resp: ChrisResponse.Acknowledged): Future[PollOutcome] =
    for {
      _ <- persistUpdate(ctx.sub, baseUpdate(ctx.fullReturn).copy(submittableStatus = Some(UniversalStatus.ACCEPTED.toString)))
      _ <- setGovTalkProtocol(ctx.sub, "dataPoll")
      _ <- updateGovTalkStatistics(ctx.sub, ctx.polls, resp.pollIntervalSeconds.map(_.toString).getOrElse(ctx.rowPollInterval), resp.responseEndPoint.orElse(ctx.gatewayUrl))
    } yield polled(ctx, UniversalStatus.ACCEPTED.toString, UniversalStatus.ACCEPTED.toString)

  private def successBranch(ctx: PollContext, resp: ChrisResponse.Completed, universal: UniversalStatus): Future[PollOutcome] = {
    val endpoint = resp.responseEndPoint.orElse(ctx.gatewayUrl)
    
    val nowIso = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    
    logger.info(s"[$jobName] poll SUCCESS storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef} universalStatus=$universal utrn=${resp.utrn.getOrElse("-")}")
    for {
      _       <- persistUpdate(ctx.sub, baseUpdate(ctx.fullReturn).copy(
                   submittableStatus = Some(universal.toString),
                   IRMarkRecieved    = resp.receivedIrMark,
                   utrn              = resp.utrn,
                   acceptedDate      = Some(nowIso)
                 ))
      _       <- updateGovTalkStatistics(ctx.sub, ctx.polls, "0", endpoint)
      _       <- setGovTalkProtocol(ctx.sub, "deleteRequest")
      deleted <- sendChrisDelete(ctx.sub, endpoint, ctx.correlationId)
      _       <- if (deleted) finaliseGovTalkStatus(ctx.sub)
                 else {
                   logger.warn(s"[$jobName] ChRIS delete unsuccessful, leaving GovTalk at deleteRequest storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef}")
                   Future.unit
                 }
      _        = if (resp.utrn.isEmpty)
                   logger.warn(s"[$jobName] UC 1.44 AF11: The UTRN is not present in the Submission Response storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef}")
      _       <- auditPoll(ctx, resp)
      _       <- sendConfirmationEmail(ctx, resp.utrn)
    } yield polled(ctx, universal.toString, universal.toString)
  }

  private def errorBranch(ctx: PollContext, resp: ChrisResponse.Errored, universal: UniversalStatus): Future[PollOutcome] = {
    val deptError = universal == UniversalStatus.DEPARTMENTAL_ERROR
    val endpoint  = resp.responseEndPoint.orElse(ctx.gatewayUrl)
    val first     = resp.errors.headOption

    logger.warn(s"[$jobName] poll ERROR storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef} universalStatus=$universal departmental=$deptError numbers=${resp.errors.flatMap(_.number).mkString(",")}")
    if (deptError)
      logger.warn(s"[$jobName] The return is not validated by the HMRC Backend due to Business Validation Rules (BVR) Errors storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef}")
    else
      logger.warn(s"[$jobName] The submission failed due to fatal errors from the Government Gateway storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef}")

    val govTalkForDept: Future[Unit] =
      if (deptError)
        for {
          _ <- updateGovTalkStatistics(ctx.sub, ctx.polls, "0", endpoint)
          _ <- setGovTalkProtocol(ctx.sub, "deleteRequest")
          _ <- sendChrisDelete(ctx.sub, endpoint, ctx.correlationId)
          _ <- setGovTalkProtocol(ctx.sub, "endState")
        } yield ()
      else Future.unit

    for {
      acc1        <- persistUpdate(ctx.sub, baseUpdate(ctx.fullReturn).copy(submittableStatus = Some(universal.toString)))
      _           <- govTalkForDept
      acc2        <- persistUpdate(ctx.sub, acc1.copy(
                       govTalkErrorCode    = first.flatMap(_.number),
                       govTalkErrorType    = first.map(_.errorType),
                       govTalkErrorMessage = first.flatMap(_.text)
                     ))
      finalStatus <- recoverableTail(ctx.sub, acc2, universal, resp.errors)
      _           <- createSubmissionErrorDetails(ctx.sub, resp.errors)
      _           <- auditPoll(ctx, resp)
    } yield polled(ctx, universal.toString, finalStatus)
  }

  private val RecoverableNumbers: Set[String] = Set("1000", "2005", "3000")

  private def isRecoverable(errors: Seq[GovTalkError]): Boolean =
    errors.exists(_.number.exists(RecoverableNumbers.contains))

  private def recoverableTail(sub: SubmissionForPolling, acc: SubmissionUpdate, universal: UniversalStatus, errors: Seq[GovTalkError]): Future[String] =
    if (universal == UniversalStatus.STARTED || isRecoverable(errors)) {
      logger.info(s"[$jobName] error is recoverable, resetting submission to STARTED storn=${sub.storn} ref=${sub.returnResourceRef}")
      persistUpdate(sub, acc.copy(
        submittableStatus     = Some(UniversalStatus.STARTED.toString),
        submissionRequestDate = None
      )).map(_ => UniversalStatus.STARTED.toString)
    } else Future.successful(universal.toString)

  private def auditPoll(ctx: PollContext, resp: ChrisResponse): Future[Unit] =
    audit.auditSubmission(ctx.sub.storn, ctx.sub.returnResourceRef, ctx.correlationId, ctx.fullReturn, resp)(HeaderCarrier()).recover { case e =>
      logger.warn(s"[$jobName] CIP audit failed (suppressed) storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef}: ${e.getMessage}")
    }

  private def sendConfirmationEmail(ctx: PollContext, utrn: Option[String]): Future[Unit] =
    emailService.submitEmailConfirmation(ctx.fullReturn, utrn.getOrElse(""), None)(HeaderCarrier()).recover { case e =>
      logger.warn(s"[$jobName] confirmation email failed (suppressed) storn=${ctx.sub.storn} ref=${ctx.sub.returnResourceRef}: ${e.getMessage}")
    }

  private def baseUpdate(fullReturn: FullReturn): SubmissionUpdate = {
    val existing = fullReturn.submission
    SubmissionUpdate(
      IRMarkRecieved        = existing.flatMap(_.irmarkReceived),
      utrn                  = existing.flatMap(_.UTRN),
      email                 = existing.flatMap(_.email),
      submissionRequestDate = existing.flatMap(_.submissionRequestDate),
      acceptedDate          = existing.flatMap(_.acceptedDate),
      submittableStatus     = existing.flatMap(_.submissionStatus),
      govTalkErrorCode      = existing.flatMap(_.govtalkErrorCode),
      govTalkErrorType      = existing.flatMap(_.govtalkErrorType),
      govTalkErrorMessage   = existing.flatMap(_.govtalkErrorMessage),
      IRMarkSent            = existing.flatMap(_.irmarkSent)
    )
  }

  private def persistUpdate(sub: SubmissionForPolling, acc: SubmissionUpdate): Future[SubmissionUpdate] =
    chrisService.updateSubmission(UpdateSubmissionRequest(sub.storn, sub.returnResourceRef, acc)).map { _ =>
      logger.info(s"[$jobName] submission updated status=${acc.submittableStatus.getOrElse("-")} storn=${sub.storn} ref=${sub.returnResourceRef}")
      acc
    }

  private def createSubmissionErrorDetails(sub: SubmissionForPolling, errors: Seq[GovTalkError]): Future[Unit] =
    errors.zipWithIndex.foldLeft(Future.unit) { case (acc, (err, index)) =>
      acc.flatMap { _ =>
        val req = CreateSubmissionErrorDetailRequest(
          storn                  = sub.storn,
          returnResourceRef      = sub.returnResourceRef,
          submissionErrorDetails = SubmissionErrorDetail(
            position     = index.toString,
            errorMessage = err.number.fold(err.text.getOrElse(""))(code => s"$code: ${err.text.getOrElse("")}")
          )
        )
        chrisService.createSubmissionErrorDetail(req).map(_ => ())
      }
    }

  private def setGovTalkProtocol(sub: SubmissionForPolling, protocolStatus: String): Future[Unit] =
    chrisService.updateGovTalkStatus(UpdateGovTalkStatusRequest(
      userIdentifier    = sub.storn,
      formResultId      = sub.returnResourceRef,
      endStateTimestamp = nowSqlTimestamp,
      protocolStatus    = protocolStatus
    )).map(_ => ())

  private def updateGovTalkStatistics(sub: SubmissionForPolling, polls: Int, pollInterval: String, gatewayUrl: Option[String]): Future[Unit] =
    chrisService.updateGovTalkStatistics(UpdateGovTalkStatisticsRequest(
      userIdentifier = sub.storn,
      formResultId   = sub.returnResourceRef,
      govTalkStatus  = GovTalkStatusStatistics(
        lastMessageTimestamp = nowSqlTimestamp,
        numberOfPolls        = polls.toString,
        pollInterval         = pollInterval,
        gatewayUrl           = gatewayUrl.getOrElse(chrisConnector.defaultPath)
      )
    )).map(_ => ())

  private def finaliseGovTalkStatus(sub: SubmissionForPolling): Future[Unit] =
    (for {
      _ <- setGovTalkProtocol(sub, "endState")
      _ <- chrisService.resetGovTalkStatus(buildResetRequest(sub)).map(_ => ())
    } yield {
      logger.info(s"[$jobName] GovTalk status finalised (endState + reset) storn=${sub.storn} ref=${sub.returnResourceRef}")
      ()
    }).recover { case e =>
      logger.warn(s"[$jobName] GovTalk finalise (endState/reset) failed storn=${sub.storn} ref=${sub.returnResourceRef}: ${e.getMessage}")
      ()
    }

  private def buildResetRequest(sub: SubmissionForPolling): ResetGovTalkStatusRequest = {
    val now = nowSqlTimestamp
    ResetGovTalkStatusRequest(
      userIdentifier = sub.storn,
      formResultId   = sub.returnResourceRef,
      correlationId  = "empty",
      govTalkStatus  = GovTalkStatusReset(
        formLock             = "N",
        createTimestamp      = now,
        endStateTimestamp    = None,
        lastMessageTimestamp = now,
        numberOfPolls        = "0",
        pollInterval         = "0",
        protocolStatusOld    = "endState",
        protocolStatusNew    = "initial",
        gatewayUrl           = chrisConnector.defaultPath
      )
    )
  }

  private def sendChrisDelete(sub: SubmissionForPolling, endpoint: Option[String], correlationId: String): Future[Boolean] =
    chrisConnector.delete(endpoint, correlationId)(HeaderCarrier()).map {
      case ChrisDeleteResponse.Deleted(_, _)  => true
      case ChrisDeleteResponse.NotFound(_, _) => true
      case ChrisDeleteResponse.Errored(errors, _, _) =>
        logger.warn(s"[$jobName] ChRIS delete returned errors storn=${sub.storn} ref=${sub.returnResourceRef}: ${errors.mkString("; ")}")
        false
      case ChrisDeleteResponse.TransportError(msg, _) =>
        logger.warn(s"[$jobName] ChRIS delete transport error storn=${sub.storn} ref=${sub.returnResourceRef}: $msg")
        false
    }.recover { case e =>
      logger.warn(s"[$jobName] ChRIS delete failed storn=${sub.storn} ref=${sub.returnResourceRef}: ${e.getMessage}")
      false
    }

  private def notPolled(sub: SubmissionForPolling): PollOutcome =
    PollOutcome(sub, polled = false, pollResult = "-", newReturnStatus = sub.submissionStatus, correlationId = "(not polled)")

  private def polled(ctx: PollContext, pollResult: String, newReturnStatus: String): PollOutcome =
    PollOutcome(ctx.sub, polled = true, pollResult = pollResult, newReturnStatus = newReturnStatus, correlationId = ctx.correlationId)

  private def nowSqlTimestamp: String =
    ZonedDateTime.now(ZoneOffset.UTC)
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
}
