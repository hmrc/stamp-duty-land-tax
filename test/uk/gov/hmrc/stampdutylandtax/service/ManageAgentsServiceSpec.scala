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
import models.agent.{AgentDetailsResponse, AgentDetailsRequest, SdltOrganisationResponse, SubmitAgentDetailsResponse}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import service.ManageAgentsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ManageAgentsServiceSpec extends SpecBase {

  "ManageAgentsService" - {

    "getAgentDetails" - {

      "should return Some(AgentDetailsResponse) when connector successfully finds an agent" in new BaseSetup {
        private val storn = "STN-123"
        private val arn   = "ARN-999"
        private val resp  = AgentDetailsResponse(
          agentName            = "Acme Property",
          agentId              = Some("AGT001"),
          addressLine1         = Some("High Street"),
          addressLine2         = Some("Westminster"),
          addressLine3         = Some("London"),
          addressLine4         = Some("Greater London"),
          postcode             = Some("SW1A 2AA"),
          phone                = Some("02079460000"),
          email                = Some("info@acmeagents.co.uk"),
          agentReferenceNumber = arn
        )

        when(mockFormp.getAgentDetails(eqTo(storn), eqTo(arn))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(resp)))

        val result = service.getAgentDetails(storn, arn).futureValue
        result mustBe Some(resp)
        verify(mockFormp, times(1)).getAgentDetails(eqTo(storn), eqTo(arn))(any[HeaderCarrier])
      }

      "should return None when connector fails to find the agent by storn" in new BaseSetup {
        private val storn = "STN-123"
        private val arn   = "ARN-NONE"

        when(mockFormp.getAgentDetails(eqTo(storn), eqTo(arn))(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val result = service.getAgentDetails(storn, arn).futureValue
        result mustBe None
        verify(mockFormp, times(1)).getAgentDetails(eqTo(storn), eqTo(arn))(any[HeaderCarrier])
      }

      "should propagate exceptions from the connector" in new BaseSetup {
        private val storn = "STN-ERR"
        private val arn   = "ARN-ERR"

        when(mockFormp.getAgentDetails(eqTo(storn), eqTo(arn))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val ex = intercept[RuntimeException] {
          service.getAgentDetails(storn, arn).futureValue
        }
        ex.getMessage must include("boom")
        verify(mockFormp, times(1)).getAgentDetails(eqTo(storn), eqTo(arn))(any[HeaderCarrier])
      }
    }

    "submitAgentDetails" - {

      "should delegate to connector and successfully return SubmitAgentDetailsResponse" in new BaseSetup {
        private val req = AgentDetailsRequest(
          agentName    = "22A Harborview Estates",
          addressLine1 = Some("Queensway"),
          addressLine2 = None,
          addressLine3 = Some("Birmingham"),
          addressLine4 = None,
          postcode     = Some("B2 4ND"),
          phone        = Some("01214567890"),
          email        = Some("info@harborviewestates.co.uk")
        )
        private val resp = SubmitAgentDetailsResponse("ARN123456")

        when(mockFormp.submitAgentDetails(eqTo(req))(any[HeaderCarrier]))
          .thenReturn(Future.successful(resp))

        val result = service.submitAgentDetails(req).futureValue
        result mustBe resp
        verify(mockFormp, times(1)).submitAgentDetails(eqTo(req))(any[HeaderCarrier])
      }

      "should propagate exceptions from the connector" in new BaseSetup {
        private val req = AgentDetailsRequest(
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

    "removeAgent" - {

      "should return true when the connector successfully removes an agent" in new BaseSetup {
        private val storn = "STN-DEL"
        private val arn   = "ARN-DEL"

        when(mockFormp.removeAgent(eqTo(storn), eqTo(arn))(any[HeaderCarrier]))
          .thenReturn(Future.successful(true))

        val result = service.removeAgent(storn, arn).futureValue
        result mustBe true
        verify(mockFormp, times(1)).removeAgent(eqTo(storn), eqTo(arn))(any[HeaderCarrier])
      }

      "should return false when connector fails to remove an agent" in new BaseSetup {
        private val storn = "STN-DEL"
        private val arn   = "ARN-NOT-FOUND"

        when(mockFormp.removeAgent(eqTo(storn), eqTo(arn))(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))

        val result = service.removeAgent(storn, arn).futureValue
        result mustBe false
        verify(mockFormp, times(1)).removeAgent(eqTo(storn), eqTo(arn))(any[HeaderCarrier])
      }

      "should propagate exceptions from the connector" in new BaseSetup {
        private val storn = "STN-DEL-ERR"
        private val arn   = "ARN-DEL-ERR"

        when(mockFormp.removeAgent(eqTo(storn), eqTo(arn))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val ex = intercept[RuntimeException] {
          service.removeAgent(storn, arn).futureValue
        }
        ex.getMessage must include("boom")
        verify(mockFormp, times(1)).removeAgent(eqTo(storn), eqTo(arn))(any[HeaderCarrier])
      }
    }
    
    "getSdltOrganisation" - {

      "should delegate to connector and return SdltOrganisation" in new BaseSetup {

        private val storn = "STN-ORG"

        private val expected = SdltOrganisationResponse(
          storn = storn,
          version = 1,
          isReturnUser = "true",
          doNotDisplayWelcomePage = "Yes",
          agents = Seq(
            AgentDetailsResponse(
              agentReferenceNumber = "ARN001",
              agentName = "John",
              agentId = Some("AGT001"),
              addressLine1 = Some("1 High Street"),
              addressLine2 = Some("Westminster"),
              addressLine3 = Some("London"),
              addressLine4 = Some("Greater London"),
              postcode = Some("SW72AZ"),
              phone = Some("02079460000"),
              email = Some("info@acme.co.uk")
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
