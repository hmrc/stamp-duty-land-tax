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
import service.filing.VendorReturnsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class VendorReturnsControllerSpec extends SpecBase {

  "VendorReturnsController" - {

    "POST /create-vendor (createVendor)" - {

      "return CREATED with vendor response when service returns successfully" in new BaseSetup {
        when(mockVendorReturnsService.createVendor(eqTo(testCreateVendorRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateVendorReturn))

        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(testCreateVendorRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreateVendorReturn)
        verify(mockVendorReturnsService).createVendor(eqTo(testCreateVendorRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "name" -> "Smith",
          "addressLine1" -> "Main Street",
          "isRepresentedByAgent" -> "YES"
        )
        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "name" -> "Smith",
          "addressLine1" -> "Main Street",
          "isRepresentedByAgent" -> "YES"
        )
        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when name is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "addressLine1" -> "Main Street",
          "isRepresentedByAgent" -> "YES"
        )
        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when addressLine1 is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "name" -> "Smith",
          "isRepresentedByAgent" -> "YES"
        )
        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when isRepresentedByAgent is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "name" -> "Smith",
          "addressLine1" -> "Main Street"
        )
        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockVendorReturnsService.createVendor(any[CreateVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(testCreateVendorRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockVendorReturnsService.createVendor(any[CreateVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(testCreateVendorRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: CreateVendorRequest = testCreateVendorRequest
        when(mockVendorReturnsService.createVendor(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateVendorReturn))

        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockVendorReturnsService).createVendor(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: CreateVendorRequest = testCreateVendorRequestMinimal
        when(mockVendorReturnsService.createVendor(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateVendorReturn))

        val result: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockVendorReturnsService).createVendor(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle different isRepresentedByAgent values" in new BaseSetup {
        val yesRequest: CreateVendorRequest = testCreateVendorRequest.copy(isRepresentedByAgent = "YES")
        val noRequest: CreateVendorRequest = testCreateVendorRequest.copy(isRepresentedByAgent = "NO")

        when(mockVendorReturnsService.createVendor(any[CreateVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateVendorReturn))

        val result1: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(yesRequest)))
        val result2: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(noRequest)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
      }

      "handle different stornId formats" in new BaseSetup {
        val request1: CreateVendorRequest = testCreateVendorRequest.copy(stornId = "STORN12345")
        val request2: CreateVendorRequest = testCreateVendorRequest.copy(stornId = "STORN-ABC-123")
        val request3: CreateVendorRequest = testCreateVendorRequest.copy(stornId = "12345678")

        when(mockVendorReturnsService.createVendor(any[CreateVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateVendorReturn))

        val result1: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.createVendor()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }

    "POST /update-vendor (updateVendor)" - {

      "return CREATED with update response when service returns successfully" in new BaseSetup {
        when(mockVendorReturnsService.updateVendor(eqTo(testUpdateVendorRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateVendorReturn))

        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.toJson(testUpdateVendorRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testUpdateVendorReturn)
        verify(mockVendorReturnsService).updateVendor(eqTo(testUpdateVendorRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "name" -> "Smith Updated",
          "addressLine1" -> "Main Street",
          "isRepresentedByAgent" -> "YES",
          "vendorResourceRef" -> "VRF-001"
        )
        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when vendorResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "name" -> "Smith Updated",
          "addressLine1" -> "Main Street",
          "isRepresentedByAgent" -> "YES"
        )
        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockVendorReturnsService.updateVendor(any[UpdateVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.toJson(testUpdateVendorRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockVendorReturnsService.updateVendor(any[UpdateVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.toJson(testUpdateVendorRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: UpdateVendorRequest = testUpdateVendorRequest
        when(mockVendorReturnsService.updateVendor(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateVendorReturn))

        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockVendorReturnsService).updateVendor(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: UpdateVendorRequest = testUpdateVendorRequestMinimal
        when(mockVendorReturnsService.updateVendor(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateVendorReturn))

        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockVendorReturnsService).updateVendor(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle updated false response" in new BaseSetup {
        when(mockVendorReturnsService.updateVendor(eqTo(testUpdateVendorRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(UpdateVendorReturn(updated = false)))

        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.toJson(testUpdateVendorRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "updated").as[Boolean] mustBe false
      }

      "handle request with nextVendorId" in new BaseSetup {
        val requestWithNextVendor: UpdateVendorRequest = testUpdateVendorRequest.copy(nextVendorId = Some("VID-999"))
        when(mockVendorReturnsService.updateVendor(eqTo(requestWithNextVendor))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateVendorReturn))

        val result: Future[Result] = controller.updateVendor()(fakeRequest.withBody(Json.toJson(requestWithNextVendor)))

        status(result) mustBe CREATED
        verify(mockVendorReturnsService).updateVendor(eqTo(requestWithNextVendor))(any[HeaderCarrier])
      }
    }

    "POST /delete-vendor (deleteVendor)" - {

      "return CREATED with delete response when service returns successfully" in new BaseSetup {
        when(mockVendorReturnsService.deleteVendor(eqTo(testDeleteVendorRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteVendorReturn))

        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(testDeleteVendorRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testDeleteVendorReturn)
        verify(mockVendorReturnsService).deleteVendor(eqTo(testDeleteVendorRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "vendorResourceRef" -> "VRF-001",
          "vendorId" -> "VID-001"
        )
        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when vendorResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "vendorId" -> "VID-001"
        )
        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when vendorId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "vendorResourceRef" -> "VRF-001"
        )
        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockVendorReturnsService.deleteVendor(any[DeleteVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(testDeleteVendorRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockVendorReturnsService.deleteVendor(any[DeleteVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(testDeleteVendorRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle deleted false response" in new BaseSetup {
        when(mockVendorReturnsService.deleteVendor(eqTo(testDeleteVendorRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(DeleteVendorReturn(deleted = false)))

        val result: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(testDeleteVendorRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "deleted").as[Boolean] mustBe false
      }

      "handle different storn formats" in new BaseSetup {
        val request1: DeleteVendorRequest = testDeleteVendorRequest.copy(storn = "STORN12345")
        val request2: DeleteVendorRequest = testDeleteVendorRequest.copy(storn = "STORN-ABC-123")
        val request3: DeleteVendorRequest = testDeleteVendorRequest.copy(storn = "12345678")

        when(mockVendorReturnsService.deleteVendor(any[DeleteVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteVendorReturn))

        val result1: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different vendorResourceRef formats" in new BaseSetup {
        val request1: DeleteVendorRequest = testDeleteVendorRequest.copy(vendorResourceRef = "VRF-001")
        val request2: DeleteVendorRequest = testDeleteVendorRequest.copy(vendorResourceRef = "123456")
        val request3: DeleteVendorRequest = testDeleteVendorRequest.copy(vendorResourceRef = "ABC-123-XYZ")

        when(mockVendorReturnsService.deleteVendor(any[DeleteVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteVendorReturn))

        val result1: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different returnResourceRef formats" in new BaseSetup {
        val request1: DeleteVendorRequest = testDeleteVendorRequest.copy(returnResourceRef = "VID-001")
        val request2: DeleteVendorRequest = testDeleteVendorRequest.copy(returnResourceRef = "VID-ABC-123")
        val request3: DeleteVendorRequest = testDeleteVendorRequest.copy(returnResourceRef = "12345678")

        when(mockVendorReturnsService.deleteVendor(any[DeleteVendorRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteVendorReturn))

        val result1: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deleteVendor()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }
  }

  private trait BaseSetup {
    val mockVendorReturnsService: VendorReturnsService = mock[VendorReturnsService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new VendorReturnsController(cc, mockVendorReturnsService)

    val testCreateVendorRequest: CreateVendorRequest = CreateVendorRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      title = Some("Mr"),
      forename1 = Some("John"),
      forename2 = Some("Paul"),
      name = "Smith",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Apartment 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = Some("TE23 5TT"),
      isRepresentedByAgent = "YES"
    )

    val testCreateVendorRequestMinimal: CreateVendorRequest = CreateVendorRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      title = None,
      forename1 = None,
      forename2 = None,
      name = "Smith",
      houseNumber = None,
      addressLine1 = "Main Street",
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      postcode = None,
      isRepresentedByAgent = "YES"
    )

    val testCreateVendorReturn: CreateVendorReturn = CreateVendorReturn(
      vendorResourceRef = "VRF-001",
      vendorId = "VID-001"
    )

    val testUpdateVendorRequest: UpdateVendorRequest = UpdateVendorRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      title = Some("Mr"),
      forename1 = Some("John"),
      forename2 = Some("Paul"),
      name = "Smith Updated",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Apartment 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = Some("TE23 5TT"),
      isRepresentedByAgent = "YES",
      vendorResourceRef = "VRF-001",
      nextVendorId = Some("VID-002")
    )

    val testUpdateVendorRequestMinimal: UpdateVendorRequest = UpdateVendorRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      title = None,
      forename1 = None,
      forename2 = None,
      name = "Smith",
      houseNumber = None,
      addressLine1 = "Main Street",
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      postcode = None,
      isRepresentedByAgent = "YES",
      vendorResourceRef = "VRF-001",
      nextVendorId = None
    )

    val testUpdateVendorReturn: UpdateVendorReturn = UpdateVendorReturn(
      updated = true
    )

    val testDeleteVendorRequest: DeleteVendorRequest = DeleteVendorRequest(
      storn = "STORN12345",
      vendorResourceRef = "VRF-001",
      returnResourceRef = "VID-001"
    )

    val testDeleteVendorReturn: DeleteVendorReturn = DeleteVendorReturn(
      deleted = true
    )
  }
}