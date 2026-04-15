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

package uk.gov.hmrc.stampdutylandtax.models.filingSpecs

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues}
import play.api.libs.json.*
import models.filing._

class TransactionReturnSpec extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  private val minimalTransactionPayload = TransactionPayload()

  private val completeTransactionPayload = TransactionPayload(
    claimingRelief                = Some("YES"),
    reliefAmount                  = Some("10000"),
    reliefReason                  = Some("REASON"),
    reliefSchemeNumber            = Some("SCH-001"),
    isLinked                      = Some("NO"),
    totalConsiderLinked           = Some("50000"),
    totalConsider                 = Some("200000"),
    considerBuild                 = Some("10000"),
    considerCash                  = Some("150000"),
    considerContingent            = Some("5000"),
    considerDebt                  = Some("0"),
    considerEmploy                = Some("0"),
    considerOther                 = Some("0"),
    considerLand                  = Some("0"),
    considerServices              = Some("0"),
    considerSharesQtd             = Some("0"),
    considerSharesUnqtd           = Some("0"),
    considerVat                   = Some("0"),
    includesChattel               = Some("NO"),
    includesGoodwill              = Some("NO"),
    includesOther                 = Some("NO"),
    includesStock                 = Some("NO"),
    usedAsFactory                 = Some("NO"),
    usedAsHotel                   = Some("NO"),
    usedAsIndustrial              = Some("NO"),
    usedAsOffice                  = Some("YES"),
    usedAsOther                   = Some("NO"),
    usedAsShop                    = Some("NO"),
    usedAsWarehouse               = Some("NO"),
    contractDate                  = Some("2024-01-15"),
    isDependOnFutureEvent         = Some("NO"),
    transactionDescription        = Some("Purchase"),
    newTransactionDescription     = Some("New Purchase"),
    effectiveDate                 = Some("2024-02-01"),
    isLandExchanged               = Some("NO"),
    exLandHouseNumber             = Some("10"),
    exLandAddress1                = Some("Exchange Street"),
    exLandAddress2                = Some("Floor 2"),
    exLandAddress3                = Some("Block A"),
    exLandAddress4                = Some("District"),
    exLandPostcode                = Some("EX12 3CH"),
    agreedDeferPay                = Some("NO"),
    postTransactionRulingApplied  = Some("NO"),
    isPursuantToPreviousOption    = Some("NO"),
    restAffectInt                 = Some("NO"),
    restDetails                   = Some("None"),
    postTransactionRulingFollowed = Some("NO"),
    isPartOfSaleOfBusiness        = Some("NO"),
    totalConsiderationOfBusiness  = Some("0")
  )

  private val validUpdateTransactionRequestJson = Json.obj(
    "storn"             -> "12345",
    "returnResourceRef" -> "45678",
    "transaction"       -> Json.obj()
  )

  private val updateTransactionRequest = UpdateTransactionRequest(
    storn             = "12345",
    returnResourceRef = "45678",
    transaction       = minimalTransactionPayload
  )

  private val completeUpdateTransactionRequest = UpdateTransactionRequest(
    storn             = "12345",
    returnResourceRef = "45678",
    transaction       = completeTransactionPayload
  )

  private val validUpdateTransactionReturnJsonTrue  = Json.obj("updated" -> true)
  private val validUpdateTransactionReturnJsonFalse = Json.obj("updated" -> false)
  private val updateTransactionReturnTrue           = UpdateTransactionReturn(updated = true)
  private val updateTransactionReturnFalse          = UpdateTransactionReturn(updated = false)

  "TransactionPayload" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[TransactionPayload]]
      }

      "must deserialize empty JSON object as all-None payload" in {
        val result = Json.fromJson[TransactionPayload](Json.obj()).asEither.value
        result mustBe minimalTransactionPayload
      }

      "must deserialize complete JSON with all fields" in {
        val json   = Json.toJson(completeTransactionPayload)
        val result = Json.fromJson[TransactionPayload](json).asEither.value

        result.claimingRelief                mustBe Some("YES")
        result.reliefAmount                  mustBe Some("10000")
        result.totalConsider                 mustBe Some("200000")
        result.contractDate                  mustBe Some("2024-01-15")
        result.effectiveDate                 mustBe Some("2024-02-01")
        result.postTransactionRulingFollowed mustBe Some("NO")
        result.totalConsiderationOfBusiness  mustBe Some("0")
      }

      "must deserialize JSON with null optional fields as None" in {
        val json = Json.obj(
          "claimingRelief" -> JsNull,
          "totalConsider"  -> JsNull,
          "effectiveDate"  -> JsNull
        )
        val result = Json.fromJson[TransactionPayload](json).asEither.value

        result.claimingRelief mustBe None
        result.totalConsider  mustBe None
        result.effectiveDate  mustBe None
      }

      "must fail to deserialize when a field has invalid type" in {
        val json = Json.obj("totalConsider" -> 123)
        Json.fromJson[TransactionPayload](json).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[TransactionPayload]]
      }

      "must serialize complete payload with all fields" in {
        val json = Json.toJson(completeTransactionPayload)

        (json \ "claimingRelief").as[String]               mustBe "YES"
        (json \ "reliefAmount").as[String]                 mustBe "10000"
        (json \ "totalConsider").as[String]                mustBe "200000"
        (json \ "contractDate").as[String]                 mustBe "2024-01-15"
        (json \ "effectiveDate").as[String]                mustBe "2024-02-01"
        (json \ "totalConsiderationOfBusiness").as[String] mustBe "0"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(completeTransactionPayload)
        json mustBe a[JsObject]
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[TransactionPayload]]
      }

      "must round-trip with complete payload" in {
        val json   = Json.toJson(completeTransactionPayload)
        val result = Json.fromJson[TransactionPayload](json).asEither.value
        result mustEqual completeTransactionPayload
      }

      "must round-trip with minimal payload" in {
        val json   = Json.toJson(minimalTransactionPayload)
        val result = Json.fromJson[TransactionPayload](json).asEither.value
        result mustEqual minimalTransactionPayload
      }

      "must round-trip with mixed optional fields" in {
        val mixed = TransactionPayload(
          claimingRelief  = Some("YES"),
          totalConsider   = Some("100000"),
          effectiveDate   = Some("2024-06-01"),
          contractDate    = None,
          isLandExchanged = Some("NO")
        )
        val json   = Json.toJson(mixed)
        val result = Json.fromJson[TransactionPayload](json).asEither.value
        result mustEqual mixed
      }
    }

    "case class" - {

      "must support equality" in {
        minimalTransactionPayload mustEqual minimalTransactionPayload.copy()
      }

      "must support copy with modifications" in {
        val modified = minimalTransactionPayload.copy(claimingRelief = Some("YES"), totalConsider = Some("50000"))
        modified.claimingRelief mustBe Some("YES")
        modified.totalConsider  mustBe Some("50000")
        modified.contractDate   mustBe None
      }

      "must not be equal when fields differ" in {
        minimalTransactionPayload must not equal minimalTransactionPayload.copy(claimingRelief = Some("YES"))
      }
    }
  }

  "UpdateTransactionRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[UpdateTransactionRequest]]
      }

      "must deserialize valid JSON with minimal transaction" in {
        val result = Json.fromJson[UpdateTransactionRequest](validUpdateTransactionRequestJson).asEither.value

        result.storn             mustBe "12345"
        result.returnResourceRef mustBe "45678"
        result.transaction       mustBe minimalTransactionPayload
      }

      "must fail to deserialize when storn is missing" in {
        Json.fromJson[UpdateTransactionRequest](validUpdateTransactionRequestJson - "storn").asEither.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        Json.fromJson[UpdateTransactionRequest](validUpdateTransactionRequestJson - "returnResourceRef").asEither.isLeft mustBe true
      }

      "must fail to deserialize when transaction is missing" in {
        Json.fromJson[UpdateTransactionRequest](validUpdateTransactionRequestJson - "transaction").asEither.isLeft mustBe true
      }

      "must fail to deserialize when storn has invalid type" in {
        val json = validUpdateTransactionRequestJson ++ Json.obj("storn" -> 123)
        Json.fromJson[UpdateTransactionRequest](json).asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[UpdateTransactionRequest](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[UpdateTransactionRequest]]
      }

      "must serialize minimal request" in {
        val json = Json.toJson(updateTransactionRequest)

        (json \ "storn").as[String]             mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "45678"
        (json \ "transaction").isDefined        mustBe true
      }

      "must serialize complete request" in {
        val json = Json.toJson(completeUpdateTransactionRequest)

        (json \ "storn").as[String]                                    mustBe "12345"
        (json \ "returnResourceRef").as[String]                        mustBe "45678"
        (json \ "transaction" \ "claimingRelief").as[String]           mustBe "YES"
        (json \ "transaction" \ "totalConsider").as[String]            mustBe "200000"
        (json \ "transaction" \ "effectiveDate").as[String]            mustBe "2024-02-01"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(updateTransactionRequest)
        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("storn", "returnResourceRef", "transaction")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[UpdateTransactionRequest]]
      }

      "must round-trip with minimal request" in {
        val json   = Json.toJson(updateTransactionRequest)
        val result = Json.fromJson[UpdateTransactionRequest](json).asEither.value
        result mustEqual updateTransactionRequest
      }

      "must round-trip with complete request" in {
        val json   = Json.toJson(completeUpdateTransactionRequest)
        val result = Json.fromJson[UpdateTransactionRequest](json).asEither.value
        result mustEqual completeUpdateTransactionRequest
      }
    }

    "case class" - {

      "must support equality" in {
        updateTransactionRequest mustEqual updateTransactionRequest.copy()
      }

      "must support copy with modifications" in {
        val modified = updateTransactionRequest.copy(storn = "99999")
        modified.storn             mustBe "99999"
        modified.returnResourceRef mustBe updateTransactionRequest.returnResourceRef
      }

      "must not be equal when fields differ" in {
        updateTransactionRequest must not equal updateTransactionRequest.copy(storn = "DIFFERENT")
      }
    }
  }
  
  "UpdateTransactionReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[UpdateTransactionReturn]]
      }

      "must deserialize valid JSON with updated true" in {
        Json.fromJson[UpdateTransactionReturn](validUpdateTransactionReturnJsonTrue).asEither.value.updated mustBe true
      }

      "must deserialize valid JSON with updated false" in {
        Json.fromJson[UpdateTransactionReturn](validUpdateTransactionReturnJsonFalse).asEither.value.updated mustBe false
      }

      "must fail to deserialize when updated is missing" in {
        Json.fromJson[UpdateTransactionReturn](Json.obj()).asEither.isLeft mustBe true
      }

      "must fail to deserialize when updated has invalid type" in {
        Json.fromJson[UpdateTransactionReturn](Json.obj("updated" -> "invalid")).asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[UpdateTransactionReturn](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[UpdateTransactionReturn]]
      }

      "must serialize with updated true" in {
        (Json.toJson(updateTransactionReturnTrue) \ "updated").as[Boolean] mustBe true
      }

      "must serialize with updated false" in {
        (Json.toJson(updateTransactionReturnFalse) \ "updated").as[Boolean] mustBe false
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(updateTransactionReturnTrue)
        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("updated")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[UpdateTransactionReturn]]
      }

      "must round-trip with updated true" in {
        Json.fromJson[UpdateTransactionReturn](Json.toJson(updateTransactionReturnTrue)).asEither.value mustEqual updateTransactionReturnTrue
      }

      "must round-trip with updated false" in {
        Json.fromJson[UpdateTransactionReturn](Json.toJson(updateTransactionReturnFalse)).asEither.value mustEqual updateTransactionReturnFalse
      }
    }

    "case class" - {

      "must create instance with updated true" in {
        updateTransactionReturnTrue.updated mustBe true
      }

      "must create instance with updated false" in {
        updateTransactionReturnFalse.updated mustBe false
      }

      "must support equality" in {
        updateTransactionReturnTrue mustEqual updateTransactionReturnTrue.copy()
      }

      "must support copy with modifications" in {
        updateTransactionReturnTrue.copy(updated = false).updated mustBe false
      }

      "must not be equal when fields differ" in {
        updateTransactionReturnTrue must not equal updateTransactionReturnFalse
      }
    }
  }
}