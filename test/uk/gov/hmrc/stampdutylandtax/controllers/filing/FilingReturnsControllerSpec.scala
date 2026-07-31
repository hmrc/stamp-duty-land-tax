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
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import service.filing.FilingReturnsService
import uk.gov.hmrc.http.HeaderCarrier
import models.purge.{DeleteReturnRequest, DeleteReturnResponse}

import scala.concurrent.{ExecutionContext, Future}

class FilingReturnsControllerSpec extends SpecBase {

  "FilingReturnsController" - {

    "POST /create-return (createReturn)" - {

      "return CREATED with return response when service returns successfully" in new BaseSetup {
        when(mockFilingReturnsService.createReturn(eqTo(testCreateReturnRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateReturnResponse))

        val result: Future[Result] = controller.createReturn()(fakeRequest.withBody(Json.toJson(testCreateReturnRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreateReturnResponse)
        verify(mockFilingReturnsService).createReturn(eqTo(testCreateReturnRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createReturn()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createReturn()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockFilingReturnsService.createReturn(any[CreateReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createReturn()(fakeRequest.withBody(Json.toJson(testCreateReturnRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockFilingReturnsService.createReturn(any[CreateReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.createReturn()(fakeRequest.withBody(Json.toJson(testCreateReturnRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: CreateReturnRequest = testCreateReturnRequest // Assume this has all optional fields populated
        when(mockFilingReturnsService.createReturn(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateReturnResponse))

        val result: Future[Result] = controller.createReturn()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockFilingReturnsService).createReturn(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: CreateReturnRequest = testCreateReturnRequestMinimal // Assume this has only required fields
        when(mockFilingReturnsService.createReturn(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateReturnResponse))

        val result: Future[Result] = controller.createReturn()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockFilingReturnsService).createReturn(eqTo(minimalRequest))(any[HeaderCarrier])
      }
    }

    "POST /get-full-return (getFullReturn)" - {

      "return CREATED with full return when service returns successfully" in new BaseSetup {
        when(mockFilingReturnsService.getFullReturn(eqTo(testGetReturnByRefRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testFullReturn))

        val result: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(testGetReturnByRefRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testFullReturn)
        verify(mockFilingReturnsService).getFullReturn(eqTo(testGetReturnByRefRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj("storn" -> "STORN123456")
        val result: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj("returnResourceRef" -> "RRF-2024-001")
        val result: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when both fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockFilingReturnsService.getFullReturn(any[GetReturnByRefRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(testGetReturnByRefRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockFilingReturnsService.getFullReturn(any[GetReturnByRefRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(testGetReturnByRefRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle different returnResourceRef formats" in new BaseSetup {
        val request1: GetReturnByRefRequest = GetReturnByRefRequest("123456", "STORN123456")
        val request2: GetReturnByRefRequest = GetReturnByRefRequest("RRF-2024-001", "STORN123456")
        val request3: GetReturnByRefRequest = GetReturnByRefRequest("ABC-123-XYZ", "STORN123456")

        when(mockFilingReturnsService.getFullReturn(any[GetReturnByRefRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testFullReturn))

        val result1: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different storn formats" in new BaseSetup {
        val request1: GetReturnByRefRequest = GetReturnByRefRequest("RRF-2024-001", "STORN123456")
        val request2: GetReturnByRefRequest = GetReturnByRefRequest("RRF-2024-001", "STORN-ABC-123")
        val request3: GetReturnByRefRequest = GetReturnByRefRequest("RRF-2024-001", "12345678")

        when(mockFilingReturnsService.getFullReturn(any[GetReturnByRefRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testFullReturn))

        val result1: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.getFullReturn()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }

    "POST /update-return (updateReturnInfo)" - {

      "return OK with update response when service returns successfully" in new BaseSetup {
        when(mockFilingReturnsService.updateReturnInfo(eqTo(testUpdateReturnRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnResponse))

        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(testUpdateReturnRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testUpdateReturnResponse)
        verify(mockFilingReturnsService).updateReturnInfo(eqTo(testUpdateReturnRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "100001",
          "mainPurchaserID" -> "1",
          "mainVendorID" -> "1",
          "mainLandID" -> "1",
          "IRMarkGenerated" -> "IRMark123456",
          "landCertForEachProp" -> "Y",
          "declaration" -> "Y"
        )
        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN123456",
          "mainPurchaserID" -> "1",
          "mainVendorID" -> "1",
          "mainLandID" -> "1",
          "IRMarkGenerated" -> "IRMark123456",
          "landCertForEachProp" -> "Y",
          "declaration" -> "Y"
        )
        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockFilingReturnsService.updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(testUpdateReturnRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockFilingReturnsService.updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(testUpdateReturnRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with Y values for boolean fields" in new BaseSetup {
        val request: UpdateReturnRequest = testUpdateReturnRequest.copy(
          landCertForEachProp = Some("Y"),
          declaration = Some("Y")
        )
        when(mockFilingReturnsService.updateReturnInfo(eqTo(request))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnResponse))

        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request)))

        status(result) mustBe OK
        verify(mockFilingReturnsService).updateReturnInfo(eqTo(request))(any[HeaderCarrier])
      }

      "handle valid payload with N values for boolean fields" in new BaseSetup {
        val request: UpdateReturnRequest = testUpdateReturnRequest.copy(
          landCertForEachProp = Some("N"),
          declaration = Some("N")
        )
        when(mockFilingReturnsService.updateReturnInfo(eqTo(request))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnResponse))

        val result: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request)))

        status(result) mustBe OK
        verify(mockFilingReturnsService).updateReturnInfo(eqTo(request))(any[HeaderCarrier])
      }

      "handle different entity IDs" in new BaseSetup {
        val request1: UpdateReturnRequest = testUpdateReturnRequest.copy(
          mainPurchaserID = Some("1"),
          mainVendorID = Some("1"),
          mainLandID = Some("1")
        )
        val request2: UpdateReturnRequest = testUpdateReturnRequest.copy(
          mainPurchaserID = Some("100"),
          mainVendorID = Some("200"),
          mainLandID = Some("300")
        )

        when(mockFilingReturnsService.updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnResponse))

        val result1: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request2)))

        status(result1) mustBe OK
        status(result2) mustBe OK
      }

      "handle different IRMark formats" in new BaseSetup {
        val request1: UpdateReturnRequest = testUpdateReturnRequest.copy(IRMarkGenerated = Some("IRMark123456"))
        val request2: UpdateReturnRequest = testUpdateReturnRequest.copy(IRMarkGenerated = Some("IRMark-ABC-123"))
        val request3: UpdateReturnRequest = testUpdateReturnRequest.copy(IRMarkGenerated = Some("12345678"))

        when(mockFilingReturnsService.updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnResponse))

        val result1: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe OK
        status(result2) mustBe OK
        status(result3) mustBe OK
      }

      "handle different return resource reference formats" in new BaseSetup {
        val request1: UpdateReturnRequest = testUpdateReturnRequest.copy(returnResourceRef = "100001")
        val request2: UpdateReturnRequest = testUpdateReturnRequest.copy(returnResourceRef = "RRF-2024-001")
        val request3: UpdateReturnRequest = testUpdateReturnRequest.copy(returnResourceRef = "999999")

        when(mockFilingReturnsService.updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnResponse))

        val result1: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.updateReturnInfo()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe OK
        status(result2) mustBe OK
        status(result3) mustBe OK
      }
    }

    "POST /delete-return (deleteReturn)" - {

      "return CREATED with delete response when service returns successfully" in new BaseSetup {
        when(mockFilingReturnsService.deleteReturn(eqTo(testDeleteReturnRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteReturnResponse))

        val result: Future[Result] = controller.deleteReturn()(fakeRequest.withBody(Json.toJson(testDeleteReturnRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testDeleteReturnResponse)
        verify(mockFilingReturnsService).deleteReturn(eqTo(testDeleteReturnRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.deleteReturn()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "[deleteReturn] Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj("storn" -> "STORN123456")
        val result: Future[Result] = controller.deleteReturn()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "[deleteReturn] Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj("returnResourceRef" -> "RRF-2024-001")
        val result: Future[Result] = controller.deleteReturn()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "[deleteReturn] Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deleteReturn()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "[deleteReturn] Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockFilingReturnsService.deleteReturn(any[DeleteReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deleteReturn()(fakeRequest.withBody(Json.toJson(testDeleteReturnRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockFilingReturnsService.deleteReturn(any[DeleteReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.deleteReturn()(fakeRequest.withBody(Json.toJson(testDeleteReturnRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }
  }

  private trait BaseSetup {
    val mockFilingReturnsService: FilingReturnsService = mock[FilingReturnsService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new FilingReturnsController(cc, mockFilingReturnsService, fakeIdentifierAction)


    val testUpdateReturnRequest: UpdateReturnRequest = UpdateReturnRequest(
      storn = "STORN123456",
      returnResourceRef = "100001",
      mainPurchaserID = Some("1"),
      mainVendorID = Some("1"),
      mainLandID = Some("1"),
      IRMarkGenerated = Some("IRMark123456"),
      landCertForEachProp = Some("Y"),
      declaration = Some("Y")
    )

    val testUpdateReturnResponse: UpdateReturnReturn = UpdateReturnReturn(
      updated = true
    )

    val testCreateReturnRequest: CreateReturnRequest = CreateReturnRequest(
      stornId = "STORN123456",
      purchaserIsCompany = "N",
      surNameOrCompanyName = "Smith",
      houseNumber = Some(42),
      addressLine1 = "High Street",
      addressLine2 = Some("Kensington"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      postcode = Some("SW1A 1AA"),
      transactionType = "RESIDENTIAL"
    )

    val testCreateReturnRequestMinimal: CreateReturnRequest = CreateReturnRequest(
      stornId = "STORN123456",
      purchaserIsCompany = "N",
      surNameOrCompanyName = "Smith",
      houseNumber = None,
      addressLine1 = "High Street",
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      postcode = None,
      transactionType = "RESIDENTIAL"
    )

    val testCreateReturnResponse: CreateReturnResult = CreateReturnResult(
      returnResourceRef = "RRF-2024-001"
    )

    val testGetReturnByRefRequest: GetReturnByRefRequest = GetReturnByRefRequest(
      returnResourceRef = "RRF-2024-001",
      storn = "STORN123456"
    )

    val testFullReturn: FullReturn = FullReturn(
      stornId = Some("STORN123456"),
      returnResourceRef = Some("RRF-2024-001")
    )
    
    val testDeleteReturnRequest: DeleteReturnRequest = DeleteReturnRequest(
      storn = "STORN123456",
      returnResourceRef = "RRF-2024-001"
    )

    val testDeleteReturnResponse: DeleteReturnResponse = DeleteReturnResponse(
      deleted = true
    )
    
  }
}