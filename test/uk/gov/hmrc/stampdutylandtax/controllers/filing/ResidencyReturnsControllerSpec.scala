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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import service.filing.ResidencyReturnsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ResidencyReturnsControllerSpec extends SpecBase {

  "ResidencyReturnsController" - {

    "POST /create-residency (createResidency)" - {

      "return CREATED with residency response when service returns successfully" in new BaseSetup {
        when(mockResidencyReturnsService.createResidency(eqTo(testCreateResidencyRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateResidencyReturn))

        val result: Future[Result] = controller.createResidency()(fakeRequest.withBody(Json.toJson(testCreateResidencyRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreateResidencyReturn)
        verify(mockResidencyReturnsService).createResidency(eqTo(testCreateResidencyRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createResidency()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createResidency()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "residency" -> Json.obj(
            "isNonUkResidents" -> "NO",
            "isCompany"        -> "NO",
            "isCrownRelief"    -> "NO"
          )
        )
        val result: Future[Result] = controller.createResidency()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "residency" -> Json.obj(
            "isNonUkResidents" -> "NO",
            "isCompany"        -> "NO",
            "isCrownRelief"    -> "NO"
          )
        )
        val result: Future[Result] = controller.createResidency()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when residency payload is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId"           -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.createResidency()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockResidencyReturnsService.createResidency(any[CreateResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createResidency()(fakeRequest.withBody(Json.toJson(testCreateResidencyRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockResidencyReturnsService.createResidency(any[CreateResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.createResidency()(fakeRequest.withBody(Json.toJson(testCreateResidencyRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle different residency flag combinations" in new BaseSetup {
        val nonUkRequest    = testCreateResidencyRequest.copy(residency = testResidencyPayload.copy(isNonUkResidents = "YES"))
        val companyRequest  = testCreateResidencyRequest.copy(residency = testResidencyPayload.copy(isCompany = "YES"))
        val crownRequest    = testCreateResidencyRequest.copy(residency = testResidencyPayload.copy(isCrownRelief = "YES"))

        when(mockResidencyReturnsService.createResidency(any[CreateResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateResidencyReturn))

        val result1: Future[Result] = controller.createResidency()(fakeRequest.withBody(Json.toJson(nonUkRequest)))
        val result2: Future[Result] = controller.createResidency()(fakeRequest.withBody(Json.toJson(companyRequest)))
        val result3: Future[Result] = controller.createResidency()(fakeRequest.withBody(Json.toJson(crownRequest)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }

    "POST /update-residency (updateResidency)" - {

      "return CREATED with update response when service returns successfully" in new BaseSetup {
        when(mockResidencyReturnsService.updateResidency(eqTo(testUpdateResidencyRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateResidencyReturn))

        val result: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.toJson(testUpdateResidencyRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testUpdateResidencyReturn)
        verify(mockResidencyReturnsService).updateResidency(eqTo(testUpdateResidencyRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "residency" -> Json.obj(
            "isNonUkResidents" -> "NO",
            "isCompany"        -> "NO",
            "isCrownRelief"    -> "NO"
          )
        )
        val result: Future[Result] = controller.updateResidency()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when residency payload is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId"           -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.updateResidency()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockResidencyReturnsService.updateResidency(any[UpdateResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.toJson(testUpdateResidencyRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockResidencyReturnsService.updateResidency(any[UpdateResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.toJson(testUpdateResidencyRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle updated false response" in new BaseSetup {
        when(mockResidencyReturnsService.updateResidency(eqTo(testUpdateResidencyRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(UpdateResidencyReturn(updated = false)))

        val result: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.toJson(testUpdateResidencyRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "updated").as[Boolean] mustBe false
      }

      "handle different residency flag combinations" in new BaseSetup {
        val nonUkRequest   = testUpdateResidencyRequest.copy(residency = testResidencyPayload.copy(isNonUkResidents = "YES"))
        val companyRequest = testUpdateResidencyRequest.copy(residency = testResidencyPayload.copy(isCompany = "YES"))
        val crownRequest   = testUpdateResidencyRequest.copy(residency = testResidencyPayload.copy(isCrownRelief = "YES"))

        when(mockResidencyReturnsService.updateResidency(any[UpdateResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateResidencyReturn))

        val result1: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.toJson(nonUkRequest)))
        val result2: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.toJson(companyRequest)))
        val result3: Future[Result] = controller.updateResidency()(fakeRequest.withBody(Json.toJson(crownRequest)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }

    "POST /delete-residency (deleteResidency)" - {

      "return CREATED with delete response when service returns successfully" in new BaseSetup {
        when(mockResidencyReturnsService.deleteResidency(eqTo(testDeleteResidencyRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteResidencyReturn))

        val result: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.toJson(testDeleteResidencyRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testDeleteResidencyReturn)
        verify(mockResidencyReturnsService).deleteResidency(eqTo(testDeleteResidencyRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345"
        )
        val result: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockResidencyReturnsService.deleteResidency(any[DeleteResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.toJson(testDeleteResidencyRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockResidencyReturnsService.deleteResidency(any[DeleteResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.toJson(testDeleteResidencyRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle deleted false response" in new BaseSetup {
        when(mockResidencyReturnsService.deleteResidency(eqTo(testDeleteResidencyRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(DeleteResidencyReturn(deleted = false)))

        val result: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.toJson(testDeleteResidencyRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "deleted").as[Boolean] mustBe false
      }

      "handle different storn formats" in new BaseSetup {
        val request1: DeleteResidencyRequest = testDeleteResidencyRequest.copy(storn = "STORN12345")
        val request2: DeleteResidencyRequest = testDeleteResidencyRequest.copy(storn = "STORN-ABC-123")
        val request3: DeleteResidencyRequest = testDeleteResidencyRequest.copy(storn = "12345678")

        when(mockResidencyReturnsService.deleteResidency(any[DeleteResidencyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteResidencyReturn))

        val result1: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deleteResidency()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }
  }

  private trait BaseSetup {
    val mockResidencyReturnsService: ResidencyReturnsService = mock[ResidencyReturnsService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new ResidencyReturnsController(cc, mockResidencyReturnsService, fakeIdentifierAction)

    val testResidencyPayload: ResidencyPayload = ResidencyPayload(
      isNonUkResidents = "NO",
      isCompany        = "NO",
      isCrownRelief    = "NO"
    )

    val testCreateResidencyRequest: CreateResidencyRequest = CreateResidencyRequest(
      stornId           = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      residency         = testResidencyPayload
    )

    val testCreateResidencyReturn: CreateResidencyReturn = CreateResidencyReturn(
      residencyResourceRef = "RRF-001",
      residencyId          = "RID-001"
    )

    val testUpdateResidencyRequest: UpdateResidencyRequest = UpdateResidencyRequest(
      stornId           = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      residency         = testResidencyPayload
    )

    val testUpdateResidencyReturn: UpdateResidencyReturn = UpdateResidencyReturn(
      updated = true
    )

    val testDeleteResidencyRequest: DeleteResidencyRequest = DeleteResidencyRequest(
      storn             = "STORN12345",
      returnResourceRef = "RRF-2024-001"
    )

    val testDeleteResidencyReturn: DeleteResidencyReturn = DeleteResidencyReturn(
      deleted = true
    )
  }
}