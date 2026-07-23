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
import service.submission.{GovTalkContext, GovTalkOutcomeHandler}
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
  govTalk: GovTalkOutcomeHandler,
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
          withGovTalkLock(sub, row, correlationId) {
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

  private def ctxOf(sub: SubmissionForPolling, correlationId: String): GovTalkContext =
    GovTalkContext(sub.storn, sub.returnResourceRef, correlationId)

  private def withGovTalkLock(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse, correlationId: String)(body: => Future[PollOutcome]): Future[Option[PollOutcome]] =
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
            govTalk.releaseLock(ctxOf(sub, correlationId)).transformWith(_ => Future.fromTry(result)).map(Some(_))
          }
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
      _          <- govTalk.updateStatistics(ctxOf(sub, correlationId), gatewayUrl, rowInterval(row), polls.toString)
      resp       <- chrisConnector.poll(gatewayUrl, correlationId)(HeaderCarrier())
      outcome    <- handlePollResponse(sub, row, resp, fullReturn, correlationId, polls, gatewayUrl)
    } yield outcome
  }

  private def rowInterval(row: SelectGovTalkStatusResponse): Option[Int] =
    row.pollInterval.flatMap(s => Try(s.trim.toInt).toOption)

  private def handlePollResponse(sub: SubmissionForPolling,
                                 row: SelectGovTalkStatusResponse,
                                 resp: ChrisResponse,
                                 fullReturn: FullReturn,
                                 correlationId: String,
                                 polls: Int,
                                 gatewayUrl: Option[String]): Future[PollOutcome] = {
    val ctx = ctxOf(sub, correlationId)
    resp match {
      case t: ChrisResponse.TransportError =>
        logger.warn(s"[$jobName] no response from ChRIS (${t.message}); will poll again next cycle storn=${sub.storn} ref=${sub.returnResourceRef}")
        Future.successful(PollOutcome(sub, polled = true, pollResult = "-", newReturnStatus = sub.submissionStatus, correlationId = "(not polled)"))

      case a: ChrisResponse.Acknowledged =>
        for {
          _ <- govTalk.setProtocol(ctx, "dataPoll")
          _ <- govTalk.updateStatistics(ctx, a.responseEndPoint.orElse(gatewayUrl), a.pollIntervalSeconds.orElse(rowInterval(row)), polls.toString)
        } yield PollOutcome(sub, polled = true, pollResult = UniversalStatus.ACCEPTED.toString, newReturnStatus = UniversalStatus.ACCEPTED.toString, correlationId = correlationId)

      case c: ChrisResponse.Completed =>
        val universal = UniversalStatus.fromChrisResponse(c, fullReturn.submission.flatMap(_.irmarkSent))
        logger.info(s"[$jobName] poll SUCCESS storn=${sub.storn} ref=${sub.returnResourceRef} universalStatus=$universal utrn=${c.utrn.getOrElse("-")}")
        govTalk
          .handleCompleted(ctx, fullReturn, c, universal, baseUpdate(fullReturn), polls.toString, None, HeaderCarrier())
          .map(_ => PollOutcome(sub, polled = true, pollResult = universal.toString, newReturnStatus = universal.toString, correlationId = correlationId))

      case e: ChrisResponse.Errored =>
        val universal = UniversalStatus.fromChrisResponse(e, fullReturn.submission.flatMap(_.irmarkSent))
        logger.warn(s"[$jobName] poll ERROR storn=${sub.storn} ref=${sub.returnResourceRef} universalStatus=$universal numbers=${e.errors.flatMap(_.number).mkString(",")}")
        govTalk
          .handleErrored(ctx, fullReturn, e.errors, e.responseEndPoint.orElse(gatewayUrl), universal, baseUpdate(fullReturn), polls.toString, HeaderCarrier())
          .map(acc => PollOutcome(sub, polled = true, pollResult = universal.toString, newReturnStatus = acc.submittableStatus.getOrElse(universal.toString), correlationId = correlationId))
    }
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
}
