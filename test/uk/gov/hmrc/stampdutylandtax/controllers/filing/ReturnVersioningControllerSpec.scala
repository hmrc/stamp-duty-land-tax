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

import base.SpecBase
import models.filing.{ReturnVersionUpdateRequest, ReturnVersionUpdateReturn}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsNull, JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import service.filing.ReturnVersioningService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReturnVersioningControllerSpec extends SpecBase {

  "ReturnVersioningController" - {

    "POST /update-return-version (updateReturnVersion)" - {

      "return CREATED with return version update response when service returns successfully" in new BaseSetup {
        when(mockReturnVersioningService.updateReturnVersion(eqTo(testReturnVersionUpdateRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(testReturnVersionUpdateRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testReturnVersionUpdateReturn)
        verify(mockReturnVersioningService).updateReturnVersion(eqTo(testReturnVersionUpdateRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "currentVersion" -> "1.0"
        )
        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "currentVersion" -> "1.0"
        )
        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when currentVersion is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(testReturnVersionUpdateRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(testReturnVersionUpdateRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle newVersion false response" in new BaseSetup {
        when(mockReturnVersioningService.updateReturnVersion(eqTo(testReturnVersionUpdateRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(ReturnVersionUpdateReturn(newVersion = 1)))

        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(testReturnVersionUpdateRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "newVersion").as[Int] mustBe 1
      }

      "handle different version formats" in new BaseSetup {
        val request1: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "1.0")
        val request2: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "2.5.1")
        val request3: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "10")

        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result1: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle complex version strings" in new BaseSetup {
        val request1: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "1.0.0")
        val request2: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "2.5.1-beta")
        val request3: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "3.14.159")

        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result1: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different storn formats" in new BaseSetup {
        val request1: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(storn = "STORN12345")
        val request2: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(storn = "STORN-ABC-123")
        val request3: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(storn = "12345678")

        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result1: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different returnResourceRef formats" in new BaseSetup {
        val request1: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(returnResourceRef = "RRF-2024-001")
        val request2: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(returnResourceRef = "123456")
        val request3: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(returnResourceRef = "ABC-123-XYZ")

        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result1: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle version increment scenario" in new BaseSetup {
        val request1: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "1.0")
        val request2: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "1.1")
        val request3: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "2.0")

        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result1: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED

        verify(mockReturnVersioningService).updateReturnVersion(eqTo(request1))(any[HeaderCarrier])
        verify(mockReturnVersioningService).updateReturnVersion(eqTo(request2))(any[HeaderCarrier])
        verify(mockReturnVersioningService).updateReturnVersion(eqTo(request3))(any[HeaderCarrier])
      }

      "return BAD_REQUEST when field has invalid type" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> 123,
          "returnResourceRef" -> "RRF-2024-001",
          "currentVersion" -> "1.0"
        )
        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "handle empty string values" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "",
          "returnResourceRef" -> "RRF-2024-001",
          "currentVersion" -> "1.0"
        )
        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe CREATED
      }

      "handle null values in JSON" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> JsNull,
          "returnResourceRef" -> "RRF-2024-001",
          "currentVersion" -> "1.0"
        )
        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "handle timeout exception from service" in new BaseSetup {
        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new java.util.concurrent.TimeoutException("Request timeout")))

        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(testReturnVersionUpdateRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "verify service is called exactly once per request" in new BaseSetup {
        when(mockReturnVersioningService.updateReturnVersion(eqTo(testReturnVersionUpdateRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(testReturnVersionUpdateRequest)))

        status(result) mustBe CREATED
        verify(mockReturnVersioningService).updateReturnVersion(eqTo(testReturnVersionUpdateRequest))(any[HeaderCarrier])
      }

      "handle consecutive requests independently" in new BaseSetup {
        val request1: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "1.0")
        val request2: ReturnVersionUpdateRequest = testReturnVersionUpdateRequest.copy(currentVersion = "2.0")

        when(mockReturnVersioningService.updateReturnVersion(any[ReturnVersionUpdateRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testReturnVersionUpdateReturn))

        val result1: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnVersion()(fakeRequest.withBody(Json.toJson(request2)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED

        verify(mockReturnVersioningService).updateReturnVersion(eqTo(request1))(any[HeaderCarrier])
        verify(mockReturnVersioningService).updateReturnVersion(eqTo(request2))(any[HeaderCarrier])
      }
    }
  }

  private trait BaseSetup {
    val mockReturnVersioningService: ReturnVersioningService = mock[ReturnVersioningService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new ReturnVersioningController(cc, mockReturnVersioningService)

    val testReturnVersionUpdateRequest: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
      storn = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      currentVersion = "1.0"
    )

    val testReturnVersionUpdateReturn: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(
      newVersion = 2
    )
  }
}