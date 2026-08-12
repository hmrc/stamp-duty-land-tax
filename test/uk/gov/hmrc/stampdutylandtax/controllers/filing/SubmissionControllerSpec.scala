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

package uk.gov.hmrc.stampdutylandtax.controllers.filing

import base.SpecBase
import models.filing.*
import models.auth.IdentifierRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{ACCEPTED, BAD_GATEWAY, BAD_REQUEST, CONFLICT, INTERNAL_SERVER_ERROR, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, AnyContentAsJson, BodyParser, Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction

import java.time.LocalDate
import play.api.test.Helpers.{contentAsJson, status}
import service.submission.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.stampdutylandtax.service.submission.SdltReturnFixtures.*

import scala.concurrent.{ExecutionContext, Future}

class SubmissionControllerSpec extends SpecBase {

  "SubmissionController" - {

    "POST /submit (submit)" - {

      "return 200 SUBMITTED (receipt) when the outcome is SUBMITTED with a UTRN" in new BaseSetup {
        onOutcome(UniversalStatus.SUBMITTED, Some(utrn))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe OK
        (contentAsJson(result) \ "utrn").as[String]     mustBe utrn
        (contentAsJson(result) \ "receipt").as[Boolean] mustBe true
        (contentAsJson(result) \ "_type").as[String]    mustBe "submitted"
        verify(mockSubmissionService).submit(any, any, any, any, any)(any[HeaderCarrier])
      }

      "return 200 SUBMITTED (no receipt) when the outcome is SUBMITTED_NO_RECEIPT with a UTRN" in new BaseSetup {
        onOutcome(UniversalStatus.SUBMITTED_NO_RECEIPT, Some(utrn))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe OK
        (contentAsJson(result) \ "utrn").as[String]     mustBe utrn
        (contentAsJson(result) \ "receipt").as[Boolean] mustBe false
        (contentAsJson(result) \ "_type").as[String]    mustBe "submitted"
      }

      "return 202 ACKNOWLEDGED when the outcome is ACCEPTED" in new BaseSetup {
        onOutcome(UniversalStatus.ACCEPTED)

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe ACCEPTED
        (contentAsJson(result) \ "_type").as[String] mustBe "acknowledged"
      }

      "return 503 RETRYABLE when the outcome is STARTED (recoverable)" in new BaseSetup {
        onOutcome(UniversalStatus.STARTED)

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe SERVICE_UNAVAILABLE
        (contentAsJson(result) \ "_type").as[String] mustBe "retryable"
      }

      "return 400 REJECTED when the outcome is a DEPARTMENTAL_ERROR (business validation)" in new BaseSetup {
        onOutcome(UniversalStatus.DEPARTMENTAL_ERROR, errs = Seq(govTalkError))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "errors").isDefined mustBe true
        (contentAsJson(result) \ "_type").as[String] mustBe "rejected"
      }

      "return 502 FAILED when the outcome is a FATAL_ERROR" in new BaseSetup {
        onOutcome(UniversalStatus.FATAL_ERROR, errs = Seq(govTalkError))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "errors").isDefined mustBe true
        (contentAsJson(result) \ "_type").as[String] mustBe "failed"
      }

      "return 502 FAILED when the outcome is SUBMITTED but carries no UTRN (AF11)" in new BaseSetup {
        onOutcome(UniversalStatus.SUBMITTED, utrnOpt = None)

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "errors").isDefined mustBe true
        (contentAsJson(result) \ "_type").as[String] mustBe "failed"
      }

      "return BAD_REQUEST when the JSON body is not a valid SubmitRequest" in new BaseSetup {
        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(Json.arr())))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "error").as[String] mustBe "Invalid submit payload"
        (contentAsJson(result) \ "details").isDefined mustBe true
      }

      "return BAD_REQUEST when the request has no JSON body" in new BaseSetup {
        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsEmpty))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "error").as[String] mustBe "Expected application/json body"
      }

      "return 200 SUBMITTED when the body has a fullReturn but no email" in new BaseSetup {
        onOutcome(UniversalStatus.SUBMITTED, Some(utrn))

        val bodyWithoutEmail: JsValue = Json.obj("fullReturn" -> Json.toJson(fullReturn))
        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(bodyWithoutEmail)))

        status(result) mustBe OK
        (contentAsJson(result) \ "utrn").as[String] mustBe utrn
      }

      "return 500 when the submission service fails unexpectedly" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("kaboom")))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "error").as[String] mustBe "Submission failed"
      }

      "return BAD_REQUEST with a Rejected response when the service raises a schema validation error" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.failed(SchemaValidationException(Seq("field X is invalid", "field Y is missing"))))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when the service raises a missing-context error" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.failed(MissingSubmissionContextException("no STORN in context")))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "error").as[String]   mustBe "Missing required submission context"
        (contentAsJson(result) \ "message").as[String] mustBe "no STORN in context"
      }

      "return CONFLICT when the service raises a return lock conflict" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.failed(ReturnLockConflictException("100001", CONFLICT, "Return 100001 is locked")))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe CONFLICT
        (contentAsJson(result) \ "error").as[String] mustBe "Return version conflict; refresh and retry"
      }

      "return 500 when the service cannot acquire the GovTalk lock" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.failed(GovTalkLockNotAcquiredException("100001", new RuntimeException("lock not acquired"))))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "error").as[String] mustBe "Could not acquire submission lock; please retry"
      }

      "send Other as the sender and 2004-03-01 as the period end to the service" in new BaseSetup {
        onOutcome(UniversalStatus.SUBMITTED, Some(utrn))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe OK
        verify(mockSubmissionService).submit(any, eqTo(SenderType.Other), eqTo(LocalDate.of(2004, 3, 1)), any, any)(any[HeaderCarrier])
      }

      "send Agent as the sender to the service when the filer is an agent" in new BaseSetup {
        onOutcome(UniversalStatus.SUBMITTED, Some(utrn))

        val result: Future[Result] = agentController.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe OK
        verify(mockSubmissionService).submit(any, eqTo(SenderType.Agent), any, any, any)(any[HeaderCarrier])
      }
    }
  }

  private trait BaseSetup {
    val mockSubmissionService: SubmissionService = mock[SubmissionService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new SubmissionController(mockSubmissionService, fakeIdentifierAction, cc)

    val agentIdentifierAction: IdentifierAction = new IdentifierAction {
      override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
        block(IdentifierRequest(request, "test-credential-id", AffinityGroup.Agent))
      override def parser: BodyParser[AnyContent] = fakeIdentifierAction.parser
      override protected def executionContext: ExecutionContext = ec
    }
    val agentController = new SubmissionController(mockSubmissionService, agentIdentifierAction, cc)

    val utrn     = "123456789MA"
    val corrId   = "CORR-1"
    val returnId = "100001"

    val govTalkError: GovTalkError =
      GovTalkError(raisedBy = "HMRC", number = Some("1001"), errorType = "business", text = Some("Invalid STORN"), location = None)

    def onOutcome(status: UniversalStatus, utrnOpt: Option[String] = None, errs: Seq[GovTalkError] = Nil): Unit =
      when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
        .thenReturn(Future.successful(SubmissionOutcome(returnId, status, utrnOpt, errs)))

    val fullReturn: FullReturn = freeholdReturn(1, 1, 1)
    val validBody: JsValue = Json.obj(
      "email"      -> "filer@example.com",
      "fullReturn" -> Json.toJson(fullReturn)
    )
  }
}