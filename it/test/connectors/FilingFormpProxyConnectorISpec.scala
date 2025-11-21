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
import itutil.ApplicationWithWiremock
import models.filing.*
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

  "createVendor" should {

    val url = "/formp-proxy/filing/create/vendor"

    val payload = CreateVendorRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      title = Some("Mr"),
      forename1 = Some("John"),
      forename2 = Some("Paul"),
      name = "Smith",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Apartment 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = Some("TE23 5TT"),
      isRepresentedByAgent = "YES"
    )

    "return CreateVendorReturn when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "vendorResourceRef": "VRF-001",
                   |  "vendorId": "VID-001"
                   |}""".stripMargin
              )
          )
      )

      val result = connector.createVendor(payload).futureValue

      result mustBe CreateVendorReturn(vendorResourceRef = "VRF-001", vendorId = "VID-001")
    }

    "return CreateVendorReturn for minimal request" in {
      val minimalPayload = payload.copy(
        title = None,
        forename1 = None,
        forename2 = None,
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None
      )
      val payloadJson = Json.toJson(minimalPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "vendorResourceRef": "VRF-001", "vendorId": "VID-001" }""")
          )
      )

      val result = connector.createVendor(minimalPayload).futureValue
      result.vendorResourceRef mustBe "VRF-001"
      result.vendorId mustBe "VID-001"
    }

    "handle different isRepresentedByAgent values" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "vendorResourceRef": "VRF-001", "vendorId": "VID-001" }""")
          )
      )

      val yesPayload = payload.copy(isRepresentedByAgent = "YES")
      val noPayload = payload.copy(isRepresentedByAgent = "NO")

      connector.createVendor(yesPayload).futureValue.vendorId mustBe "VID-001"
      connector.createVendor(noPayload).futureValue.vendorId mustBe "VID-001"
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.createVendor(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.createVendor(payload).futureValue
      }
      ex.getMessage must include("400")
    }
  }

  "updateVendor" should {

    val url = "/formp-proxy/filing/update/vendor"

    val payload = UpdateVendorRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      title = Some("Mr"),
      forename1 = Some("John"),
      forename2 = Some("Paul"),
      name = "Smith Updated",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Apartment 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = Some("TE23 5TT"),
      isRepresentedByAgent = "YES",
      vendorResourceRef = "VRF-001",
      nextVendorId = Some("VID-002")
    )

    "return UpdateVendorReturn with updated=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateVendorReturn(updated = true))))
          )
      )

      val result = connector.updateVendor(payload).futureValue

      result mustBe UpdateVendorReturn(updated = true)
    }

    "return UpdateVendorReturn with updated=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateVendorReturn(updated = true))))
          )
      )

      val result = connector.updateVendor(payload).futureValue

      result mustBe UpdateVendorReturn(updated = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.updateVendor(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns NOT_FOUND" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("Not found"))
      )

      val ex = intercept[Exception] {
        connector.updateVendor(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "deleteVendor" should {

    val url = "/formp-proxy/filing/delete/vendor"

    val payload = DeleteVendorRequest(
      storn = stornId,
      vendorResourceRef = "VRF-001",
      returnResourceRef = "VID-001"
    )

    "return DeleteVendorReturn with deleted=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(DeleteVendorReturn(deleted = true))))
          )
      )

      val result = connector.deleteVendor(payload).futureValue

      result mustBe DeleteVendorReturn(deleted = true)
    }

    "return DeleteVendorReturn with deleted=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.stringify(Json.toJson(DeleteVendorReturn(deleted = true))))
          )
      )

      val result = connector.deleteVendor(payload).futureValue

      result mustBe DeleteVendorReturn(deleted = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.deleteVendor(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns NOT_FOUND" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("Not found"))
      )

      val ex = intercept[Exception] {
        connector.deleteVendor(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "createReturnAgent" should {

    val url = "/formp-proxy/filing/create/return-agent"

    val payload = CreateReturnAgentRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      agentType = "VENDOR",
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

    "return CreateReturnAgentReturn when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "returnAgentID": "AGID-001" }""")
          )
      )

      val result = connector.createReturnAgent(payload).futureValue

      result mustBe CreateReturnAgentReturn(returnAgentID = "AGID-001")
    }

    "return CreateReturnAgentReturn for minimal request" in {
      val minimalPayload = payload.copy(
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )
      val payloadJson = Json.toJson(minimalPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "returnAgentID": "AGID-001" }""")
          )
      )

      val result = connector.createReturnAgent(minimalPayload).futureValue
      result.returnAgentID mustBe "AGID-001"
    }

    "handle different agent types" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "returnAgentID": "AGID-001" }""")
          )
      )

      val solicitorPayload = payload.copy(agentType = "VENDOR")
      val conveyancerPayload = payload.copy(agentType = "CONVEYANCER")
      val otherPayload = payload.copy(agentType = "OTHER")

      connector.createReturnAgent(solicitorPayload).futureValue.returnAgentID mustBe "AGID-001"
      connector.createReturnAgent(conveyancerPayload).futureValue.returnAgentID mustBe "AGID-001"
      connector.createReturnAgent(otherPayload).futureValue.returnAgentID mustBe "AGID-001"
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.createReturnAgent(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.createReturnAgent(payload).futureValue
      }
      ex.getMessage must include("400")
    }
  }

  "updateReturnAgent" should {

    val url = "/formp-proxy/filing/update/return-agent"

    val payload = UpdateReturnAgentRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      agentType = "VENDOR",
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

    "return UpdateReturnAgentReturn with updated=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateReturnAgentReturn(updated = true))))
          )
      )

      val result = connector.updateReturnAgent(payload).futureValue

      result mustBe UpdateReturnAgentReturn(updated = true)
    }

    "return UpdateReturnAgentReturn with updated=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.stringify(Json.toJson(UpdateReturnAgentReturn(updated = true))))
          )
      )

      val result = connector.updateReturnAgent(payload).futureValue

      result mustBe UpdateReturnAgentReturn(updated = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.updateReturnAgent(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns NOT_FOUND" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("Not found"))
      )

      val ex = intercept[Exception] {
        connector.updateReturnAgent(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "deleteReturnAgent" should {

    val url = "/formp-proxy/filing/delete/return-agent"

    val payload = DeleteReturnAgentRequest(
      storn = stornId,
      returnResourceRef = returnResourceRef,
      agentType = "VENDOR"
    )

    "return DeleteReturnAgentReturn with deleted=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(DeleteReturnAgentReturn(deleted = true))))
          )
      )

      val result = connector.deleteReturnAgent(payload).futureValue

      result mustBe DeleteReturnAgentReturn(deleted = true)
    }

    "return DeleteReturnAgentReturn with deleted=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.stringify(Json.toJson(DeleteReturnAgentReturn(deleted = true))))
          )
      )

      val result = connector.deleteReturnAgent(payload).futureValue

      result mustBe DeleteReturnAgentReturn(deleted = true)
    }

    "handle different agent types" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(aResponse().withStatus(OK)
          .withBody(Json.stringify(Json.toJson(DeleteReturnAgentReturn(deleted = true))))
          )
      )

      val vendorPayload = payload.copy(agentType = "VENDOR")
      val purchaserPayload = payload.copy(agentType = "PURCHASER")

      connector.deleteReturnAgent(vendorPayload).futureValue.deleted mustBe true
      connector.deleteReturnAgent(purchaserPayload).futureValue.deleted mustBe true
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.deleteReturnAgent(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns NOT_FOUND" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("Not found"))
      )

      val ex = intercept[Exception] {
        connector.deleteReturnAgent(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "updateReturnVersioning" should {

    val url = "/formp-proxy/filing/update/return-version"

    val payload = ReturnVersionUpdateRequest(
      storn = stornId,
      returnResourceRef = returnResourceRef,
      currentVersion = "1.0"
    )

    "return ReturnVersionUpdateReturn with newVersion 1 when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(ReturnVersionUpdateReturn(newVersion = 1))))
          )
      )

      val result = connector.updateReturnVersioning(payload).futureValue

      result mustBe ReturnVersionUpdateReturn(newVersion = 1)
    }

    "return ReturnVersionUpdateReturn with newVersion = 1 when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.stringify(Json.toJson(ReturnVersionUpdateReturn(newVersion = 1))))
          )
      )

      val result = connector.updateReturnVersioning(payload).futureValue

      result mustBe ReturnVersionUpdateReturn(newVersion = 1)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.updateReturnVersioning(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns NOT_FOUND" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("Not found"))
      )

      val ex = intercept[Exception] {
        connector.updateReturnVersioning(payload).futureValue
      }
      ex.getMessage must include("404")
    }

    "propagate an upstream error when BE returns CONFLICT" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(CONFLICT).withBody("Version conflict"))
      )

      val ex = intercept[Exception] {
        connector.updateReturnVersioning(payload).futureValue
      }
      ex.getMessage must include("409")
    }
  }
}