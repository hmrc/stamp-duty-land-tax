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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{BAD_REQUEST, CONFLICT, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson, Result}
import play.api.test.Helpers.{contentAsJson, status}
import service.submission.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.stampdutylandtax.service.submission.SdltReturnFixtures.*

import scala.concurrent.{ExecutionContext, Future}

class SubmissionControllerSpec extends SpecBase {

  "SubmissionController" - {

    "POST /submit (submit)" - {

      "return OK with an Accepted response when ChRIS completes with a UTRN" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.successful(ChrisResponse.Completed(Some(utrn), None, Some(corrId), None, "<x/>")))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe OK
        (contentAsJson(result) \ "utrn").as[String] mustBe utrn
        verify(mockSubmissionService).submit(any, any, any, any, any)(any[HeaderCarrier])
      }

      "return BAD_REQUEST with a Rejected response when ChRIS returns errors" in new BaseSetup {
        val govTalkError: GovTalkError =
          GovTalkError(raisedBy = "HMRC", number = Some("1001"), errorType = "business", text = Some("Invalid STORN"), location = None)

        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.successful(ChrisResponse.Errored(Seq(govTalkError), Some(corrId), None, "<x/>")))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return 500 when ChRIS completes without an extractable UTRN" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.successful(ChrisResponse.Completed(None, None, Some(corrId), None, "<x/>")))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "error").as[String] mustBe "Submission failed"
      }

      "return 500 when ChRIS returns a transport error" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.successful(ChrisResponse.TransportError("connection reset", "<x/>")))

        val result: Future[Result] = controller.submit()(fakeRequest.withBody(AnyContentAsJson(validBody)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "error").as[String] mustBe "Submission failed"
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

      "return OK when the body has a fullReturn but no email" in new BaseSetup {
        when(mockSubmissionService.submit(any, any, any, any, any)(any[HeaderCarrier]))
          .thenReturn(Future.successful(ChrisResponse.Completed(Some(utrn), None, Some(corrId), None, "<x/>")))

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
    }
  }

  private trait BaseSetup {
    val mockSubmissionService: SubmissionService = mock[SubmissionService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new SubmissionController(mockSubmissionService, fakeIdentifierAction, cc)

    val utrn   = "123456789MA"
    val corrId = "CORR-1"

    val fullReturn: FullReturn = freeholdReturn(1, 1, 1)
    val validBody: JsValue = Json.obj(
      "email"      -> "filer@example.com",
      "fullReturn" -> Json.toJson(fullReturn)
    )
  }
}