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

import java.time.format.DateTimeFormatter
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
      .selectGovTalkStatus(SelectGovTalkStatusRequest(sub.storn, sub.submissionId))
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
      case Some(_) if !(row.protocolStatus.map(_.trim).contains("dataPoll") && intervalElapsed(row)) =>
        logger.info(s"[$jobName] poll not allowed yet ${logRef(sub)} " +
          s"protocolStatus=${row.protocolStatus.getOrElse("-")} " +
          s"lastMessage=${row.lastMessageTimestamp.getOrElse("-")} " +
          s"pollInterval=${row.pollInterval.getOrElse("-")}")
        None
      case some => some
    }

  private def intervalElapsed(row: SelectGovTalkStatusResponse): Boolean = {
    val interval = pollIntervalOf(row).toLong
    row.lastMessageTimestamp.map(_.trim).filter(_.nonEmpty).flatMap(parseTimestamp) match {
      case Some(lastMessage) =>
        !LocalDateTime.now(clock.withZone(ZoneOffset.UTC)).isBefore(lastMessage.plusSeconds(interval))
      case None              => true
    }
  }

  private val TimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  private def parseTimestamp(value: String): Option[LocalDateTime] =
    Try(LocalDateTime.parse(value.replace('T', ' '), TimestampFormatter)).toOption match {
      case None =>
        logger.warn(s"[$jobName] unparseable GovTalk last message timestamp '$value', " +
          "treating the poll interval as elapsed")
        None
      case parsed => parsed
    }

  private def pollWithLock(sub: SubmissionForPolling,
                           row: SelectGovTalkStatusResponse,
                           correlationId: String): Future[PollOutcome] =
    acquireGovTalkLock(sub, row).flatMap {
      case false => Future.successful(notPolled(sub))
      case true  =>
        pollLocked(sub, row, correlationId).transformWith { result =>
          releaseGovTalkLock(sub, row).transformWith(_ => Future.fromTry(result))
        }
    }

  private def acquireGovTalkLock(sub: SubmissionForPolling, row: SelectGovTalkStatusResponse): Future[Boolean] =
    chrisService
      .updateGovTalkStatusLock(buildLockRequest(
        sub.storn, sub.submissionId, formLockOld = "N", formLockNew = "Y", pollIntervalOf(row), row.gatewayUrl
      ))
      .map(_ => true)
      .recover { case e =>
        logger.warn(s"[$jobName] could not acquire the GovTalk row lock, skipping ${logRef(sub)}: ${e.getMessage}")
        false
      }

  private def releaseGovTalkLock(sub: SubmissionForPolling, prePoll: SelectGovTalkStatusResponse): Future[Unit] =
    chrisService
      .selectGovTalkStatus(SelectGovTalkStatusRequest(sub.storn, sub.submissionId))
      .map(Option(_))
      .recover { case e =>
        logger.warn(s"[$jobName] could not re-read the GovTalk status before releasing the lock, " +
          s"falling back to the values read before the poll ${logRef(sub)}: ${e.getMessage}")
        None
      }
      .flatMap {
        case Some(fresh) if !fresh.formLock.map(_.trim).contains("Y") =>
          logger.info(s"[$jobName] GovTalk lock already released ${logRef(sub)}")
          Future.unit
        case fresh =>
          val current = fresh.getOrElse(prePoll)
          chrisService.updateGovTalkStatusLock(buildLockRequest(
            sub.storn,
            sub.submissionId,
            formLockOld  = "Y",
            formLockNew  = "N",
            pollInterval = pollIntervalOf(current),
            gatewayUrl   = current.gatewayUrl
          )).map { _ =>
            logger.info(s"[$jobName] GovTalk lock released ${logRef(sub)}")
            ()
          }
      }
      .recover { case e =>
        logger.error(s"[$jobName] GovTalk lock release FAILED, the row is left locked and " +
          s"will be skipped until it is cleared ${logRef(sub)}: ${e.getMessage}")
        ()
      }

  private def pollIntervalOf(row: SelectGovTalkStatusResponse): String =
    row.pollInterval.flatMap(s => Try(s.trim.toLong).toOption).getOrElse(0L).toString

  private case class PollContext(
    sub: SubmissionForPolling,
    correlationId: String,
    fullReturn: FullReturn,
    polls: Int,
    rowPollInterval: String,
    gatewayUrl: Option[String]
  ) {
    val storn: String        = sub.storn
    val returnId: String     = sub.returnResourceRef
    val formResultId: String = sub.submissionId
  }

  private def pollLocked(sub: SubmissionForPolling,
                         row: SelectGovTalkStatusResponse,
                         correlationId: String): Future[PollOutcome] =
    for {
      fullReturn <- filingConnector.getFullReturn(GetReturnByRefRequest(sub.returnResourceRef, sub.storn))
      ctx         = PollContext(
                      sub             = sub,
                      correlationId   = correlationId,
                      fullReturn      = fullReturn,
                      polls           = row.numberOfPolls.flatMap(n => Try(n.trim.toInt).toOption).getOrElse(0) + 1,
                      rowPollInterval = pollIntervalOf(row),
                      gatewayUrl      = row.gatewayUrl
                    )
      _          <- updateGovTalkStatistics(ctx.storn, ctx.formResultId, ctx.gatewayUrl,
                      ctx.correlationId, ctx.rowPollInterval, ctx.polls)
      resp       <- sendChrisPoll(ctx.storn, ctx.returnId, ctx.gatewayUrl, ctx.correlationId)
      outcome    <- handlePollResponse(ctx, resp)
    } yield outcome

  private def handlePollResponse(ctx: PollContext, resp: ChrisResponse): Future[PollOutcome] =
    resp match {
      case t: ChrisResponse.TransportError =>
        logger.warn(s"[$jobName] no response from ChRIS (${t.message}), will poll again next cycle ${logRef(ctx.sub)}")
        Future.successful(PollOutcome(ctx.sub, polled = false,
          pollResult      = ctx.sub.submissionStatus,
          newReturnStatus = ctx.sub.submissionStatus,
          correlationId   = ctx.correlationId))

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
      _ <- persistUpdate(ctx.storn, ctx.returnId,
             baseUpdate(ctx.fullReturn).copy(submittableStatus = Some(UniversalStatus.ACCEPTED.toString)),
             ctx.correlationId)
      _ <- setGovTalkProtocol(ctx.storn, ctx.formResultId, "dataPoll", ctx.correlationId)
      _  = warnOnCorrelationIdMismatch(ctx.storn, ctx.returnId, ctx.correlationId, resp.correlationId)
      _ <- updateGovTalkStatistics(ctx.storn, ctx.formResultId, nextEndpoint,
             ctx.correlationId, nextInterval, ctx.polls)
    } yield polled(ctx, UniversalStatus.ACCEPTED.toString, UniversalStatus.ACCEPTED.toString)
  }

  private def successBranch(ctx: PollContext,
                            resp: ChrisResponse.Completed,
                            universal: UniversalStatus): Future[PollOutcome] = {
    val endpoint   = resp.responseEndPoint.orElse(ctx.gatewayUrl)

    logger.info(s"[$jobName] poll SUCCESS ${logRef(ctx.sub)} universalStatus=$universal " +
      s"utrn=${resp.utrn.getOrElse("-")}")
    for {
      _       <- completeSuccessfulSubmission(
                   ctx.storn,
                   ctx.returnId,
                   ctx.formResultId,
                   ctx.correlationId,
                   baseUpdate(ctx.fullReturn).copy(
                     submittableStatus = Some(universal.toString),
                     IRMarkRecieved    = resp.receivedIrMark,
                     utrn              = resp.utrn,
                     acceptedDate      = Some(nowIso)
                   ),
                   endpoint,
                   ctx.polls
                 )
      _        = if (resp.utrn.isEmpty)
                   logger.warn(s"[$jobName] UC 1.44 AF11: The UTRN is not present in the " +
                     s"Submission Response ${logRef(ctx.sub)}")
      _       <- emailService.submitEmailConfirmation(ctx.fullReturn, resp.utrn.getOrElse(""), None).recover { case e =>
                   logger.warn(s"[$jobName] confirmation email failed ${logRef(ctx.sub)}: ${e.getMessage}")
                 }
      _       <- audit.auditSubmission(ctx.storn, ctx.returnId, ctx.correlationId, ctx.fullReturn, resp)
    } yield polled(ctx, universal.toString, universal.toString)
  }

  private def errorBranch(ctx: PollContext,
                          resp: ChrisResponse.Errored,
                          universal: UniversalStatus): Future[PollOutcome] = {
    val deptError   = universal == UniversalStatus.DEPARTMENTAL_ERROR
    val errorStatus = if (deptError) UniversalStatus.DEPARTMENTAL_ERROR else UniversalStatus.FATAL_ERROR
    val endpoint    = resp.responseEndPoint.orElse(ctx.gatewayUrl)
    val firstError  = resp.errors.headOption

    logger.warn(s"[$jobName] poll ERROR ${logRef(ctx.sub)} universalStatus=$errorStatus " +
      s"departmental=$deptError numbers=${resp.errors.flatMap(_.number).mkString(",")}")
    if (deptError)
      logger.warn(s"[$jobName] The return is not validated by the HMRC Backend due to " +
        s"Business Validation Rules (BVR) Errors ${logRef(ctx.sub)}")
    else
      logger.warn(s"[$jobName] The submission failed due to fatal errors from the " +
        s"Government Gateway ${logRef(ctx.sub)}")

    for {
      acc1        <- persistUpdate(ctx.storn, ctx.returnId,
                       baseUpdate(ctx.fullReturn).copy(submittableStatus = Some(errorStatus.toString)),
                       ctx.correlationId)
      _           <- if (deptError)
                       closeDepartmentalGovTalk(ctx.storn, ctx.returnId, ctx.formResultId,
                         ctx.correlationId, endpoint, ctx.polls)
                     else Future.unit
      acc2        <- persistUpdate(ctx.storn, ctx.returnId, acc1.copy(
                       govTalkErrorCode    = firstError.flatMap(_.number),
                       govTalkErrorType    = firstError.map(_.errorType),
                       govTalkErrorMessage = firstError.flatMap(_.text)
                     ), ctx.correlationId)
      finalStatus <- recoverableTail(ctx.sub, acc2, errorStatus, resp.errors, ctx.correlationId)
      _           <- createSubmissionErrorDetails(ctx.storn, ctx.returnId, resp.errors, ctx.correlationId)
      _           <- audit.auditSubmission(ctx.storn, ctx.returnId, ctx.correlationId, ctx.fullReturn, resp)
    } yield polled(ctx, errorStatus.toString, finalStatus)
  }

  private def recoverableTail(sub: SubmissionForPolling,
                              acc: SubmissionUpdate,
                              errorStatus: UniversalStatus,
                              errors: Seq[GovTalkError],
                              correlationId: String): Future[String] =
    if (errorStatus != UniversalStatus.DEPARTMENTAL_ERROR && isRecoverable(errors)) {
      logger.info(s"[$jobName] error is recoverable, resetting submission to STARTED ${logRef(sub)}")
      persistUpdate(sub.storn, sub.returnResourceRef, acc.copy(
        submittableStatus     = Some(UniversalStatus.STARTED.toString),
        submissionRequestDate = None
      ), correlationId).map(_ => UniversalStatus.STARTED.toString)
    } else Future.successful(errorStatus.toString)

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
    PollOutcome(sub, polled = false, pollResult = "-",
      newReturnStatus = sub.submissionStatus, correlationId = "(not polled)")

  private def polled(ctx: PollContext, pollResult: String, newReturnStatus: String): PollOutcome =
    PollOutcome(ctx.sub, polled = true, pollResult = pollResult,
      newReturnStatus = newReturnStatus, correlationId = ctx.correlationId)
}
