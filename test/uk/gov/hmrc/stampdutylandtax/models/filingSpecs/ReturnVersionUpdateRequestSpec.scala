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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues}
import play.api.libs.json.*
import models.filing._

class ReturnVersionUpdateRequestSpec extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  private val validReturnVersionUpdateRequestJson = Json.obj(
    "storn" -> "12345",
    "returnResourceRef" -> "RRF-2024-001",
    "currentVersion" -> "1.0"
  )

  private val returnVersionUpdateRequest = ReturnVersionUpdateRequest(
    storn = "12345",
    returnResourceRef = "RRF-2024-001",
    currentVersion = "1.0"
  )

  private val validReturnVersionUpdateReturnJson = Json.obj(
    "newVersion" -> 1
  )
  
  private val returnVersionUpdateReturn = ReturnVersionUpdateReturn(newVersion = 1)

  "ReturnVersionUpdateRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[ReturnVersionUpdateRequest]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[ReturnVersionUpdateRequest](validReturnVersionUpdateRequestJson).asEither.value

        result.storn mustBe "12345"
        result.returnResourceRef mustBe "RRF-2024-001"
        result.currentVersion mustBe "1.0"
      }

      "must deserialize JSON with different version formats" in {
        val jsonV2 = Json.obj(
          "storn" -> "12345",
          "returnResourceRef" -> "RRF-2024-001",
          "currentVersion" -> "2.5.1"
        )

        val result = Json.fromJson[ReturnVersionUpdateRequest](jsonV2).asEither.value

        result.currentVersion mustBe "2.5.1"
      }

      "must deserialize JSON with numeric version string" in {
        val json = Json.obj(
          "storn" -> "12345",
          "returnResourceRef" -> "RRF-2024-001",
          "currentVersion" -> "3"
        )

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither.value

        result.currentVersion mustBe "3"
      }

      "must fail to deserialize when storn is missing" in {
        val json = validReturnVersionUpdateRequestJson - "storn"

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        val json = validReturnVersionUpdateRequestJson - "returnResourceRef"

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when currentVersion is missing" in {
        val json = validReturnVersionUpdateRequestJson - "currentVersion"

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when storn has invalid type" in {
        val json = validReturnVersionUpdateRequestJson ++ Json.obj("storn" -> 123)

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef has invalid type" in {
        val json = validReturnVersionUpdateRequestJson ++ Json.obj("returnResourceRef" -> true)

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when currentVersion has invalid type" in {
        val json = validReturnVersionUpdateRequestJson ++ Json.obj("currentVersion" -> 456)

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when all fields are missing" in {
        val json = Json.obj()

        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[ReturnVersionUpdateRequest]]
      }

      "must serialize ReturnVersionUpdateRequest with all fields" in {
        val json = Json.toJson(returnVersionUpdateRequest)

        (json \ "storn").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "RRF-2024-001"
        (json \ "currentVersion").as[String] mustBe "1.0"
      }

      "must serialize ReturnVersionUpdateRequest with different version" in {
        val request = returnVersionUpdateRequest.copy(currentVersion = "2.5.1")
        val json = Json.toJson(request)

        (json \ "currentVersion").as[String] mustBe "2.5.1"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(returnVersionUpdateRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("storn", "returnResourceRef", "currentVersion")
      }

      "must produce JSON with exactly three fields" in {
        val json = Json.toJson(returnVersionUpdateRequest)

        json.as[JsObject].keys.size mustBe 3
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[ReturnVersionUpdateRequest]]
      }

      "must round-trip serialize and deserialize" in {
        val json = Json.toJson(returnVersionUpdateRequest)
        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither.value

        result mustEqual returnVersionUpdateRequest
      }

      "must round-trip with version 2.0" in {
        val request = returnVersionUpdateRequest.copy(currentVersion = "2.0")
        val json = Json.toJson(request)
        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither.value

        result mustEqual request
      }

      "must round-trip with complex version string" in {
        val request = returnVersionUpdateRequest.copy(currentVersion = "3.14.159")
        val json = Json.toJson(request)
        val result = Json.fromJson[ReturnVersionUpdateRequest](json).asEither.value

        result mustEqual request
        result.currentVersion mustBe "3.14.159"
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        returnVersionUpdateRequest.storn mustBe "12345"
        returnVersionUpdateRequest.returnResourceRef mustBe "RRF-2024-001"
        returnVersionUpdateRequest.currentVersion mustBe "1.0"
      }

      "must support equality" in {
        val request1 = returnVersionUpdateRequest
        val request2 = returnVersionUpdateRequest.copy()

        request1 mustEqual request2
      }

      "must support copy with modifications" in {
        val modified = returnVersionUpdateRequest.copy(storn = "54321")

        modified.storn mustBe "54321"
        modified.returnResourceRef mustBe returnVersionUpdateRequest.returnResourceRef
        modified.currentVersion mustBe returnVersionUpdateRequest.currentVersion
      }

      "must support copy with version modification" in {
        val modified = returnVersionUpdateRequest.copy(currentVersion = "2.0")

        modified.currentVersion mustBe "2.0"
        modified.storn mustBe returnVersionUpdateRequest.storn
        modified.returnResourceRef mustBe returnVersionUpdateRequest.returnResourceRef
      }

      "must not be equal when storn differs" in {
        val request1 = returnVersionUpdateRequest
        val request2 = returnVersionUpdateRequest.copy(storn = "99999")

        request1 must not equal request2
      }

      "must not be equal when returnResourceRef differs" in {
        val request1 = returnVersionUpdateRequest
        val request2 = returnVersionUpdateRequest.copy(returnResourceRef = "RRF-2025-999")

        request1 must not equal request2
      }

      "must not be equal when currentVersion differs" in {
        val request1 = returnVersionUpdateRequest
        val request2 = returnVersionUpdateRequest.copy(currentVersion = "2.0")

        request1 must not equal request2
      }

      "must not be equal when multiple fields differ" in {
        val request1 = returnVersionUpdateRequest
        val request2 = returnVersionUpdateRequest.copy(
          storn = "99999",
          returnResourceRef = "RRF-2025-999",
          currentVersion = "3.0"
        )

        request1 must not equal request2
      }

      "must support creating with different version formats" in {
        val v1 = ReturnVersionUpdateRequest("12345", "RRF-001", "1.0")
        val v2 = ReturnVersionUpdateRequest("12345", "RRF-001", "2.5.1")
        val v3 = ReturnVersionUpdateRequest("12345", "RRF-001", "10")

        v1.currentVersion mustBe "1.0"
        v2.currentVersion mustBe "2.5.1"
        v3.currentVersion mustBe "10"
      }
    }
  }

  "ReturnVersionUpdateReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[ReturnVersionUpdateReturn]]
      }

      "must deserialize valid JSON with newVersion true" in {
        val result = Json.fromJson[ReturnVersionUpdateReturn](validReturnVersionUpdateReturnJson).asEither.value

        result.newVersion mustBe 1
      }

      "must fail to deserialize when newVersion is missing" in {
        val json = Json.obj()

        val result = Json.fromJson[ReturnVersionUpdateReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when newVersion has invalid type" in {
        val json = Json.obj("newVersion" -> "invalid")

        val result = Json.fromJson[ReturnVersionUpdateReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when newVersion is a number" in {
        val json = Json.obj("newVersion" -> "121231")

        val result = Json.fromJson[ReturnVersionUpdateReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when newVersion is null" in {
        val json = Json.obj("newVersion" -> JsNull)

        val result = Json.fromJson[ReturnVersionUpdateReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[ReturnVersionUpdateReturn](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[ReturnVersionUpdateReturn]]
      }

      "must serialize ReturnVersionUpdateReturn with newVersion true" in {
        val json = Json.toJson(returnVersionUpdateReturn)

        (json \ "newVersion").as[Int] mustBe 1
      }
      

      "must produce valid JSON structure" in {
        val json = Json.toJson(returnVersionUpdateReturn)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("newVersion")
      }

      "must produce JSON with exactly one field" in {
        val json = Json.toJson(returnVersionUpdateReturn)

        json.as[JsObject].keys.size mustBe 1
      }

      "must produce int value not string" in {
        val json = Json.toJson(returnVersionUpdateReturn)

        (json \ "newVersion").get mustBe a[JsNumber]
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[ReturnVersionUpdateReturn]]
      }

      "must round-trip serialize and deserialize with newVersion 1" in {
        val json = Json.toJson(returnVersionUpdateReturn)
        val result = Json.fromJson[ReturnVersionUpdateReturn](json).asEither.value

        result mustEqual returnVersionUpdateReturn
      }
    }

    "case class" - {

      "must create instance with newVersion 1" in {
        returnVersionUpdateReturn.newVersion mustBe 1
      }
      

      "must support equality" in {
        val versionReturn1 = returnVersionUpdateReturn
        val versionReturn2 = returnVersionUpdateReturn.copy()

        versionReturn1 mustEqual versionReturn2
      }

      "must support copy with modifications" in {
        val modified = returnVersionUpdateReturn.copy(newVersion = 2)

        modified.newVersion mustBe 2
      }

      "must not be equal when fields differ" in {
        val versionReturn1 = returnVersionUpdateReturn
        val versionReturn2 = returnVersionUpdateReturn.copy(newVersion = 2)

        versionReturn1 must not equal versionReturn2
      }
    }
  }
}