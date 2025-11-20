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

import models.filing._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues}
import play.api.libs.json.*

class ReturnAgentSpec extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  private val validDeleteReturnAgentReturnJsonTrue = Json.obj(
    "deleted" -> true
  )

  private val validDeleteReturnAgentReturnJsonFalse = Json.obj(
    "deleted" -> false
  )

  private val deleteReturnAgentReturnTrue = DeleteReturnAgentReturn(deleted = true)
  private val deleteReturnAgentReturnFalse = DeleteReturnAgentReturn(deleted = false)

  private val validDeleteReturnAgentRequestJson = Json.obj(
    "storn" -> "12345",
    "returnResourceRef" -> "RRF-2024-001",
    "agentType" -> "VENDOR"
  )

  private val deleteReturnAgentRequest = DeleteReturnAgentRequest(
    storn = "12345",
    returnResourceRef = "RRF-2024-001",
    agentType = "VENDOR"
  )

  private val validUpdateReturnAgentReturnJsonTrue = Json.obj(
    "updated" -> true
  )

  private val validUpdateReturnAgentReturnJsonFalse = Json.obj(
    "updated" -> false
  )

  private val updateReturnAgentReturnTrue = UpdateReturnAgentReturn(updated = true)
  private val updateReturnAgentReturnFalse = UpdateReturnAgentReturn(updated = false)


  private val validUpdateReturnAgentRequestJsonComplete = Json.obj(
    "stornId" -> "12345",
    "returnResourceRef" -> "RRF-2024-001",
    "agentType" -> "VENDOR",
    "name" -> "Smith & Partners Updated",
    "houseNumber" -> "10",
    "addressLine1" -> "Main Street",
    "addressLine2" -> "Suite 5",
    "addressLine3" -> "Building A",
    "addressLine4" -> "District B",
    "postcode" -> "TE23 5TT",
    "phoneNumber" -> "01234567890",
    "email" -> "agent@example.com",
    "agentReference" -> "AGT-001",
    "isAuthorised" -> "YES"
  )

  private val validUpdateReturnAgentRequestJsonMinimal = Json.obj(
    "stornId" -> "12345",
    "returnResourceRef" -> "RRF-2024-001",
    "agentType" -> "VENDOR",
    "name" -> "Smith & Partners",
    "addressLine1" -> "Main Street",
    "postcode" -> "TE23 5TT"
  )

  private val completeUpdateReturnAgentRequest = UpdateReturnAgentRequest(
    stornId = "12345",
    returnResourceRef = "RRF-2024-001",
    agentType = "VENDOR",
    name = "Smith & Partners Updated",
    houseNumber = Some("10"),
    addressLine1 = "Main Street",
    addressLine2 = Some("Suite 5"),
    addressLine3 = Some("Building A"),
    addressLine4 = Some("District B"),
    postcode = "TE23 5TT",
    phoneNumber = Some("01234567890"),
    email = Some("agent@example.com"),
    agentReference = Some("AGT-001"),
    isAuthorised = Some("YES")
  )

  private val minimalUpdateReturnAgentRequest = UpdateReturnAgentRequest(
    stornId = "12345",
    returnResourceRef = "RRF-2024-001",
    agentType = "VENDOR",
    name = "Smith & Partners",
    addressLine1 = "Main Street",
    postcode = "TE23 5TT"
  )


  private val validCreateReturnAgentRequestJsonComplete = Json.obj(
    "stornId" -> "12345",
    "returnResourceRef" -> "RRF-2024-001",
    "agentType" -> "VENDOR",
    "name" -> "Smith & Partners",
    "houseNumber" -> "10",
    "addressLine1" -> "Main Street",
    "addressLine2" -> "Suite 5",
    "addressLine3" -> "Building A",
    "addressLine4" -> "District B",
    "postcode" -> "TE23 5TT",
    "phoneNumber" -> "01234567890",
    "email" -> "agent@example.com",
    "agentReference" -> "AGT-001",
    "isAuthorised" -> "YES"
  )

  private val validCreateReturnAgentRequestJsonMinimal = Json.obj(
    "stornId" -> "12345",
    "returnResourceRef" -> "RRF-2024-001",
    "agentType" -> "VENDOR",
    "name" -> "Smith & Partners",
    "addressLine1" -> "Main Street",
    "postcode" -> "TE23 5TT"
  )

  private val completeCreateReturnAgentRequest = CreateReturnAgentRequest(
    stornId = "12345",
    returnResourceRef = "RRF-2024-001",
    agentType = "VENDOR",
    name = "Smith & Partners",
    houseNumber = Some("10"),
    addressLine1 = "Main Street",
    addressLine2 = Some("Suite 5"),
    addressLine3 = Some("Building A"),
    addressLine4 = Some("District B"),
    postcode = "TE23 5TT",
    phoneNumber = Some("01234567890"),
    email = Some("agent@example.com"),
    agentReference = Some("AGT-001"),
    isAuthorised = Some("YES")
  )

  private val minimalCreateReturnAgentRequest = CreateReturnAgentRequest(
    stornId = "12345",
    returnResourceRef = "RRF-2024-001",
    agentType = "VENDOR",
    name = "Smith & Partners",
    addressLine1 = "Main Street",
    postcode = "TE23 5TT"
  )

  private val validCreateReturnAgentReturnJson = Json.obj(
    "returnAgentID" -> "AGID-001"
  )

  private val createReturnAgentReturn = CreateReturnAgentReturn(
    returnAgentID = "AGID-001"
  )

  "CreateReturnAgentRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[CreateReturnAgentRequest]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[CreateReturnAgentRequest](validCreateReturnAgentRequestJsonComplete).asEither.value

        result.stornId mustBe "12345"
        result.returnResourceRef mustBe "RRF-2024-001"
        result.agentType mustBe "VENDOR"
        result.name mustBe "Smith & Partners"
        result.houseNumber mustBe Some("10")
        result.addressLine1 mustBe "Main Street"
        result.addressLine2 mustBe Some("Suite 5")
        result.addressLine3 mustBe Some("Building A")
        result.addressLine4 mustBe Some("District B")
        result.postcode mustBe "TE23 5TT"
        result.phoneNumber mustBe Some("01234567890")
        result.email mustBe Some("agent@example.com")
        result.agentReference mustBe Some("AGT-001")
        result.isAuthorised mustBe Some("YES")
      }

      "must deserialize valid JSON with only required fields" in {
        val result = Json.fromJson[CreateReturnAgentRequest](validCreateReturnAgentRequestJsonMinimal).asEither.value

        result.stornId mustBe "12345"
        result.returnResourceRef mustBe "RRF-2024-001"
        result.agentType mustBe "VENDOR"
        result.name mustBe "Smith & Partners"
        result.houseNumber must not be defined
        result.addressLine1 mustBe "Main Street"
        result.addressLine2 must not be defined
        result.addressLine3 must not be defined
        result.addressLine4 must not be defined
        result.postcode mustBe "TE23 5TT"
        result.phoneNumber must not be defined
        result.email must not be defined
        result.agentReference must not be defined
        result.isAuthorised must not be defined
      }

      "must deserialize JSON with null optional fields" in {
        val json = Json.obj(
          "stornId" -> "12345",
          "returnResourceRef" -> "RRF-2024-001",
          "agentType" -> "VENDOR",
          "name" -> "Smith & Partners",
          "houseNumber" -> JsNull,
          "addressLine1" -> "Main Street",
          "addressLine2" -> JsNull,
          "addressLine3" -> JsNull,
          "addressLine4" -> JsNull,
          "postcode" -> "TE23 5TT",
          "phoneNumber" -> JsNull,
          "email" -> JsNull,
          "agentReference" -> JsNull,
          "isAuthorised" -> JsNull
        )

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither.value

        result.houseNumber must not be defined
        result.addressLine2 must not be defined
        result.phoneNumber must not be defined
        result.email must not be defined
      }

      "must fail to deserialize when stornId is missing" in {
        val json = validCreateReturnAgentRequestJsonComplete - "stornId"

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        val json = validCreateReturnAgentRequestJsonComplete - "returnResourceRef"

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when agentType is missing" in {
        val json = validCreateReturnAgentRequestJsonComplete - "agentType"

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when name is missing" in {
        val json = validCreateReturnAgentRequestJsonComplete - "name"

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when addressLine1 is missing" in {
        val json = validCreateReturnAgentRequestJsonComplete - "addressLine1"

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when postcode is missing" in {
        val json = validCreateReturnAgentRequestJsonComplete - "postcode"

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when houseNumber has invalid type" in {
        val json = validCreateReturnAgentRequestJsonComplete ++ Json.obj("houseNumber" -> 123)

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when required field has invalid type" in {
        val json = validCreateReturnAgentRequestJsonComplete ++ Json.obj("stornId" -> 123)

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[CreateReturnAgentRequest]]
      }

      "must serialize CreateReturnAgentRequest with all fields" in {
        val json = Json.toJson(completeCreateReturnAgentRequest)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "RRF-2024-001"
        (json \ "agentType").as[String] mustBe "VENDOR"
        (json \ "name").as[String] mustBe "Smith & Partners"
        (json \ "houseNumber").asOpt[String] mustBe Some("10")
        (json \ "addressLine1").as[String] mustBe "Main Street"
        (json \ "addressLine2").asOpt[String] mustBe Some("Suite 5")
        (json \ "addressLine3").asOpt[String] mustBe Some("Building A")
        (json \ "addressLine4").asOpt[String] mustBe Some("District B")
        (json \ "postcode").as[String] mustBe "TE23 5TT"
        (json \ "phoneNumber").asOpt[String] mustBe Some("01234567890")
        (json \ "email").asOpt[String] mustBe Some("agent@example.com")
        (json \ "agentReference").asOpt[String] mustBe Some("AGT-001")
        (json \ "isAuthorised").asOpt[String] mustBe Some("YES")
      }

      "must serialize CreateReturnAgentRequest with only required fields" in {
        val json = Json.toJson(minimalCreateReturnAgentRequest)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "RRF-2024-001"
        (json \ "agentType").as[String] mustBe "VENDOR"
        (json \ "name").as[String] mustBe "Smith & Partners"
        (json \ "addressLine1").as[String] mustBe "Main Street"
        (json \ "postcode").as[String] mustBe "TE23 5TT"
      }

      "must serialize None optional fields correctly" in {
        val json = Json.toJson(minimalCreateReturnAgentRequest)

        val deserialized = Json.fromJson[CreateReturnAgentRequest](json).asEither.value
        deserialized.houseNumber must not be defined
        deserialized.addressLine2 must not be defined
        deserialized.phoneNumber must not be defined
        deserialized.email must not be defined
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(completeCreateReturnAgentRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("stornId", "returnResourceRef", "agentType", "name", "houseNumber", "addressLine1", "addressLine2", "addressLine3", "addressLine4", "postcode", "phoneNumber", "email", "agentReference", "isAuthorised")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[CreateReturnAgentRequest]]
      }

      "must round-trip serialize and deserialize with all fields" in {
        val json = Json.toJson(completeCreateReturnAgentRequest)
        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither.value

        result mustEqual completeCreateReturnAgentRequest
      }

      "must round-trip serialize and deserialize with only required fields" in {
        val json = Json.toJson(minimalCreateReturnAgentRequest)
        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither.value

        result mustEqual minimalCreateReturnAgentRequest
      }

      "must round-trip with mixed optional fields" in {
        val mixedCreateReturnAgentRequest = CreateReturnAgentRequest(
          stornId = "12345",
          returnResourceRef = "RRF-2024-001",
          agentType = "VENDOR",
          name = "Smith & Partners",
          houseNumber = Some("10"),
          addressLine1 = "Main Street",
          addressLine2 = None,
          addressLine3 = Some("Building A"),
          addressLine4 = None,
          postcode = "TE23 5TT",
          phoneNumber = Some("01234567890"),
          email = None,
          agentReference = Some("AGT-001"),
          isAuthorised = None
        )

        val json = Json.toJson(mixedCreateReturnAgentRequest)
        val result = Json.fromJson[CreateReturnAgentRequest](json).asEither.value

        result mustEqual mixedCreateReturnAgentRequest
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        completeCreateReturnAgentRequest.stornId mustBe "12345"
        completeCreateReturnAgentRequest.returnResourceRef mustBe "RRF-2024-001"
        completeCreateReturnAgentRequest.agentType mustBe "VENDOR"
        completeCreateReturnAgentRequest.houseNumber mustBe Some("10")
        completeCreateReturnAgentRequest.email mustBe Some("agent@example.com")
      }

      "must create instance with only required fields" in {
        minimalCreateReturnAgentRequest.stornId mustBe "12345"
        minimalCreateReturnAgentRequest.houseNumber must not be defined
        minimalCreateReturnAgentRequest.email must not be defined
        minimalCreateReturnAgentRequest.isAuthorised must not be defined
      }

      "must support equality" in {
        val agentRequest1 = minimalCreateReturnAgentRequest
        val agentRequest2 = minimalCreateReturnAgentRequest.copy()

        agentRequest1 mustEqual agentRequest2
      }

      "must support equality with all fields" in {
        val agentRequest1 = completeCreateReturnAgentRequest
        val agentRequest2 = completeCreateReturnAgentRequest.copy()

        agentRequest1 mustEqual agentRequest2
      }

      "must support copy with modifications" in {
        val modified = minimalCreateReturnAgentRequest.copy(
          houseNumber = Some("99"),
          email = Some("new@example.com"),
          isAuthorised = Some("NO")
        )

        modified.houseNumber mustBe Some("99")
        modified.email mustBe Some("new@example.com")
        modified.isAuthorised mustBe Some("NO")
        modified.stornId mustBe minimalCreateReturnAgentRequest.stornId
      }

      "must not be equal when required fields differ" in {
        val agentRequest1 = minimalCreateReturnAgentRequest
        val agentRequest2 = minimalCreateReturnAgentRequest.copy(stornId = "DIFFERENT")

        agentRequest1 must not equal agentRequest2
      }

      "must not be equal when optional fields differ" in {
        val agentRequest1 = minimalCreateReturnAgentRequest
        val agentRequest2 = minimalCreateReturnAgentRequest.copy(houseNumber = Some("100"))

        agentRequest1 must not equal agentRequest2
      }
    }
  }

  "CreateReturnAgentReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[CreateReturnAgentReturn]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[CreateReturnAgentReturn](validCreateReturnAgentReturnJson).asEither.value

        result.returnAgentID mustBe "AGID-001"
      }

      "must fail to deserialize when returnAgentID is missing" in {
        val json = Json.obj()

        val result = Json.fromJson[CreateReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnAgentID has invalid type" in {
        val json = Json.obj("returnAgentID" -> 123)

        val result = Json.fromJson[CreateReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[CreateReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[CreateReturnAgentReturn]]
      }

      "must serialize CreateReturnAgentReturn with all fields" in {
        val json = Json.toJson(createReturnAgentReturn)

        (json \ "returnAgentID").as[String] mustBe "AGID-001"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(createReturnAgentReturn)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("returnAgentID")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[CreateReturnAgentReturn]]
      }

      "must round-trip serialize and deserialize" in {
        val json = Json.toJson(createReturnAgentReturn)
        val result = Json.fromJson[CreateReturnAgentReturn](json).asEither.value

        result mustEqual createReturnAgentReturn
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        createReturnAgentReturn.returnAgentID mustBe "AGID-001"
      }

      "must support equality" in {
        val agentReturn1 = createReturnAgentReturn
        val agentReturn2 = createReturnAgentReturn.copy()

        agentReturn1 mustEqual agentReturn2
      }

      "must support copy with modifications" in {
        val modified = createReturnAgentReturn.copy(returnAgentID = "AGID-002")

        modified.returnAgentID mustBe "AGID-002"
      }

      "must not be equal when fields differ" in {
        val agentReturn1 = createReturnAgentReturn
        val agentReturn2 = createReturnAgentReturn.copy(returnAgentID = "AGID-002")

        agentReturn1 must not equal agentReturn2
      }
    }
  }

  "UpdateReturnAgentRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[UpdateReturnAgentRequest]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[UpdateReturnAgentRequest](validUpdateReturnAgentRequestJsonComplete).asEither.value

        result.stornId mustBe "12345"
        result.returnResourceRef mustBe "RRF-2024-001"
        result.agentType mustBe "VENDOR"
        result.name mustBe "Smith & Partners Updated"
        result.houseNumber mustBe Some("10")
        result.addressLine1 mustBe "Main Street"
        result.addressLine2 mustBe Some("Suite 5")
        result.addressLine3 mustBe Some("Building A")
        result.addressLine4 mustBe Some("District B")
        result.postcode mustBe "TE23 5TT"
        result.phoneNumber mustBe Some("01234567890")
        result.email mustBe Some("agent@example.com")
        result.agentReference mustBe Some("AGT-001")
        result.isAuthorised mustBe Some("YES")
      }

      "must deserialize valid JSON with only required fields" in {
        val result = Json.fromJson[UpdateReturnAgentRequest](validUpdateReturnAgentRequestJsonMinimal).asEither.value

        result.stornId mustBe "12345"
        result.returnResourceRef mustBe "RRF-2024-001"
        result.agentType mustBe "VENDOR"
        result.name mustBe "Smith & Partners"
        result.houseNumber must not be defined
        result.addressLine1 mustBe "Main Street"
        result.addressLine2 must not be defined
        result.addressLine3 must not be defined
        result.addressLine4 must not be defined
        result.postcode mustBe "TE23 5TT"
        result.phoneNumber must not be defined
        result.email must not be defined
        result.agentReference must not be defined
        result.isAuthorised must not be defined
      }

      "must deserialize JSON with null optional fields" in {
        val json = Json.obj(
          "stornId" -> "12345",
          "returnResourceRef" -> "RRF-2024-001",
          "agentType" -> "VENDOR",
          "name" -> "Smith & Partners",
          "houseNumber" -> JsNull,
          "addressLine1" -> "Main Street",
          "addressLine2" -> JsNull,
          "addressLine3" -> JsNull,
          "addressLine4" -> JsNull,
          "postcode" -> "TE23 5TT",
          "phoneNumber" -> JsNull,
          "email" -> JsNull,
          "agentReference" -> JsNull,
          "isAuthorised" -> JsNull
        )

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither.value

        result.houseNumber must not be defined
        result.addressLine2 must not be defined
        result.phoneNumber must not be defined
        result.email must not be defined
      }

      "must fail to deserialize when stornId is missing" in {
        val json = validUpdateReturnAgentRequestJsonComplete - "stornId"

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        val json = validUpdateReturnAgentRequestJsonComplete - "returnResourceRef"

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when agentType is missing" in {
        val json = validUpdateReturnAgentRequestJsonComplete - "agentType"

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when name is missing" in {
        val json = validUpdateReturnAgentRequestJsonComplete - "name"

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when addressLine1 is missing" in {
        val json = validUpdateReturnAgentRequestJsonComplete - "addressLine1"

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when postcode is missing" in {
        val json = validUpdateReturnAgentRequestJsonComplete - "postcode"

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when houseNumber has invalid type" in {
        val json = validUpdateReturnAgentRequestJsonComplete ++ Json.obj("houseNumber" -> 123)

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when required field has invalid type" in {
        val json = validUpdateReturnAgentRequestJsonComplete ++ Json.obj("stornId" -> 123)

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[UpdateReturnAgentRequest]]
      }

      "must serialize UpdateReturnAgentRequest with all fields" in {
        val json = Json.toJson(completeUpdateReturnAgentRequest)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "RRF-2024-001"
        (json \ "agentType").as[String] mustBe "VENDOR"
        (json \ "name").as[String] mustBe "Smith & Partners Updated"
        (json \ "houseNumber").asOpt[String] mustBe Some("10")
        (json \ "addressLine1").as[String] mustBe "Main Street"
        (json \ "addressLine2").asOpt[String] mustBe Some("Suite 5")
        (json \ "addressLine3").asOpt[String] mustBe Some("Building A")
        (json \ "addressLine4").asOpt[String] mustBe Some("District B")
        (json \ "postcode").as[String] mustBe "TE23 5TT"
        (json \ "phoneNumber").asOpt[String] mustBe Some("01234567890")
        (json \ "email").asOpt[String] mustBe Some("agent@example.com")
        (json \ "agentReference").asOpt[String] mustBe Some("AGT-001")
        (json \ "isAuthorised").asOpt[String] mustBe Some("YES")
      }

      "must serialize UpdateReturnAgentRequest with only required fields" in {
        val json = Json.toJson(minimalUpdateReturnAgentRequest)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "RRF-2024-001"
        (json \ "agentType").as[String] mustBe "VENDOR"
        (json \ "name").as[String] mustBe "Smith & Partners"
        (json \ "addressLine1").as[String] mustBe "Main Street"
        (json \ "postcode").as[String] mustBe "TE23 5TT"
      }

      "must serialize None optional fields correctly" in {
        val json = Json.toJson(minimalUpdateReturnAgentRequest)

        val deserialized = Json.fromJson[UpdateReturnAgentRequest](json).asEither.value
        deserialized.houseNumber must not be defined
        deserialized.addressLine2 must not be defined
        deserialized.phoneNumber must not be defined
        deserialized.email must not be defined
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(completeUpdateReturnAgentRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("stornId", "returnResourceRef", "agentType", "name", "houseNumber", "addressLine1", "addressLine2", "addressLine3", "addressLine4", "postcode", "phoneNumber", "email", "agentReference", "isAuthorised")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[UpdateReturnAgentRequest]]
      }

      "must round-trip serialize and deserialize with all fields" in {
        val json = Json.toJson(completeUpdateReturnAgentRequest)
        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither.value

        result mustEqual completeUpdateReturnAgentRequest
      }

      "must round-trip serialize and deserialize with only required fields" in {
        val json = Json.toJson(minimalUpdateReturnAgentRequest)
        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither.value

        result mustEqual minimalUpdateReturnAgentRequest
      }

      "must round-trip with mixed optional fields" in {
        val mixedUpdateReturnAgentRequest = UpdateReturnAgentRequest(
          stornId = "12345",
          returnResourceRef = "RRF-2024-001",
          agentType = "VENDOR",
          name = "Smith & Partners",
          houseNumber = Some("10"),
          addressLine1 = "Main Street",
          addressLine2 = None,
          addressLine3 = Some("Building A"),
          addressLine4 = None,
          postcode = "TE23 5TT",
          phoneNumber = Some("01234567890"),
          email = None,
          agentReference = Some("AGT-001"),
          isAuthorised = None
        )

        val json = Json.toJson(mixedUpdateReturnAgentRequest)
        val result = Json.fromJson[UpdateReturnAgentRequest](json).asEither.value

        result mustEqual mixedUpdateReturnAgentRequest
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        completeUpdateReturnAgentRequest.stornId mustBe "12345"
        completeUpdateReturnAgentRequest.returnResourceRef mustBe "RRF-2024-001"
        completeUpdateReturnAgentRequest.agentType mustBe "VENDOR"
        completeUpdateReturnAgentRequest.houseNumber mustBe Some("10")
        completeUpdateReturnAgentRequest.email mustBe Some("agent@example.com")
      }

      "must create instance with only required fields" in {
        minimalUpdateReturnAgentRequest.stornId mustBe "12345"
        minimalUpdateReturnAgentRequest.houseNumber must not be defined
        minimalUpdateReturnAgentRequest.email must not be defined
        minimalUpdateReturnAgentRequest.isAuthorised must not be defined
      }

      "must support equality" in {
        val agentRequest1 = minimalUpdateReturnAgentRequest
        val agentRequest2 = minimalUpdateReturnAgentRequest.copy()

        agentRequest1 mustEqual agentRequest2
      }

      "must support equality with all fields" in {
        val agentRequest1 = completeUpdateReturnAgentRequest
        val agentRequest2 = completeUpdateReturnAgentRequest.copy()

        agentRequest1 mustEqual agentRequest2
      }

      "must support copy with modifications" in {
        val modified = minimalUpdateReturnAgentRequest.copy(
          houseNumber = Some("99"),
          email = Some("new@example.com"),
          isAuthorised = Some("NO")
        )

        modified.houseNumber mustBe Some("99")
        modified.email mustBe Some("new@example.com")
        modified.isAuthorised mustBe Some("NO")
        modified.stornId mustBe minimalUpdateReturnAgentRequest.stornId
      }

      "must not be equal when required fields differ" in {
        val agentRequest1 = minimalUpdateReturnAgentRequest
        val agentRequest2 = minimalUpdateReturnAgentRequest.copy(stornId = "DIFFERENT")

        agentRequest1 must not equal agentRequest2
      }

      "must not be equal when optional fields differ" in {
        val agentRequest1 = minimalUpdateReturnAgentRequest
        val agentRequest2 = minimalUpdateReturnAgentRequest.copy(houseNumber = Some("100"))

        agentRequest1 must not equal agentRequest2
      }
    }
  }

  "UpdateReturnAgentReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[UpdateReturnAgentReturn]]
      }

      "must deserialize valid JSON with updated true" in {
        val result = Json.fromJson[UpdateReturnAgentReturn](validUpdateReturnAgentReturnJsonTrue).asEither.value

        result.updated mustBe true
      }

      "must deserialize valid JSON with updated false" in {
        val result = Json.fromJson[UpdateReturnAgentReturn](validUpdateReturnAgentReturnJsonFalse).asEither.value

        result.updated mustBe false
      }

      "must fail to deserialize when updated is missing" in {
        val json = Json.obj()

        val result = Json.fromJson[UpdateReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when updated has invalid type" in {
        val json = Json.obj("updated" -> "invalid")

        val result = Json.fromJson[UpdateReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[UpdateReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[UpdateReturnAgentReturn]]
      }

      "must serialize UpdateReturnAgentReturn with updated true" in {
        val json = Json.toJson(updateReturnAgentReturnTrue)

        (json \ "updated").as[Boolean] mustBe true
      }

      "must serialize UpdateReturnAgentReturn with updated false" in {
        val json = Json.toJson(updateReturnAgentReturnFalse)

        (json \ "updated").as[Boolean] mustBe false
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(updateReturnAgentReturnTrue)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("updated")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[UpdateReturnAgentReturn]]
      }

      "must round-trip serialize and deserialize with updated true" in {
        val json = Json.toJson(updateReturnAgentReturnTrue)
        val result = Json.fromJson[UpdateReturnAgentReturn](json).asEither.value

        result mustEqual updateReturnAgentReturnTrue
      }

      "must round-trip serialize and deserialize with updated false" in {
        val json = Json.toJson(updateReturnAgentReturnFalse)
        val result = Json.fromJson[UpdateReturnAgentReturn](json).asEither.value

        result mustEqual updateReturnAgentReturnFalse
      }
    }

    "case class" - {

      "must create instance with updated true" in {
        updateReturnAgentReturnTrue.updated mustBe true
      }

      "must create instance with updated false" in {
        updateReturnAgentReturnFalse.updated mustBe false
      }

      "must support equality" in {
        val agentReturn1 = updateReturnAgentReturnTrue
        val agentReturn2 = updateReturnAgentReturnTrue.copy()

        agentReturn1 mustEqual agentReturn2
      }

      "must support copy with modifications" in {
        val modified = updateReturnAgentReturnTrue.copy(updated = false)

        modified.updated mustBe false
      }

      "must not be equal when fields differ" in {
        val agentReturn1 = updateReturnAgentReturnTrue
        val agentReturn2 = updateReturnAgentReturnFalse

        agentReturn1 must not equal agentReturn2
      }
    }
  }

  "DeleteReturnAgentRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[DeleteReturnAgentRequest]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[DeleteReturnAgentRequest](validDeleteReturnAgentRequestJson).asEither.value

        result.storn mustBe "12345"
        result.returnResourceRef mustBe "RRF-2024-001"
        result.agentType mustBe "VENDOR"
      }

      "must fail to deserialize when storn is missing" in {
        val json = validDeleteReturnAgentRequestJson - "storn"

        val result = Json.fromJson[DeleteReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        val json = validDeleteReturnAgentRequestJson - "returnResourceRef"

        val result = Json.fromJson[DeleteReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when agentType is missing" in {
        val json = validDeleteReturnAgentRequestJson - "agentType"

        val result = Json.fromJson[DeleteReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when storn has invalid type" in {
        val json = validDeleteReturnAgentRequestJson ++ Json.obj("storn" -> 123)

        val result = Json.fromJson[DeleteReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef has invalid type" in {
        val json = validDeleteReturnAgentRequestJson ++ Json.obj("returnResourceRef" -> true)

        val result = Json.fromJson[DeleteReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when agentType has invalid type" in {
        val json = validDeleteReturnAgentRequestJson ++ Json.obj("agentType" -> 456)

        val result = Json.fromJson[DeleteReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[DeleteReturnAgentRequest](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[DeleteReturnAgentRequest]]
      }

      "must serialize DeleteReturnAgentRequest with all fields" in {
        val json = Json.toJson(deleteReturnAgentRequest)

        (json \ "storn").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "RRF-2024-001"
        (json \ "agentType").as[String] mustBe "VENDOR"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(deleteReturnAgentRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("storn", "returnResourceRef", "agentType")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[DeleteReturnAgentRequest]]
      }

      "must round-trip serialize and deserialize" in {
        val json = Json.toJson(deleteReturnAgentRequest)
        val result = Json.fromJson[DeleteReturnAgentRequest](json).asEither.value

        result mustEqual deleteReturnAgentRequest
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        deleteReturnAgentRequest.storn mustBe "12345"
        deleteReturnAgentRequest.returnResourceRef mustBe "RRF-2024-001"
        deleteReturnAgentRequest.agentType mustBe "VENDOR"
      }

      "must support equality" in {
        val agentRequest1 = deleteReturnAgentRequest
        val agentRequest2 = deleteReturnAgentRequest.copy()

        agentRequest1 mustEqual agentRequest2
      }

      "must support copy with modifications" in {
        val modified = deleteReturnAgentRequest.copy(storn = "54321")

        modified.storn mustBe "54321"
        modified.returnResourceRef mustBe deleteReturnAgentRequest.returnResourceRef
        modified.agentType mustBe deleteReturnAgentRequest.agentType
      }

      "must not be equal when fields differ" in {
        val agentRequest1 = deleteReturnAgentRequest
        val agentRequest2 = deleteReturnAgentRequest.copy(agentType = "CONVEYANCER")

        agentRequest1 must not equal agentRequest2
      }

      "must not be equal when multiple fields differ" in {
        val agentRequest1 = deleteReturnAgentRequest
        val agentRequest2 = deleteReturnAgentRequest.copy(
          storn = "99999",
          returnResourceRef = "RRF-2025-999"
        )

        agentRequest1 must not equal agentRequest2
      }
    }
  }

  "DeleteReturnAgentReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[DeleteReturnAgentReturn]]
      }

      "must deserialize valid JSON with deleted true" in {
        val result = Json.fromJson[DeleteReturnAgentReturn](validDeleteReturnAgentReturnJsonTrue).asEither.value

        result.deleted mustBe true
      }

      "must deserialize valid JSON with deleted false" in {
        val result = Json.fromJson[DeleteReturnAgentReturn](validDeleteReturnAgentReturnJsonFalse).asEither.value

        result.deleted mustBe false
      }

      "must fail to deserialize when deleted is missing" in {
        val json = Json.obj()

        val result = Json.fromJson[DeleteReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when deleted has invalid type" in {
        val json = Json.obj("deleted" -> "invalid")

        val result = Json.fromJson[DeleteReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[DeleteReturnAgentReturn](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[DeleteReturnAgentReturn]]
      }

      "must serialize DeleteReturnAgentReturn with deleted true" in {
        val json = Json.toJson(deleteReturnAgentReturnTrue)

        (json \ "deleted").as[Boolean] mustBe true
      }

      "must serialize DeleteReturnAgentReturn with deleted false" in {
        val json = Json.toJson(deleteReturnAgentReturnFalse)

        (json \ "deleted").as[Boolean] mustBe false
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(deleteReturnAgentReturnTrue)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("deleted")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[DeleteReturnAgentReturn]]
      }

      "must round-trip serialize and deserialize with deleted true" in {
        val json = Json.toJson(deleteReturnAgentReturnTrue)
        val result = Json.fromJson[DeleteReturnAgentReturn](json).asEither.value

        result mustEqual deleteReturnAgentReturnTrue
      }

      "must round-trip serialize and deserialize with deleted false" in {
        val json = Json.toJson(deleteReturnAgentReturnFalse)
        val result = Json.fromJson[DeleteReturnAgentReturn](json).asEither.value

        result mustEqual deleteReturnAgentReturnFalse
      }
    }

    "case class" - {

      "must create instance with deleted true" in {
        deleteReturnAgentReturnTrue.deleted mustBe true
      }

      "must create instance with deleted false" in {
        deleteReturnAgentReturnFalse.deleted mustBe false
      }

      "must support equality" in {
        val agentReturn1 = deleteReturnAgentReturnTrue
        val agentReturn2 = deleteReturnAgentReturnTrue.copy()

        agentReturn1 mustEqual agentReturn2
      }

      "must support copy with modifications" in {
        val modified = deleteReturnAgentReturnTrue.copy(deleted = false)

        modified.deleted mustBe false
      }

      "must not be equal when fields differ" in {
        val agentReturn1 = deleteReturnAgentReturnTrue
        val agentReturn2 = deleteReturnAgentReturnFalse

        agentReturn1 must not equal agentReturn2
      }
    }
  }

}
