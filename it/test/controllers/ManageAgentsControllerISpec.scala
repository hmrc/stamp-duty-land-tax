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
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.http.Status

class ManageAgentsControllerISpec extends BaseSpec 
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  lazy val getOrganisation = s"http://localhost:$port/stamp-duty-land-tax/manage-agents/get-sdlt-organisation?storn=1001"

  def stubGetOrg(): Unit = {
    stubPost("/stamp-duty-land-tax-stub/organisation", Status.OK, getOrgJsonBodyResponse)
  }

  private val getOrgJsonBodyResponse: String =
    """{
      |  "storn": "storn",
      |  "version": "1",
      |  "agents": []
      |}""".stripMargin

  "Organisation" should {

    "call get-sdlt-organisation" when {

      "return a 404:Forbidden when no auth in scope" in {
        stubUnauthorised()
        val result = wsClient.url(getOrganisation)
          .get()

        result.status shouldBe FORBIDDEN
      }

      "return a 200:OK when no auth in scope2" in {
        stubAuthorised()
        stubGetOrg()
        val result = wsClient.url(getOrganisation)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .get()

        result.status shouldBe OK
      }
    }
  }

}