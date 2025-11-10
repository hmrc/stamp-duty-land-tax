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

import com.github.tomakehurst.wiremock.client.WireMock.*
import connectors.FilingFormpProxyConnector
import itutil.ApplicationWithWiremock
import models.filing.{CreateReturnRequest, CreateReturnResult, GetReturnByRefRequest, GetReturnRequest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class FilingFormpProxyConnectorISpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock
  with BeforeAndAfterEach {
  
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector: FilingFormpProxyConnector = app.injector.instanceOf[FilingFormpProxyConnector]

  private val stornId = "STORN123456"
  private val returnResourceRef = "RRF-2024-001"

  "createReturn" should {

    val url = "/formp-proxy/create/return"

    val payload = CreateReturnRequest(
      stornId = stornId,
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

    "return CreateReturnResult when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)(CreateReturnRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "returnResourceRef": "$returnResourceRef"
                   |}""".stripMargin
              )
          )
      )

      val result = connector.createReturn(payload).futureValue

      result mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
    }

    "return CreateReturnResult for company purchaser" in {
      val companyPayload = payload.copy(
        purchaserIsCompany = "Y",
        surNameOrCompanyName = "ABC Property Ltd",
        transactionType = "NON_RESIDENTIAL"
      )
      val payloadJson = Json.toJson(companyPayload)(CreateReturnRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "returnResourceRef": "$returnResourceRef" }""")
          )
      )

      val result = connector.createReturn(companyPayload).futureValue
      result mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
    }

    "return CreateReturnResult for minimal request" in {
      val minimalPayload = payload.copy(
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None
      )
      val payloadJson = Json.toJson(minimalPayload)(CreateReturnRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "returnResourceRef": "$returnResourceRef" }""")
          )
      )

      val result = connector.createReturn(minimalPayload).futureValue
      result mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
    }

    "return CreateReturnResult for different transaction types" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "returnResourceRef": "$returnResourceRef" }""")
          )
      )

      val residentialPayload = payload.copy(transactionType = "RESIDENTIAL")
      val nonResidentialPayload = payload.copy(transactionType = "NON_RESIDENTIAL")
      val mixedPayload = payload.copy(transactionType = "MIXED")

      connector.createReturn(residentialPayload).futureValue mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
      connector.createReturn(nonResidentialPayload).futureValue mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
      connector.createReturn(mixedPayload).futureValue mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
    }

    "fail when BE returns OK with invalid JSON" in {
      val payloadJson = Json.toJson(payload)(CreateReturnRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""{ "unexpectedField": true }"""))
      )

      val ex = intercept[Exception] {
        connector.createReturn(payload).futureValue
      }
      ex.getMessage.toLowerCase must include("error")
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)(CreateReturnRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.createReturn(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)(CreateReturnRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.createReturn(payload).futureValue
      }
      ex.getMessage must include("400")
    }

    "handle different storn ID formats" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "returnResourceRef": "$returnResourceRef" }""")
          )
      )

      val payload1 = payload.copy(stornId = "STORN12345")
      val payload2 = payload.copy(stornId = "STORN-ABC-123")
      val payload3 = payload.copy(stornId = "12345678")

      connector.createReturn(payload1).futureValue mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
      connector.createReturn(payload2).futureValue mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
      connector.createReturn(payload3).futureValue mustBe CreateReturnResult(returnResourceRef = returnResourceRef)
    }
  }

  "getFullReturn" should {

    val url = "/formp-proxy/retrieve-return"

    val payload = GetReturnByRefRequest(
      returnResourceRef = returnResourceRef,
      storn = stornId
    )

    "return GetReturnRequest when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)(GetReturnByRefRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "stornId": "$stornId",
                   |  "returnResourceRef": "$returnResourceRef",
                   |  "sdltOrganisation": {
                   |    "isReturnUser": "Y",
                   |    "storn": "$stornId"
                   |  },
                   |  "returnInfo": {
                   |    "returnID": "$returnResourceRef",
                   |    "storn": "$stornId",
                   |    "status": "STARTED"
                   |  }
                   |}""".stripMargin
              )
          )
      )

      val result = connector.getFullReturn(payload).futureValue

      result.stornId mustBe Some(stornId)
      result.returnResourceRef mustBe Some(returnResourceRef)
      result.sdltOrganisation must not be None
      result.returnInfo must not be None
    }

    "return GetReturnRequest with minimal data" in {
      val payloadJson = Json.toJson(payload)(GetReturnByRefRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "stornId": "$stornId",
                   |  "returnResourceRef": "$returnResourceRef"
                   |}""".stripMargin
              )
          )
      )

      val result = connector.getFullReturn(payload).futureValue

      result.stornId mustBe Some(stornId)
      result.returnResourceRef mustBe Some(returnResourceRef)
    }

    "handle different returnResourceRef formats" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "stornId": "$stornId",
                   |  "returnResourceRef": "response-ref"
                   |}""".stripMargin
              )
          )
      )

      val payload1 = GetReturnByRefRequest("123456", stornId)
      val payload2 = GetReturnByRefRequest("RRF-2024-001", stornId)
      val payload3 = GetReturnByRefRequest("ABC-123-XYZ", stornId)

      connector.getFullReturn(payload1).futureValue.stornId mustBe Some(stornId)
      connector.getFullReturn(payload2).futureValue.stornId mustBe Some(stornId)
      connector.getFullReturn(payload3).futureValue.stornId mustBe Some(stornId)
    }

    "handle different storn formats" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "stornId": "response-storn",
                   |  "returnResourceRef": "$returnResourceRef"
                   |}""".stripMargin
              )
          )
      )

      val payload1 = GetReturnByRefRequest(returnResourceRef, "STORN123456")
      val payload2 = GetReturnByRefRequest(returnResourceRef, "STORN-ABC-123")
      val payload3 = GetReturnByRefRequest(returnResourceRef, "12345678")

      connector.getFullReturn(payload1).futureValue.returnResourceRef mustBe Some(returnResourceRef)
      connector.getFullReturn(payload2).futureValue.returnResourceRef mustBe Some(returnResourceRef)
      connector.getFullReturn(payload3).futureValue.returnResourceRef mustBe Some(returnResourceRef)
    }

    "fail when BE returns OK with invalid JSON" in {
      val payloadJson = Json.toJson(payload)(GetReturnByRefRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(OK).withBody("""not-json"""))
      )

      val ex = intercept[Exception] {
        connector.getFullReturn(payload).futureValue
      }
      ex.getMessage.toLowerCase must (include("json") or include("error") or include("parse"))
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)(GetReturnByRefRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.getFullReturn(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns NOT_FOUND" in {
      val payloadJson = Json.toJson(payload)(GetReturnByRefRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("Not found"))
      )

      val ex = intercept[Exception] {
        connector.getFullReturn(payload).futureValue
      }
      ex.getMessage must include("404")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)(GetReturnByRefRequest.format)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.getFullReturn(payload).futureValue
      }
      ex.getMessage must include("400")
    }
  }
}