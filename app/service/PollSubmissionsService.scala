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
import service.filing.{ChrisService, GovTalkProtocol}
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
  override val chrisConnector: ChrisConnector,
  override val chrisService: ChrisService,
  filingConnector: FilingFormpProxyConnector,
  audit: SubmissionAuditService,
  emailService: EmailService,
  servicesConfig: ServicesConfig,
  lockRepositoryProvider: MongoLockRepository,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends ScheduledService[Either[ScheduleStatus.JobFailed, List[String]]]
    with GovTalkProtocol
    with Logging {

  protected val logPrefix: String = "PollSubmissionsJob"

  protected def logRef(storn: String, returnId: String, correlationId: String): String =
    s"storn=$storn ref=$returnId"

  protected def chrisHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier = HeaderCarrier()

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

  private def logRef(sub: SubmissionForPolling): String =
    s"storn=${sub.storn} ref=${sub.returnResourceRef}"

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
        logger.warn(s"[$jobName] no correlation id on GovTalk status, skipping ${logRef(sub)}")
        None
      case Some(_) if !pollAllowed(row) =>
        logger.info(s"[$jobName] poll not allowed yet ${logRef(sub)} ${rowSummary(row)}")
        None
      case some => some
    }

  private def rowSummary(row: SelectGovTalkStatusResponse): String =
    s"protocolStatus=${row.protocolStatus.getOrElse("-")} lastMessage=${row.lastMessageTimestamp.getOrElse("-")} pollInterval=${row.pollInterval.getOrElse("-")}"

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
      .updateGovTalkStatusLock(buildLockRequest(sub.storn, sub.returnResourceRef, formLockOld = "N", formLockNew = "Y", row.pollInterval.getOrElse("0"), gatewayUrlOf(row)))
      .map(_ => true)
      .recover { case e =>
        logger.info(s"[$jobName] GovTalk status already locked, skipping ${logRef(sub)}: ${e.getMessage}")
        false
      }

  private def releaseGovTalkLock(sub: SubmissionForPolling): Future[Unit] =
    chrisService
      .selectGovTalkStatus(SelectGovTalkStatusRequest(sub.storn, sub.returnResourceRef))
      .flatMap { fresh =>
        if (fresh.formLock.map(_.trim).contains("Y"))
          chrisService.updateGovTalkStatusLock(buildLockRequest(sub.storn, sub.returnResourceRef, formLockOld = "Y", formLockNew = "N", fresh.pollInterval.getOrElse("0"), gatewayUrlOf(fresh))).map { _ =>
            logger.info(s"[$jobName] GovTalk lock released ${logRef(sub)}")
            ()
          }
        else {
          logger.info(s"[$jobName] GovTalk lock already released ${logRef(sub)}")
          Future.unit
        }
      }
      .recover { case e =>
        logger.error(s"[$jobName] GovTalk lock release failed ${logRef(sub)}: ${e.getMessage}")
        ()
      }

  private def gatewayUrlOf(row: SelectGovTalkStatusResponse): Option[String] = {
    val chrisHost = servicesConfig.baseUrl("chris").stripSuffix("/")
    row.gatewayUrl.map(_.trim.stripSuffix("/")).filter(_.nonEmpty) match {
      case Some(url) if url == chrisHost =>
        logger.info(s"[$jobName] stored gateway url is the bare ChRIS host, falling back to the full submission url")
        None
      case stored => stored
    }
  }

  private case class PollContext(
    sub: SubmissionForPolling,
    correlationId: String,
    fullReturn: FullReturn,
    polls: Int,
    rowPollInterval: String,
    gatewayUrl: Option[String]
  ) {
    val storn: String    = sub.storn
    val returnId: String = sub.returnResourceRef
  }

  private def nextPollCount(row: SelectGovTalkStatusResponse): Int =
    row.numberOfPolls.flatMap(s => Try(s.trim.toInt).toOption).getOrElse(0) + 1

  private def pollLocked(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse, correlationId: String): Future[PollOutcome] =
    for {
      fullReturn <- filingConnector.getFullReturn(GetReturnByRefRequest(sub.returnResourceRef, sub.storn))
      ctx         = PollContext(
                      sub             = sub,
                      correlationId   = correlationId,
                      fullReturn      = fullReturn,
                      polls           = nextPollCount(row),
                      rowPollInterval = row.pollInterval.getOrElse("0"),
                      gatewayUrl      = gatewayUrlOf(row)
                    )
      _          <- updateGovTalkStatistics(ctx.storn, ctx.returnId, ctx.gatewayUrl, ctx.correlationId, ctx.rowPollInterval, ctx.polls)
      resp       <- chrisConnector.poll(ctx.gatewayUrl, ctx.correlationId)(HeaderCarrier())
      outcome    <- handlePollResponse(ctx, resp)
    } yield outcome

  private def handlePollResponse(ctx: PollContext, resp: ChrisResponse): Future[PollOutcome] =
    resp match {
      case t: ChrisResponse.TransportError =>
        logger.warn(s"[$jobName] no response from ChRIS (${t.message}), will poll again next cycle ${logRef(ctx.sub)}")
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

  private def acknowledgementBranch(ctx: PollContext, resp: ChrisResponse.Acknowledged): Future[PollOutcome] = {
    val nextInterval = resp.pollIntervalSeconds.map(_.toString).getOrElse(ctx.rowPollInterval)
    val nextEndpoint = resp.responseEndPoint.orElse(ctx.gatewayUrl)

    for {
      _ <- persistUpdate(ctx.storn, ctx.returnId, baseUpdate(ctx.fullReturn).copy(submittableStatus = Some(UniversalStatus.ACCEPTED.toString)), ctx.correlationId)
      _ <- setGovTalkProtocol(ctx.storn, ctx.returnId, "dataPoll", ctx.correlationId)
      _ <- updateGovTalkStatistics(ctx.storn, ctx.returnId, nextEndpoint, ctx.correlationId, nextInterval, ctx.polls)
    } yield polled(ctx, UniversalStatus.ACCEPTED.toString, UniversalStatus.ACCEPTED.toString)
  }

  private def successBranch(ctx: PollContext, resp: ChrisResponse.Completed, universal: UniversalStatus): Future[PollOutcome] = {
    val endpoint   = resp.responseEndPoint.orElse(ctx.gatewayUrl)

    logger.info(s"[$jobName] poll SUCCESS ${logRef(ctx.sub)} universalStatus=$universal utrn=${resp.utrn.getOrElse("-")}")
    for {
      _       <- persistUpdate(ctx.storn, ctx.returnId, baseUpdate(ctx.fullReturn).copy(
                   submittableStatus = Some(universal.toString),
                   IRMarkRecieved    = resp.receivedIrMark,
                   utrn              = resp.utrn,
                   acceptedDate      = Some(nowIso)
                 ), ctx.correlationId)
      _       <- updateGovTalkStatistics(ctx.storn, ctx.returnId, endpoint, ctx.correlationId, "0", ctx.polls)
      _       <- setGovTalkProtocol(ctx.storn, ctx.returnId, "deleteRequest", ctx.correlationId)
      deleted <- sendChrisDelete(ctx.storn, ctx.returnId, endpoint, ctx.correlationId)
      _       <- if (deleted) finaliseGovTalkStatus(ctx.storn, ctx.returnId, ctx.correlationId)
                 else {
                   logger.warn(s"[$jobName] ChRIS delete unsuccessful, leaving GovTalk at deleteRequest ${logRef(ctx.sub)}")
                   Future.unit
                 }
      _        = if (resp.utrn.isEmpty)
                   logger.warn(s"[$jobName] UC 1.44 AF11: The UTRN is not present in the Submission Response ${logRef(ctx.sub)}")
      _       <- auditPoll(ctx, resp)
      _       <- sendConfirmationEmail(ctx, resp.utrn)
    } yield polled(ctx, universal.toString, universal.toString)
  }

  private def errorBranch(ctx: PollContext, resp: ChrisResponse.Errored, universal: UniversalStatus): Future[PollOutcome] = {
    val deptError   = universal == UniversalStatus.DEPARTMENTAL_ERROR
    val errorStatus = if (deptError) UniversalStatus.DEPARTMENTAL_ERROR else UniversalStatus.FATAL_ERROR
    val endpoint    = resp.responseEndPoint.orElse(ctx.gatewayUrl)
    val firstError  = resp.errors.headOption

    logger.warn(s"[$jobName] poll ERROR ${logRef(ctx.sub)} universalStatus=$errorStatus departmental=$deptError numbers=${resp.errors.flatMap(_.number).mkString(",")}")
    if (deptError)
      logger.warn(s"[$jobName] The return is not validated by the HMRC Backend due to Business Validation Rules (BVR) Errors ${logRef(ctx.sub)}")
    else
      logger.warn(s"[$jobName] The submission failed due to fatal errors from the Government Gateway ${logRef(ctx.sub)}")

    for {
      acc1        <- persistUpdate(ctx.storn, ctx.returnId, baseUpdate(ctx.fullReturn).copy(submittableStatus = Some(errorStatus.toString)), ctx.correlationId)
      _           <- if (deptError) closeDepartmentalGovTalk(ctx, endpoint) else Future.unit
      acc2        <- persistUpdate(ctx.storn, ctx.returnId, acc1.copy(
                       govTalkErrorCode    = firstError.flatMap(_.number),
                       govTalkErrorType    = firstError.map(_.errorType),
                       govTalkErrorMessage = firstError.flatMap(_.text)
                     ), ctx.correlationId)
      finalStatus <- recoverableTail(ctx.sub, acc2, errorStatus, resp.errors, ctx.correlationId)
      _           <- createSubmissionErrorDetails(ctx.storn, ctx.returnId, resp.errors, ctx.correlationId)
      _           <- auditPoll(ctx, resp)
    } yield polled(ctx, errorStatus.toString, finalStatus)
  }

  private def closeDepartmentalGovTalk(ctx: PollContext, endpoint: Option[String]): Future[Unit] =
    for {
      _ <- updateGovTalkStatistics(ctx.storn, ctx.returnId, endpoint, ctx.correlationId, "0", ctx.polls)
      _ <- setGovTalkProtocol(ctx.storn, ctx.returnId, "deleteRequest", ctx.correlationId)
      _ <- sendChrisDelete(ctx.storn, ctx.returnId, endpoint, ctx.correlationId)
      _ <- setGovTalkProtocol(ctx.storn, ctx.returnId, "endState", ctx.correlationId)
    } yield ()

  private def recoverableTail(sub: SubmissionForPolling, acc: SubmissionUpdate, errorStatus: UniversalStatus, errors: Seq[GovTalkError], correlationId: String): Future[String] =
    if (errorStatus != UniversalStatus.DEPARTMENTAL_ERROR && isRecoverable(errors)) {
      logger.info(s"[$jobName] error is recoverable, resetting submission to STARTED ${logRef(sub)}")
      persistUpdate(sub.storn, sub.returnResourceRef, acc.copy(
        submittableStatus     = Some(UniversalStatus.STARTED.toString),
        submissionRequestDate = None
      ), correlationId).map(_ => UniversalStatus.STARTED.toString)
    } else Future.successful(errorStatus.toString)

  private def auditPoll(ctx: PollContext, resp: ChrisResponse): Future[Unit] =
    audit.auditSubmission(ctx.storn, ctx.returnId, ctx.correlationId, ctx.fullReturn, resp)(HeaderCarrier()).recover { case e =>
      logger.warn(s"[$jobName] CIP audit failed ${logRef(ctx.sub)}: ${e.getMessage}")
    }

  private def sendConfirmationEmail(ctx: PollContext, utrn: Option[String]): Future[Unit] =
    emailService.submitEmailConfirmation(ctx.fullReturn, utrn.getOrElse(""), None)(HeaderCarrier()).recover { case e =>
      logger.warn(s"[$jobName] confirmation email failed ${logRef(ctx.sub)}: ${e.getMessage}")
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

  private def notPolled(sub: SubmissionForPolling): PollOutcome =
    PollOutcome(sub, polled = false, pollResult = "-", newReturnStatus = sub.submissionStatus, correlationId = "(not polled)")

  private def polled(ctx: PollContext, pollResult: String, newReturnStatus: String): PollOutcome =
    PollOutcome(ctx.sub, polled = true, pollResult = pollResult, newReturnStatus = newReturnStatus, correlationId = ctx.correlationId)
}
