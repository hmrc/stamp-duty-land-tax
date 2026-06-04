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
import service.filing.LeaseReturnsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class LeaseReturnsControllerSpec extends SpecBase {

  "LeaseReturnsController" - {

    "POST /create-lease (createLease)" - {

      "return CREATED with lease response when service returns successfully" in new BaseSetup {
        when(mockLeaseReturnsService.createLease(eqTo(testCreateLeaseRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateLeaseResponse))

        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.toJson(testCreateLeaseRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreateLeaseResponse)
        verify(mockLeaseReturnsService).createLease(eqTo(testCreateLeaseRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "100001",
          "lease" -> Json.obj()
        )
        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "lease" -> Json.obj()
        )
        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when lease is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "returnResourceRef" -> "100001"
        )
        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockLeaseReturnsService.createLease(any[CreateLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.toJson(testCreateLeaseRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockLeaseReturnsService.createLease(any[CreateLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.toJson(testCreateLeaseRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: CreateLeaseRequest = testCreateLeaseRequestComplete
        when(mockLeaseReturnsService.createLease(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateLeaseResponse))

        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockLeaseReturnsService).createLease(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with no optional fields populated" in new BaseSetup {
        val minimalRequest: CreateLeaseRequest = testCreateLeaseRequestMinimal
        when(mockLeaseReturnsService.createLease(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateLeaseResponse))

        val result: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockLeaseReturnsService).createLease(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle different lease types" in new BaseSetup {
        val request1: CreateLeaseRequest = testCreateLeaseRequest.copy(lease = testLeasePayload.copy(leaseType = Some("ASSIGNED")))
        val request2: CreateLeaseRequest = testCreateLeaseRequest.copy(lease = testLeasePayload.copy(leaseType = Some("GRANTED")))
        val request3: CreateLeaseRequest = testCreateLeaseRequest.copy(lease = testLeasePayload.copy(leaseType = Some("SURRENDERED")))

        when(mockLeaseReturnsService.createLease(any[CreateLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateLeaseResponse))

        val result1: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.createLease()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }

    "POST /update-lease (updateLease)" - {

      "return OK with update response when service returns successfully" in new BaseSetup {
        when(mockLeaseReturnsService.updateLease(eqTo(testUpdateLeaseRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateLeaseResponse))

        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.toJson(testUpdateLeaseRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testUpdateLeaseResponse)
        verify(mockLeaseReturnsService).updateLease(eqTo(testUpdateLeaseRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "100001",
          "lease" -> Json.obj()
        )
        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "lease" -> Json.obj()
        )
        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when lease is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "returnResourceRef" -> "100001"
        )
        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockLeaseReturnsService.updateLease(any[UpdateLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.toJson(testUpdateLeaseRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockLeaseReturnsService.updateLease(any[UpdateLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.toJson(testUpdateLeaseRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: UpdateLeaseRequest = testUpdateLeaseRequestComplete
        when(mockLeaseReturnsService.updateLease(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateLeaseResponse))

        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe OK
        verify(mockLeaseReturnsService).updateLease(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with no optional fields populated" in new BaseSetup {
        val minimalRequest: UpdateLeaseRequest = testUpdateLeaseRequestMinimal
        when(mockLeaseReturnsService.updateLease(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateLeaseResponse))

        val result: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe OK
        verify(mockLeaseReturnsService).updateLease(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle different lease types" in new BaseSetup {
        val request1: UpdateLeaseRequest = testUpdateLeaseRequest.copy(lease = testLeasePayload.copy(leaseType = Some("ASSIGNED")))
        val request2: UpdateLeaseRequest = testUpdateLeaseRequest.copy(lease = testLeasePayload.copy(leaseType = Some("GRANTED")))
        val request3: UpdateLeaseRequest = testUpdateLeaseRequest.copy(lease = testLeasePayload.copy(leaseType = Some("SURRENDERED")))

        when(mockLeaseReturnsService.updateLease(any[UpdateLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateLeaseResponse))

        val result1: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.updateLease()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe OK
        status(result2) mustBe OK
        status(result3) mustBe OK
      }
    }

    "POST /delete-lease (deleteLease)" - {

      "return OK with delete response when service returns successfully" in new BaseSetup {
        when(mockLeaseReturnsService.deleteLease(eqTo(testDeleteLeaseRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteLeaseResponse))

        val result: Future[Result] = controller.deleteLease()(fakeRequest.withBody(Json.toJson(testDeleteLeaseRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testDeleteLeaseResponse)
        verify(mockLeaseReturnsService).deleteLease(eqTo(testDeleteLeaseRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.deleteLease()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "100001"
        )
        val result: Future[Result] = controller.deleteLease()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN123456"
        )
        val result: Future[Result] = controller.deleteLease()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deleteLease()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockLeaseReturnsService.deleteLease(any[DeleteLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deleteLease()(fakeRequest.withBody(Json.toJson(testDeleteLeaseRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockLeaseReturnsService.deleteLease(any[DeleteLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.deleteLease()(fakeRequest.withBody(Json.toJson(testDeleteLeaseRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle different resource reference formats" in new BaseSetup {
        val request1: DeleteLeaseRequest = testDeleteLeaseRequest.copy(returnResourceRef = "100001")
        val request2: DeleteLeaseRequest = testDeleteLeaseRequest.copy(returnResourceRef = "999999")
        val request3: DeleteLeaseRequest = testDeleteLeaseRequest.copy(returnResourceRef = "RRF-2024-001")

        when(mockLeaseReturnsService.deleteLease(any[DeleteLeaseRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteLeaseResponse))

        val result1: Future[Result] = controller.deleteLease()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deleteLease()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deleteLease()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe OK
        status(result2) mustBe OK
        status(result3) mustBe OK
      }
    }
  }

  private trait BaseSetup {
    val mockLeaseReturnsService: LeaseReturnsService = mock[LeaseReturnsService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new LeaseReturnsController(cc, mockLeaseReturnsService, fakeIdentifierAction)

    val testLeasePayload: LeasePayload = LeasePayload(
      isAnnualRentOver1000 = Some("true"),
      contractEndDate = Some("2026-01-01"),
      contractStartDate = Some("2025-01-01"),
      leaseType = Some("GRANTED"),
      netPresentValue = Some("1000"),
      totalPremiumPayable = Some("500"),
      rentFreePeriod = Some("0"),
      startingRent = Some("100"),
      startingRentEndDate = Some("2025-12-31"),
      laterRentKnown = Some("false"),
      vatAmount = Some("20")
    )

    val testLeasePayloadMinimal: LeasePayload = LeasePayload(
      isAnnualRentOver1000 = None,
      contractEndDate = None,
      contractStartDate = None,
      leaseType = None,
      netPresentValue = None,
      totalPremiumPayable = None,
      rentFreePeriod = None,
      startingRent = None,
      startingRentEndDate = None,
      laterRentKnown = None,
      vatAmount = None
    )

    val testCreateLeaseRequest: CreateLeaseRequest = CreateLeaseRequest(
      stornId = "STORN123456",
      returnResourceRef = "100001",
      lease = testLeasePayload
    )

    val testCreateLeaseRequestComplete: CreateLeaseRequest = testCreateLeaseRequest

    val testCreateLeaseRequestMinimal: CreateLeaseRequest = CreateLeaseRequest(
      stornId = "STORN123456",
      returnResourceRef = "100001",
      lease = testLeasePayloadMinimal
    )

    val testCreateLeaseResponse: CreateLeaseReturn = CreateLeaseReturn(
      created = true
    )

    val testUpdateLeaseRequest: UpdateLeaseRequest = UpdateLeaseRequest(
      stornId = "STORN123456",
      returnResourceRef = "100001",
      lease = testLeasePayload
    )

    val testUpdateLeaseRequestComplete: UpdateLeaseRequest = testUpdateLeaseRequest

    val testUpdateLeaseRequestMinimal: UpdateLeaseRequest = UpdateLeaseRequest(
      stornId = "STORN123456",
      returnResourceRef = "100001",
      lease = testLeasePayloadMinimal
    )

    val testUpdateLeaseResponse: UpdateLeaseReturn = UpdateLeaseReturn(
      updated = true
    )

    val testDeleteLeaseRequest: DeleteLeaseRequest = DeleteLeaseRequest(
      storn = "STORN123456",
      returnResourceRef = "100001"
    )

    val testDeleteLeaseResponse: DeleteLeaseReturn = DeleteLeaseReturn(
      deleted = true
    )
  }
}