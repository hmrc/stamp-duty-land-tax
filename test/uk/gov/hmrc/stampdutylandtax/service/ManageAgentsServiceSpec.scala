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

package uk.gov.hmrc.stampdutylandtax.service

import base.SpecBase
import connectors.FormpProxyConnector
import models.agent.{CreatePredefinedAgentRequest, CreatedAgent, DeletePredefinedAgentRequest, DeletePredefinedAgentResponse, SdltOrganisationResponse, CreatePredefinedAgentResponse}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import service.ManageAgentsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ManageAgentsServiceSpec extends SpecBase {

  "ManageAgentsService" - {

    "submitAgentDetails" - {

      "should delegate to connector and successfully return CreatePredefinedAgentResponse" in new BaseSetup {
        private val req = CreatePredefinedAgentRequest(
          storn        = "STN001",
          agentName    = "22A Harborview Estates",
          addressLine1 = Some("Queensway"),
          addressLine2 = None,
          addressLine3 = Some("Birmingham"),
          addressLine4 = None,
          postcode     = Some("B2 4ND"),
          phone        = Some("01214567890"),
          email        = Some("info@harborviewestates.co.uk")
        )
        private val resp = CreatePredefinedAgentResponse("ARN123456", "07524")

        when(mockFormp.submitAgentDetails(eqTo(req))(any[HeaderCarrier]))
          .thenReturn(Future.successful(resp))

        val result = service.submitAgentDetails(req).futureValue
        result mustBe resp
        verify(mockFormp, times(1)).submitAgentDetails(eqTo(req))(any[HeaderCarrier])
      }

      "should propagate exceptions from the connector" in new BaseSetup {
        private val req = CreatePredefinedAgentRequest(
          storn        = "STN001",
          agentName    = "?? Bad Data Inc",
          addressLine1 = Some("Unknown"),
          addressLine2 = None,
          addressLine3 = Some("Nowhere"),
          addressLine4 = None,
          postcode     = None,
          phone        = None,
          email        = Some("bad@example.com")
        )

        when(mockFormp.submitAgentDetails(eqTo(req))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("kaboom")))

        val ex = intercept[RuntimeException] {
          service.submitAgentDetails(req).futureValue
        }
        ex.getMessage must include("kaboom")
        verify(mockFormp, times(1)).submitAgentDetails(eqTo(req))(any[HeaderCarrier])
      }
    }

    "updateAgent" - {

      "should return 204 when the connector successfully updates an agent" in new BaseSetup {

        when(mockFormp.updateAgentDetails(eqTo(testAgentDetailsAfterCreation))(any[HeaderCarrier]))
          .thenReturn(Future.successful(204))

        val result = service.updateAgentDetails(testAgentDetailsAfterCreation).futureValue
        result mustBe 204
        verify(mockFormp, times(1)).updateAgentDetails(eqTo(testAgentDetailsAfterCreation))(any[HeaderCarrier])
      }

      "should propagate exceptions from the connector" in new BaseSetup {

        when(mockFormp.updateAgentDetails(eqTo(testAgentDetailsAfterCreation))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val ex = intercept[RuntimeException] {
          service.updateAgentDetails(testAgentDetailsAfterCreation).futureValue
        }
        ex.getMessage must include("boom")
        verify(mockFormp, times(1)).updateAgentDetails(eqTo(testAgentDetailsAfterCreation))(any[HeaderCarrier])
      }
    }

    "deletePredefinedAgent" - {

      "should return JSON Boolean when the connector successfully removes an agent" in new BaseSetup {
        private val req = DeletePredefinedAgentRequest("STN001", "100001")

        when(mockFormp.deletePredefinedAgent(eqTo(req))(any[HeaderCarrier]))
          .thenReturn(Future.successful(DeletePredefinedAgentResponse(true)))

        val result: DeletePredefinedAgentResponse = service.deletePredefinedAgent(req).futureValue
        result mustBe DeletePredefinedAgentResponse(true)
        verify(mockFormp, times(1)).deletePredefinedAgent(eqTo(req))(any[HeaderCarrier])
      }

      "should propagate exceptions from the connector" in new BaseSetup {
        private val req = DeletePredefinedAgentRequest("STN001-ERR", "100001-ERR")

        when(mockFormp.deletePredefinedAgent(eqTo(req))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val ex = intercept[RuntimeException] {
          service.deletePredefinedAgent(req).futureValue
        }
        ex.getMessage must include("boom")
        verify(mockFormp, times(1)).deletePredefinedAgent(eqTo(req))(any[HeaderCarrier])
      }
    }
    
    "getSdltOrganisation" - {

      "should delegate to connector and return SdltOrganisation" in new BaseSetup {

        private val storn = "STN-ORG"

        private val expected = SdltOrganisationResponse(
          storn                   = storn,
          version                 = Some("1"),
          isReturnUser            = Some("true"),
          doNotDisplayWelcomePage = Some("Yes"),
          agents = Seq(
            CreatedAgent(
              agentId                = Some("AGT001"),
              storn                  = Some(storn),
              name                   = Some("John"),
              houseNumber            = None,
              address1               = Some("1 High Street"),
              address2               = Some("Westminster"),
              address3               = Some("London"),
              address4               = Some("Greater London"),
              postcode               = Some("SW72AZ"),
              phone                  = Some("02079460000"),
              email                  = Some("info@acme.co.uk"),
              dxAddress              = None,
              agentResourceReference = Some("ARN001")
            )
          )
        )

        when(mockFormp.getSdltOrganisation(eqTo(storn))(any[HeaderCarrier]))
          .thenReturn(Future.successful(expected))

        val result = service.getSdltOrganisation(storn).futureValue
        result mustBe expected

        verify(mockFormp, times(1)).getSdltOrganisation(eqTo(storn))(any[HeaderCarrier])
      }

      "should propagate exceptions from the connector" in new BaseSetup {
        private val storn = "STN-ORG-ERR"

        when(mockFormp.getSdltOrganisation(eqTo(storn))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val ex = intercept[RuntimeException] {
          service.getSdltOrganisation(storn).futureValue
        }
        ex.getMessage must include("boom")

        verify(mockFormp, times(1)).getSdltOrganisation(eqTo(storn))(any[HeaderCarrier])
      }
    }
  }

  private trait BaseSetup {
    val mockFormp: FormpProxyConnector = mock[FormpProxyConnector]
    implicit val ec: ExecutionContext   = cc.executionContext
    implicit val hc: HeaderCarrier      = HeaderCarrier()
    val service = new ManageAgentsService(mockFormp)
  }
}
