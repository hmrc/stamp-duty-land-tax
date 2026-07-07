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
                                    audit: SubmissionAuditService,
                                    chrisService: ChrisService,
                                    appConfig: ServicesConfig,
                                    emailServiceConnector: EmailServiceConnector
                                  )(implicit ec: ExecutionContext)
  extends Logging:

  def submit(fullReturn: FullReturn,
             sender: SenderType,
             periodEnd: LocalDate,
             credentialIdentifier: String)(implicit hc: HeaderCarrier): Future[ChrisResponse] =
    val correlationId = newCorrelationId()

    requireContext(fullReturn, credentialIdentifier) match
      case Left(msg) =>
        logger.error(s"[SubmissionService] $msg")
        Future.failed(MissingSubmissionContextException(msg))

      case Right(ctx) =>
        for
          _                <- prepareReturn(ctx, fullReturn, correlationId)
          submitUrl        <- prepareGovTalkStatus(ctx, correlationId)
          built            <- buildAndValidate(ctx, fullReturn, sender, periodEnd, correlationId)
          (envelope, seed) = built
          sentIrMark       = seed.IRMarkSent.getOrElse("")
          _                <- acquireGovTalkLock(ctx, correlationId)
          resp             <- sendAndHandle(ctx, fullReturn, envelope, seed, sentIrMark, submitUrl, correlationId)
        yield resp

  private def prepareReturn(ctx: SubmissionContext, fullReturn: FullReturn, correlationId: String)
                           (implicit hc: HeaderCarrier): Future[Unit] =
    for
      _ <- lockReturn(ctx, correlationId)
      _ <- handleExistingSubmission(ctx, fullReturn, correlationId)
    yield ()

  private def lockReturn(ctx: SubmissionContext, correlationId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    chrisService.lockReturn(LockReturnRequest(ctx.storn, ctx.returnId, ctx.version)).flatMap {
      case Right(_) =>
        logger.debug(s"[SubmissionService] return lock acquired returnId=${ctx.returnId} corrId=$correlationId")
        Future.successful(())
      case Left(error) =>
        logger.warn(s"[SubmissionService] return lock conflict returnId=${ctx.returnId} corrId=$correlationId: ${error.statusCode} ${error.message}")
        Future.failed(ReturnLockConflictException(ctx.returnId, error.statusCode, error.message))
    }

  private def handleExistingSubmission(ctx: SubmissionContext, fullReturn: FullReturn, correlationId: String)
                                      (implicit hc: HeaderCarrier): Future[Unit] =
    fullReturn.submission match
      case Some(existing) if isResubmittable(existing) =>
        logger.debug(s"[SubmissionService] clearing prior error details returnId=${ctx.returnId} corrId=$correlationId")
        chrisService.deleteSubmissionErrorDetail(DeleteSubmissionErrorDetailRequest(ctx.storn, ctx.returnId)).map(_ => ())

      case Some(_) =>
        logger.debug(s"[SubmissionService] submission exists but not re-submittable returnId=${ctx.returnId} corrId=$correlationId")
        Future.successful(())

      case None =>
        logger.debug(s"[SubmissionService] creating new submission returnId=${ctx.returnId} corrId=$correlationId")
        val email = fullReturn.returnAgent.flatMap(_.headOption).flatMap(_.email).getOrElse("")
        chrisService.createSubmission(CreateSubmissionRequest(ctx.storn, ctx.returnId, email)).map(_ => ())

  private def isResubmittable(existing: Submission): Boolean =
    existing.submissionStatus.exists { s =>
      val v = s.trim.toUpperCase
      v == "ERROR" || v == "FAILED"
    }


  private def prepareGovTalkStatus(ctx: SubmissionContext, correlationId: String)
                                  (implicit hc: HeaderCarrier): Future[Option[String]] =
    selectGovTalkStatus(ctx).flatMap {
      case Some(existing) =>
        val storedUrl = existing.gatewayUrl.map(_.trim).filter(_.nonEmpty)
        logger.debug(s"[SubmissionService] resetting GovTalk Status formResultId=${ctx.returnId} corrId=$correlationId storedGatewayUrl=${storedUrl.getOrElse("-")}")
        chrisService.resetGovTalkStatus(buildResetRequest(ctx, correlationId)).map(_ => storedUrl)

      case None =>
        logger.debug(s"[SubmissionService] inserting initial GovTalk Status formResultId=${ctx.returnId} corrId=$correlationId")
        chrisService.insertInitialGovTalkStatus(buildInitialInsertRequest(ctx, correlationId)).map(_ => None)
    }

  private def selectGovTalkStatus(ctx: SubmissionContext)(implicit hc: HeaderCarrier): Future[Option[SelectGovTalkStatusResponse]] =
    chrisService.selectGovTalkStatus(SelectGovTalkStatusRequest(ctx.storn, ctx.returnId))
      .map(Some(_))
      .recover { case _ => None }

  private def buildInitialInsertRequest(ctx: SubmissionContext, correlationId: String): InsertInitialGovTalkStatusRequest =
    val now = nowIso
    InsertInitialGovTalkStatusRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.returnId,
      correlationId  = correlationId,
      govTalkStatus  = GovTalkStatusInitial(
        formLock             = "false",
        createTimestamp      = now,
        endStateTimestamp    = None,
        lastMessageTimestamp = now,
        numberOfPolls        = "0",
        pollInterval         = "0",
        protocolStatus       = "initial",
        gatewayUrl           = appConfig.baseUrl("chris")
      )
    )

  private def buildResetRequest(ctx: SubmissionContext, correlationId: String): ResetGovTalkStatusRequest =
    val now = nowIso
    ResetGovTalkStatusRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.returnId,
      correlationId  = "empty",
      govTalkStatus  = GovTalkStatusReset(
        formLock             = "false",
        createTimestamp      = now,
        endStateTimestamp    = None,
        lastMessageTimestamp = now,
        numberOfPolls        = "0",
        pollInterval         = "0",
        protocolStatusOld    = "",
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
    val sdlt = SdltReturnMapper.toSdltElement(fullReturn)

    validator.validateSdlt(sdlt) match
      case Left(errors) =>
        logger.error(s"[SubmissionService] schema validation failed returnId=${ctx.returnId} corrId=$correlationId: ${errors.mkString("; ")}")
        Future.failed(SchemaValidationException(errors))

      case Right(_) =>
        val irMarkResult = envelopeBuilder.submissionRequest(sdlt, ctx.storn, periodEnd, sender, ctx.credentialIdentifier)
        logger.debug(s"[SubmissionService] IRmark computed returnId=${ctx.returnId} corrId=$correlationId b32=${irMarkResult.base32}")

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
          .map(_ => (irMarkResult.envelope, pending))

  private def acquireGovTalkLock(ctx: SubmissionContext, correlationId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    val req = UpdateGovTalkStatusLockRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.returnId,
      govTalkStatus  = GovTalkStatusLock(formLockOld = "false", formLockNew = "true", pollInterval = "0", gatewayUrl = appConfig.baseUrl("chris"))
    )
    chrisService.updateGovTalkStatusLock(req).map { _ =>
      logger.debug(s"[SubmissionService] GovTalk lock acquired formResultId=${ctx.returnId} corrId=$correlationId")
      ()
    }.recoverWith { case e =>
      logger.error(s"[SubmissionService] GovTalk lock NOT acquired formResultId=${ctx.returnId} corrId=$correlationId", e)
      Future.failed(GovTalkLockNotAcquiredException(ctx.returnId, e))
    }

  private def releaseGovTalkLock(ctx: SubmissionContext, correlationId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    val req = UpdateGovTalkStatusLockRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.returnId,
      govTalkStatus  = GovTalkStatusLock(formLockOld = "true", formLockNew = "false", pollInterval = "0", gatewayUrl = appConfig.baseUrl("chris"))
    )
    chrisService.updateGovTalkStatusLock(req).map { _ =>
      logger.debug(s"[SubmissionService] GovTalk lock released formResultId=${ctx.returnId} corrId=$correlationId")
      ()
    }.recover { case e =>
      logger.error(s"[SubmissionService] GovTalk lock release FAILED (suppressed) formResultId=${ctx.returnId} corrId=$correlationId", e)
      ()
    }

  private def sendAndHandle(ctx: SubmissionContext,
                            fullReturn: FullReturn,
                            envelope: Elem,
                            seed: SubmissionUpdate,
                            sentIrMark: String,
                            submitUrl: Option[String],
                            correlationId: String)
                           (implicit hc: HeaderCarrier): Future[ChrisResponse] =
    val work: Future[ChrisResponse] =
      for
        resp <- connector.submit(envelope, submitUrl, correlationId)
        _    <- handleResponse(ctx, fullReturn, resp, seed, sentIrMark, correlationId)
      yield resp

    work.transformWith { outcome =>
      val skipDatetime = outcome match
        case Success(resp) => isRecoverableResp(resp)
        case Failure(_)    => false

      (for
        _ <- releaseGovTalkLock(ctx, correlationId)
        _ <- if skipDatetime then Future.unit
        else ensureSubmissionRequestDatetime(ctx, seed, correlationId)
      yield ()).transformWith(_ => Future.fromTry(outcome))
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
                             correlationId: String)
                            (implicit hc: HeaderCarrier): Future[Unit] =
    val universal = UniversalStatus.fromChrisResponse(resp, Some(sentIrMark))
    logger.info(s"[SubmissionService] resolved returnId=${ctx.returnId} corrId=$correlationId universalStatus=$universal")

    resp match
      case c: ChrisResponse.Completed =>
        successBranch(ctx, fullReturn, c, universal, seed, correlationId)

      case a: ChrisResponse.Acknowledged =>
        acknowledgementBranch(ctx, a, seed, correlationId)

      case e: ChrisResponse.Errored =>
        errorBranch(ctx, fullReturn, e.errors, e.responseEndPoint, universal, seed, correlationId)

      case ChrisResponse.TransportError(msg, _) =>
        logger.error(s"[SubmissionService] transport error returnId=${ctx.returnId} corrId=$correlationId: $msg")
        errorBranch(ctx, fullReturn, Nil, None, universal, seed, correlationId)

  private def successBranch(ctx: SubmissionContext,
                            fullReturn: FullReturn,
                            resp: ChrisResponse.Completed,
                            universal: UniversalStatus,
                            seed: SubmissionUpdate,
                            correlationId: String)
                           (implicit hc: HeaderCarrier): Future[Unit] =
    for
      acc1 <- persistStatus(ctx, seed, universal, correlationId)
      _ <- updateGovTalkStatistics(ctx, resp.responseEndPoint, None, correlationId)
      _ <- setGovTalkProtocol(ctx, "deleteRequest", correlationId)
      _ <- sendChrisDelete(ctx, resp.responseEndPoint, correlationId)
      _ <- setGovTalkProtocol(ctx, "endState", correlationId)
      _ <- audit.auditSubmission(ctx.storn, ctx.returnId, correlationId, fullReturn, resp)
      _ <- persistUpdate(ctx, acc1.copy(IRMarkRecieved = resp.receivedIrMark, utrn = resp.utrn), correlationId)
      _ <- emailServiceConnector.submitEmailConfirmation(
        EmailServiceRequest(
          email = fullReturn.submission.flatMap(_.email).getOrElse(""),
          templateId = "sdlt_submission_confirmation",
          linkExpiryDuration = fullReturn.???,
          continueUrl = fullReturn.???,
          templateParameters = Map(
            "purchaserName" -> fullReturn.???,
            "propertyAddress" -> fullReturn.???,
            "utrn" -> resp.utrn.toString
          )
        )
      )
    yield ()

  private def acknowledgementBranch(ctx: SubmissionContext,
                                    resp: ChrisResponse.Acknowledged,
                                    seed: SubmissionUpdate,
                                    correlationId: String)
                                   (implicit hc: HeaderCarrier): Future[Unit] =
    for
      acc1 <- persistStatus(ctx, seed, UniversalStatus.ACCEPTED, correlationId)
      _    <- setGovTalkProtocol(ctx, "dataPoll", correlationId)
      acc2 <- persistUpdate(ctx, acc1.copy(acceptedDate = Some(nowIso)), correlationId)
      _    <- updateGovTalkStatistics(ctx, resp.responseEndPoint, resp.pollIntervalSeconds, correlationId)
    yield ()

  private def errorBranch(ctx: SubmissionContext,
                          fullReturn: FullReturn,
                          errors: Seq[GovTalkError],
                          responseEndPoint: Option[String],
                          universal: UniversalStatus,
                          seed: SubmissionUpdate,
                          correlationId: String)
                         (implicit hc: HeaderCarrier): Future[Unit] =
    val deptError = universal == UniversalStatus.DEPARTMENTAL_ERROR
    val first     = errors.headOption

    val govTalkForDept: Future[Unit] =
      if deptError then
        for
          _ <- updateGovTalkStatistics(ctx, responseEndPoint, None, correlationId)
          _ <- setGovTalkProtocol(ctx, "deleteRequest", correlationId)
          _ <- sendChrisDelete(ctx, responseEndPoint, correlationId)
          _ <- setGovTalkProtocol(ctx, "endState", correlationId)
        yield ()
      else Future.unit

    for
      acc1 <- persistStatus(ctx, seed, universal, correlationId)
      _    <- govTalkForDept
      acc2 <- persistUpdate(ctx, acc1.copy(
        govTalkErrorCode    = first.flatMap(_.number),
        govTalkErrorType    = first.map(_.errorType),
        govTalkErrorMessage = first.flatMap(_.text)
      ), correlationId)
      _    <- createSubmissionErrorDetails(ctx, errors, correlationId)
      _    <- recoverableTail(ctx, acc2, errors, correlationId)
      _    <- audit.auditSubmission(ctx.storn, ctx.returnId, correlationId, fullReturn,
        ChrisResponse.Errored(errors, Some(correlationId), responseEndPoint, "<error/>")) // CIP failure
    yield ()

  private def recoverableTail(ctx: SubmissionContext, acc: SubmissionUpdate, errors: Seq[GovTalkError], correlationId: String)
                             (implicit hc: HeaderCarrier): Future[Unit] =
    if isRecoverable(errors) then
      persistUpdate(ctx, acc.copy(
        submittableStatus     = Some(UniversalStatus.STARTED.toString),
        submissionRequestDate = None
      ), correlationId).map(_ => ())
    else
      logger.debug(s"[SubmissionService] error not recoverable returnId=${ctx.returnId} corrId=$correlationId")
      Future.unit

  private def persistStatus(ctx: SubmissionContext, acc: SubmissionUpdate, status: UniversalStatus, correlationId: String)
                           (implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    persistUpdate(ctx, acc.copy(submittableStatus = Some(status.toString)), correlationId)

  private def persistUpdate(ctx: SubmissionContext, acc: SubmissionUpdate, correlationId: String)
                           (implicit hc: HeaderCarrier): Future[SubmissionUpdate] =
    chrisService.updateSubmission(UpdateSubmissionRequest(ctx.storn, ctx.returnId, acc)).map { _ =>
      logger.debug(s"[SubmissionService] submission updated status=${acc.submittableStatus.getOrElse("-")} returnId=${ctx.returnId} corrId=$correlationId")
      acc
    }

  private def ensureSubmissionRequestDatetime(ctx: SubmissionContext, seed: SubmissionUpdate, correlationId: String)
                                             (implicit hc: HeaderCarrier): Future[Unit] =
    val update = seed.copy(submissionRequestDate = Some(nowIso))
    chrisService.updateSubmission(UpdateSubmissionRequest(ctx.storn, ctx.returnId, update)).map { _ =>
      logger.debug(s"[SubmissionService] submission-request datetime ensured returnId=${ctx.returnId} corrId=$correlationId")
      ()
    }.recover { case e =>
      logger.warn(s"[SubmissionService] ensure submission-request datetime FAILED (suppressed) returnId=${ctx.returnId} corrId=$correlationId: ${e.getMessage}")
      ()
    }

  private def createSubmissionErrorDetails(ctx: SubmissionContext, errors: Seq[GovTalkError], correlationId: String)
                                          (implicit hc: HeaderCarrier): Future[Unit] =
    if errors.isEmpty then Future.unit
    else
      errors.foldLeft(Future.unit) { (acc, err) =>
        acc.flatMap { _ =>
          val req = CreateSubmissionErrorDetailRequest(
            storn                  = ctx.storn,
            returnResourceRef      = ctx.returnId,
            submissionErrorDetails = SubmissionErrorDetail(
              position     = err.location.getOrElse(""),
              errorMessage = err.text.getOrElse("")
            )
          )
          chrisService.createSubmissionErrorDetail(req).map(_ => ())
        }
      }

  private def setGovTalkProtocol(ctx: SubmissionContext, protocolStatus: String, correlationId: String)
                                (implicit hc: HeaderCarrier): Future[Unit] =
    val req = UpdateGovTalkStatusRequest(
      userIdentifier    = ctx.storn,
      formResultId      = ctx.returnId,
      endStateTimestamp = nowIso,
      protocolStatus    = protocolStatus
    )
    chrisService.updateGovTalkStatus(req).map { _ =>
      logger.debug(s"[SubmissionService] govtalk protocolStatus=$protocolStatus returnId=${ctx.returnId} corrId=$correlationId")
      ()
    }

  private def updateGovTalkStatistics(ctx: SubmissionContext,
                                      responseEndPoint: Option[String],
                                      pollIntervalSeconds: Option[Int],
                                      correlationId: String)
                                     (implicit hc: HeaderCarrier): Future[Unit] =
    val gatewayUrl = responseEndPoint.filter(_.nonEmpty).getOrElse(appConfig.baseUrl("chris"))
    val req = UpdateGovTalkStatisticsRequest(
      userIdentifier = ctx.storn,
      formResultId   = ctx.returnId,
      govTalkStatus  = GovTalkStatusStatistics(
        lastMessageTimestamp = nowIso,
        numberOfPolls        = "0",
        pollInterval         = pollIntervalSeconds.map(_.toString).getOrElse("0"),
        gatewayUrl           = gatewayUrl
      )
    )
    chrisService.updateGovTalkStatistics(req).map { _ =>
      logger.debug(s"[SubmissionService] govtalk statistics updated gatewayUrl=$gatewayUrl returnId=${ctx.returnId} corrId=$correlationId")
      ()
    }

  private def sendChrisDelete(ctx: SubmissionContext, endpoint: Option[String], correlationId: String)
                             (implicit hc: HeaderCarrier): Future[Unit] =
    connector.delete(endpoint, correlationId).map {
      case ChrisDeleteResponse.Deleted(_, _) =>
        logger.debug(s"[SubmissionService] ChRIS resource deleted returnId=${ctx.returnId} corrId=$correlationId")
        ()
      case ChrisDeleteResponse.NotFound(_, _) =>
        logger.debug(s"[SubmissionService] ChRIS resource already gone (2000) returnId=${ctx.returnId} corrId=$correlationId")
        ()
      case ChrisDeleteResponse.Errored(errors, _, _) =>
        logger.warn(s"[SubmissionService] ChRIS DELETE returned errors (suppressed) returnId=${ctx.returnId} corrId=$correlationId: ${errors.mkString("; ")}")
        ()
      case ChrisDeleteResponse.TransportError(msg, _) =>
        logger.warn(s"[SubmissionService] ChRIS DELETE transport error (suppressed) returnId=${ctx.returnId} corrId=$correlationId: $msg")
        ()
    }.recover { case e =>
      logger.warn(s"[SubmissionService] ChRIS DELETE failed (suppressed) returnId=${ctx.returnId} corrId=$correlationId: ${e.getMessage}")
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