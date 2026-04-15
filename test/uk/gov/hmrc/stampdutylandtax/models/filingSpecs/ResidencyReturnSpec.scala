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

class ResidencyReturnSpec extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  private val validResidencyPayloadJson = Json.obj(
    "isNonUkResidents" -> "NO",
    "isCompany"        -> "NO",
    "isCrownRelief"    -> "NO"
  )

  private val residencyPayload = ResidencyPayload(
    isNonUkResidents = "NO",
    isCompany        = "NO",
    isCrownRelief    = "NO"
  )

  private val validCreateResidencyRequestJson = Json.obj(
    "stornId"           -> "12345",
    "returnResourceRef" -> "45678",
    "residency"         -> validResidencyPayloadJson
  )

  private val createResidencyRequest = CreateResidencyRequest(
    stornId           = "12345",
    returnResourceRef = "45678",
    residency         = residencyPayload
  )

  private val validCreateResidencyReturnJson = Json.obj(
    "residencyResourceRef" -> "RRF-001",
    "residencyId"          -> "RID-001"
  )

  private val createResidencyReturn = CreateResidencyReturn(
    residencyResourceRef = "RRF-001",
    residencyId          = "RID-001"
  )

  private val validUpdateResidencyRequestJson = Json.obj(
    "stornId"           -> "12345",
    "returnResourceRef" -> "45678",
    "residency"         -> validResidencyPayloadJson
  )

  private val updateResidencyRequest = UpdateResidencyRequest(
    stornId           = "12345",
    returnResourceRef = "45678",
    residency         = residencyPayload
  )

  private val validUpdateResidencyReturnJsonTrue  = Json.obj("updated" -> true)
  private val validUpdateResidencyReturnJsonFalse = Json.obj("updated" -> false)
  private val updateResidencyReturnTrue           = UpdateResidencyReturn(updated = true)
  private val updateResidencyReturnFalse          = UpdateResidencyReturn(updated = false)

  private val validDeleteResidencyRequestJson = Json.obj(
    "storn"             -> "12345",
    "returnResourceRef" -> "45678"
  )

  private val deleteResidencyRequest = DeleteResidencyRequest(
    storn             = "12345",
    returnResourceRef = "45678"
  )

  private val validDeleteResidencyReturnJsonTrue  = Json.obj("deleted" -> true)
  private val validDeleteResidencyReturnJsonFalse = Json.obj("deleted" -> false)
  private val deleteResidencyReturnTrue           = DeleteResidencyReturn(deleted = true)
  private val deleteResidencyReturnFalse          = DeleteResidencyReturn(deleted = false)

  "ResidencyPayload" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[ResidencyPayload]]
      }

      "must deserialize valid JSON" in {
        val result = Json.fromJson[ResidencyPayload](validResidencyPayloadJson).asEither.value

        result.isNonUkResidents mustBe "NO"
        result.isCompany        mustBe "NO"
        result.isCrownRelief    mustBe "NO"
      }

      "must deserialize YES values" in {
        val json = Json.obj(
          "isNonUkResidents" -> "YES",
          "isCompany"        -> "YES",
          "isCrownRelief"    -> "YES"
        )
        val result = Json.fromJson[ResidencyPayload](json).asEither.value

        result.isNonUkResidents mustBe "YES"
        result.isCompany        mustBe "YES"
        result.isCrownRelief    mustBe "YES"
      }

      "must fail to deserialize when isNonUkResidents is missing" in {
        val result = Json.fromJson[ResidencyPayload](validResidencyPayloadJson - "isNonUkResidents").asEither
        result.isLeft mustBe true
      }

      "must fail to deserialize when isCompany is missing" in {
        val result = Json.fromJson[ResidencyPayload](validResidencyPayloadJson - "isCompany").asEither
        result.isLeft mustBe true
      }

      "must fail to deserialize when isCrownRelief is missing" in {
        val result = Json.fromJson[ResidencyPayload](validResidencyPayloadJson - "isCrownRelief").asEither
        result.isLeft mustBe true
      }

      "must fail to deserialize when field has invalid type" in {
        val json = validResidencyPayloadJson ++ Json.obj("isNonUkResidents" -> 123)
        Json.fromJson[ResidencyPayload](json).asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[ResidencyPayload](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[ResidencyPayload]]
      }

      "must serialize all fields" in {
        val json = Json.toJson(residencyPayload)

        (json \ "isNonUkResidents").as[String] mustBe "NO"
        (json \ "isCompany").as[String]        mustBe "NO"
        (json \ "isCrownRelief").as[String]    mustBe "NO"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(residencyPayload)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("isNonUkResidents", "isCompany", "isCrownRelief")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[ResidencyPayload]]
      }

      "must round-trip serialize and deserialize" in {
        val json   = Json.toJson(residencyPayload)
        val result = Json.fromJson[ResidencyPayload](json).asEither.value
        result mustEqual residencyPayload
      }
    }

    "case class" - {

      "must support equality" in {
        residencyPayload mustEqual residencyPayload.copy()
      }

      "must support copy with modifications" in {
        val modified = residencyPayload.copy(isNonUkResidents = "YES")
        modified.isNonUkResidents mustBe "YES"
        modified.isCompany        mustBe residencyPayload.isCompany
      }

      "must not be equal when fields differ" in {
        residencyPayload must not equal residencyPayload.copy(isCompany = "YES")
      }
    }
  }

  "CreateResidencyRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[CreateResidencyRequest]]
      }

      "must deserialize valid JSON" in {
        val result = Json.fromJson[CreateResidencyRequest](validCreateResidencyRequestJson).asEither.value

        result.stornId           mustBe "12345"
        result.returnResourceRef mustBe "45678"
        result.residency         mustBe residencyPayload
      }

      "must fail to deserialize when stornId is missing" in {
        Json.fromJson[CreateResidencyRequest](validCreateResidencyRequestJson - "stornId").asEither.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        Json.fromJson[CreateResidencyRequest](validCreateResidencyRequestJson - "returnResourceRef").asEither.isLeft mustBe true
      }

      "must fail to deserialize when residency is missing" in {
        Json.fromJson[CreateResidencyRequest](validCreateResidencyRequestJson - "residency").asEither.isLeft mustBe true
      }

      "must fail to deserialize when stornId has invalid type" in {
        val json = validCreateResidencyRequestJson ++ Json.obj("stornId" -> 123)
        Json.fromJson[CreateResidencyRequest](json).asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[CreateResidencyRequest](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[CreateResidencyRequest]]
      }

      "must serialize all fields" in {
        val json = Json.toJson(createResidencyRequest)

        (json \ "stornId").as[String]           mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "45678"
        (json \ "residency" \ "isNonUkResidents").as[String] mustBe "NO"
        (json \ "residency" \ "isCompany").as[String]        mustBe "NO"
        (json \ "residency" \ "isCrownRelief").as[String]    mustBe "NO"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(createResidencyRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("stornId", "returnResourceRef", "residency")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[CreateResidencyRequest]]
      }

      "must round-trip serialize and deserialize" in {
        val json   = Json.toJson(createResidencyRequest)
        val result = Json.fromJson[CreateResidencyRequest](json).asEither.value
        result mustEqual createResidencyRequest
      }
    }

    "case class" - {

      "must support equality" in {
        createResidencyRequest mustEqual createResidencyRequest.copy()
      }

      "must support copy with modifications" in {
        val modified = createResidencyRequest.copy(stornId = "99999")
        modified.stornId           mustBe "99999"
        modified.returnResourceRef mustBe createResidencyRequest.returnResourceRef
      }

      "must not be equal when fields differ" in {
        createResidencyRequest must not equal createResidencyRequest.copy(stornId = "DIFFERENT")
      }
    }
  }

  "CreateResidencyReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[CreateResidencyReturn]]
      }

      "must deserialize valid JSON" in {
        val result = Json.fromJson[CreateResidencyReturn](validCreateResidencyReturnJson).asEither.value

        result.residencyResourceRef mustBe "RRF-001"
        result.residencyId          mustBe "RID-001"
      }

      "must fail to deserialize when residencyResourceRef is missing" in {
        Json.fromJson[CreateResidencyReturn](validCreateResidencyReturnJson - "residencyResourceRef").asEither.isLeft mustBe true
      }

      "must fail to deserialize when residencyId is missing" in {
        Json.fromJson[CreateResidencyReturn](validCreateResidencyReturnJson - "residencyId").asEither.isLeft mustBe true
      }

      "must fail to deserialize when field has invalid type" in {
        val json = validCreateResidencyReturnJson ++ Json.obj("residencyId" -> 123)
        Json.fromJson[CreateResidencyReturn](json).asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[CreateResidencyReturn](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[CreateResidencyReturn]]
      }

      "must serialize all fields" in {
        val json = Json.toJson(createResidencyReturn)

        (json \ "residencyResourceRef").as[String] mustBe "RRF-001"
        (json \ "residencyId").as[String]          mustBe "RID-001"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(createResidencyReturn)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("residencyResourceRef", "residencyId")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[CreateResidencyReturn]]
      }

      "must round-trip serialize and deserialize" in {
        val json   = Json.toJson(createResidencyReturn)
        val result = Json.fromJson[CreateResidencyReturn](json).asEither.value
        result mustEqual createResidencyReturn
      }
    }

    "case class" - {

      "must support equality" in {
        createResidencyReturn mustEqual createResidencyReturn.copy()
      }

      "must support copy with modifications" in {
        val modified = createResidencyReturn.copy(residencyId = "RID-999")
        modified.residencyId          mustBe "RID-999"
        modified.residencyResourceRef mustBe createResidencyReturn.residencyResourceRef
      }

      "must not be equal when fields differ" in {
        createResidencyReturn must not equal createResidencyReturn.copy(residencyId = "DIFFERENT")
      }
    }
  }

  "UpdateResidencyRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[UpdateResidencyRequest]]
      }

      "must deserialize valid JSON" in {
        val result = Json.fromJson[UpdateResidencyRequest](validUpdateResidencyRequestJson).asEither.value

        result.stornId           mustBe "12345"
        result.returnResourceRef mustBe "45678"
        result.residency         mustBe residencyPayload
      }

      "must fail to deserialize when stornId is missing" in {
        Json.fromJson[UpdateResidencyRequest](validUpdateResidencyRequestJson - "stornId").asEither.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        Json.fromJson[UpdateResidencyRequest](validUpdateResidencyRequestJson - "returnResourceRef").asEither.isLeft mustBe true
      }

      "must fail to deserialize when residency is missing" in {
        Json.fromJson[UpdateResidencyRequest](validUpdateResidencyRequestJson - "residency").asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[UpdateResidencyRequest](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[UpdateResidencyRequest]]
      }

      "must serialize all fields" in {
        val json = Json.toJson(updateResidencyRequest)

        (json \ "stornId").as[String]           mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "45678"
        (json \ "residency" \ "isNonUkResidents").as[String] mustBe "NO"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(updateResidencyRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("stornId", "returnResourceRef", "residency")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[UpdateResidencyRequest]]
      }

      "must round-trip serialize and deserialize" in {
        val json   = Json.toJson(updateResidencyRequest)
        val result = Json.fromJson[UpdateResidencyRequest](json).asEither.value
        result mustEqual updateResidencyRequest
      }
    }

    "case class" - {

      "must support equality" in {
        updateResidencyRequest mustEqual updateResidencyRequest.copy()
      }

      "must support copy with modifications" in {
        val modified = updateResidencyRequest.copy(stornId = "99999")
        modified.stornId mustBe "99999"
        modified.returnResourceRef mustBe updateResidencyRequest.returnResourceRef
      }

      "must not be equal when fields differ" in {
        updateResidencyRequest must not equal updateResidencyRequest.copy(stornId = "DIFFERENT")
      }
    }
  }

  "UpdateResidencyReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[UpdateResidencyReturn]]
      }

      "must deserialize valid JSON with updated true" in {
        Json.fromJson[UpdateResidencyReturn](validUpdateResidencyReturnJsonTrue).asEither.value.updated mustBe true
      }

      "must deserialize valid JSON with updated false" in {
        Json.fromJson[UpdateResidencyReturn](validUpdateResidencyReturnJsonFalse).asEither.value.updated mustBe false
      }

      "must fail to deserialize when updated is missing" in {
        Json.fromJson[UpdateResidencyReturn](Json.obj()).asEither.isLeft mustBe true
      }

      "must fail to deserialize when updated has invalid type" in {
        Json.fromJson[UpdateResidencyReturn](Json.obj("updated" -> "invalid")).asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[UpdateResidencyReturn](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[UpdateResidencyReturn]]
      }

      "must serialize with updated true" in {
        (Json.toJson(updateResidencyReturnTrue) \ "updated").as[Boolean] mustBe true
      }

      "must serialize with updated false" in {
        (Json.toJson(updateResidencyReturnFalse) \ "updated").as[Boolean] mustBe false
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(updateResidencyReturnTrue)
        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("updated")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[UpdateResidencyReturn]]
      }

      "must round-trip with updated true" in {
        Json.fromJson[UpdateResidencyReturn](Json.toJson(updateResidencyReturnTrue)).asEither.value mustEqual updateResidencyReturnTrue
      }

      "must round-trip with updated false" in {
        Json.fromJson[UpdateResidencyReturn](Json.toJson(updateResidencyReturnFalse)).asEither.value mustEqual updateResidencyReturnFalse
      }
    }

    "case class" - {

      "must create instance with updated true" in {
        updateResidencyReturnTrue.updated mustBe true
      }

      "must create instance with updated false" in {
        updateResidencyReturnFalse.updated mustBe false
      }

      "must support equality" in {
        updateResidencyReturnTrue mustEqual updateResidencyReturnTrue.copy()
      }

      "must support copy with modifications" in {
        updateResidencyReturnTrue.copy(updated = false).updated mustBe false
      }

      "must not be equal when fields differ" in {
        updateResidencyReturnTrue must not equal updateResidencyReturnFalse
      }
    }
  }

  "DeleteResidencyRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[DeleteResidencyRequest]]
      }

      "must deserialize valid JSON" in {
        val result = Json.fromJson[DeleteResidencyRequest](validDeleteResidencyRequestJson).asEither.value

        result.storn             mustBe "12345"
        result.returnResourceRef mustBe "45678"
      }

      "must fail to deserialize when storn is missing" in {
        Json.fromJson[DeleteResidencyRequest](validDeleteResidencyRequestJson - "storn").asEither.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        Json.fromJson[DeleteResidencyRequest](validDeleteResidencyRequestJson - "returnResourceRef").asEither.isLeft mustBe true
      }

      "must fail to deserialize when storn has invalid type" in {
        val json = validDeleteResidencyRequestJson ++ Json.obj("storn" -> 123)
        Json.fromJson[DeleteResidencyRequest](json).asEither.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef has invalid type" in {
        val json = validDeleteResidencyRequestJson ++ Json.obj("returnResourceRef" -> 456)
        Json.fromJson[DeleteResidencyRequest](json).asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[DeleteResidencyRequest](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[DeleteResidencyRequest]]
      }

      "must serialize all fields" in {
        val json = Json.toJson(deleteResidencyRequest)

        (json \ "storn").as[String]             mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "45678"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(deleteResidencyRequest)
        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("storn", "returnResourceRef")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[DeleteResidencyRequest]]
      }

      "must round-trip serialize and deserialize" in {
        Json.fromJson[DeleteResidencyRequest](Json.toJson(deleteResidencyRequest)).asEither.value mustEqual deleteResidencyRequest
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        deleteResidencyRequest.storn             mustBe "12345"
        deleteResidencyRequest.returnResourceRef mustBe "45678"
      }

      "must support equality" in {
        deleteResidencyRequest mustEqual deleteResidencyRequest.copy()
      }

      "must support copy with modifications" in {
        val modified = deleteResidencyRequest.copy(storn = "99999")
        modified.storn             mustBe "99999"
        modified.returnResourceRef mustBe deleteResidencyRequest.returnResourceRef
      }

      "must not be equal when fields differ" in {
        deleteResidencyRequest must not equal deleteResidencyRequest.copy(returnResourceRef = "DIFFERENT")
      }
    }
  }

  "DeleteResidencyReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[DeleteResidencyReturn]]
      }

      "must deserialize valid JSON with deleted true" in {
        Json.fromJson[DeleteResidencyReturn](validDeleteResidencyReturnJsonTrue).asEither.value.deleted mustBe true
      }

      "must deserialize valid JSON with deleted false" in {
        Json.fromJson[DeleteResidencyReturn](validDeleteResidencyReturnJsonFalse).asEither.value.deleted mustBe false
      }

      "must fail to deserialize when deleted is missing" in {
        Json.fromJson[DeleteResidencyReturn](Json.obj()).asEither.isLeft mustBe true
      }

      "must fail to deserialize when deleted has invalid type" in {
        Json.fromJson[DeleteResidencyReturn](Json.obj("deleted" -> "invalid")).asEither.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        Json.fromJson[DeleteResidencyReturn](Json.obj("invalidField" -> "value")).asEither.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[DeleteResidencyReturn]]
      }

      "must serialize with deleted true" in {
        (Json.toJson(deleteResidencyReturnTrue) \ "deleted").as[Boolean] mustBe true
      }

      "must serialize with deleted false" in {
        (Json.toJson(deleteResidencyReturnFalse) \ "deleted").as[Boolean] mustBe false
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(deleteResidencyReturnTrue)
        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("deleted")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[DeleteResidencyReturn]]
      }

      "must round-trip with deleted true" in {
        Json.fromJson[DeleteResidencyReturn](Json.toJson(deleteResidencyReturnTrue)).asEither.value mustEqual deleteResidencyReturnTrue
      }

      "must round-trip with deleted false" in {
        Json.fromJson[DeleteResidencyReturn](Json.toJson(deleteResidencyReturnFalse)).asEither.value mustEqual deleteResidencyReturnFalse
      }
    }

    "case class" - {

      "must create instance with deleted true" in {
        deleteResidencyReturnTrue.deleted mustBe true
      }

      "must create instance with deleted false" in {
        deleteResidencyReturnFalse.deleted mustBe false
      }

      "must support equality" in {
        deleteResidencyReturnTrue mustEqual deleteResidencyReturnTrue.copy()
      }

      "must support copy with modifications" in {
        deleteResidencyReturnTrue.copy(deleted = false).deleted mustBe false
      }

      "must not be equal when fields differ" in {
        deleteResidencyReturnTrue must not equal deleteResidencyReturnFalse
      }
    }
  }
}