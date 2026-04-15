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

class TransactionReturnsControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix            = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val updateTransactionUrl = s"$servicePrefix/filing/update/transaction"

  def stubUpdateTransactionResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/transaction", Status.CREATED,
      Json.toJson(UpdateTransactionReturn(updated = true)).toString)
  }

  "TransactionReturns" should {

    "call updateTransaction" when {

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdateTransactionResponse()
        val jsonBody = Json.toJson(
          UpdateTransactionRequest(
            storn             = "storn",
            returnResourceRef = "ref",
            transaction       = TransactionPayload()
          )
        )

        val result = wsClient.url(updateTransactionUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 201:Created:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdateTransactionResponse()
        val jsonBody = Json.toJson(
          UpdateTransactionRequest(
            storn             = "storn",
            returnResourceRef = "ref",
            transaction       = TransactionPayload()
          )
        )

        val result = wsClient.url(updateTransactionUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 201:Created:: authorised request with complete transaction payload" in {
        stubAuthorisedAsActivated()
        stubUpdateTransactionResponse()
        val jsonBody = Json.toJson(
          UpdateTransactionRequest(
            storn             = "storn",
            returnResourceRef = "ref",
            transaction       = TransactionPayload(
              claimingRelief  = Some("YES"),
              totalConsider   = Some("200000"),
              effectiveDate   = Some("2024-02-01"),
              contractDate    = Some("2024-01-15"),
              isLandExchanged = Some("NO")
            )
          )
        )

        val result = wsClient.url(updateTransactionUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }
    }
  }
}