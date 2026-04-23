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
import play.api.http.Status.{CREATED, FORBIDDEN, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class ResidencyReturnsControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val createResidencyUrl = s"$servicePrefix/filing/create/residency"
  lazy val updateResidencyUrl = s"$servicePrefix/filing/update/residency"
  lazy val deleteResidencyUrl = s"$servicePrefix/filing/delete/residency"

  def stubCreateResidencyResponse(): Unit = {
    stubPost("/formp-proxy/filing/create/residency", Status.CREATED,
      Json.toJson(CreateResidencyReturn(created = true)).toString)
  }

  def stubUpdateResidencyResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/residency", Status.OK,
      Json.toJson(UpdateResidencyReturn(updated = true)).toString)
  }

  def stubDeleteResidencyResponse(): Unit = {
    stubPost("/formp-proxy/filing/delete/residency", Status.OK,
      Json.toJson(DeleteResidencyReturn(deleted = true)).toString)
  }

  "ResidencyReturns" should {

    "call createResidency" when {

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreateResidencyResponse()
        val jsonBody = Json.toJson(
          CreateResidencyRequest(
            stornId = "storn",
            returnResourceRef = "ref",
            residency = ResidencyPayload(
              isNonUkResidents = "NO",
              isCompany        = "NO",
              isCrownRelief    = "NO"
            )
          )
        )

        val result = wsClient.url(createResidencyUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 201:Created:: authorised request" in {
        stubAuthorisedAsActivated()
        stubCreateResidencyResponse()
        val jsonBody = Json.toJson(
          CreateResidencyRequest(
            stornId = "storn",
            returnResourceRef = "ref",
            residency = ResidencyPayload(
              isNonUkResidents = "NO",
              isCompany        = "NO",
              isCrownRelief    = "NO"
            )
          )
        )

        val result = wsClient.url(createResidencyUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }
    }

    "call updateResidency" when {

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdateResidencyResponse()
        val jsonBody = Json.toJson(
          UpdateResidencyRequest(
            stornId = "storn",
            returnResourceRef = "ref",
            residency = ResidencyPayload(
              isNonUkResidents = "NO",
              isCompany        = "NO",
              isCrownRelief    = "NO"
            )
          )
        )

        val result = wsClient.url(updateResidencyUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdateResidencyResponse()
        val jsonBody = Json.toJson(
          UpdateResidencyRequest(
            stornId = "storn",
            returnResourceRef = "ref",
            residency = ResidencyPayload(
              isNonUkResidents = "NO",
              isCompany        = "NO",
              isCrownRelief    = "NO"
            )
          )
        )

        val result = wsClient.url(updateResidencyUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }
    }

    "call deleteResidency" when {

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubDeleteResidencyResponse()
        val jsonBody = Json.toJson(
          DeleteResidencyRequest(storn = "storn", returnResourceRef = "ref"))

        val result = wsClient.url(deleteResidencyUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAsActivated()
        stubDeleteResidencyResponse()
        val jsonBody = Json.toJson(
          DeleteResidencyRequest(storn = "storn", returnResourceRef = "ref"))

        val result = wsClient.url(deleteResidencyUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }
    }
  }
}