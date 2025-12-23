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
import models.filing.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import service.filing.PurchaserReturnsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class PurchaserReturnsControllerSpec extends SpecBase {

  "PurchaserReturnsController" - {

    "POST /create-purchaser (createPurchaser)" - {

      "return CREATED with purchaser response when service returns successfully" in new BaseSetup {
        when(mockPurchaserReturnsService.createPurchaser(eqTo(testCreatePurchaserRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreatePurchaserReturn))

        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(testCreatePurchaserRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreatePurchaserReturn)
        verify(mockPurchaserReturnsService).createPurchaser(eqTo(testCreatePurchaserRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "isCompany" -> "NO",
          "isTrustee" -> "NO",
          "isConnectedToVendor" -> "NO",
          "isRepresentedByAgent" -> "YES",
          "address1" -> "Park Avenue"
        )
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "isCompany" -> "NO",
          "isTrustee" -> "NO",
          "isConnectedToVendor" -> "NO",
          "isRepresentedByAgent" -> "YES",
          "address1" -> "Park Avenue"
        )
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when isCompany is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "isTrustee" -> "NO",
          "isConnectedToVendor" -> "NO",
          "isRepresentedByAgent" -> "YES",
          "address1" -> "Park Avenue"
        )
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when isTrustee is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "isCompany" -> "NO",
          "isConnectedToVendor" -> "NO",
          "isRepresentedByAgent" -> "YES",
          "address1" -> "Park Avenue"
        )
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when isConnectedToVendor is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "isCompany" -> "NO",
          "isTrustee" -> "NO",
          "isRepresentedByAgent" -> "YES",
          "address1" -> "Park Avenue"
        )
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when isRepresentedByAgent is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "isCompany" -> "NO",
          "isTrustee" -> "NO",
          "isConnectedToVendor" -> "NO",
          "address1" -> "Park Avenue"
        )
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when address1 is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "isCompany" -> "NO",
          "isTrustee" -> "NO",
          "isConnectedToVendor" -> "NO",
          "isRepresentedByAgent" -> "YES"
        )
        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPurchaserReturnsService.createPurchaser(any[CreatePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(testCreatePurchaserRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockPurchaserReturnsService.createPurchaser(any[CreatePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(testCreatePurchaserRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: CreatePurchaserRequest = testCreatePurchaserRequest
        when(mockPurchaserReturnsService.createPurchaser(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreatePurchaserReturn))

        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockPurchaserReturnsService).createPurchaser(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: CreatePurchaserRequest = testCreatePurchaserRequestMinimal
        when(mockPurchaserReturnsService.createPurchaser(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreatePurchaserReturn))

        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockPurchaserReturnsService).createPurchaser(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle company purchaser request" in new BaseSetup {
        val companyRequest: CreatePurchaserRequest = testCreatePurchaserRequestCompany
        when(mockPurchaserReturnsService.createPurchaser(eqTo(companyRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreatePurchaserReturn))

        val result: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(companyRequest)))

        status(result) mustBe CREATED
        verify(mockPurchaserReturnsService).createPurchaser(eqTo(companyRequest))(any[HeaderCarrier])
      }

      "handle different flag combinations" in new BaseSetup {
        val trusteeRequest: CreatePurchaserRequest = testCreatePurchaserRequest.copy(isTrustee = "YES")
        val connectedRequest: CreatePurchaserRequest = testCreatePurchaserRequest.copy(isConnectedToVendor = "YES")
        val noAgentRequest: CreatePurchaserRequest = testCreatePurchaserRequest.copy(isRepresentedByAgent = "NO")

        when(mockPurchaserReturnsService.createPurchaser(any[CreatePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreatePurchaserReturn))

        val result1: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(trusteeRequest)))
        val result2: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(connectedRequest)))
        val result3: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(noAgentRequest)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different stornId formats" in new BaseSetup {
        val request1: CreatePurchaserRequest = testCreatePurchaserRequest.copy(stornId = "STORN12345")
        val request2: CreatePurchaserRequest = testCreatePurchaserRequest.copy(stornId = "STORN-ABC-123")
        val request3: CreatePurchaserRequest = testCreatePurchaserRequest.copy(stornId = "12345678")

        when(mockPurchaserReturnsService.createPurchaser(any[CreatePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreatePurchaserReturn))

        val result1: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.createPurchaser()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }

    "POST /update-purchaser (updatePurchaser)" - {

      "return CREATED with update response when service returns successfully" in new BaseSetup {
        when(mockPurchaserReturnsService.updatePurchaser(eqTo(testUpdatePurchaserRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdatePurchaserReturn))

        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.toJson(testUpdatePurchaserRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testUpdatePurchaserReturn)
        verify(mockPurchaserReturnsService).updatePurchaser(eqTo(testUpdatePurchaserRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "purchaserResourceRef" -> "PRF-001",
          "isCompany" -> "NO",
          "isTrustee" -> "NO",
          "isConnectedToVendor" -> "NO",
          "isRepresentedByAgent" -> "YES",
          "address1" -> "Park Avenue"
        )
        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when purchaserResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "isCompany" -> "NO",
          "isTrustee" -> "NO",
          "isConnectedToVendor" -> "NO",
          "isRepresentedByAgent" -> "YES",
          "address1" -> "Park Avenue"
        )
        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPurchaserReturnsService.updatePurchaser(any[UpdatePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.toJson(testUpdatePurchaserRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockPurchaserReturnsService.updatePurchaser(any[UpdatePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.toJson(testUpdatePurchaserRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: UpdatePurchaserRequest = testUpdatePurchaserRequest
        when(mockPurchaserReturnsService.updatePurchaser(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdatePurchaserReturn))

        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockPurchaserReturnsService).updatePurchaser(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: UpdatePurchaserRequest = testUpdatePurchaserRequestMinimal
        when(mockPurchaserReturnsService.updatePurchaser(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdatePurchaserReturn))

        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockPurchaserReturnsService).updatePurchaser(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle updated false response" in new BaseSetup {
        when(mockPurchaserReturnsService.updatePurchaser(eqTo(testUpdatePurchaserRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(UpdatePurchaserReturn(updated = false)))

        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.toJson(testUpdatePurchaserRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "updated").as[Boolean] mustBe false
      }

      "handle request with nextPurchaserId" in new BaseSetup {
        val requestWithNextPurchaser: UpdatePurchaserRequest = testUpdatePurchaserRequest.copy(nextPurchaserId = Some("PID-999"))
        when(mockPurchaserReturnsService.updatePurchaser(eqTo(requestWithNextPurchaser))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdatePurchaserReturn))

        val result: Future[Result] = controller.updatePurchaser()(fakeRequest.withBody(Json.toJson(requestWithNextPurchaser)))

        status(result) mustBe CREATED
        verify(mockPurchaserReturnsService).updatePurchaser(eqTo(requestWithNextPurchaser))(any[HeaderCarrier])
      }
    }

    "POST /delete-purchaser (deletePurchaser)" - {

      "return CREATED with delete response when service returns successfully" in new BaseSetup {
        when(mockPurchaserReturnsService.deletePurchaser(eqTo(testDeletePurchaserRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeletePurchaserReturn))

        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(testDeletePurchaserRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testDeletePurchaserReturn)
        verify(mockPurchaserReturnsService).deletePurchaser(eqTo(testDeletePurchaserRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "purchaserResourceRef" -> "PRF-001",
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when purchaserResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "purchaserResourceRef" -> "PRF-001"
        )
        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPurchaserReturnsService.deletePurchaser(any[DeletePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(testDeletePurchaserRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockPurchaserReturnsService.deletePurchaser(any[DeletePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(testDeletePurchaserRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle deleted false response" in new BaseSetup {
        when(mockPurchaserReturnsService.deletePurchaser(eqTo(testDeletePurchaserRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(DeletePurchaserReturn(deleted = false)))

        val result: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(testDeletePurchaserRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "deleted").as[Boolean] mustBe false
      }

      "handle different storn formats" in new BaseSetup {
        val request1: DeletePurchaserRequest = testDeletePurchaserRequest.copy(storn = "STORN12345")
        val request2: DeletePurchaserRequest = testDeletePurchaserRequest.copy(storn = "STORN-ABC-123")
        val request3: DeletePurchaserRequest = testDeletePurchaserRequest.copy(storn = "12345678")

        when(mockPurchaserReturnsService.deletePurchaser(any[DeletePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeletePurchaserReturn))

        val result1: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different purchaserResourceRef formats" in new BaseSetup {
        val request1: DeletePurchaserRequest = testDeletePurchaserRequest.copy(purchaserResourceRef = "PRF-001")
        val request2: DeletePurchaserRequest = testDeletePurchaserRequest.copy(purchaserResourceRef = "123456")
        val request3: DeletePurchaserRequest = testDeletePurchaserRequest.copy(purchaserResourceRef = "ABC-123-XYZ")

        when(mockPurchaserReturnsService.deletePurchaser(any[DeletePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeletePurchaserReturn))

        val result1: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different returnResourceRef formats" in new BaseSetup {
        val request1: DeletePurchaserRequest = testDeletePurchaserRequest.copy(returnResourceRef = "RRF-001")
        val request2: DeletePurchaserRequest = testDeletePurchaserRequest.copy(returnResourceRef = "RRF-ABC-123")
        val request3: DeletePurchaserRequest = testDeletePurchaserRequest.copy(returnResourceRef = "12345678")

        when(mockPurchaserReturnsService.deletePurchaser(any[DeletePurchaserRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeletePurchaserReturn))

        val result1: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deletePurchaser()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }

    "POST /create-company-details (createCompanyDetails)" - {

      "return CREATED with company details response when service returns successfully" in new BaseSetup {
        when(mockPurchaserReturnsService.createCompanyDetails(eqTo(testCreateCompanyDetailsRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateCompanyDetailsReturn))

        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.toJson(testCreateCompanyDetailsRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreateCompanyDetailsReturn)
        verify(mockPurchaserReturnsService).createCompanyDetails(eqTo(testCreateCompanyDetailsRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "purchaserResourceRef" -> "PRF-001"
        )
        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "purchaserResourceRef" -> "PRF-001"
        )
        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when purchaserResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPurchaserReturnsService.createCompanyDetails(any[CreateCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.toJson(testCreateCompanyDetailsRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockPurchaserReturnsService.createCompanyDetails(any[CreateCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.toJson(testCreateCompanyDetailsRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: CreateCompanyDetailsRequest = testCreateCompanyDetailsRequest
        when(mockPurchaserReturnsService.createCompanyDetails(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateCompanyDetailsReturn))

        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockPurchaserReturnsService).createCompanyDetails(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: CreateCompanyDetailsRequest = testCreateCompanyDetailsRequestMinimal
        when(mockPurchaserReturnsService.createCompanyDetails(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateCompanyDetailsReturn))

        val result: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockPurchaserReturnsService).createCompanyDetails(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle different company type combinations" in new BaseSetup {
        val propertyRequest: CreateCompanyDetailsRequest = testCreateCompanyDetailsRequest.copy(
          compTypeBank = Some("NO"),
          compTypeProperty = Some("YES")
        )
        val charityRequest: CreateCompanyDetailsRequest = testCreateCompanyDetailsRequest.copy(
          compTypeBank = Some("NO"),
          compTypeOcharity = Some("YES")
        )

        when(mockPurchaserReturnsService.createCompanyDetails(any[CreateCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateCompanyDetailsReturn))

        val result1: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.toJson(propertyRequest)))
        val result2: Future[Result] = controller.createCompanyDetails()(fakeRequest.withBody(Json.toJson(charityRequest)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
      }
    }

    "POST /update-company-details (updateCompanyDetails)" - {

      "return CREATED with update response when service returns successfully" in new BaseSetup {
        when(mockPurchaserReturnsService.updateCompanyDetails(eqTo(testUpdateCompanyDetailsRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateCompanyDetailsReturn))

        val result: Future[Result] = controller.updateCompanyDetails()(fakeRequest.withBody(Json.toJson(testUpdateCompanyDetailsRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testUpdateCompanyDetailsReturn)
        verify(mockPurchaserReturnsService).updateCompanyDetails(eqTo(testUpdateCompanyDetailsRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateCompanyDetails()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateCompanyDetails()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "purchaserResourceRef" -> "PRF-001"
        )
        val result: Future[Result] = controller.updateCompanyDetails()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPurchaserReturnsService.updateCompanyDetails(any[UpdateCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateCompanyDetails()(fakeRequest.withBody(Json.toJson(testUpdateCompanyDetailsRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockPurchaserReturnsService.updateCompanyDetails(any[UpdateCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateCompanyDetails()(fakeRequest.withBody(Json.toJson(testUpdateCompanyDetailsRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle updated false response" in new BaseSetup {
        when(mockPurchaserReturnsService.updateCompanyDetails(eqTo(testUpdateCompanyDetailsRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(UpdateCompanyDetailsReturn(updated = false)))

        val result: Future[Result] = controller.updateCompanyDetails()(fakeRequest.withBody(Json.toJson(testUpdateCompanyDetailsRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "updated").as[Boolean] mustBe false
      }
    }

    "POST /delete-company-details (deleteCompanyDetails)" - {

      "return CREATED with delete response when service returns successfully" in new BaseSetup {
        when(mockPurchaserReturnsService.deleteCompanyDetails(eqTo(testDeleteCompanyDetailsRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteCompanyDetailsReturn))

        val result: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(testDeleteCompanyDetailsRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testDeleteCompanyDetailsReturn)
        verify(mockPurchaserReturnsService).deleteCompanyDetails(eqTo(testDeleteCompanyDetailsRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345"
        )
        val result: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPurchaserReturnsService.deleteCompanyDetails(any[DeleteCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(testDeleteCompanyDetailsRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockPurchaserReturnsService.deleteCompanyDetails(any[DeleteCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(testDeleteCompanyDetailsRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle deleted false response" in new BaseSetup {
        when(mockPurchaserReturnsService.deleteCompanyDetails(eqTo(testDeleteCompanyDetailsRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(DeleteCompanyDetailsReturn(deleted = false)))

        val result: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(testDeleteCompanyDetailsRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "deleted").as[Boolean] mustBe false
      }

      "handle different storn formats" in new BaseSetup {
        val request1: DeleteCompanyDetailsRequest = testDeleteCompanyDetailsRequest.copy(storn = "STORN12345")
        val request2: DeleteCompanyDetailsRequest = testDeleteCompanyDetailsRequest.copy(storn = "STORN-ABC-123")
        val request3: DeleteCompanyDetailsRequest = testDeleteCompanyDetailsRequest.copy(storn = "12345678")

        when(mockPurchaserReturnsService.deleteCompanyDetails(any[DeleteCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteCompanyDetailsReturn))

        val result1: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different returnResourceRef formats" in new BaseSetup {
        val request1: DeleteCompanyDetailsRequest = testDeleteCompanyDetailsRequest.copy(returnResourceRef = "RRF-001")
        val request2: DeleteCompanyDetailsRequest = testDeleteCompanyDetailsRequest.copy(returnResourceRef = "123456")
        val request3: DeleteCompanyDetailsRequest = testDeleteCompanyDetailsRequest.copy(returnResourceRef = "ABC-123-XYZ")

        when(mockPurchaserReturnsService.deleteCompanyDetails(any[DeleteCompanyDetailsRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteCompanyDetailsReturn))

        val result1: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deleteCompanyDetails()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }
  }

  private trait BaseSetup {
    val mockPurchaserReturnsService: PurchaserReturnsService = mock[PurchaserReturnsService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new PurchaserReturnsController(cc, mockPurchaserReturnsService, fakeIdentifierAction)

    val testCreatePurchaserRequest: CreatePurchaserRequest = CreatePurchaserRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      isCompany = "NO",
      isTrustee = "NO",
      isConnectedToVendor = "NO",
      isRepresentedByAgent = "YES",
      title = Some("Mr"),
      surname = Some("Jones"),
      forename1 = Some("David"),
      forename2 = Some("Michael"),
      companyName = None,
      houseNumber = Some("25"),
      address1 = "Park Avenue",
      address2 = Some("Flat 3"),
      address3 = Some("Central District"),
      address4 = Some("London"),
      postcode = Some("SW1A 2AA"),
      phone = Some("02012345678"),
      nino = Some("AB123456C"),
      isUkCompany = None,
      hasNino = Some("YES"),
      dateOfBirth = Some("1980-01-15"),
      registrationNumber = None,
      placeOfRegistration = None
    )

    val testCreatePurchaserRequestMinimal: CreatePurchaserRequest = CreatePurchaserRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      isCompany = "NO",
      isTrustee = "NO",
      isConnectedToVendor = "NO",
      isRepresentedByAgent = "YES",
      title = None,
      surname = None,
      forename1 = None,
      forename2 = None,
      companyName = None,
      houseNumber = None,
      address1 = "Park Avenue",
      address2 = None,
      address3 = None,
      address4 = None,
      postcode = None,
      phone = None,
      nino = None,
      isUkCompany = None,
      hasNino = None,
      dateOfBirth = None,
      registrationNumber = None,
      placeOfRegistration = None
    )

    val testCreatePurchaserRequestCompany: CreatePurchaserRequest = CreatePurchaserRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      isCompany = "YES",
      isTrustee = "NO",
      isConnectedToVendor = "NO",
      isRepresentedByAgent = "YES",
      title = None,
      surname = None,
      forename1 = None,
      forename2 = None,
      companyName = Some("XYZ Properties Ltd"),
      houseNumber = Some("25"),
      address1 = "Park Avenue",
      address2 = Some("Flat 3"),
      address3 = Some("Central District"),
      address4 = Some("London"),
      postcode = Some("SW1A 2AA"),
      phone = Some("02012345678"),
      nino = None,
      isUkCompany = Some("YES"),
      hasNino = None,
      dateOfBirth = None,
      registrationNumber = Some("12345678"),
      placeOfRegistration = Some("England and Wales")
    )

    val testCreatePurchaserReturn: CreatePurchaserReturn = CreatePurchaserReturn(
      purchaserResourceRef = "PRF-001",
      purchaserId = "PID-001"
    )

    val testUpdatePurchaserRequest: UpdatePurchaserRequest = UpdatePurchaserRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      purchaserResourceRef = "PRF-001",
      isCompany = "NO",
      isTrustee = "NO",
      isConnectedToVendor = "NO",
      isRepresentedByAgent = "YES",
      title = Some("Mr"),
      surname = Some("Jones Updated"),
      forename1 = Some("David"),
      forename2 = Some("Michael"),
      companyName = None,
      houseNumber = Some("25"),
      address1 = "Park Avenue",
      address2 = Some("Flat 3"),
      address3 = Some("Central District"),
      address4 = Some("London"),
      postcode = Some("SW1A 2AA"),
      phone = Some("02012345678"),
      nino = Some("AB123456C"),
      nextPurchaserId = Some("PID-002"),
      isUkCompany = None,
      hasNino = Some("YES"),
      dateOfBirth = Some("1980-01-15"),
      registrationNumber = None,
      placeOfRegistration = None
    )

    val testUpdatePurchaserRequestMinimal: UpdatePurchaserRequest = UpdatePurchaserRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      purchaserResourceRef = "PRF-001",
      isCompany = "NO",
      isTrustee = "NO",
      isConnectedToVendor = "NO",
      isRepresentedByAgent = "YES",
      title = None,
      surname = None,
      forename1 = None,
      forename2 = None,
      companyName = None,
      houseNumber = None,
      address1 = "Park Avenue",
      address2 = None,
      address3 = None,
      address4 = None,
      postcode = None,
      phone = None,
      nino = None,
      nextPurchaserId = None,
      isUkCompany = None,
      hasNino = None,
      dateOfBirth = None,
      registrationNumber = None,
      placeOfRegistration = None
    )

    val testUpdatePurchaserReturn: UpdatePurchaserReturn = UpdatePurchaserReturn(
      updated = true
    )

    val testDeletePurchaserRequest: DeletePurchaserRequest = DeletePurchaserRequest(
      storn = "STORN12345",
      purchaserResourceRef = "PRF-001",
      returnResourceRef = "RRF-2024-001"
    )

    val testDeletePurchaserReturn: DeletePurchaserReturn = DeletePurchaserReturn(
      deleted = true
    )

    val testCreateCompanyDetailsRequest: CreateCompanyDetailsRequest = CreateCompanyDetailsRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      purchaserResourceRef = "PRF-001",
      utr = Some("1234567890"),
      vatReference = Some("GB123456789"),
      compTypeBank = Some("YES"),
      compTypeBuilder = Some("NO"),
      compTypeBuildsoc = Some("NO"),
      compTypeCentgov = Some("NO"),
      compTypeIndividual = Some("NO"),
      compTypeInsurance = Some("NO"),
      compTypeLocalauth = Some("NO"),
      compTypeOcharity = Some("NO"),
      compTypeOcompany = Some("NO"),
      compTypeOfinancial = Some("NO"),
      compTypePartship = Some("NO"),
      compTypeProperty = Some("NO"),
      compTypePubliccorp = Some("NO"),
      compTypeSoletrader = Some("NO"),
      compTypePenfund = Some("NO")
    )

    val testCreateCompanyDetailsRequestMinimal: CreateCompanyDetailsRequest = CreateCompanyDetailsRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      purchaserResourceRef = "PRF-001",
      utr = None,
      vatReference = None,
      compTypeBank = None,
      compTypeBuilder = None,
      compTypeBuildsoc = None,
      compTypeCentgov = None,
      compTypeIndividual = None,
      compTypeInsurance = None,
      compTypeLocalauth = None,
      compTypeOcharity = None,
      compTypeOcompany = None,
      compTypeOfinancial = None,
      compTypePartship = None,
      compTypeProperty = None,
      compTypePubliccorp = None,
      compTypeSoletrader = None,
      compTypePenfund = None
    )

    val testCreateCompanyDetailsReturn: CreateCompanyDetailsReturn = CreateCompanyDetailsReturn(
      companyDetailsId = "CID-001"
    )

    val testUpdateCompanyDetailsRequest: UpdateCompanyDetailsRequest = UpdateCompanyDetailsRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      purchaserResourceRef = "PRF-001",
      utr = Some("9876543210"),
      vatReference = Some("GB987654321"),
      compTypeBank = Some("NO"),
      compTypeBuilder = Some("YES"),
      compTypeBuildsoc = Some("NO"),
      compTypeCentgov = Some("NO"),
      compTypeIndividual = Some("NO"),
      compTypeInsurance = Some("NO"),
      compTypeLocalauth = Some("NO"),
      compTypeOcharity = Some("NO"),
      compTypeOcompany = Some("NO"),
      compTypeOfinancial = Some("NO"),
      compTypePartship = Some("NO"),
      compTypeProperty = Some("NO"),
      compTypePubliccorp = Some("NO"),
      compTypeSoletrader = Some("NO"),
      compTypePenfund = Some("NO")
    )

    val testUpdateCompanyDetailsReturn: UpdateCompanyDetailsReturn = UpdateCompanyDetailsReturn(
      updated = true
    )

    val testDeleteCompanyDetailsRequest: DeleteCompanyDetailsRequest = DeleteCompanyDetailsRequest(
      storn = "STORN12345",
      returnResourceRef = "RRF-2024-001"
    )

    val testDeleteCompanyDetailsReturn: DeleteCompanyDetailsReturn = DeleteCompanyDetailsReturn(
      deleted = true
    )
  }
}