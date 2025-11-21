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
import service.filing.ReturnAgentService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReturnAgentControllerSpec extends SpecBase {

  "ReturnAgentController" - {

    "POST /create-return-agent (createReturnAgent)" - {

      "return CREATED with return agent response when service returns successfully" in new BaseSetup {
        when(mockReturnAgentService.createReturnAgent(eqTo(testCreateReturnAgentRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateReturnAgentReturn))

        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.toJson(testCreateReturnAgentRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreateReturnAgentReturn)
        verify(mockReturnAgentService).createReturnAgent(eqTo(testCreateReturnAgentRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "agentType" -> "SOLICITOR",
          "name" -> "Smith & Partners",
          "addressLine1" -> "Main Street",
          "postcode" -> "TE23 5TT"
        )
        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "agentType" -> "SOLICITOR",
          "name" -> "Smith & Partners",
          "addressLine1" -> "Main Street",
          "postcode" -> "TE23 5TT"
        )
        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when agentType is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "name" -> "Smith & Partners",
          "addressLine1" -> "Main Street",
          "postcode" -> "TE23 5TT"
        )
        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when name is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "agentType" -> "SOLICITOR",
          "addressLine1" -> "Main Street",
          "postcode" -> "TE23 5TT"
        )
        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when addressLine1 is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "agentType" -> "SOLICITOR",
          "name" -> "Smith & Partners",
          "postcode" -> "TE23 5TT"
        )
        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when postcode is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001",
          "agentType" -> "SOLICITOR",
          "name" -> "Smith & Partners",
          "addressLine1" -> "Main Street"
        )
        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockReturnAgentService.createReturnAgent(any[CreateReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.toJson(testCreateReturnAgentRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockReturnAgentService.createReturnAgent(any[CreateReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.toJson(testCreateReturnAgentRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: CreateReturnAgentRequest = testCreateReturnAgentRequest
        when(mockReturnAgentService.createReturnAgent(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateReturnAgentReturn))

        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockReturnAgentService).createReturnAgent(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: CreateReturnAgentRequest = testCreateReturnAgentRequestMinimal
        when(mockReturnAgentService.createReturnAgent(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateReturnAgentReturn))

        val result: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockReturnAgentService).createReturnAgent(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle different agent types" in new BaseSetup {
        val solicitorRequest: CreateReturnAgentRequest = testCreateReturnAgentRequest.copy(agentType = "SOLICITOR")
        val conveyancerRequest: CreateReturnAgentRequest = testCreateReturnAgentRequest.copy(agentType = "CONVEYANCER")

        when(mockReturnAgentService.createReturnAgent(any[CreateReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateReturnAgentReturn))

        val result1: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.toJson(solicitorRequest)))
        val result2: Future[Result] = controller.createReturnAgent()(fakeRequest.withBody(Json.toJson(conveyancerRequest)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
      }
    }

    "POST /update-return-agent (updateReturnAgent)" - {

      "return CREATED with update response when service returns successfully" in new BaseSetup {
        when(mockReturnAgentService.updateReturnAgent(eqTo(testUpdateReturnAgentRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnAgentReturn))

        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(Json.toJson(testUpdateReturnAgentRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testUpdateReturnAgentReturn)
        verify(mockReturnAgentService).updateReturnAgent(eqTo(testUpdateReturnAgentRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when stornId is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "agentType" -> "SOLICITOR",
          "name" -> "Smith & Partners Updated",
          "addressLine1" -> "Main Street",
          "postcode" -> "TE23 5TT"
        )
        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockReturnAgentService.updateReturnAgent(any[UpdateReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(Json.toJson(testUpdateReturnAgentRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockReturnAgentService.updateReturnAgent(any[UpdateReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(Json.toJson(testUpdateReturnAgentRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle valid payload with all optional fields" in new BaseSetup {
        val completeRequest: UpdateReturnAgentRequest = testUpdateReturnAgentRequest
        when(mockReturnAgentService.updateReturnAgent(eqTo(completeRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnAgentReturn))

        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(Json.toJson(completeRequest)))

        status(result) mustBe CREATED
        verify(mockReturnAgentService).updateReturnAgent(eqTo(completeRequest))(any[HeaderCarrier])
      }

      "handle valid payload with minimal required fields" in new BaseSetup {
        val minimalRequest: UpdateReturnAgentRequest = testUpdateReturnAgentRequestMinimal
        when(mockReturnAgentService.updateReturnAgent(eqTo(minimalRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnAgentReturn))

        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(Json.toJson(minimalRequest)))

        status(result) mustBe CREATED
        verify(mockReturnAgentService).updateReturnAgent(eqTo(minimalRequest))(any[HeaderCarrier])
      }

      "handle updated false response" in new BaseSetup {
        when(mockReturnAgentService.updateReturnAgent(eqTo(testUpdateReturnAgentRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(UpdateReturnAgentReturn(updated = false)))

        val result: Future[Result] = controller.updateReturnAgent()(fakeRequest.withBody(Json.toJson(testUpdateReturnAgentRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "updated").as[Boolean] mustBe false
      }
    }

    "POST /delete-return-agent (deleteReturnAgent)" - {

      "return CREATED with delete response when service returns successfully" in new BaseSetup {
        when(mockReturnAgentService.deleteReturnAgent(eqTo(testDeleteReturnAgentRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteReturnAgentReturn))

        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(testDeleteReturnAgentRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testDeleteReturnAgentReturn)
        verify(mockReturnAgentService).deleteReturnAgent(eqTo(testDeleteReturnAgentRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "agentType" -> "SOLICITOR"
        )
        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "agentType" -> "SOLICITOR"
        )
        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when agentType is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockReturnAgentService.deleteReturnAgent(any[DeleteReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(testDeleteReturnAgentRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockReturnAgentService.deleteReturnAgent(any[DeleteReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(testDeleteReturnAgentRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle deleted false response" in new BaseSetup {
        when(mockReturnAgentService.deleteReturnAgent(eqTo(testDeleteReturnAgentRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(DeleteReturnAgentReturn(deleted = false)))

        val result: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(testDeleteReturnAgentRequest)))

        status(result) mustBe CREATED
        (contentAsJson(result) \ "deleted").as[Boolean] mustBe false
      }

      "handle different agent types" in new BaseSetup {
        val solicitorRequest: DeleteReturnAgentRequest = testDeleteReturnAgentRequest.copy(agentType = "SOLICITOR")
        val conveyancerRequest: DeleteReturnAgentRequest = testDeleteReturnAgentRequest.copy(agentType = "CONVEYANCER")

        when(mockReturnAgentService.deleteReturnAgent(any[DeleteReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteReturnAgentReturn))

        val result1: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(solicitorRequest)))
        val result2: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(conveyancerRequest)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
      }

      "handle different storn formats" in new BaseSetup {
        val request1: DeleteReturnAgentRequest = testDeleteReturnAgentRequest.copy(storn = "STORN12345")
        val request2: DeleteReturnAgentRequest = testDeleteReturnAgentRequest.copy(storn = "STORN-ABC-123")
        val request3: DeleteReturnAgentRequest = testDeleteReturnAgentRequest.copy(storn = "12345678")

        when(mockReturnAgentService.deleteReturnAgent(any[DeleteReturnAgentRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testDeleteReturnAgentReturn))

        val result1: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(request1)))
        val result2: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(request2)))
        val result3: Future[Result] = controller.deleteReturnAgent()(fakeRequest.withBody(Json.toJson(request3)))

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }
  }

  private trait BaseSetup {
    val mockReturnAgentService: ReturnAgentService = mock[ReturnAgentService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new ReturnAgentController(cc, mockReturnAgentService, fakeIdentifierAction)

    val testCreateReturnAgentRequest: CreateReturnAgentRequest = CreateReturnAgentRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      agentType = "SOLICITOR",
      name = "Smith & Partners",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Suite 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = "TE23 5TT",
      phoneNumber = Some("01234567890"),
      email = Some("agent@example.com"),
      agentReference = Some("AGT-001"),
      isAuthorised = Some("YES")
    )

    val testCreateReturnAgentRequestMinimal: CreateReturnAgentRequest = CreateReturnAgentRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      agentType = "SOLICITOR",
      name = "Smith & Partners",
      houseNumber = None,
      addressLine1 = "Main Street",
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      postcode = "TE23 5TT",
      phoneNumber = None,
      email = None,
      agentReference = None,
      isAuthorised = None
    )

    val testCreateReturnAgentReturn: CreateReturnAgentReturn = CreateReturnAgentReturn(
      returnAgentID = "AGID-001"
    )

    val testUpdateReturnAgentRequest: UpdateReturnAgentRequest = UpdateReturnAgentRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      agentType = "SOLICITOR",
      name = "Smith & Partners Updated",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Suite 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = "TE23 5TT",
      phoneNumber = Some("01234567890"),
      email = Some("agent@example.com"),
      agentReference = Some("AGT-001"),
      isAuthorised = Some("YES")
    )

    val testUpdateReturnAgentRequestMinimal: UpdateReturnAgentRequest = UpdateReturnAgentRequest(
      stornId = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      agentType = "SOLICITOR",
      name = "Smith & Partners",
      houseNumber = None,
      addressLine1 = "Main Street",
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      postcode = "TE23 5TT",
      phoneNumber = None,
      email = None,
      agentReference = None,
      isAuthorised = None
    )

    val testUpdateReturnAgentReturn: UpdateReturnAgentReturn = UpdateReturnAgentReturn(
      updated = true
    )

    val testDeleteReturnAgentRequest: DeleteReturnAgentRequest = DeleteReturnAgentRequest(
      storn = "STORN12345",
      returnResourceRef = "RRF-2024-001",
      agentType = "SOLICITOR"
    )

    val testDeleteReturnAgentReturn: DeleteReturnAgentReturn = DeleteReturnAgentReturn(
      deleted = true
    )
  }
}