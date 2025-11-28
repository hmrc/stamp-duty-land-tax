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

package uk.gov.hmrc.stampdutylandtax.controllers

import base.SpecBase
import models.agent.{AgentDetailsBeforeCreation, CreatedAgent, DeletePredefinedAgentRequest, DeletePredefinedAgentResponse, SdltOrganisationResponse}
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import org.mockito.ArgumentMatchers.eq as eqTo
import play.api.test.Helpers.{contentAsJson, status}
import org.mockito.Mockito.{verify, when}
import play.api.libs.json.Json
import play.api.mvc.Result
import service.ManageAgentsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction
import uk.gov.hmrc.stampdutylandtax.controllers.agents.ManageAgentsController

import scala.concurrent.{ExecutionContext, Future}

class ManageAgentsControllerSpec extends SpecBase {

  "ManageAgentsController" - {
    
    "POST /agent-details/submit (submitAgentDetails)" - {

      "return OK with agent details when service returns agent details" in new BaseSetup {
        when(mockManageAgentsService.submitAgentDetails(any[AgentDetailsBeforeCreation])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testAgentDetailsSuccessResponse))

        val result: Future[Result] = controller.submitAgentDetails(fakeRequest.withBody(Json.toJson(testAgentDetailsRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testAgentDetailsSuccessResponse)
        verify(mockManageAgentsService).submitAgentDetails(eqTo(testAgentDetailsRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        when(mockManageAgentsService.submitAgentDetails(any[AgentDetailsBeforeCreation])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testAgentDetailsSuccessResponse))

        val result: Future[Result] = controller.submitAgentDetails(fakeRequest.withBody(Json.toJson(Json.obj())))

        status(result) mustBe BAD_REQUEST
      }

      "propagate UpstreamErrorResponse status & message" in new BaseSetup {
        when(mockManageAgentsService.submitAgentDetails(any[AgentDetailsBeforeCreation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result: Future[Result] = controller.submitAgentDetails(fakeRequest.withBody(Json.toJson(testAgentDetailsRequest)))

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "return INTERNAL_SERVER_ERROR Unexpected error on unknown exception" in new BaseSetup {
        when(mockManageAgentsService.submitAgentDetails(any[AgentDetailsBeforeCreation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.submitAgentDetails(fakeRequest.withBody(Json.toJson(testAgentDetailsRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal("Unexpected error")
      }
    }

    "POST /delete/predefined-agent (deletePredefinedAgent)" - {

      val req = DeletePredefinedAgentRequest("STN001", "100001")

      "returns 200 Ok when service runs successfully" in new BaseSetup {
        when(mockManageAgentsService.deletePredefinedAgent(eqTo(req))(any[HeaderCarrier]))
          .thenReturn(Future.successful(DeletePredefinedAgentResponse(true)))

        val result: Future[Result] = controller.deletePredefinedAgent(fakeRequest.withBody(Json.toJson(testDeletePredefinedAgentRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("deleted" -> true)
        verify(mockManageAgentsService).deletePredefinedAgent(eqTo(req))(any[HeaderCarrier])
      }

      "propagate UpstreamErrorResponse status & message" in new BaseSetup {
        when(mockManageAgentsService.deletePredefinedAgent(eqTo(req))(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result: Future[Result] = controller.deletePredefinedAgent(fakeRequest.withBody(Json.toJson(testDeletePredefinedAgentRequest)))

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "return INTERNAL_SERVER_ERROR Unexpected error on unknown exception" in new BaseSetup {
        when(mockManageAgentsService.deletePredefinedAgent(eqTo(req))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.deletePredefinedAgent()(fakeRequest.withBody(Json.toJson(testDeletePredefinedAgentRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal("Unexpected error")
      }
    }
    
    "GET organisation/storn/:storn (getSdltOrganisation)" - {

      "return OK with organisation payload when service returns the organisation" in new BaseSetup {
        val testOrg = SdltOrganisationResponse(
          storn = "A-123",
          version = Some("1"),
          isReturnUser = Some("1"),
          doNotDisplayWelcomePage = Some("No"),
          agents = Seq(
            CreatedAgent(
              agentId = Some("AGT001"),
              storn = Some("A-123"),
              name = Some("Anderson Legal LLP"),
              houseNumber = None,
              address1 = Some("10 Downing Street"),
              address2 = Some("Westminster"),
              address3 = Some("London"),
              address4 = Some("United Kingdom"),
              postcode = Some("SW1A 2AA"),
              phone = Some("02079460001"),
              email = Some("info@andersonlegal.co.uk"),
              dxAddress = None,
              agentResourceReference = Some("ARN001")
            )
          )
        )

        when(mockManageAgentsService.getSdltOrganisation(eqTo("A-123"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testOrg))

        val result: Future[Result] = controller.getSdltOrganisation("A-123")(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testOrg)
        verify(mockManageAgentsService).getSdltOrganisation(eqTo("A-123"))(any[HeaderCarrier])
      }

      "return OK with empty agents when service returns an organisation with no agents" in new BaseSetup {
        val emptyOrg = SdltOrganisationResponse(
          storn = "A-123",
          version = Some("1"),
          isReturnUser = Some("1"),
          doNotDisplayWelcomePage = Some("No"),
          agents = Nil
        )

        when(mockManageAgentsService.getSdltOrganisation(eqTo("A-123"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(emptyOrg))

        val result: Future[Result] = controller.getSdltOrganisation("A-123")(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(emptyOrg)
        verify(mockManageAgentsService).getSdltOrganisation(eqTo("A-123"))(any[HeaderCarrier])
      }

      "propagate UpstreamErrorResponse status & message" in new BaseSetup {
        when(mockManageAgentsService.getSdltOrganisation(eqTo("A-123"))(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result: Future[Result] = controller.getSdltOrganisation("A-123")(fakeRequest)

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "return INTERNAL_SERVER_ERROR Unexpected error on unknown exception" in new BaseSetup {
        when(mockManageAgentsService.getSdltOrganisation(eqTo("A-123"))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getSdltOrganisation("A-123")(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal("Unexpected error")
      }
    }
  }

  private trait BaseSetup {
    val mockManageAgentsService: ManageAgentsService = mock[ManageAgentsService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new ManageAgentsController(cc, mockManageAgentsService, fakeIdentifierAction)
  }
}
