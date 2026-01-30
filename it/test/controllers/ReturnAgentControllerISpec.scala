/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import base.BaseSpec
import itutil.ApplicationWithWiremock
import models.filing.*
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.http.Status.{CREATED, FORBIDDEN}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class ReturnAgentControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val deleteReturnAgent = s"$servicePrefix/filing/delte/return-agent"
  lazy val createReturnAgent = s"$servicePrefix/filing/create/return-agent"
  lazy val updateReturnAgent = s"$servicePrefix/filing/update/return-agent"


  def stubDeleteReturnAgentResponse(): Unit = {
    stubPost("/formp-proxy/filing/delete/return-agent", Status.OK,
      Json.toJson(DeleteReturnAgentReturn(deleted = true)).toString)
  }

  def stubCreateReturnAgentResponse(): Unit = {
    stubPost("/formp-proxy/filing/create/return-agent", Status.OK,
      Json.toJson(
        CreateReturnAgentReturn(
          returnAgentID = "returnAgentID"
        )
      ).toString)
  }

  def stubUpdateReturnAgentResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/return-agent", Status.OK,
      Json.toJson(
        UpdateReturnAgentReturn(
          updated = true
        )
      ).toString)
  }


  "Agent Returns" should {

    "call DeleteReturnAgent" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubDeleteReturnAgentResponse()
        val jsonBody = Json.toJson(DeleteReturnAgentRequest(storn = "storn", returnResourceRef = "returnRef", agentType = "agentType"))
        val result = wsClient.url(deleteReturnAgent)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 201:CREATED:: authorised request with not yet activated enrollment" in {
        stubAuthorisedAsNotYetActivated()
        stubDeleteReturnAgentResponse()
        val jsonBody = Json.toJson(DeleteReturnAgentRequest(storn = "storn", returnResourceRef = "returnRef", agentType = "agentType"))
        val result = wsClient.url(deleteReturnAgent)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubDeleteReturnAgentResponse()
        val jsonBody = Json.toJson(DeleteReturnAgentRequest(storn = "storn", returnResourceRef = "returnRef", agentType = "agentType"))
        val result = wsClient.url(deleteReturnAgent)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

    }

    "call ReturnAgent" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubCreateReturnAgentResponse()
        val jsonBody = Json.toJson(
          CreateReturnAgentRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            agentType = "agentType",
            name = "name",
            houseNumber= None,
            addressLine1 = "addressLine1",
            addressLine2= None,
            addressLine3 = None,
            addressLine4= None,
            postcode = "postcode",
            phoneNumber= None,
            email = None,
            agentReference = None,
            isAuthorised = None
          )
        )
        val result = wsClient.url(createReturnAgent)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 404:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreateReturnAgentResponse()
        val jsonBody = Json.toJson(
          CreateReturnAgentRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            agentType = "agentType",
            name = "name",
            houseNumber = None,
            addressLine1 = "addressLine1",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postcode = "postcode",
            phoneNumber = None,
            email = None,
            agentReference = None,
            isAuthorised = None
          )
        )
        val result = wsClient.url(createReturnAgent)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call UpdateReturnAgent" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdateReturnAgentResponse()
        val jsonBody = Json.toJson(
          UpdateReturnAgentRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            agentType = "agentType",
            name = "name",
            houseNumber = None,
            addressLine1 = "addressLine1",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postcode = "postcode",
            phoneNumber = None,
            email = None,
            agentReference = None,
            isAuthorised = None
          )
        )
        val result = wsClient.url(updateReturnAgent)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdateReturnAgentResponse()
        val jsonBody = Json.toJson(
          UpdateReturnAgentRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            agentType = "agentType",
            name = "name",
            houseNumber = None,
            addressLine1 = "addressLine1",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postcode = "postcode",
            phoneNumber = None,
            email = None,
            agentReference = None,
            isAuthorised = None
          )
        )
        val result = wsClient.url(updateReturnAgent)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

  }

}