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
import models.filing.{ReturnVersionUpdateRequest, ReturnVersionUpdateReturn}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.http.Status.{CREATED, FORBIDDEN}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class ReturnVersioningControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val updateReturnVersionUrl = s"$servicePrefix/filing/update/return-version"

  def stubUpdateReturnVersionResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/return-version", Status.CREATED,
      Json.toJson( ReturnVersionUpdateReturn(newVersion = 1)).toString)
  }

  "ReturnsVersioning" should {

    "call updateReturnVersion" when {

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdateReturnVersionResponse()
        val jsonBody = Json.toJson(ReturnVersionUpdateRequest(storn = "storn", returnResourceRef = "Ref", currentVersion = "2"))

        val result = wsClient.url(updateReturnVersionUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdateReturnVersionResponse()
        val jsonBody = Json.toJson(ReturnVersionUpdateRequest(storn = "storn", returnResourceRef = "Ref", currentVersion = "2"))
        
        val result = wsClient.url(updateReturnVersionUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

    }
  }

}