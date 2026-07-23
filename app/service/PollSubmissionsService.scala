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

import java.time.format.DateTimeFormatter
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
      pollAll().map(Right(_)).recover { case e: Exception =>
        logger.error(s"[$jobName] failed while polling submissions", e)
        Left(FailedToPollSubmissions(e))
      }
    }

  private def pollAll(): Future[List[String]] = {
    logger.info(s"[$jobName] starting batch poll")
    for {
      selected <- formpConnector.getSubmissionsForPolling().map(_.submissions)
      _         = logger.info(s"[$jobName] selected for polling: ${refList(selected)}")
      outcomes <- pollSequentially(selected)
    } yield {
      val polled = outcomes.filter(_.polled)
      logger.info(s"[$jobName] polled: ${refList(polled.map(_.submission))}")
      logReport(outcomes)
      polled.map(_.submission.returnResourceRef)
    }
  }

  private def refList(submissions: List[SubmissionForPolling]): String =
    if (submissions.isEmpty) "none" else submissions.map(s => s"${s.storn}/${s.returnResourceRef}").mkString(", ")

  private def pollSequentially(selected: List[SubmissionForPolling]): Future[List[PollOutcome]] =
    selected.foldLeft(Future.successful(List.empty[PollOutcome])) { (acc, submission) =>
      acc.flatMap(outcomes => pollOne(submission).map(outcomes :+ _))
    }

  private def pollOne(sub: SubmissionForPolling): Future[PollOutcome] = {
    val notPolled = PollOutcome(sub, polled = false, pollResult = "-", newReturnStatus = sub.submissionStatus, correlationId = "(not polled)")
    chrisService.selectGovTalkStatus(SelectGovTalkStatusRequest(sub.storn, sub.returnResourceRef)).flatMap { row =>
      row.correlationId.map(_.trim).filter(c => c.nonEmpty && !c.equalsIgnoreCase("empty")) match {
        case None =>
          logger.warn(s"[$jobName] no correlation id on GovTalk status, skipping storn=${sub.storn} ref=${sub.returnResourceRef}")
          Future.successful(notPolled)
        case Some(_) if !pollAllowed(row) =>
          logger.info(s"[$jobName] poll not allowed yet storn=${sub.storn} ref=${sub.returnResourceRef} protocolStatus=${row.protocolStatus.getOrElse("-")} lastMessage=${row.lastMessageTimestamp.getOrElse("-")} pollInterval=${row.pollInterval.getOrElse("-")}")
          Future.successful(notPolled)
        case Some(correlationId) =>
          withGovTalkLock(sub, row) {
            pollLocked(sub, row, correlationId)
          }.map(_.getOrElse(notPolled))
      }
    }.recover { case e =>
      logger.warn(s"[$jobName] failed to poll submission ${sub.returnResourceRef}: ${e.getMessage}")
      notPolled
    }
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

  private val TimestampFormats: Seq[String] =
    Seq("yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")

  private def parseTimestamp(value: String): Option[LocalDateTime] =
    TimestampFormats.view.flatMap(f => Try(LocalDateTime.parse(value, DateTimeFormatter.ofPattern(f))).toOption).headOption

  private def withGovTalkLock(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse)(body: => Future[PollOutcome]): Future[Option[PollOutcome]] =
    chrisService.updateGovTalkStatusLock(lockRequest(sub, row, formLockOld = "N", formLockNew = "Y"))
      .map(_ => true)
      .recover { case e =>
        logger.info(s"[$jobName] GovTalk status already locked, skipping storn=${sub.storn} ref=${sub.returnResourceRef}: ${e.getMessage}")
        false
      }
      .flatMap {
        case false => Future.successful(None)
        case true  =>
          body.transformWith { result =>
            releaseGovTalkLock(sub).transformWith(_ => Future.fromTry(result)).map(Some(_))
          }
      }

  private def releaseGovTalkLock(sub: SubmissionForPolling): Future[Unit] =
    chrisService
      .selectGovTalkStatus(SelectGovTalkStatusRequest(sub.storn, sub.returnResourceRef))
      .flatMap { fresh =>
        if (fresh.formLock.map(_.trim).contains("Y"))
          chrisService.updateGovTalkStatusLock(lockRequest(sub, fresh, formLockOld = "Y", formLockNew = "N")).map { _ =>
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

  private def lockRequest(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse, formLockOld: String, formLockNew: String): UpdateGovTalkStatusLockRequest =
    UpdateGovTalkStatusLockRequest(
      userIdentifier = sub.storn,
      formResultId   = sub.returnResourceRef,
      govTalkStatus  = GovTalkStatusLock(
        formLockOld  = formLockOld,
        formLockNew  = formLockNew,
        pollInterval = row.pollInterval.getOrElse("0"),
        gatewayUrl   = gatewayUrlOf(row).getOrElse(servicesConfig.baseUrl("chris"))
      )
    )

  private def gatewayUrlOf(row: SelectGovTalkStatusResponse): Option[String] =
    row.gatewayUrl.map(_.trim).filter(_.nonEmpty)

  private def pollLocked(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse, correlationId: String): Future[PollOutcome] = {
    val gatewayUrl = gatewayUrlOf(row)
    val polls      = row.numberOfPolls.flatMap(s => Try(s.trim.toInt).toOption).getOrElse(0) + 1
    for {
      fullReturn <- filingConnector.getFullReturn(GetReturnByRefRequest(sub.returnResourceRef, sub.storn))
      _          <- updateStatistics(sub, polls, row.pollInterval.getOrElse("0"), gatewayUrl)
      resp       <- chrisConnector.poll(gatewayUrl, correlationId)(HeaderCarrier())
      outcome    <- handlePollResponse(sub, row, resp, fullReturn, correlationId, polls, gatewayUrl)
    } yield outcome
  }

  private def handlePollResponse(sub: SubmissionForPolling,
                                 row: SelectGovTalkStatusResponse,
                                 resp: ChrisResponse,
                                 fullReturn: FullReturn,
                                 correlationId: String,
                                 polls: Int,
                                 gatewayUrl: Option[String]): Future[PollOutcome] =
    resp match {
      case t: ChrisResponse.TransportError =>
        logger.warn(s"[$jobName] no response from ChRIS (${t.message}); will poll again next cycle storn=${sub.storn} ref=${sub.returnResourceRef}")
        Future.successful(PollOutcome(sub, polled = true, pollResult = "-", newReturnStatus = sub.submissionStatus, correlationId = "(not polled)"))

      case a: ChrisResponse.Acknowledged =>
        for {
          _ <- setProtocol(sub, "dataPoll")
          _ <- updateStatistics(sub, polls, a.pollIntervalSeconds.map(_.toString).getOrElse(row.pollInterval.getOrElse("0")), a.responseEndPoint.orElse(gatewayUrl))
        } yield PollOutcome(sub, polled = true, pollResult = UniversalStatus.ACCEPTED.toString, newReturnStatus = UniversalStatus.ACCEPTED.toString, correlationId = correlationId)

      case c: ChrisResponse.Completed =>
        val universal = UniversalStatus.fromChrisResponse(c, fullReturn.submission.flatMap(_.irmarkSent))
        successOutcome(sub, c, universal, fullReturn, correlationId, polls, gatewayUrl)

      case e: ChrisResponse.Errored =>
        val universal = UniversalStatus.fromChrisResponse(e, fullReturn.submission.flatMap(_.irmarkSent))
        errorOutcome(sub, e, universal, fullReturn, correlationId, polls, gatewayUrl)
    }

  private def successOutcome(sub: SubmissionForPolling,
                             resp: ChrisResponse.Completed,
                             universal: UniversalStatus,
                             fullReturn: FullReturn,
                             correlationId: String,
                             polls: Int,
                             gatewayUrl: Option[String]): Future[PollOutcome] = {
    logger.info(s"[$jobName] poll SUCCESS storn=${sub.storn} ref=${sub.returnResourceRef} universalStatus=$universal utrn=${resp.utrn.getOrElse("-")}")
    for {
      acc1    <- persistUpdate(sub, baseUpdate(fullReturn).copy(submittableStatus = Some(universal.toString)))
      _       <- updateStatistics(sub, polls, "0", resp.responseEndPoint.orElse(gatewayUrl))
      _       <- setProtocol(sub, "deleteRequest")
      deleted <- sendChrisDelete(sub, resp.responseEndPoint.orElse(gatewayUrl), correlationId)
      _       <- if (deleted) finaliseGovTalkStatus(sub, correlationId)
                 else {
                   logger.warn(s"[$jobName] ChRIS delete unsuccessful; leaving GovTalk at deleteRequest storn=${sub.storn} ref=${sub.returnResourceRef}")
                   Future.unit
                 }
      _       <- audit.auditSubmission(sub.storn, sub.returnResourceRef, correlationId, fullReturn, resp)(HeaderCarrier())
      _       <- persistUpdate(sub, acc1.copy(
                   IRMarkRecieved = resp.receivedIrMark,
                   utrn           = resp.utrn,
                   acceptedDate   = Some(nowIso)
                 ))
      _       <- emailService.submitEmailConfirmation(fullReturn, resp.utrn.getOrElse(""), None)(HeaderCarrier())
    } yield PollOutcome(sub, polled = true, pollResult = universal.toString, newReturnStatus = universal.toString, correlationId = correlationId)
  }

  private def errorOutcome(sub: SubmissionForPolling,
                           resp: ChrisResponse.Errored,
                           universal: UniversalStatus,
                           fullReturn: FullReturn,
                           correlationId: String,
                           polls: Int,
                           gatewayUrl: Option[String]): Future[PollOutcome] = {
    val deptError = universal == UniversalStatus.DEPARTMENTAL_ERROR
    val first     = resp.errors.headOption

    logger.warn(s"[$jobName] poll ERROR storn=${sub.storn} ref=${sub.returnResourceRef} universalStatus=$universal departmental=$deptError numbers=${resp.errors.flatMap(_.number).mkString(",")}")

    val govTalkForDept: Future[Unit] =
      if (deptError)
        for {
          _ <- updateStatistics(sub, polls, "0", resp.responseEndPoint.orElse(gatewayUrl))
          _ <- setProtocol(sub, "deleteRequest")
          _ <- sendChrisDelete(sub, resp.responseEndPoint.orElse(gatewayUrl), correlationId)
          _ <- setProtocol(sub, "endState")
        } yield ()
      else Future.unit

    for {
      acc1  <- persistUpdate(sub, baseUpdate(fullReturn).copy(submittableStatus = Some(universal.toString)))
      _     <- govTalkForDept
      acc2  <- persistUpdate(sub, acc1.copy(
                 govTalkErrorCode    = first.flatMap(_.number),
                 govTalkErrorType    = first.map(_.errorType),
                 govTalkErrorMessage = first.flatMap(_.text)
               ))
      finalStatus <- recoverableTail(sub, acc2, universal)
      _     <- createSubmissionErrorDetails(sub, resp.errors)
      _     <- audit.auditSubmission(sub.storn, sub.returnResourceRef, correlationId, fullReturn, resp)(HeaderCarrier())
    } yield PollOutcome(sub, polled = true, pollResult = universal.toString, newReturnStatus = finalStatus, correlationId = correlationId)
  }

  private def recoverableTail(sub: SubmissionForPolling, acc: SubmissionUpdate, universal: UniversalStatus): Future[String] =
    if (universal == UniversalStatus.STARTED) {
      logger.info(s"[$jobName] error is recoverable, resetting submission to STARTED storn=${sub.storn} ref=${sub.returnResourceRef}")
      persistUpdate(sub, acc.copy(
        submittableStatus     = Some(UniversalStatus.STARTED.toString),
        submissionRequestDate = None
      )).map(_ => UniversalStatus.STARTED.toString)
    } else Future.successful(universal.toString)

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
    errors.foldLeft(Future.unit) { (acc, err) =>
      acc.flatMap { _ =>
        val req = CreateSubmissionErrorDetailRequest(
          storn                  = sub.storn,
          returnResourceRef      = sub.returnResourceRef,
          submissionErrorDetails = SubmissionErrorDetail(
            position     = err.location.getOrElse(""),
            errorMessage = err.text.getOrElse("")
          )
        )
        chrisService.createSubmissionErrorDetail(req).map(_ => ())
      }
    }

  private def setProtocol(sub: SubmissionForPolling, protocolStatus: String): Future[Unit] =
    chrisService.updateGovTalkStatus(UpdateGovTalkStatusRequest(
      userIdentifier    = sub.storn,
      formResultId      = sub.returnResourceRef,
      endStateTimestamp = nowSqlTimestamp,
      protocolStatus    = protocolStatus
    )).map(_ => ())

  private def updateStatistics(sub: SubmissionForPolling, polls: Int, pollInterval: String, gatewayUrl: Option[String]): Future[Unit] =
    chrisService.updateGovTalkStatistics(UpdateGovTalkStatisticsRequest(
      userIdentifier = sub.storn,
      formResultId   = sub.returnResourceRef,
      govTalkStatus  = GovTalkStatusStatistics(
        lastMessageTimestamp = nowSqlTimestamp,
        numberOfPolls        = polls.toString,
        pollInterval         = pollInterval,
        gatewayUrl           = gatewayUrl.getOrElse(servicesConfig.baseUrl("chris"))
      )
    )).map(_ => ())

  private def finaliseGovTalkStatus(sub: SubmissionForPolling, correlationId: String): Future[Unit] =
    (for {
      _ <- setProtocol(sub, "endState")
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
        gatewayUrl           = servicesConfig.baseUrl("chris")
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

  private case class PollOutcome(submission: SubmissionForPolling, polled: Boolean, pollResult: String, newReturnStatus: String, correlationId: String)

  private val ReportDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss")

  private def logReport(outcomes: List[PollOutcome]): Unit = {
    val separator = "=" * 170
    val underline = "    " + "-" * 158
    val header    = "    STORN          SUBMISSION_ID        RETURN_RESOURCE_REF      POLL_RESULT    NEW_RETURN_STATUS        CORRELATION ID"
    val date      = LocalDateTime.now(clock).format(ReportDateFormatter)

    val rows = outcomes.map { o =>
      "    " +
        pad(o.submission.storn, 15) +
        pad(o.submission.submissionId, 20) +
        pad(o.submission.returnResourceRef, 24) +
        pad(o.pollResult, 15) +
        pad(o.newReturnStatus, 23) +
        pad(o.correlationId, 25)
    }

    val report =
      s"""
         |$separator
         |BATCH POLLING RESULTS FOR $date
         |
         |$header
         |$underline
         |${rows.mkString("\n")}
         |$underline
         |$separator
         |""".stripMargin

    logger.info(report)
  }

  private def pad(value: String, width: Int): String =
    if (value.length > width) value.take(width - 3) + "..."
    else value.padTo(width, ' ')

  private def nowIso: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  private val SqlTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  private def nowSqlTimestamp: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(SqlTimestampFormatter)
}
