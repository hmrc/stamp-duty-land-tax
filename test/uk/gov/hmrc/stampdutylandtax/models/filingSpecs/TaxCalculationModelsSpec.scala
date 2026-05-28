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

package uk.gov.hmrc.stampdutylandtax.models.filingSpecs

import models.filing.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

class TaxCalculationModelsSpec extends AnyFreeSpec with Matchers {

  "UpdateTaxCalculationRequest" - {

    "must serialize to JSON correctly with valid values" in {
      val request = UpdateTaxCalculationRequest(
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

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "amountPaid").as[String] mustBe "2000"
      (json \ "includesPenalty").as[String] mustBe "YES"
      (json \ "taxDue").as[String] mustBe "8000"
      (json \ "calcPenaltyDue").as[String] mustBe "500"
      (json \ "calcTaxDue").as[String] mustBe "8000"
      (json \ "calcTaxRate1").as[String] mustBe "3"
      (json \ "calcTaxRate2").as[String] mustBe "7"
      (json \ "calcTotalTaxPenaltyDue").as[String] mustBe "8500"
      (json \ "calcTotalNpvTax").as[String] mustBe "1000"
      (json \ "calcTotalPremiumTax").as[String] mustBe "7500"
      (json \ "taxDuePremium").as[String] mustBe "7500"
      (json \ "taxDueNpv").as[String] mustBe "1000"
      (json \ "honestyDeclaration").as[String] mustBe "YES"
    }

    "must deserialize from JSON correctly with Y values" in {
      val json = Json.obj(
        "stornId" -> "STORN12345",
        "returnResourceRef" -> "100001",
        "amountPaid" -> "2000",
        "includesPenalty" -> "YES",
        "taxDue" -> "8000",
        "calcPenaltyDue" -> "500",
        "calcTaxDue" -> "8000",
        "calcTaxRate1" -> "3",
        "calcTaxRate2" -> "7",
        "calcTotalTaxPenaltyDue" -> "8500",
        "calcTotalNpvTax" -> "1000",
        "calcTotalPremiumTax" -> "7500",
        "taxDuePremium" -> "7500",
        "taxDueNpv" -> "1000",
        "honestyDeclaration" -> "YES"
      )

      val result = json.validate[UpdateTaxCalculationRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.amountPaid mustBe Some("2000")
      request.includesPenalty mustBe Some("YES")
      request.taxDue mustBe Some("8000")
      request.calcPenaltyDue mustBe Some("500")
      request.calcTaxDue mustBe Some("8000")
      request.calcTaxRate1 mustBe Some("3")
      request.calcTaxRate2 mustBe Some("7")
      request.calcTotalTaxPenaltyDue mustBe Some("8500")
      request.calcTotalNpvTax mustBe Some("1000")
      request.calcTotalPremiumTax mustBe Some("7500")
      request.taxDuePremium mustBe Some("7500")
      request.taxDueNpv mustBe Some("1000")
      request.honestyDeclaration mustBe Some("YES")
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef"   -> "100001"
      )

      val result = json.validate[UpdateTaxCalculationRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn"               -> "STORN12345",
      )

      val result = json.validate[UpdateTaxCalculationRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when all fields are missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateTaxCalculationRequest]

      result.isError mustBe true
    }

  }
  "UpdateTaxCalculationReturn" - {
    "must serialize to JSON correctly with updated=true" in {
      val response = UpdateTaxCalculationReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly with updated=false" in {
      val response = UpdateTaxCalculationReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly with updated=true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateTaxCalculationReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must deserialize from JSON correctly with updated=false" in {
      val json = Json.obj("updated" -> false)

      val result = json.validate[UpdateTaxCalculationReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe false
    }

    "must fail to deserialize when updated is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateTaxCalculationReturn]

      result.isError mustBe true
    }

    "must fail to deserialize when updated is not a boolean" in {
      val json = Json.obj("updated" -> "true")

      val result = json.validate[UpdateTaxCalculationReturn]

      result.isError mustBe true
    }

    "must fail to deserialize when updated is a number" in {
      val json = Json.obj("updated" -> 1)

      val result = json.validate[UpdateTaxCalculationReturn]

      result.isError mustBe true
    }
  }
}
