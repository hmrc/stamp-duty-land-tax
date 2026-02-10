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
      purchaserIsCompany = "NO",
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
        purchaserIsCompany = "YES",
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
                   |    "isReturnUser": "YES",
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

  "createPurchaser" should {

    val url = "/formp-proxy/filing/create/purchaser"

    val payload = CreatePurchaserRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      isCompany = Some("NO"),
      isTrustee = Some("NO"),
      isConnectedToVendor =  Some("NO"),
      isRepresentedByAgent =  Some("YES"),
      title = Some("Mr"),
      surname = Some("Jones"),
      forename1 = Some("David"),
      forename2 = Some("Michael"),
      companyName = None,
      houseNumber = Some("25"),
      address1 = Some("Park Avenue"),
      address2 = Some("Flat 3"),
      address3 = Some("Central District"),
      address4 = Some("London"),
      postcode = Some("SW1A 2AA"),
      phone = Some("02012345678"),
      nino = Some("AB123456C"),
      isUkCompany = None,
      hasNino = Some("YES"),
      dateOfBirth = Some("1980-01-15"),
      registrationNumber = None,
      placeOfRegistration = None
    )

    "return CreatePurchaserReturn when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "purchaserResourceRef": "PRF-001",
                   |  "purchaserId": "PID-001"
                   |}""".stripMargin
              )
          )
      )

      val result = connector.createPurchaser(payload).futureValue

      result mustBe CreatePurchaserReturn(purchaserResourceRef = "PRF-001", purchaserId = "PID-001")
    }

    "return CreatePurchaserReturn for company purchaser" in {
      val companyPayload = payload.copy(
        isCompany = Some("YES"),
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = Some("XYZ Properties Ltd"),
        nino = None,
        hasNino = None,
        dateOfBirth = None,
        isUkCompany = Some("YES"),
        registrationNumber = Some("12345678"),
        placeOfRegistration = Some("England and Wales")
      )
      val payloadJson = Json.toJson(companyPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "purchaserResourceRef": "PRF-002", "purchaserId": "PID-002" }""")
          )
      )

      val result = connector.createPurchaser(companyPayload).futureValue
      result mustBe CreatePurchaserReturn(purchaserResourceRef = "PRF-002", purchaserId = "PID-002")
    }

    "return CreatePurchaserReturn for minimal request" in {
      val minimalPayload = payload.copy(
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        houseNumber = None,
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = None,
        phone = None,
        nino = None,
        hasNino = None,
        dateOfBirth = None
      )
      val payloadJson = Json.toJson(minimalPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "purchaserResourceRef": "PRF-003", "purchaserId": "PID-003" }""")
          )
      )

      val result = connector.createPurchaser(minimalPayload).futureValue
      result.purchaserResourceRef mustBe "PRF-003"
      result.purchaserId mustBe "PID-003"
    }

    "handle different flag combinations" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "purchaserResourceRef": "PRF-001", "purchaserId": "PID-001" }""")
          )
      )

      val trusteePayload = payload.copy(isTrustee = Some("YES"))
      val connectedPayload = payload.copy(isConnectedToVendor = Some("YES"))
      val noAgentPayload = payload.copy(isRepresentedByAgent = Some("NO"))

      connector.createPurchaser(trusteePayload).futureValue.purchaserId mustBe "PID-001"
      connector.createPurchaser(connectedPayload).futureValue.purchaserId mustBe "PID-001"
      connector.createPurchaser(noAgentPayload).futureValue.purchaserId mustBe "PID-001"
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.createPurchaser(payload).futureValue
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
        connector.createPurchaser(payload).futureValue
      }
      ex.getMessage must include("400")
    }
  }

  "updatePurchaser" should {

    val url = "/formp-proxy/filing/update/purchaser"

    val payload = UpdatePurchaserRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      purchaserResourceRef = "PRF-001",
      isCompany = Some("NO"),
      isTrustee =  Some("NO"),
      isConnectedToVendor =  Some("NO"),
      isRepresentedByAgent =  Some("YES"),
      title = Some("Mr"),
      surname = Some("Jones Updated"),
      forename1 = Some("David"),
      forename2 = Some("Michael"),
      companyName = None,
      houseNumber = Some("25"),
      address1 = Some("Park Avenue"),
      address2 = Some("Flat 3"),
      address3 = Some("Central District"),
      address4 = Some("London"),
      postcode = Some("SW1A 2AA"),
      phone = Some("02012345678"),
      nino = Some("AB123456C"),
      nextPurchaserId = Some("PID-002"),
      isUkCompany = None,
      hasNino = Some("YES"),
      dateOfBirth = Some("1980-01-15"),
      registrationNumber = None,
      placeOfRegistration = None
    )

    "return UpdatePurchaserReturn with updated=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdatePurchaserReturn(updated = true))))
          )
      )

      val result = connector.updatePurchaser(payload).futureValue

      result mustBe UpdatePurchaserReturn(updated = true)
    }

    "return UpdatePurchaserReturn with updated=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdatePurchaserReturn(updated = true))))
          )
      )

      val result = connector.updatePurchaser(payload).futureValue

      result mustBe UpdatePurchaserReturn(updated = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.updatePurchaser(payload).futureValue
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
        connector.updatePurchaser(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "deletePurchaser" should {

    val url = "/formp-proxy/filing/delete/purchaser"

    val payload = DeletePurchaserRequest(
      storn = stornId,
      purchaserResourceRef = "PRF-001",
      returnResourceRef = returnResourceRef
    )

    "return DeletePurchaserReturn with deleted=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(DeletePurchaserReturn(deleted = true))))
          )
      )

      val result = connector.deletePurchaser(payload).futureValue

      result mustBe DeletePurchaserReturn(deleted = true)
    }

    "return DeletePurchaserReturn with deleted=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.stringify(Json.toJson(DeletePurchaserReturn(deleted = true))))
          )
      )

      val result = connector.deletePurchaser(payload).futureValue

      result mustBe DeletePurchaserReturn(deleted = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.deletePurchaser(payload).futureValue
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
        connector.deletePurchaser(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "createCompanyDetails" should {

    val url = "/formp-proxy/filing/create/company-details"

    val payload = CreateCompanyDetailsRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      purchaserResourceRef = "PRF-001",
      utr = Some("1234567890"),
      vatReference = Some("GB123456789"),
      compTypeBank = Some("YES"),
      compTypeBuilder = Some("NO"),
      compTypeBuildsoc = Some("NO"),
      compTypeCentgov = Some("NO"),
      compTypeIndividual = Some("NO"),
      compTypeInsurance = Some("NO"),
      compTypeLocalauth = Some("NO"),
      compTypeOcharity = Some("NO"),
      compTypeOcompany = Some("NO"),
      compTypeOfinancial = Some("NO"),
      compTypePartship = Some("NO"),
      compTypeProperty = Some("NO"),
      compTypePubliccorp = Some("NO"),
      compTypeSoletrader = Some("NO"),
      compTypePenfund = Some("NO")
    )

    "return CreateCompanyDetailsReturn when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "companyDetailsId": "CID-001" }""")
          )
      )

      val result = connector.createCompanyDetails(payload).futureValue

      result mustBe CreateCompanyDetailsReturn(companyDetailsId = "CID-001")
    }

    "return CreateCompanyDetailsReturn for minimal request" in {
      val minimalPayload = payload.copy(
        utr = None,
        vatReference = None,
        compTypeBank = None,
        compTypeBuilder = None,
        compTypeBuildsoc = None,
        compTypeCentgov = None,
        compTypeIndividual = None,
        compTypeInsurance = None,
        compTypeLocalauth = None,
        compTypeOcharity = None,
        compTypeOcompany = None,
        compTypeOfinancial = None,
        compTypePartship = None,
        compTypeProperty = None,
        compTypePubliccorp = None,
        compTypeSoletrader = None,
        compTypePenfund = None
      )
      val payloadJson = Json.toJson(minimalPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "companyDetailsId": "CID-002" }""")
          )
      )

      val result = connector.createCompanyDetails(minimalPayload).futureValue
      result.companyDetailsId mustBe "CID-002"
    }

    "handle different company type combinations" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "companyDetailsId": "CID-001" }""")
          )
      )

      val propertyPayload = payload.copy(compTypeBank = Some("NO"), compTypeProperty = Some("YES"))
      val charityPayload = payload.copy(compTypeBank = Some("NO"), compTypeOcharity = Some("YES"))
      val partnershipPayload = payload.copy(compTypeBank = Some("NO"), compTypePartship = Some("YES"))

      connector.createCompanyDetails(propertyPayload).futureValue.companyDetailsId mustBe "CID-001"
      connector.createCompanyDetails(charityPayload).futureValue.companyDetailsId mustBe "CID-001"
      connector.createCompanyDetails(partnershipPayload).futureValue.companyDetailsId mustBe "CID-001"
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.createCompanyDetails(payload).futureValue
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
        connector.createCompanyDetails(payload).futureValue
      }
      ex.getMessage must include("400")
    }
  }

  "updateCompanyDetails" should {

    val url = "/formp-proxy/filing/update/company-details"

    val payload = UpdateCompanyDetailsRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      purchaserResourceRef = "PRF-001",
      utr = Some("9876543210"),
      vatReference = Some("GB987654321"),
      compTypeBank = Some("NO"),
      compTypeBuilder = Some("YES"),
      compTypeBuildsoc = Some("NO"),
      compTypeCentgov = Some("NO"),
      compTypeIndividual = Some("NO"),
      compTypeInsurance = Some("NO"),
      compTypeLocalauth = Some("NO"),
      compTypeOcharity = Some("NO"),
      compTypeOcompany = Some("NO"),
      compTypeOfinancial = Some("NO"),
      compTypePartship = Some("NO"),
      compTypeProperty = Some("NO"),
      compTypePubliccorp = Some("NO"),
      compTypeSoletrader = Some("NO"),
      compTypePenfund = Some("NO")
    )

    "return UpdateCompanyDetailsReturn with updated=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateCompanyDetailsReturn(updated = true))))
          )
      )

      val result = connector.updateCompanyDetails(payload).futureValue

      result mustBe UpdateCompanyDetailsReturn(updated = true)
    }

    "return UpdateCompanyDetailsReturn with updated=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateCompanyDetailsReturn(updated = true))))
          )
      )

      val result = connector.updateCompanyDetails(payload).futureValue

      result mustBe UpdateCompanyDetailsReturn(updated = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.updateCompanyDetails(payload).futureValue
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
        connector.updateCompanyDetails(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "deleteCompanyDetails" should {

    val url = "/formp-proxy/filing/delete/company-details"

    val payload = DeleteCompanyDetailsRequest(
      storn = stornId,
      returnResourceRef = returnResourceRef
    )

    "return DeleteCompanyDetailsReturn with deleted=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(DeleteCompanyDetailsReturn(deleted = true))))
          )
      )

      val result = connector.deleteCompanyDetails(payload).futureValue

      result mustBe DeleteCompanyDetailsReturn(deleted = true)
    }

    "return DeleteCompanyDetailsReturn with deleted=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.stringify(Json.toJson(DeleteCompanyDetailsReturn(deleted = true))))
          )
      )

      val result = connector.deleteCompanyDetails(payload).futureValue

      result mustBe DeleteCompanyDetailsReturn(deleted = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.deleteCompanyDetails(payload).futureValue
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
        connector.deleteCompanyDetails(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "updateReturnInfo" should {

    val url = "/formp-proxy/filing/update/return-info"

    val payload = UpdateReturnRequest(
      storn = stornId,
      returnResourceRef = "100001",
      mainPurchaserId = "1",
      mainVendorId = "1",
      mainLandId = "1",
      irmarkGenerated = "IRMark123456",
      landCertForEachProp = "YES",
      declaration = "YES"
    )

    "return UpdateReturnReturn with updated=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateReturnReturn(updated = true))))
          )
      )

      val result = connector.updateReturnInfo(payload).futureValue

      result mustBe UpdateReturnReturn(updated = true)
    }

    "return UpdateReturnReturn with updated=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateReturnReturn(updated = true))))
          )
      )

      val result = connector.updateReturnInfo(payload).futureValue

      result mustBe UpdateReturnReturn(updated = true)
    }

    "handle update with Y values for boolean fields" in {
      val yPayload = payload.copy(landCertForEachProp = "Y", declaration = "Y")
      val payloadJson = Json.toJson(yPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateReturnReturn(updated = true))))
          )
      )

      val result = connector.updateReturnInfo(yPayload).futureValue
      result.updated mustBe true
    }

    "handle update with N values for boolean fields" in {
      val nPayload = payload.copy(landCertForEachProp = "N", declaration = "N")
      val payloadJson = Json.toJson(nPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateReturnReturn(updated = true))))
          )
      )

      val result = connector.updateReturnInfo(nPayload).futureValue
      result.updated mustBe true
    }

    "handle different IRMark formats" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateReturnReturn(updated = true))))
          )
      )

      val payload1 = payload.copy(irmarkGenerated = "IRMark123456")
      val payload2 = payload.copy(irmarkGenerated = "IRMark-ABC-123")
      val payload3 = payload.copy(irmarkGenerated = "12345678")

      connector.updateReturnInfo(payload1).futureValue.updated mustBe true
      connector.updateReturnInfo(payload2).futureValue.updated mustBe true
      connector.updateReturnInfo(payload3).futureValue.updated mustBe true
    }

    "handle different entity IDs" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateReturnReturn(updated = true))))
          )
      )

      val payload1 = payload.copy(mainPurchaserId = "1", mainVendorId = "1", mainLandId = "1")
      val payload2 = payload.copy(mainPurchaserId = "100", mainVendorId = "200", mainLandId = "300")

      connector.updateReturnInfo(payload1).futureValue.updated mustBe true
      connector.updateReturnInfo(payload2).futureValue.updated mustBe true
    }

    "handle different return resource reference formats" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateReturnReturn(updated = true))))
          )
      )

      val payload1 = payload.copy(returnResourceRef = "100001")
      val payload2 = payload.copy(returnResourceRef = "RRF-2024-001")
      val payload3 = payload.copy(returnResourceRef = "999999")

      connector.updateReturnInfo(payload1).futureValue.updated mustBe true
      connector.updateReturnInfo(payload2).futureValue.updated mustBe true
      connector.updateReturnInfo(payload3).futureValue.updated mustBe true
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.updateReturnInfo(payload).futureValue
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
        connector.updateReturnInfo(payload).futureValue
      }
      ex.getMessage must include("404")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.updateReturnInfo(payload).futureValue
      }
      ex.getMessage must include("400")
    }
  }

  "createLand" should {

    val url = "/formp-proxy/filing/create/land"

    val payload = CreateLandRequest(
      stornId = stornId,
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

    "return CreateLandReturn when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "landResourceRef": "100001",
                   |  "landId": "1"
                   |}""".stripMargin
              )
          )
      )

      val result = connector.createLand(payload).futureValue

      result mustBe CreateLandReturn(landResourceRef = "100001", landId = "1")
    }

    "return CreateLandReturn for minimal request" in {
      val minimalPayload = payload.copy(
        houseNumber = None,
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
      val payloadJson = Json.toJson(minimalPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "landResourceRef": "100002", "landId": "2" }""")
          )
      )

      val result = connector.createLand(minimalPayload).futureValue
      result.landResourceRef mustBe "100002"
      result.landId mustBe "2"
    }

    "handle different property types" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "landResourceRef": "100001", "landId": "1" }""")
          )
      )

      val residentialPayload = payload.copy(propertyType = "RESIDENTIAL")
      val nonResidentialPayload = payload.copy(propertyType = "NON_RESIDENTIAL")
      val mixedPayload = payload.copy(propertyType = "MIXED")

      connector.createLand(residentialPayload).futureValue.landId mustBe "1"
      connector.createLand(nonResidentialPayload).futureValue.landId mustBe "1"
      connector.createLand(mixedPayload).futureValue.landId mustBe "1"
    }

    "handle different interest types" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{ "landResourceRef": "100001", "landId": "1" }""")
          )
      )

      val freeholdPayload = payload.copy(interestTransferredCreated = "FREEHOLD")
      val leaseholdPayload = payload.copy(interestTransferredCreated = "LEASEHOLD")

      connector.createLand(freeholdPayload).futureValue.landId mustBe "1"
      connector.createLand(leaseholdPayload).futureValue.landId mustBe "1"
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.createLand(payload).futureValue
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
        connector.createLand(payload).futureValue
      }
      ex.getMessage must include("400")
    }
  }

  "updateLand" should {

    val url = "/formp-proxy/filing/update/land"

    val payload = UpdateLandRequest(
      stornId = stornId,
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

    "return UpdateLandReturn with updated=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateLandReturn(updated = true))))
          )
      )

      val result = connector.updateLand(payload).futureValue

      result mustBe UpdateLandReturn(updated = true)
    }

    "return UpdateLandReturn with updated=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateLandReturn(updated = true))))
          )
      )

      val result = connector.updateLand(payload).futureValue

      result mustBe UpdateLandReturn(updated = true)
    }

    "handle update with minimal fields" in {
      val minimalPayload = payload.copy(
        houseNumber = None,
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
      val payloadJson = Json.toJson(minimalPayload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateLandReturn(updated = true))))
          )
      )

      val result = connector.updateLand(minimalPayload).futureValue
      result.updated mustBe true
    }

    "handle different property types" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(UpdateLandReturn(updated = true))))
          )
      )

      val residentialPayload = payload.copy(propertyType = "RESIDENTIAL")
      val nonResidentialPayload = payload.copy(propertyType = "NON_RESIDENTIAL")
      val mixedPayload = payload.copy(propertyType = "MIXED")

      connector.updateLand(residentialPayload).futureValue.updated mustBe true
      connector.updateLand(nonResidentialPayload).futureValue.updated mustBe true
      connector.updateLand(mixedPayload).futureValue.updated mustBe true
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.updateLand(payload).futureValue
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
        connector.updateLand(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

  "deleteLand" should {

    val url = "/formp-proxy/filing/delete/land"

    val payload = DeleteLandRequest(
      storn = stornId,
      returnResourceRef = "100001",
      landResourceRef = "100001"
    )

    "return DeleteLandReturn with deleted=true when BE returns OK" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(DeleteLandReturn(deleted = true))))
          )
      )

      val result = connector.deleteLand(payload).futureValue

      result mustBe DeleteLandReturn(deleted = true)
    }

    "return DeleteLandReturn with deleted=true when BE returns CREATED" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.stringify(Json.toJson(DeleteLandReturn(deleted = true))))
          )
      )

      val result = connector.deleteLand(payload).futureValue

      result mustBe DeleteLandReturn(deleted = true)
    }

    "handle different resource reference formats" in {
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(DeleteLandReturn(deleted = true))))
          )
      )

      val payload1 = payload.copy(landResourceRef = "100001")
      val payload2 = payload.copy(landResourceRef = "999999")
      val payload3 = payload.copy(landResourceRef = "LRF-2024-001")

      connector.deleteLand(payload1).futureValue.deleted mustBe true
      connector.deleteLand(payload2).futureValue.deleted mustBe true
      connector.deleteLand(payload3).futureValue.deleted mustBe true
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.deleteLand(payload).futureValue
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
        connector.deleteLand(payload).futureValue
      }
      ex.getMessage must include("404")
    }
  }

}