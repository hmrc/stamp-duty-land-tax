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

class FillingReturnsControllerISpec
    extends BaseSpec
    with GuiceOneServerPerSuite
    with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val createReturn = s"$servicePrefix/filing/create/return"
  lazy val getFullReturn = s"$servicePrefix/filing/receive/full-return"

  def stubCreateReturnResponse(): Unit = {
    stubPost(
      "/formp-proxy/create/return",
      Status.OK,
      Json
        .toJson(CreateReturnResult(returnResourceRef = "returnResourceRef"))
        .toString
    )
  }

  def stubGetFullReturnResponse(): Unit = {
    stubPost(
      "/formp-proxy/retrieve-return",
      Status.OK,
      Json
        .toJson(
          GetReturnRequest()
        )
        .toString
    )
  }

  "Filling Returns" should {

    "call CreateReturn" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubCreateReturnResponse()
        val jsonBody = Json.toJson(
          CreateReturnRequest(
            stornId = "stornId",
            purchaserIsCompany = "true",
            surNameOrCompanyName = "surNameOrCompanyName",
            houseNumber = None,
            addressLine1 = "addressLine1",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postcode = None,
            transactionType = "transactionType"
          )
        )
        val result = wsClient
          .url(createReturn)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreateReturnResponse()
        val jsonBody = Json.toJson(
          CreateReturnRequest(
            stornId = "stornId",
            purchaserIsCompany = "true",
            surNameOrCompanyName = "surNameOrCompanyName",
            houseNumber = None,
            addressLine1 = "addressLine1",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postcode = None,
            transactionType = "transactionType"
          )
        )
        val result = wsClient
          .url(createReturn)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

    }

    "call GetFullReturn" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubGetFullReturnResponse()
        val jsonBody = Json.toJson(
          GetReturnByRefRequest(
            returnResourceRef = "purchaseRef",
            storn = "storn"
          )
        )
        val result = wsClient
          .url(getFullReturn)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 404:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubGetFullReturnResponse()
        val jsonBody = Json.toJson(
          GetReturnByRefRequest(
            returnResourceRef = "purchaseRef",
            storn = "storn"
          )
        )
        val result = wsClient
          .url(getFullReturn)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

  }

}
