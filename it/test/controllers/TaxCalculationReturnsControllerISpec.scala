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
import play.api.http.Status.{ FORBIDDEN, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class TaxCalculationReturnsControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val updateTaxCalc = s"$servicePrefix/filing/update/tax-calculation"


  def stubUpdateTaxCalcResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/tax-calculation", Status.OK,
      Json.toJson(
        UpdateTaxCalculationReturn(
          updated = true
        )
      ).toString)
  }

  "Tax Calculation Returns" should {

    "call UpdateTaxCalculationReturns" when {

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdateTaxCalcResponse()
        val jsonBody = Json.toJson(
          UpdateTaxCalculationRequest(
            stornId = "STORN12345",
            returnResourceRef = "100001",
            amountPaid = Some("2000"),
            includesPenalty = Some("YES"),
            taxDue = Some("8000"),
            calcPenaltyDue = Some("500"),
            calcTaxDue = Some("8000"),
            calcTaxRate1 = Some("3"),
            calcTaxRate2 = Some("7"),
            calcTotalTaxPenaltyDue = Some("8500"),
            calcTotalNpvTax = Some("1000"),
            calcTotalPremiumTax = Some("7500"),
            taxDuePremium = Some("7500"),
            taxDueNpv = Some("1000"),
            honestyDeclaration = Some("YES")
          )
        )
        val result = wsClient.url(updateTaxCalc)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdateTaxCalcResponse()
        val jsonBody = Json.toJson(
          UpdateTaxCalculationRequest(
            stornId = "STORN12345",
            returnResourceRef = "100001",
            amountPaid = Some("2000"),
            includesPenalty = Some("YES"),
            taxDue = Some("8000"),
            calcPenaltyDue = Some("500"),
            calcTaxDue = Some("8000"),
            calcTaxRate1 = Some("3"),
            calcTaxRate2 = Some("7"),
            calcTotalTaxPenaltyDue = Some("8500"),
            calcTotalNpvTax = Some("1000"),
            calcTotalPremiumTax = Some("7500"),
            taxDuePremium = Some("7500"),
            taxDueNpv = Some("1000"),
            honestyDeclaration = Some("YES")
          )
        )
        val result = wsClient.url(updateTaxCalc)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

  }
}