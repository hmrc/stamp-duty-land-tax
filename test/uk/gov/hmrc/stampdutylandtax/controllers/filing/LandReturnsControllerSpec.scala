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
import service.filing.FilingLandService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class LandReturnsControllerSpec extends SpecBase {

  "LandReturnsController" - {

    "POST /create-land (createLand)" - {

      "return CREATED with land response when service returns successfully" in new BaseSetup {
        when(
          mockFilingLandService.createLand(eqTo(testCreateLandRequest))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testCreateLandResponse))

        val result: Future[Result] = controller.createLand()(
          fakeRequest.withBody(Json.toJson(testCreateLandRequest))
        )

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreateLandResponse)
        verify(mockFilingLandService).createLand(eqTo(testCreateLandRequest))(
          any[HeaderCarrier]
        )
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createLand()(
          fakeRequest.withBody(Json.obj("invalid" -> "data"))
        )

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "100001",
          "propertyType" -> "RESIDENTIAL",
          "interestTransferredCreated" -> "FREEHOLD",
          "addressLine1" -> "High Street"
        )
        val result: Future[Result] =
          controller.createLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "propertyType" -> "RESIDENTIAL",
          "interestTransferredCreated" -> "FREEHOLD",
          "addressLine1" -> "High Street"
        )
        val result: Future[Result] =
          controller.createLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when propertyType is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "returnResourceRef" -> "100001",
          "interestTransferredCreated" -> "FREEHOLD",
          "addressLine1" -> "High Street"
        )
        val result: Future[Result] =
          controller.createLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when interestTransferredCreated is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "returnResourceRef" -> "100001",
          "propertyType" -> "RESIDENTIAL",
          "addressLine1" -> "High Street"
        )
        val result: Future[Result] =
          controller.createLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when addressLine1 is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "returnResourceRef" -> "100001",
          "propertyType" -> "RESIDENTIAL",
          "interestTransferredCreated" -> "FREEHOLD"
        )
        val result: Future[Result] =
          controller.createLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] =
          controller.createLand()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(
          mockFilingLandService.createLand(any[CreateLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createLand()(
          fakeRequest.withBody(Json.toJson(testCreateLandRequest))
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(
          mockFilingLandService.createLand(any[CreateLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.createLand()(
          fakeRequest.withBody(Json.toJson(testCreateLandRequest))
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: CreateLandRequest = testCreateLandRequestComplete
        when(
          mockFilingLandService.createLand(eqTo(completeRequest))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testCreateLandResponse))

        val result: Future[Result] = controller.createLand()(
          fakeRequest.withBody(Json.toJson(completeRequest))
        )

        status(result) mustBe CREATED
        verify(mockFilingLandService).createLand(eqTo(completeRequest))(
          any[HeaderCarrier]
        )
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: CreateLandRequest = testCreateLandRequestMinimal
        when(
          mockFilingLandService.createLand(eqTo(minimalRequest))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testCreateLandResponse))

        val result: Future[Result] = controller.createLand()(
          fakeRequest.withBody(Json.toJson(minimalRequest))
        )

        status(result) mustBe CREATED
        verify(mockFilingLandService).createLand(eqTo(minimalRequest))(
          any[HeaderCarrier]
        )
      }

      "handle different property types" in new BaseSetup {
        val request1: CreateLandRequest =
          testCreateLandRequest.copy(propertyType = "RESIDENTIAL")
        val request2: CreateLandRequest =
          testCreateLandRequest.copy(propertyType = "NON_RESIDENTIAL")
        val request3: CreateLandRequest =
          testCreateLandRequest.copy(propertyType = "MIXED")

        when(
          mockFilingLandService.createLand(any[CreateLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testCreateLandResponse))

        val result1: Future[Result] =
          controller.createLand()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] =
          controller.createLand()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] =
          controller.createLand()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }

      "handle different interest types" in new BaseSetup {
        val request1: CreateLandRequest =
          testCreateLandRequest.copy(interestTransferredCreated = "FREEHOLD")
        val request2: CreateLandRequest =
          testCreateLandRequest.copy(interestTransferredCreated = "LEASEHOLD")

        when(
          mockFilingLandService.createLand(any[CreateLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testCreateLandResponse))

        val result1: Future[Result] =
          controller.createLand()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] =
          controller.createLand()(fakeRequest.withBody(Json.toJson(request2)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
      }
    }

    "POST /update-land (updateLand)" - {

      "return OK with update response when service returns successfully" in new BaseSetup {
        when(
          mockFilingLandService.updateLand(eqTo(testUpdateLandRequest))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testUpdateLandResponse))

        val result: Future[Result] = controller.updateLand()(
          fakeRequest.withBody(Json.toJson(testUpdateLandRequest))
        )

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testUpdateLandResponse)
        verify(mockFilingLandService).updateLand(eqTo(testUpdateLandRequest))(
          any[HeaderCarrier]
        )
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateLand()(
          fakeRequest.withBody(Json.obj("invalid" -> "data"))
        )

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "100001",
          "landResourceRef" -> "100001",
          "propertyType" -> "RESIDENTIAL",
          "interestTransferredCreated" -> "FREEHOLD",
          "addressLine1" -> "High Street"
        )
        val result: Future[Result] =
          controller.updateLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "landResourceRef" -> "100001",
          "propertyType" -> "RESIDENTIAL",
          "interestTransferredCreated" -> "FREEHOLD",
          "addressLine1" -> "High Street"
        )
        val result: Future[Result] =
          controller.updateLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when landResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN123456",
          "returnResourceRef" -> "100001",
          "propertyType" -> "RESIDENTIAL",
          "interestTransferredCreated" -> "FREEHOLD",
          "addressLine1" -> "High Street"
        )
        val result: Future[Result] =
          controller.updateLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] =
          controller.updateLand()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(
          mockFilingLandService.updateLand(any[UpdateLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateLand()(
          fakeRequest.withBody(Json.toJson(testUpdateLandRequest))
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(
          mockFilingLandService.updateLand(any[UpdateLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateLand()(
          fakeRequest.withBody(Json.toJson(testUpdateLandRequest))
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: UpdateLandRequest = testUpdateLandRequestComplete
        when(
          mockFilingLandService.updateLand(eqTo(completeRequest))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testUpdateLandResponse))

        val result: Future[Result] = controller.updateLand()(
          fakeRequest.withBody(Json.toJson(completeRequest))
        )

        status(result) mustBe OK
        verify(mockFilingLandService).updateLand(eqTo(completeRequest))(
          any[HeaderCarrier]
        )
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: UpdateLandRequest = testUpdateLandRequestMinimal
        when(
          mockFilingLandService.updateLand(eqTo(minimalRequest))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testUpdateLandResponse))

        val result: Future[Result] = controller.updateLand()(
          fakeRequest.withBody(Json.toJson(minimalRequest))
        )

        status(result) mustBe OK
        verify(mockFilingLandService).updateLand(eqTo(minimalRequest))(
          any[HeaderCarrier]
        )
      }

      "handle different property types" in new BaseSetup {
        val request1: UpdateLandRequest =
          testUpdateLandRequest.copy(propertyType = "RESIDENTIAL")
        val request2: UpdateLandRequest =
          testUpdateLandRequest.copy(propertyType = "NON_RESIDENTIAL")
        val request3: UpdateLandRequest =
          testUpdateLandRequest.copy(propertyType = "MIXED")

        when(
          mockFilingLandService.updateLand(any[UpdateLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testUpdateLandResponse))

        val result1: Future[Result] =
          controller.updateLand()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] =
          controller.updateLand()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] =
          controller.updateLand()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe OK
        status(result2) mustBe OK
        status(result3) mustBe OK
      }
    }

    "POST /delete-land (deleteLand)" - {

      "return OK with delete response when service returns successfully" in new BaseSetup {
        when(
          mockFilingLandService.deleteLand(eqTo(testDeleteLandRequest))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testDeleteLandResponse))

        val result: Future[Result] = controller.deleteLand()(
          fakeRequest.withBody(Json.toJson(testDeleteLandRequest))
        )

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testDeleteLandResponse)
        verify(mockFilingLandService).deleteLand(eqTo(testDeleteLandRequest))(
          any[HeaderCarrier]
        )
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.deleteLand()(
          fakeRequest.withBody(Json.obj("invalid" -> "data"))
        )

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "100001",
          "landResourceRef" -> "100001"
        )
        val result: Future[Result] =
          controller.deleteLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN123456",
          "landResourceRef" -> "100001"
        )
        val result: Future[Result] =
          controller.deleteLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when landResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN123456",
          "returnResourceRef" -> "100001"
        )
        val result: Future[Result] =
          controller.deleteLand()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] =
          controller.deleteLand()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(
          mockFilingLandService.deleteLand(any[DeleteLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deleteLand()(
          fakeRequest.withBody(Json.toJson(testDeleteLandRequest))
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(
          mockFilingLandService.deleteLand(any[DeleteLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.deleteLand()(
          fakeRequest.withBody(Json.toJson(testDeleteLandRequest))
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle different resource reference formats" in new BaseSetup {
        val request1: DeleteLandRequest =
          testDeleteLandRequest.copy(landResourceRef = "100001")
        val request2: DeleteLandRequest =
          testDeleteLandRequest.copy(landResourceRef = "999999")
        val request3: DeleteLandRequest =
          testDeleteLandRequest.copy(landResourceRef = "LRF-2024-001")

        when(
          mockFilingLandService.deleteLand(any[DeleteLandRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(testDeleteLandResponse))

        val result1: Future[Result] =
          controller.deleteLand()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] =
          controller.deleteLand()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] =
          controller.deleteLand()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe OK
        status(result2) mustBe OK
        status(result3) mustBe OK
      }
    }
  }

  private trait BaseSetup {
    val mockFilingLandService: FilingLandService = mock[FilingLandService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller =
      new LandReturnsController(cc, mockFilingLandService, fakeIdentifierAction)

    val testCreateLandRequest: CreateLandRequest = CreateLandRequest(
      stornId = "STORN123456",
      returnResourceRef = "100001",
      propertyType = "RESIDENTIAL",
      interestTransferredCreated = "FREEHOLD",
      houseNumber = Some("42"),
      addressLine1 = "High Street",
      addressLine2 = Some("Kensington"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      postcode = Some("SW1A 1AA"),
      landArea = Some("500"),
      areaUnit = Some("SQUARE_METERS"),
      localAuthorityNumber = Some("LA12345"),
      mineralRights = Some("YES"),
      nlpgUprn = Some("100012345678"),
      willSendPlansByPost = Some("NO"),
      titleNumber = Some("TN123456")
    )

    val testCreateLandRequestComplete: CreateLandRequest = testCreateLandRequest

    val testCreateLandRequestMinimal: CreateLandRequest = CreateLandRequest(
      stornId = "STORN123456",
      returnResourceRef = "100001",
      propertyType = "RESIDENTIAL",
      interestTransferredCreated = "FREEHOLD",
      houseNumber = None,
      addressLine1 = "High Street",
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      postcode = None,
      landArea = None,
      areaUnit = None,
      localAuthorityNumber = None,
      mineralRights = None,
      nlpgUprn = None,
      willSendPlansByPost = None,
      titleNumber = None
    )

    val testCreateLandResponse: CreateLandReturn = CreateLandReturn(
      landResourceRef = "100001",
      landId = "1"
    )

    val testUpdateLandRequest: UpdateLandRequest = UpdateLandRequest(
      stornId = "STORN123456",
      returnResourceRef = "100001",
      landResourceRef = "100001",
      propertyType = "RESIDENTIAL",
      interestTransferredCreated = "FREEHOLD",
      houseNumber = Some("42"),
      addressLine1 = "High Street",
      addressLine2 = Some("Kensington"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      postcode = Some("SW1A 1AA"),
      landArea = Some("500"),
      areaUnit = Some("SQUARE_METERS"),
      localAuthorityNumber = Some("LA12345"),
      mineralRights = Some("YES"),
      nlpgUprn = Some("100012345678"),
      willSendPlansByPost = Some("NO"),
      titleNumber = Some("TN123456"),
      nextLandId = Some("100002")
    )

    val testUpdateLandRequestComplete: UpdateLandRequest = testUpdateLandRequest

    val testUpdateLandRequestMinimal: UpdateLandRequest = UpdateLandRequest(
      stornId = "STORN123456",
      returnResourceRef = "100001",
      landResourceRef = "100001",
      propertyType = "RESIDENTIAL",
      interestTransferredCreated = "FREEHOLD",
      houseNumber = None,
      addressLine1 = "High Street",
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      postcode = None,
      landArea = None,
      areaUnit = None,
      localAuthorityNumber = None,
      mineralRights = None,
      nlpgUprn = None,
      willSendPlansByPost = None,
      titleNumber = None,
      nextLandId = None
    )

    val testUpdateLandResponse: UpdateLandReturn = UpdateLandReturn(
      updated = true
    )

    val testDeleteLandRequest: DeleteLandRequest = DeleteLandRequest(
      storn = "STORN123456",
      returnResourceRef = "100001",
      landResourceRef = "100001"
    )

    val testDeleteLandResponse: DeleteLandReturn = DeleteLandReturn(
      deleted = true
    )
  }
}
