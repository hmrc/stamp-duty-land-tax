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
import models.agent.{DeletePredefinedAgentRequest, DeletePredefinedAgentResponse}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class ManageAgentsControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  lazy val getOrganisationUrl = s"http://localhost:$port/stamp-duty-land-tax/manage-agents/get-sdlt-organisation?storn=1001"
  lazy val deleteAgentUrl = s"http://localhost:$port/stamp-duty-land-tax/manage-agents/delete/predefined-agent"

  def stubGetOrgResponse(): Unit = {
    stubPost("/stamp-duty-land-tax-stub/organisation", Status.OK, getOrgJsonBodyResponse)
  }

  def stubDeleteAgentResponse(): Unit = {
    stubPost("/stamp-duty-land-tax-stub/delete/predefined-agent", Status.OK,
      Json.toJson(DeletePredefinedAgentResponse(deleted = true)).toString)
  }

  private val getOrgJsonBodyResponse: String =
    """{
      |  "storn": "storn",
      |  "version": "1",
      |  "agents": []
      |}""".stripMargin

  "Organisation" should {

    "call GetOrganisation" when {

      "return a 404:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        val result = wsClient.url(getOrganisationUrl)
          .get()

        result.status shouldBe FORBIDDEN
      }

      "return a 200:OK:: authorised request" in {
        stubAuthorised()
        stubGetOrgResponse()
        val result = wsClient.url(getOrganisationUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .get()

        result.status shouldBe OK
      }
    }
  }

  "Agent" should {

    "call DeletePredefinedAgent" when {

      "return a 200:OK:: authorised request" in {
        stubAuthorised()
        stubDeleteAgentResponse()
        val jsonBody = Json.toJson(DeletePredefinedAgentRequest(storn = "storn", agentReferenceNumber = "agentRef"))
        val result = wsClient.url(deleteAgentUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 404:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubDeleteAgentResponse()
        val jsonBody = Json.toJson(DeletePredefinedAgentRequest(storn = "storn", agentReferenceNumber = "agentRef"))
        val result = wsClient.url(deleteAgentUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

    }

  }

}