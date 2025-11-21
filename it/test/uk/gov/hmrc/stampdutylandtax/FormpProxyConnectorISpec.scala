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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, post, stubFor, urlPathEqualTo}
import itutil.ApplicationWithWiremock
import models.agent.{AgentDetailsBeforeCreation, AgentDetailsResponse, SdltOrganisationResponse, SubmitAgentDetailsResponse}
import models.manage.{ReturnSummary, SdltReturnRecordRequest, SdltReturnRecordResponse}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import play.api.libs.json.{JsBoolean, Json}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class FormpProxyConnectorISpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock {
  
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector: FormpProxyConnector = app.injector.instanceOf[FormpProxyConnector]

  private val storn = "STN001"
  private val arn   = "ARN001"

  "getAgentDetails" should {

    val url = "/stamp-duty-land-tax-stub/manage-agents/agent-details"

    "return AgentDetails when BE returns OK with valid JSON" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn","agentReferenceNumber":"$arn"}""", true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                """{
                  |  "agentName": "Sunrise Realty",
                  |  "agentId": "AGT001",
                  |  "addressLine1": "8B Baker Street",
                  |  "addressLine2": null,
                  |  "addressLine3": "Manchester",
                  |  "addressLine4": null,
                  |  "postcode": "M1 2AB",
                  |  "phone": "01611234567",
                  |  "email": "contact@sunriserealty.co.uk",
                  |  "agentReferenceNumber": "ARN001"
                  |}""".stripMargin
              )
          )
      )

      val result = connector.getAgentDetails(storn, arn).futureValue

      result mustBe Some(AgentDetailsResponse(
        agentName            = "Sunrise Realty",
        agentId              = Some("AGT001"),
        addressLine1         = Some("8B Baker Street"),
        addressLine2         = None,
        addressLine3         = Some("Manchester"),
        addressLine4         = None,
        postcode             = Some("M1 2AB"),
        phone                = Some("01611234567"),
        email                = Some("contact@sunriserealty.co.uk"),
        agentReferenceNumber = "ARN001"
      ))
    }

    "fail when BE returns OK with invalid JSON" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn","agentReferenceNumber":"$arn"}""", true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "unexpectedField": true }"""))
      )

      val ex = intercept[Exception] {
        connector.getAgentDetails(storn, arn).futureValue
      }
      ex.getMessage.toLowerCase must include ("error")
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn","agentReferenceNumber":"$arn"}""", true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.getAgentDetails(storn, arn).futureValue
      }
      ex.getMessage must include ("returned 500")
    }
  }

  "submitAgentDetails" should {

    val url = "/stamp-duty-land-tax-stub/manage-agents/agent-details/submit"

    val payload = AgentDetailsBeforeCreation(
      storn       = "STN001",
      agentName   = "Acme Property Agents Ltd",
      addressLine1 = Some("42 High Street"),
      addressLine2 = Some("Westminster"),
      addressLine3 = Some("London"),
      addressLine4 = Some("Greater London"),
      postcode     = Some("SW1A 2AA"),
      phone        = Some("02079460000"),
      email        = Some("info@acmeagents.co.uk")
    )

    "return SubmitAgentDetailsResponse when BE returns OK with valid JSON" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(payload)), true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "agentResourceRef": "ARN4324234", "agentId" : "1234" }"""))
      )

      val result = connector.submitAgentDetails(payload).futureValue
      result mustBe SubmitAgentDetailsResponse(agentResourceRef = "ARN4324234", agentId = "1234")
    }

    "fail when BE returns OK with invalid JSON" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(payload)), true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "unexpectedField": true }"""))
      )

      val ex = intercept[Exception] {
        connector.submitAgentDetails(payload).futureValue
      }
      ex.getMessage.toLowerCase must include ("agentresourceref")
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(payload)), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.submitAgentDetails(payload).futureValue
      }
      ex.getMessage must include ("returned 500")
    }
  }

  "removeAgent" should {

    val url = "/stamp-duty-land-tax-stub/manage-agents/agent-details/remove"

    "return true when BE returns OK with valid JSON boolean" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn","agentReferenceNumber":"$arn"}""", true, true))
          .willReturn(aResponse().withStatus(OK).withBody(Json.stringify(JsBoolean(true))))
      )

      val result = connector.removeAgent(storn, arn).futureValue
      result mustBe true
    }

    "fail when BE returns OK with invalid JSON" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn","agentReferenceNumber":"$arn"}""", true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "unexpectedField": true }"""))
      )

      val ex = intercept[Exception] {
        connector.removeAgent(storn, arn).futureValue
      }
      ex.getMessage.toLowerCase must include ("jsboolean")
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn","agentReferenceNumber":"$arn"}""", true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.removeAgent(storn, arn).futureValue
      }
      ex.getMessage must include ("returned 500")
    }
  }

  "getReturns" should {

    val url = "/stamp-duty-land-tax-stub/returns"

    val request = SdltReturnRecordRequest(storn = storn, None, false, None)

    "return SdltReturnRecordResponse when BE returns OK with valid JSON" in {

      val responseBody =
        s"""
           |{
           |  "storn": "$storn",
           |  "returnSummaryCount": 1,
           |  "returnSummaryList": [
           |    {
           |      "returnReference": "RET123",
           |      "utrn": "UTRN001",
           |      "status": "SUBMITTED",
           |      "dateSubmitted": "2025-11-10",
           |      "purchaserName": "John Smith",
           |      "address": "10 Downing Street, London",
           |      "agentReference": "Smith & Co"
           |    }
           |  ]
           |}
         """.stripMargin

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request)), true, true))
          .willReturn(aResponse().withStatus(OK).withBody(responseBody))
      )

      val result = connector.getReturns(request).futureValue
      result mustBe SdltReturnRecordResponse(
        returnSummaryCount = Some(1),
        returnSummaryList = List(
          ReturnSummary(
            returnReference = "RET123",
            utrn = Some("UTRN001"),
            status = "SUBMITTED",
            dateSubmitted = Some(LocalDate.parse("2025-11-10")),
            purchaserName = "John Smith",
            address = "10 Downing Street, London",
            agentReference = Some("Smith & Co")
          )
        )
      )
    }

    "fail when BE returns OK with invalid JSON" in {

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request)), true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "unexpectedField": true }"""))
      )

      val ex = intercept[Exception] {
        connector.getReturns(request).futureValue
      }
      ex.getMessage.toLowerCase must include("error")
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request)), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.getReturns(request).futureValue
      }
      ex.getMessage must include("returned 500")
    }
  }

  "getSdltOrganisation" should {

    val url = "/stamp-duty-land-tax-stub/organisation"

    "return SdltOrganisation when BE returns OK with valid JSON" in {
      val responseJson =
        s"""
           |{
           |  "storn": "STN001",
           |  "version": 1,
           |  "isReturnUser": "true",
           |  "doNotDisplayWelcomePage": "Yes",
           |  "agents": [
           |    {
           |      "agentReferenceNumber": "ARN001",
           |      "agentName": "John",
           |      "agentId": "AGT001",
           |      "addressLine1": "1 High Street",
           |      "addressLine2": "Westminster",
           |      "addressLine3": "London",
           |      "addressLine4": "Greater London",
           |      "postcode": "SW72AZ",
           |      "phone": "02079460000",
           |      "email": "info@acme.co.uk"
           |    }
           |  ]
           |}
           |
         """.stripMargin

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn"}""", true, true))
          .willReturn(aResponse().withStatus(OK).withBody(responseJson))
      )

      val result = connector.getSdltOrganisation(storn).futureValue

       result mustBe SdltOrganisationResponse(
         storn                   = storn,
         version                 = 1,
         isReturnUser            = "true",
         doNotDisplayWelcomePage = "Yes",
         agents = Seq(AgentDetailsResponse(
           agentReferenceNumber = "ARN001",
           agentName            = "John",
           agentId              = Some("AGT001"),
           addressLine1         = Some("1 High Street"),
           addressLine2         = Some("Westminster"),
           addressLine3         = Some("London"),
           addressLine4         = Some("Greater London"),
           postcode             = Some("SW72AZ"),
           phone                = Some("02079460000"),
           email                = Some("info@acme.co.uk")
         ))
       )

      result.toString must include("SW72AZ")
      result.toString must include("AGT001")
      result.toString must include("John")
      result.toString must include("ARN001")
      result.toString must include("02079460000")
      result.toString must include("1 High Street")
      result.toString must include("Westminster")
      result.toString must include("London")
      result.toString must include("Greater London")
      result.toString must include("info@acme.co.uk")
    }

    "fail when BE returns OK with invalid JSON" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn"}""", true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "unexpectedField": true }"""))
      )

      val ex = intercept[Exception] {
        connector.getSdltOrganisation(storn).futureValue
      }
      ex.getMessage.toLowerCase must include("error")
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn"}""", true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.getSdltOrganisation(storn).futureValue
      }
      ex.getMessage must include("returned 500")
    }
  }
}
