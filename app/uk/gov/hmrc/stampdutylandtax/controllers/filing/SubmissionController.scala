/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.stampdutylandtax.controllers.filing

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction
import models.filing.FullReturn
import models.filing.*
import service.submission.*
import uk.gov.hmrc.auth.core.AffinityGroup

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class SubmissionController @Inject() (
                                       submissionService: SubmissionService,
                                       identify: IdentifierAction,
                                       cc: ControllerComponents
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with Logging:

  def submit(): Action[AnyContent] = identify.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    logger.info(s"[SubmissionController][submit] request received affinityGroup=${request.affinityGroup} hasCredential=${request.credentialId.nonEmpty} contentType=${request.contentType.getOrElse("-")}")

    request.body.asJson match
      case None =>
        logger.warn("[SubmissionController][submit] rejected: expected application/json body but none present")
        Future.successful(BadRequest(Json.obj("error" -> "Expected application/json body")))

      case Some(json) =>
        json.validate[SubmitRequest] match
          case JsError(errs) =>
            logger.warn(s"[SubmissionController][submit] SubmitRequest JSON parse failed: $errs")
            Future.successful(BadRequest(Json.obj("error" -> "Invalid submit payload", "details" -> JsError.toJson(JsError(errs)))))
          case JsSuccess(SubmitRequest(email, fullReturn), _) =>
            logger.info(s"[SubmissionController][submit] payload parsed OK returnId=${fullReturn.returnResourceRef.getOrElse("-")} storn=${fullReturn.stornId.getOrElse("-")} version=${fullReturn.returnInfo.flatMap(_.version).getOrElse("-")} hasEmail=${email.isDefined}")
            runSubmission(fullReturn, email, request.credentialId, request.affinityGroup)
  }

  private def runSubmission(fullReturn: FullReturn, email: Option[String], credentialId: String, affinityGroup: AffinityGroup)(implicit hc: HeaderCarrier): Future[Result] =
    val returnId  = fullReturn.returnResourceRef.getOrElse("")
    val sender    = resolveSenderType(affinityGroup)
    val periodEnd = resolvePeriodEnd(fullReturn)

    logger.info(s"[SubmissionController] runSubmission START returnId=$returnId sender=$sender periodEnd=$periodEnd affinityGroup=$affinityGroup hasCredential=${credentialId.nonEmpty}")

    submissionService.submit(fullReturn, sender, periodEnd, credentialId).map { chrisResponse =>
      logger.info(s"[SubmissionController] service returned returnId=$returnId response=${chrisResponse.getClass.getSimpleName}")
      toSubmissionResponse(returnId, chrisResponse) match
        case accepted: SubmissionResponse.Accepted =>
          logger.info(s"[SubmissionController] returnId=$returnId -> 200 OK ACCEPTED utrn=${accepted.utrn}")
          Ok(Json.toJson(accepted: SubmissionResponse))
        case rejected: SubmissionResponse.Rejected =>
          logger.warn(s"[SubmissionController] returnId=$returnId -> 400 REJECTED errorCount=${rejected.errors.size} codes=${rejected.errors.flatMap(_.code).mkString(",")}")
          BadRequest(Json.toJson(rejected: SubmissionResponse))
    }.recover {
      case e: SchemaValidationException =>
        val rejected: SubmissionResponse = SubmissionResponse.Rejected(
          returnId = returnId,
          errors   = e.validationErrors.map(msg => SubmissionError(code = None, message = msg))
        )
        logger.warn(s"[SubmissionController] returnId=$returnId -> 400 schema-validation rejected: ${e.validationErrors.mkString("; ")}")
        BadRequest(Json.toJson(rejected))

      case e: MissingSubmissionContextException =>
        logger.warn(s"[SubmissionController] returnId=$returnId -> 400 missing context: ${e.getMessage}")
        BadRequest(Json.obj("error" -> "Missing required submission context", "message" -> e.getMessage))

      case e: ReturnLockConflictException =>
        logger.warn(s"[SubmissionController] returnId=${e.returnId} -> 409 lock conflict (status=${e.status})")
        Conflict(Json.obj("error" -> "Return version conflict; refresh and retry", "message" -> e.getMessage))

      case e: GovTalkLockNotAcquiredException =>
        logger.error(s"[SubmissionController] returnId=$returnId -> 500 GovTalk lock not acquired", e)
        InternalServerError(Json.obj("error" -> "Could not acquire submission lock; please retry", "message" -> e.getMessage))

      case NonFatal(e) =>
        logger.error(s"[SubmissionController] returnId=$returnId -> 500 unexpected error (${e.getClass.getSimpleName}): ${e.getMessage}", e)
        InternalServerError(Json.obj("error" -> "Submission failed", "message" -> Option(e.getMessage).getOrElse("")))
    }

  private def toSubmissionResponse(returnId: String, resp: ChrisResponse): SubmissionResponse =
    resp match
      case ChrisResponse.Completed(Some(utrn), _, _, _, _) =>
        logger.info(s"[SubmissionController] returnId=$returnId mapping Completed(utrn=$utrn) -> Accepted")
        SubmissionResponse.Accepted(returnId, utrn)

      case ChrisResponse.Completed(None, _, _, _, _) =>
        logger.error(s"[SubmissionController] returnId=$returnId Completed with NO UTRN; treating as transport error (will surface as 500)")
        throw new RuntimeException("ChRIS returned success without an extractable UTRN")

      case ChrisResponse.Errored(govtalkErrors, _, _, _) =>
        logger.warn(s"[SubmissionController] returnId=$returnId mapping Errored(count=${govtalkErrors.size}, numbers=${govtalkErrors.flatMap(_.number).mkString(",")}) -> Rejected")
        SubmissionResponse.Rejected(
          returnId = returnId,
          errors   = govtalkErrors.map(e => SubmissionError(code = e.number, message = e.text.getOrElse("Unspecified error"), location = e.location))
        )

      case ChrisResponse.TransportError(msg, _) =>
        logger.error(s"[SubmissionController] returnId=$returnId ChRIS transport error (will surface as 500): $msg")
        throw new RuntimeException(s"ChRIS transport error: $msg")

  private def resolveSenderType(affinityGroup: AffinityGroup): SenderType =
    affinityGroup match
      case AffinityGroup.Organisation => SenderType.Company
      case AffinityGroup.Agent        => SenderType.Agent
      case AffinityGroup.Individual   => SenderType.Individual

  private def resolvePeriodEnd(fullReturn: FullReturn): LocalDate = LocalDate.now()