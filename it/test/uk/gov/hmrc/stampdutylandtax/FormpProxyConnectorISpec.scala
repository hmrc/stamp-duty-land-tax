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
import models.agent.{CreatePredefinedAgentRequest, CreatedAgent, DeletePredefinedAgentRequest, DeletePredefinedAgentResponse, SdltOrganisationResponse, CreatePredefinedAgentResponse}
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

  "submitAgentDetails" should {

    val url = "/stamp-duty-land-tax-stub/create/predefined-agent"

    val payload = CreatePredefinedAgentRequest(
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

    "return CreatePredefinedAgentResponse when BE returns OK with valid JSON" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(payload)), true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "agentResourceRef": "ARN4324234", "agentId" : "1234" }"""))
      )

      val result = connector.submitAgentDetails(payload).futureValue
      result mustBe CreatePredefinedAgentResponse(agentResourceRef = "ARN4324234", agentId = "1234")
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

  "deletePredefinedAgent" should {

    val url = "/stamp-duty-land-tax-stub/delete/predefined-agent"
    val req = DeletePredefinedAgentRequest(storn, arn)

    "return Unit when BE returns OK with valid JSON object" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn","agentReferenceNumber":"$arn"}""", true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "deleted": true }"""))
      )

      val result = connector.deletePredefinedAgent(req).futureValue
      result mustBe DeletePredefinedAgentResponse(true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(s"""{"storn":"$storn","agentReferenceNumber":"$arn"}""", true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.deletePredefinedAgent(req).futureValue
      }
      ex.getMessage must include ("boom")
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
           |  "version": "1",
           |  "isReturnUser": "true",
           |  "doNotDisplayWelcomePage": "Yes",
           |  "agents": [
           |    {
           |      "agentResourceReference": "ARN001",
           |      "name": "John",
           |      "storn": "STN001",
           |      "agentId": "AGT001",
           |      "address1": "1 High Street",
           |      "address2": "Westminster",
           |      "address3": "London",
           |      "address4": "Greater London",
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
        storn = storn,
        version = Some("1"),
        isReturnUser = Some("true"),
        doNotDisplayWelcomePage = Some("Yes"),
        agents = Seq(
          CreatedAgent(
            storn                  = Some(storn),
            agentId                = Some("AGT001"),
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
