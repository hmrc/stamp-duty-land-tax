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

package service.submission

import com.google.inject.Inject
import connectors.ChrisConnector
import models.filing.*
import models.submission.*
import play.api.Logging
import service.filing.ChrisService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

final case class GovTalkContext(storn: String, formResultId: String, correlationId: String)

class GovTalkOutcomeHandler @Inject() (
  chrisService: ChrisService,
  chrisConnector: ChrisConnector,
  audit: SubmissionAuditService,
  emailService: EmailService,
  appConfig: ServicesConfig
)(implicit ec: ExecutionContext)
  extends Logging:

  def handleCompleted(ctx: GovTalkContext,
                      fullReturn: FullReturn,
                      resp: ChrisResponse.Completed,
                      universal: UniversalStatus,
                      seed: SubmissionUpdate,
                      numberOfPolls: String,
                      email: Option[String],
                      externalHc: HeaderCarrier)(implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    logger.info(s"[GovTalkOutcomeHandler] SUCCESS ref=${ctx.formResultId} corrId=${ctx.correlationId} universalStatus=$universal utrn=${resp.utrn.getOrElse("-")}")
    for
      acc1    <- persistStatus(ctx, seed, universal)
      _       <- updateStatistics(ctx, resp.responseEndPoint, None, numberOfPolls)
      _       <- setProtocol(ctx, "deleteRequest")
      deleted <- sendChrisDelete(ctx, resp.responseEndPoint, externalHc)
      _       <- if deleted then finaliseGovTalkStatus(ctx)
                 else
                   logger.warn(s"[GovTalkOutcomeHandler] ChRIS delete unsuccessful; leaving GovTalk at deleteRequest ref=${ctx.formResultId}")
                   Future.unit
      _       <- audit.auditSubmission(ctx.storn, ctx.formResultId, ctx.correlationId, fullReturn, resp)(externalHc)
      acc2    <- persistUpdate(ctx, acc1.copy(
                   IRMarkRecieved = resp.receivedIrMark,
                   utrn           = resp.utrn,
                   acceptedDate   = Some(nowIso)
                 ))
      _       <- emailService.submitEmailConfirmation(fullReturn, resp.utrn.getOrElse(""), email)(externalHc)
    yield acc2

  def handleErrored(ctx: GovTalkContext,
                    fullReturn: FullReturn,
                    errors: Seq[GovTalkError],
                    responseEndPoint: Option[String],
                    universal: UniversalStatus,
                    seed: SubmissionUpdate,
                    numberOfPolls: String,
                    externalHc: HeaderCarrier)(implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    val deptError = universal == UniversalStatus.DEPARTMENTAL_ERROR
    val first     = errors.headOption

    logger.warn(s"[GovTalkOutcomeHandler] ERROR ref=${ctx.formResultId} corrId=${ctx.correlationId} universalStatus=$universal departmental=$deptError numbers=${errors.flatMap(_.number).mkString(",")}")

    val govTalkForDept: Future[Unit] =
      if deptError then
        for
          _ <- updateStatistics(ctx, responseEndPoint, None, numberOfPolls)
          _ <- setProtocol(ctx, "deleteRequest")
          _ <- sendChrisDelete(ctx, responseEndPoint, externalHc)
          _ <- setProtocol(ctx, "endState")
        yield ()
      else Future.unit

    for
      acc1 <- persistStatus(ctx, seed, universal)
      _    <- govTalkForDept
      acc2 <- persistUpdate(ctx, acc1.copy(
                govTalkErrorCode    = first.flatMap(_.number),
                govTalkErrorType    = first.map(_.errorType),
                govTalkErrorMessage = first.flatMap(_.text)
              ))
      _    <- createSubmissionErrorDetails(ctx, errors)
      acc3 <- recoverableTail(ctx, acc2, universal)
      _    <- audit.auditSubmission(ctx.storn, ctx.formResultId, ctx.correlationId, fullReturn,
                ChrisResponse.Errored(errors, Some(ctx.correlationId), responseEndPoint, "<error/>"))(externalHc)
    yield acc3

  def releaseLock(ctx: GovTalkContext)(implicit hc: HeaderCarrier): Future[Unit] =
    chrisService
      .selectGovTalkStatus(SelectGovTalkStatusRequest(ctx.storn, ctx.formResultId))
      .flatMap { fresh =>
        if fresh.formLock.map(_.trim).contains("Y") then
          chrisService.updateGovTalkStatusLock(UpdateGovTalkStatusLockRequest(
            userIdentifier = ctx.storn,
            formResultId   = ctx.formResultId,
            govTalkStatus  = GovTalkStatusLock(
              formLockOld  = "Y",
              formLockNew  = "N",
              pollInterval = fresh.pollInterval.getOrElse("0"),
              gatewayUrl   = fresh.gatewayUrl.map(_.trim).filter(_.nonEmpty).getOrElse(appConfig.baseUrl("chris"))
            )
          )).map { _ =>
            logger.info(s"[GovTalkOutcomeHandler] GovTalk lock released ref=${ctx.formResultId}")
            ()
          }
        else
          logger.info(s"[GovTalkOutcomeHandler] GovTalk lock already released ref=${ctx.formResultId}")
          Future.unit
      }
      .recover { case e =>
        logger.error(s"[GovTalkOutcomeHandler] GovTalk lock release failed ref=${ctx.formResultId}: ${e.getMessage}")
        ()
      }

  def persistStatus(ctx: GovTalkContext, acc: SubmissionUpdate, status: UniversalStatus)(implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    persistUpdate(ctx, acc.copy(submittableStatus = Some(status.toString)))

  def persistUpdate(ctx: GovTalkContext, acc: SubmissionUpdate)(implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    chrisService.updateSubmission(UpdateSubmissionRequest(ctx.storn, ctx.formResultId, acc)).map { _ =>
      logger.info(s"[GovTalkOutcomeHandler] submission updated status=${acc.submittableStatus.getOrElse("-")} ref=${ctx.formResultId}")
      acc
    }

  def setProtocol(ctx: GovTalkContext, protocolStatus: String)(implicit hc: HeaderCarrier): Future[Unit] =
    chrisService.updateGovTalkStatus(UpdateGovTalkStatusRequest(
      userIdentifier    = ctx.storn,
      formResultId      = ctx.formResultId,
      endStateTimestamp = nowSqlTimestamp,
      protocolStatus    = protocolStatus
    )).map { _ =>
      logger.info(s"[GovTalkOutcomeHandler] GovTalk protocolStatus set to '$protocolStatus' ref=${ctx.formResultId}")
      ()
    }

  def updateStatistics(ctx: GovTalkContext,
                       responseEndPoint: Option[String],
                       pollIntervalSeconds: Option[Int],
                       numberOfPolls: String)(implicit hc: HeaderCarrier): Future[Unit] =
    val gatewayUrl = responseEndPoint.filter(_.nonEmpty).getOrElse(appConfig.baseUrl("chris"))
    chrisService.updateGovTalkStatistics(UpdateGovTalkStatisticsRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.formResultId,
      govTalkStatus  = GovTalkStatusStatistics(
        lastMessageTimestamp = nowSqlTimestamp,
        numberOfPolls        = numberOfPolls,
        pollInterval         = pollIntervalSeconds.map(_.toString).getOrElse("0"),
        gatewayUrl           = gatewayUrl
      )
    )).map { _ =>
      logger.info(s"[GovTalkOutcomeHandler] GovTalk statistics updated gatewayUrl=$gatewayUrl ref=${ctx.formResultId}")
      ()
    }

  private def recoverableTail(ctx: GovTalkContext, acc: SubmissionUpdate, universal: UniversalStatus)(implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    if universal == UniversalStatus.STARTED then
      logger.info(s"[GovTalkOutcomeHandler] error is recoverable, resetting submission to STARTED ref=${ctx.formResultId}")
      persistUpdate(ctx, acc.copy(
        submittableStatus     = Some(UniversalStatus.STARTED.toString),
        submissionRequestDate = None
      ))
    else Future.successful(acc)

  private def finaliseGovTalkStatus(ctx: GovTalkContext)(implicit hc: HeaderCarrier): Future[Unit] =
    (for
      _ <- setProtocol(ctx, "endState")
      _ <- chrisService.resetGovTalkStatus(buildResetRequest(ctx)).map(_ => ())
    yield
      logger.info(s"[GovTalkOutcomeHandler] GovTalk Status finalised (endState + reset) ref=${ctx.formResultId}")
    ).recover { case e =>
      logger.warn(s"[GovTalkOutcomeHandler] GovTalk finalise (endState/reset) failed (suppressed) ref=${ctx.formResultId}: ${e.getMessage}")
      ()
    }

  private def buildResetRequest(ctx: GovTalkContext): ResetGovTalkStatusRequest =
    val now = nowSqlTimestamp
    ResetGovTalkStatusRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.formResultId,
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
        gatewayUrl           = appConfig.baseUrl("chris")
      )
    )

  private def createSubmissionErrorDetails(ctx: GovTalkContext, errors: Seq[GovTalkError])(implicit hc: HeaderCarrier): Future[Unit] =
    errors.foldLeft(Future.unit) { (acc, err) =>
      acc.flatMap { _ =>
        chrisService.createSubmissionErrorDetail(CreateSubmissionErrorDetailRequest(
          storn                  = ctx.storn,
          returnResourceRef      = ctx.formResultId,
          submissionErrorDetails = SubmissionErrorDetail(
            position     = err.location.getOrElse(""),
            errorMessage = err.text.getOrElse("")
          )
        )).map(_ => ())
      }
    }

  private def sendChrisDelete(ctx: GovTalkContext, endpoint: Option[String], externalHc: HeaderCarrier): Future[Boolean] =
    chrisConnector.delete(endpoint, ctx.correlationId)(externalHc).map {
      case ChrisDeleteResponse.Deleted(_, _)  => true
      case ChrisDeleteResponse.NotFound(_, _) => true
      case ChrisDeleteResponse.Errored(errors, _, _) =>
        logger.warn(s"[GovTalkOutcomeHandler] ChRIS delete returned errors ref=${ctx.formResultId}: ${errors.mkString("; ")}")
        false
      case ChrisDeleteResponse.TransportError(msg, _) =>
        logger.warn(s"[GovTalkOutcomeHandler] ChRIS delete transport error ref=${ctx.formResultId}: $msg")
        false
    }.recover { case e =>
      logger.warn(s"[GovTalkOutcomeHandler] ChRIS delete failed ref=${ctx.formResultId}: ${e.getMessage}")
      false
    }

  private def nowIso: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  private val SqlTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  private def nowSqlTimestamp: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(SqlTimestampFormatter)
