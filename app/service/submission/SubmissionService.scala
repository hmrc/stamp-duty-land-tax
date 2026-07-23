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
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import connectors.{ChrisConnector, EmailServiceConnector}
import models.email.EmailServiceRequest
import models.filing.*
import models.submission.*
import service.filing.ChrisService
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

final case class SchemaValidationException(validationErrors: Seq[String])
  extends RuntimeException(s"SDLT payload failed schema validation:\n${validationErrors.mkString("\n")}")

final case class MissingSubmissionContextException(message: String)
  extends RuntimeException(message)

final case class ReturnLockConflictException(returnId: String, status: Int, message: String)
  extends RuntimeException(s"Could not lock return $returnId (HTTP $status): $message")

final case class GovTalkLockNotAcquiredException(formResultId: String, cause: Throwable)
  extends RuntimeException(s"Could not acquire GovTalk Status lock for formResultId=$formResultId", cause)

class SubmissionService @Inject() (
                                    envelopeBuilder: GovTalkEnvelopeBuilder,
                                    validator: SchemaValidator,
                                    connector: ChrisConnector,
                                    govTalk: GovTalkOutcomeHandler,
                                    chrisService: ChrisService,
                                    appConfig: ServicesConfig
                                  )(implicit ec: ExecutionContext)
  extends Logging:

  def submit(fullReturn: FullReturn,
             sender: SenderType,
             periodEnd: LocalDate,
             credentialIdentifier: String,
             email: Option[String] = None)(implicit hc: HeaderCarrier): Future[SubmissionOutcome] =
    val correlationId = newCorrelationId()

    requireContext(fullReturn, credentialIdentifier) match
      case Left(msg) =>
        logger.error(s"[SubmissionService] $msg")
        Future.failed(MissingSubmissionContextException(msg))

      case Right(ctx) =>
        logger.info(s"[SubmissionService] submit START returnId=${ctx.returnId} storn=${ctx.storn} version=${ctx.version} sender=$sender periodEnd=$periodEnd hasExistingSubmission=${fullReturn.submission.isDefined} corrId=$correlationId")
        val started: Future[SubmissionOutcome] = for
          _                <- prepareReturn(ctx, fullReturn, correlationId, email)
          submitUrl        <- prepareGovTalkStatus(ctx, correlationId)
          built            <- buildAndValidate(ctx, fullReturn, sender, periodEnd, correlationId)
          (envelope, seed) = built
          sentIrMark       = seed.IRMarkSent.getOrElse("")
          _                <- acquireGovTalkLock(ctx, correlationId)
          resp             <- sendAndHandle(ctx, fullReturn, envelope, seed, sentIrMark, submitUrl, correlationId, email)
        yield SubmissionOutcome.fromChrisResponse(ctx.returnId, resp, Some(sentIrMark))

        started.andThen {
          case Success(outcome) =>
            logger.info(s"[SubmissionService] submit END returnId=${ctx.returnId} corrId=$correlationId status=${outcome.status} utrn=${outcome.utrn.getOrElse("-")}")
          case Failure(e) =>
            logger.error(s"[SubmissionService] submit FAILED returnId=${ctx.returnId} corrId=$correlationId error=${e.getClass.getSimpleName}: ${e.getMessage}")
        }

  private def prepareReturn(ctx: SubmissionContext, fullReturn: FullReturn, correlationId: String, email: Option[String] = None)
                           (implicit hc: HeaderCarrier): Future[Unit] =
    for
      _ <- lockReturn(ctx, correlationId)
      _ <- handleExistingSubmission(ctx, fullReturn, correlationId, email)
    yield ()

  private def lockReturn(ctx: SubmissionContext, correlationId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    logger.info(s"[SubmissionService] locking return returnId=${ctx.returnId} version=${ctx.version} corrId=$correlationId")
    chrisService.lockReturn(LockReturnRequest(ctx.storn, ctx.returnId, ctx.version)).flatMap {
      case Right(_) =>
        logger.info(s"[SubmissionService] return lock ACQUIRED returnId=${ctx.returnId} corrId=$correlationId")
        Future.successful(())
      case Left(error) =>
        logger.warn(s"[SubmissionService] return lock CONFLICT returnId=${ctx.returnId} corrId=$correlationId: ${error.statusCode} ${error.message}")
        Future.failed(ReturnLockConflictException(ctx.returnId, error.statusCode, error.message))
    }

  private def handleExistingSubmission(ctx: SubmissionContext, fullReturn: FullReturn, correlationId: String, email: Option[String] = None)
                                      (implicit hc: HeaderCarrier): Future[Unit] =
    fullReturn.submission match
      case Some(existing) if isResubmittable(existing) =>
        logger.info(s"[SubmissionService] existing submission is re-submittable (status=${existing.submissionStatus.getOrElse("-")}); clearing prior error details returnId=${ctx.returnId} corrId=$correlationId")
        chrisService.deleteSubmissionErrorDetail(DeleteSubmissionErrorDetailRequest(ctx.storn, ctx.returnId)).map(_ => ())

      case Some(existing) =>
        logger.info(s"[SubmissionService] existing submission present but NOT re-submittable (status=${existing.submissionStatus.getOrElse("-")}) returnId=${ctx.returnId} corrId=$correlationId")
        Future.successful(())

      case None =>
        logger.info(s"[SubmissionService] no existing submission; creating new submission returnId=${ctx.returnId} corrId=$correlationId")
        chrisService.createSubmission(CreateSubmissionRequest(ctx.storn, ctx.returnId, email)).map(_ => ())

  private def isResubmittable(existing: Submission): Boolean =
    existing.submissionStatus.exists { s =>
      val v = s.trim.toUpperCase
      v == "DEPARTMENTAL_ERROR" || v == "FATAL_ERROR" || v == "STARTED"
    }


  private def prepareGovTalkStatus(ctx: SubmissionContext, correlationId: String)
                                  (implicit hc: HeaderCarrier): Future[Option[String]] =
    selectGovTalkStatus(ctx).flatMap {
      case Some(existing) =>
        logger.info(s"[SubmissionService] GovTalk Status row FOUND returnId=${ctx.returnId} corrId=$correlationId protocolStatus=${existing.protocolStatus.getOrElse("-")} storedGatewayUrl=${existing.gatewayUrl.getOrElse("-")} row=$existing")
        existing.protocolStatus match
          case Some(status) if status.nonEmpty =>
            val storedUrl = existing.gatewayUrl.map(_.trim).filter(_.nonEmpty)
            logger.info(s"[SubmissionService] RESETTING GovTalk Status formResultId=${ctx.returnId} corrId=$correlationId oldProtocol=$status storedGatewayUrl=${storedUrl.getOrElse("-")} -> this value will be used as the ChRIS submit URL")
            chrisService
              .resetGovTalkStatus(buildResetRequest(ctx, correlationId, status))
              .map { _ =>
                logger.info(s"[SubmissionService] GovTalk Status reset OK returnId=${ctx.returnId} corrId=$correlationId resolvedSubmitUrl=${storedUrl.getOrElse("<default>")}")
                storedUrl
              }

          case _ =>
            logger.warn(s"[SubmissionService] GovTalk Status row present but protocolStatus EMPTY formResultId=${ctx.returnId} corrId=$correlationId; deleting and re-inserting")
            deleteThenInsert(ctx, correlationId).map { _ =>
              logger.info(s"[SubmissionService] GovTalk Status delete+insert OK returnId=${ctx.returnId} corrId=$correlationId resolvedSubmitUrl=<default>")
              None
            }
      case None =>
        logger.info(s"[SubmissionService] no GovTalk Status row; inserting initial row formResultId=${ctx.returnId} corrId=$correlationId")
        chrisService.insertInitialGovTalkStatus(buildInitialInsertRequest(ctx, correlationId)).map { _ =>
          logger.info(s"[SubmissionService] initial GovTalk Status inserted returnId=${ctx.returnId} corrId=$correlationId resolvedSubmitUrl=<default>")
          None
        }
    }

  private def deleteThenInsert(ctx: SubmissionContext, correlationId: String)
                              (implicit hc: HeaderCarrier): Future[Unit] =
    logger.info(s"[SubmissionService] deleting GovTalk Status row formResultId=${ctx.returnId} corrId=$correlationId")
    chrisService.deleteGovTalkStatus(DeleteGovTalkStatusRequest(resultId = ctx.returnId))
      .flatMap { _ =>
        logger.info(s"[SubmissionService] GovTalk Status row deleted; re-inserting initial row formResultId=${ctx.returnId} corrId=$correlationId")
        chrisService.insertInitialGovTalkStatus(buildInitialInsertRequest(ctx, correlationId))
          .map(_ => ())
          .recoverWith { case e =>
            logger.error(
              s"[SubmissionService] delete succeeded but insert FAILED — GovTalk Status row for " +
                s"formResultId=${ctx.returnId} corrId=$correlationId is now MISSING, must re-seed on retry", e)
            Future.failed(e)
          }
      }

  private def selectGovTalkStatus(ctx: SubmissionContext)(implicit hc: HeaderCarrier): Future[Option[SelectGovTalkStatusResponse]] =
    logger.info(s"[SubmissionService] selecting GovTalk Status returnId=${ctx.returnId} storn=${ctx.storn} corrId=?")
    chrisService.selectGovTalkStatus(SelectGovTalkStatusRequest(ctx.storn, ctx.returnId))
      .map(resp => Option.when(resp.formResultId.exists(_.trim.nonEmpty))(resp))
      .recover { case e =>
        logger.warn(s"[SubmissionService] selectGovTalkStatus lookup failed (treating as no row) returnId=${ctx.returnId}: ${e.getMessage}")
        None
      }

  private def buildInitialInsertRequest(ctx: SubmissionContext, correlationId: String): InsertInitialGovTalkStatusRequest =
    val now = nowSqlTimestamp
    InsertInitialGovTalkStatusRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.returnId,
      correlationId  = correlationId,
      govTalkStatus  = GovTalkStatusInitial(
        formLock             = "N",
        createTimestamp      = now,
        endStateTimestamp    = None,
        lastMessageTimestamp = now,
        numberOfPolls        = "0",
        pollInterval         = "0",
        protocolStatus       = "initial",
        gatewayUrl           = appConfig.baseUrl("chris")
      )
    )

  private def buildResetRequest(ctx: SubmissionContext, correlationId: String, oldProtocol: String): ResetGovTalkStatusRequest =
    val now = nowSqlTimestamp
    ResetGovTalkStatusRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.returnId,
      correlationId  = "empty",
      govTalkStatus  = GovTalkStatusReset(
        formLock             = "N",
        createTimestamp      = now,
        endStateTimestamp    = None,          // spec F53 step 7: null for the end state date on reset
        lastMessageTimestamp = now,
        numberOfPolls        = "0",
        pollInterval         = "0",
        protocolStatusOld    = oldProtocol,
        protocolStatusNew    = "initial",
        gatewayUrl           = appConfig.baseUrl("chris")
      )
    )

  private def buildAndValidate(ctx: SubmissionContext,
                               fullReturn: FullReturn,
                               sender: SenderType,
                               periodEnd: LocalDate,
                               correlationId: String)
                              (implicit hc: HeaderCarrier): Future[(Elem, SubmissionUpdate)] =
    logger.info(s"[SubmissionService] building and validating SDLT payload returnId=${ctx.returnId} corrId=$correlationId")
    val sdlt = SdltReturnMapper.toSdltElement(fullReturn)

    validator.validateSdlt(sdlt) match
      case Left(errors) =>
        logger.error(s"[SubmissionService] schema validation FAILED returnId=${ctx.returnId} corrId=$correlationId: ${errors.mkString("; ")}")
        Future.failed(SchemaValidationException(errors))

      case Right(_) =>
        logger.info(s"[SubmissionService] schema validation OK returnId=${ctx.returnId} corrId=$correlationId")
        val irMarkResult = envelopeBuilder.submissionRequest(sdlt, ctx.storn, periodEnd, sender, ctx.credentialIdentifier)
        logger.info(s"[SubmissionService] IRmark computed returnId=${ctx.returnId} corrId=$correlationId b32=${irMarkResult.base32}")

        val pending = SubmissionUpdate(
          IRMarkRecieved        = None,
          utrn                  = None,
          email                 = None,
          submissionRequestDate = None,
          acceptedDate          = None,
          submittableStatus     = Some("PENDING"),
          govTalkErrorCode      = None,
          govTalkErrorType      = None,
          govTalkErrorMessage   = None,
          IRMarkSent            = Some(irMarkResult.base64)
        )

        chrisService
          .updateSubmission(UpdateSubmissionRequest(ctx.storn, ctx.returnId, pending))
          .map { _ =>
            logger.info(s"[SubmissionService] PENDING seed persisted returnId=${ctx.returnId} corrId=$correlationId IRMarkSent=${irMarkResult.base64}")
            (irMarkResult.envelope, pending)
          }

  private def gctx(ctx: SubmissionContext, correlationId: String): GovTalkContext =
    GovTalkContext(ctx.storn, ctx.returnId, correlationId)

  private def acquireGovTalkLock(ctx: SubmissionContext, correlationId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    logger.info(s"[SubmissionService] acquiring GovTalk lock (N->Y) formResultId=${ctx.returnId} corrId=$correlationId")
    val req = UpdateGovTalkStatusLockRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.returnId,
      govTalkStatus  = GovTalkStatusLock(formLockOld = "N", formLockNew = "Y", pollInterval = "0", gatewayUrl = appConfig.baseUrl("chris"))
    )
    chrisService.updateGovTalkStatusLock(req).map { _ =>
      logger.info(s"[SubmissionService] GovTalk lock ACQUIRED formResultId=${ctx.returnId} corrId=$correlationId")
      ()
    }.recoverWith { case e =>
      logger.error(s"[SubmissionService] GovTalk lock NOT acquired formResultId=${ctx.returnId} corrId=$correlationId", e)
      Future.failed(GovTalkLockNotAcquiredException(ctx.returnId, e))
    }

  private def sendAndHandle(ctx: SubmissionContext,
                            fullReturn: FullReturn,
                            envelope: Elem,
                            seed: SubmissionUpdate,
                            sentIrMark: String,
                            submitUrl: Option[String],
                            correlationId: String,
                            email: Option[String] = None)
                           (implicit hc: HeaderCarrier): Future[ChrisResponse] =
    logger.info(s"[SubmissionService] SUBMITTING to ChRIS returnId=${ctx.returnId} corrId=$correlationId submitUrl=${submitUrl.getOrElse("<default (connector fallback)>")}")

    val work: Future[(ChrisResponse, SubmissionUpdate)] =
      for
        resp     <- connector.submit(envelope, submitUrl, correlationId)
        _        =  logger.info(s"[SubmissionService] ChRIS response received returnId=${ctx.returnId} corrId=$correlationId response=${resp.getClass.getSimpleName}")
        finalAcc <- handleResponse(ctx, fullReturn, resp, seed, sentIrMark, correlationId, email)
      yield (resp, finalAcc)

    work.transformWith { outcome =>
      val skipDatetime = outcome match
        case Success((resp, _)) => isRecoverableResp(resp)
        case Failure(_)         => true

      logger.info(s"[SubmissionService] post-submit cleanup returnId=${ctx.returnId} corrId=$correlationId skipDatetimeFooter=$skipDatetime outcome=${outcome.fold(_.getClass.getSimpleName, _._1.getClass.getSimpleName)}")

      val stampDatetime: Future[Unit] = outcome match
        case Success((_, finalAcc)) if !skipDatetime =>
          ensureSubmissionRequestDatetime(ctx, finalAcc, correlationId)
        case _ =>
          Future.unit

      (for
        _ <- govTalk.releaseLock(gctx(ctx, correlationId))
        _ <- stampDatetime
      yield ()).transformWith(_ => Future.fromTry(outcome.map(_._1)))
    }

  private val RecoverableNumbers: Set[String] = Set("1000", "2005", "3000")

  private def isRecoverable(errors: Seq[GovTalkError]): Boolean =
    errors.exists(_.number.exists(RecoverableNumbers.contains))

  private def isRecoverableResp(resp: ChrisResponse): Boolean = resp match
    case e: ChrisResponse.Errored => isRecoverable(e.errors)
    case _ => false

  private def handleResponse(ctx: SubmissionContext,
                             fullReturn: FullReturn,
                             resp: ChrisResponse,
                             seed: SubmissionUpdate,
                             sentIrMark: String,
                             correlationId: String,
                             email: Option[String] = None)
                            (implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    val universal = UniversalStatus.fromChrisResponse(resp, Some(sentIrMark))
    logger.info(s"[SubmissionService] resolved returnId=${ctx.returnId} corrId=$correlationId universalStatus=$universal responseType=${resp.getClass.getSimpleName}")

    resp match
      case c: ChrisResponse.Completed =>
        logger.info(s"[SubmissionService] handling COMPLETED returnId=${ctx.returnId} corrId=$correlationId utrn=${c.utrn.getOrElse("-")} receivedIrMark=${c.receivedIrMark.getOrElse("-")}")
        govTalk.handleCompleted(gctx(ctx, correlationId), fullReturn, c, universal, seed, "0", email, hc)

      case a: ChrisResponse.Acknowledged =>
        logger.info(s"[SubmissionService] handling ACKNOWLEDGED returnId=${ctx.returnId} corrId=$correlationId pollInterval=${a.pollIntervalSeconds.getOrElse(0)}")
        acknowledgementBranch(ctx, a, seed, correlationId)

      case e: ChrisResponse.Errored =>
        logger.warn(s"[SubmissionService] handling ERRORED returnId=${ctx.returnId} corrId=$correlationId errorCount=${e.errors.size} numbers=${e.errors.flatMap(_.number).mkString(",")}")
        govTalk.handleErrored(gctx(ctx, correlationId), fullReturn, e.errors, e.responseEndPoint, universal, seed, "0", hc)

      case ChrisResponse.TransportError(msg, _) =>
        logger.error(s"[SubmissionService] handling TRANSPORT ERROR returnId=${ctx.returnId} corrId=$correlationId: $msg")
        govTalk.handleErrored(gctx(ctx, correlationId), fullReturn, Nil, None, universal, seed, "0", hc)

  private def acknowledgementBranch(ctx: SubmissionContext,
                                    resp: ChrisResponse.Acknowledged,
                                    seed: SubmissionUpdate,
                                    correlationId: String)
                                   (implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    logger.info(s"[SubmissionService] ACK branch returnId=${ctx.returnId} corrId=$correlationId pollInterval=${resp.pollIntervalSeconds.getOrElse(0)} responseEndPoint=${resp.responseEndPoint.getOrElse("-")}")
    for
      acc1 <- govTalk.persistStatus(gctx(ctx, correlationId), seed, UniversalStatus.ACCEPTED)
      _    <- govTalk.setProtocol(gctx(ctx, correlationId), "dataPoll")
      acc2 <- govTalk.persistUpdate(gctx(ctx, correlationId), acc1.copy(acceptedDate = Some(nowIso)))
      _    <- govTalk.updateStatistics(gctx(ctx, correlationId), resp.responseEndPoint, resp.pollIntervalSeconds, "0")
      _    =  logger.info(s"[SubmissionService] ACK branch complete returnId=${ctx.returnId} corrId=$correlationId")
    yield acc2

  private def ensureSubmissionRequestDatetime(ctx: SubmissionContext, base: SubmissionUpdate, correlationId: String)
                                             (implicit hc: HeaderCarrier): Future[Unit] =
    val update = base.copy(submissionRequestDate = Some(nowIso))
    govTalk.persistUpdate(gctx(ctx, correlationId), update).map { _ =>
      logger.info(s"[SubmissionService] submission-request datetime ensured returnId=${ctx.returnId} corrId=$correlationId")
      ()
    }.recover { case e =>
      logger.warn(s"[SubmissionService] ensure submission-request datetime FAILED (suppressed) returnId=${ctx.returnId} corrId=$correlationId: ${e.getMessage}")
      ()
    }

  private case class SubmissionContext(storn: String, returnId: String, version: Int, credentialIdentifier: String)

  private def requireContext(fullReturn: FullReturn, credentialIdentifier: String): Either[String, SubmissionContext] =
    (fullReturn.stornId, fullReturn.returnResourceRef, fullReturn.returnInfo.flatMap(_.version)) match
      case _ if credentialIdentifier.trim.isEmpty =>
        Left("Missing credentialIdentifier; cannot submit. Auth context did not provide a credential ID.")
      case (Some(storn), Some(returnId), Some(version)) =>
        Right(SubmissionContext(storn, returnId, version.toInt, credentialIdentifier))
      case _ =>
        Left(s"FullReturn missing one of stornId / returnResourceRef / returnInfo.version; cannot submit. " +
          s"stornId=${fullReturn.stornId}, returnResourceRef=${fullReturn.returnResourceRef}, " +
          s"version=${fullReturn.returnInfo.flatMap(_.version)}")

  private def newCorrelationId(): String = UUID.randomUUID().toString.replace("-", "")

  private def nowIso: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  private val SqlTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  private def nowSqlTimestamp: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(SqlTimestampFormatter)