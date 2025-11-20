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

class VendorReturnSpec extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  private val validDeleteVendorReturnJsonTrue = Json.obj(
    "deleted" -> true
  )

  private val validDeleteVendorReturnJsonFalse = Json.obj(
    "deleted" -> false
  )

  private val deleteVendorReturnTrue = DeleteVendorReturn(deleted = true)
  private val deleteVendorReturnFalse = DeleteVendorReturn(deleted = false)

  private val validDeleteVendorRequestJson = Json.obj(
    "storn" -> "12345",
    "vendorResourceRef" -> "VRF-001",
    "returnResourceRef" -> "VID-001"
  )

  private val deleteVendorRequest = DeleteVendorRequest(
    storn = "12345",
    vendorResourceRef = "VRF-001",
    returnResourceRef = "VID-001"
  )


  private val validUpdateVendorReturnJsonTrue = Json.obj(
    "updated" -> true
  )

  private val validUpdateVendorReturnJsonFalse = Json.obj(
    "updated" -> false
  )

  private val updateVendorReturnTrue = UpdateVendorReturn(updated = true)
  private val updateVendorReturnFalse = UpdateVendorReturn(updated = false)

  private val validUpdateVendorRequestJsonComplete = Json.obj(
    "stornId" -> "12345",
    "returnResourceRef" -> "45678",
    "title" -> "Mr",
    "forename1" -> "A",
    "forename2" -> "B",
    "name" -> "Test",
    "houseNumber" -> "23",
    "addressLine1" -> "Test Street",
    "addressLine2" -> "Apartment 5",
    "addressLine3" -> "Building A",
    "addressLine4" -> "District B",
    "postcode" -> "TE23 5TT",
    "isRepresentedByAgent" -> "YES",
    "vendorResourceRef" -> "VRF-001",
    "nextVendorId" -> "VID-002"
  )

  private val validUpdateVendorRequestJsonMinimal = Json.obj(
    "stornId" -> "12345",
    "returnResourceRef" -> "45678",
    "name" -> "Test",
    "addressLine1" -> "Test Street",
    "isRepresentedByAgent" -> "YES",
    "vendorResourceRef" -> "VRF-001"
  )

  private val completeUpdateVendorRequest = UpdateVendorRequest(
    stornId = "12345",
    returnResourceRef = "45678",
    title = Some("Mr"),
    forename1 = Some("A"),
    forename2 = Some("B"),
    name = "Test",
    houseNumber = Some("23"),
    addressLine1 = "Test Street",
    addressLine2 = Some("Apartment 5"),
    addressLine3 = Some("Building A"),
    addressLine4 = Some("District B"),
    postcode = Some("TE23 5TT"),
    isRepresentedByAgent = "YES",
    vendorResourceRef = "VRF-001",
    nextVendorId = Some("VID-002")
  )

  private val minimalUpdateVendorRequest = UpdateVendorRequest(
    stornId = "12345",
    returnResourceRef = "45678",
    name = "Test",
    addressLine1 = "Test Street",
    isRepresentedByAgent = "YES",
    vendorResourceRef = "VRF-001"
  )


  private val validCreateVendorRequestJsonComplete = Json.obj(
    "stornId" -> "12345",
    "returnResourceRef" -> "45678",
    "title" -> "Mr",
    "forename1" -> "A",
    "forename2" -> "B",
    "name" -> "Test",
    "houseNumber" -> "23",
    "addressLine1" -> "Test Street",
    "addressLine2" -> "Apartment 5",
    "addressLine3" -> "Building A",
    "addressLine4" -> "District B",
    "postcode" -> "TE23 5TT",
    "isRepresentedByAgent" -> "YES"
  )

  private val validCreateVendorRequestJsonMinimal = Json.obj(
    "stornId" -> "12345",
    "returnResourceRef" -> "45678",
    "name" -> "Test",
    "addressLine1" -> "Test Street",
    "isRepresentedByAgent" -> "YES"
  )

  private val completeCreateVendorRequest = CreateVendorRequest(
    stornId = "12345",
    returnResourceRef = "45678",
    title = Some("Mr"),
    forename1 = Some("A"),
    forename2 = Some("B"),
    name = "Test",
    houseNumber = Some("23"),
    addressLine1 = "Test Street",
    addressLine2 = Some("Apartment 5"),
    addressLine3 = Some("Building A"),
    addressLine4 = Some("District B"),
    postcode = Some("TE23 5TT"),
    isRepresentedByAgent = "YES"
  )

  private val minimalCreateVendorRequest = CreateVendorRequest(
    stornId = "12345",
    returnResourceRef = "45678",
    addressLine1 = "Test Street",
    isRepresentedByAgent = "YES",
    name = "Test"
  )

  "CreateVendorRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[CreateVendorRequest]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[CreateVendorRequest](validCreateVendorRequestJsonComplete).asEither.value

        result.stornId mustBe "12345"
        result.returnResourceRef mustBe "45678"
        result.title mustBe Some("Mr")
        result.forename1 mustBe Some("A")
        result.forename2 mustBe Some("B")
        result.name mustBe "Test"
        result.houseNumber mustBe Some("23")
        result.addressLine1 mustBe "Test Street"
        result.addressLine2 mustBe Some("Apartment 5")
        result.addressLine3 mustBe Some("Building A")
        result.addressLine4 mustBe Some("District B")
        result.postcode mustBe Some("TE23 5TT")
        result.isRepresentedByAgent mustBe "YES"
      }

      "must deserialize valid JSON with only required fields" in {
        val result = Json.fromJson[CreateVendorRequest](validCreateVendorRequestJsonMinimal).asEither.value

        result.stornId mustBe "12345"
        result.returnResourceRef mustBe "45678"
        result.title  must not be defined
        result.forename1  must not be defined
        result.forename2 must not be defined
        result.name mustBe "Test"
        result.houseNumber must not be defined
        result.addressLine1 mustBe "Test Street"
        result.addressLine2 must not be defined
        result.addressLine3 must not be defined
        result.addressLine4 must not be defined
        result.postcode must not be defined
        result.isRepresentedByAgent mustBe "YES"
      }

      "must deserialize JSON with null optional fields" in {
        val json = Json.obj(
          "stornId" -> "12345",
          "returnResourceRef" -> "45678",
          "title" -> JsNull,
          "forename1" -> JsNull,
          "forename2" -> JsNull,
          "name" -> "Test",
          "houseNumber" -> JsNull,
          "addressLine1" -> "Test Street",
          "addressLine2" -> JsNull,
          "addressLine3" -> JsNull,
          "addressLine4" -> JsNull,
          "postcode" -> JsNull,
          "isRepresentedByAgent" -> "YES"
        )

        val result = Json.fromJson[CreateVendorRequest](json).asEither.value

        result.houseNumber must not be defined
        result.addressLine2 must not be defined
        result.postcode must not be defined
      }

      "must fail to deserialize when stornId is missing" in {
        val json = validCreateVendorRequestJsonComplete - "stornId"

        val result = Json.fromJson[CreateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when isRepresentedByAgent is missing" in {
        val json = validCreateVendorRequestJsonComplete - "isRepresentedByAgent"

        val result = Json.fromJson[CreateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when name is missing" in {
        val json = validCreateVendorRequestJsonComplete - "name"

        val result = Json.fromJson[CreateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when addressLine1 is missing" in {
        val json = validCreateVendorRequestJsonComplete - "addressLine1"

        val result = Json.fromJson[CreateVendorRequest](json).asEither

        result.isLeft mustBe true
      }


      "must fail to deserialize when houseNumber has invalid type" in {
        val json = validCreateVendorRequestJsonComplete ++ Json.obj("houseNumber" -> 123)

        val result = Json.fromJson[CreateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when required field has invalid type" in {
        val json = validCreateVendorRequestJsonComplete ++ Json.obj("stornId" -> 123)

        val result = Json.fromJson[CreateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[CreateVendorRequest](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[CreateVendorRequest]]
      }

      "must serialize CreateVendorRequest with all fields" in {
        val json = Json.toJson(completeCreateVendorRequest)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "45678"
        (json \ "title").as[String] mustBe "Mr"
        (json \ "forename1").as[String] mustBe "A"
        (json \ "forename2").as[String] mustBe "B"
        (json \ "name").as[String] mustBe "Test"
        (json \ "houseNumber").asOpt[String] mustBe Some("23")
        (json \ "addressLine1").as[String] mustBe "Test Street"
        (json \ "addressLine2").asOpt[String] mustBe Some("Apartment 5")
        (json \ "addressLine3").asOpt[String] mustBe Some("Building A")
        (json \ "addressLine4").asOpt[String] mustBe Some("District B")
        (json \ "postcode").asOpt[String] mustBe Some("TE23 5TT")
        (json \ "isRepresentedByAgent").as[String] mustBe "YES"
      }

      "must serialize CreateVendorRequest with only required fields" in {
        val json = Json.toJson(minimalCreateVendorRequest)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "45678"
        (json \ "name").as[String] mustBe "Test"
        (json \ "addressLine1").as[String] mustBe "Test Street"
        (json \ "isRepresentedByAgent").as[String] mustBe "YES"
      }

      "must serialize None optional fields correctly" in {
        val json = Json.toJson(minimalCreateVendorRequest)

        val deserialized = Json.fromJson[CreateVendorRequest](json).asEither.value
        deserialized.houseNumber must not be defined
        deserialized.addressLine2 must not be defined
        deserialized.postcode must not be defined
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(completeCreateVendorRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("stornId", "returnResourceRef", "title", "forename1", "forename2", "name", "houseNumber", "addressLine1", "addressLine2", "addressLine3", "addressLine4", "postcode", "isRepresentedByAgent")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[CreateVendorRequest]]
      }

      "must round-trip serialize and deserialize with all fields" in {
        val json = Json.toJson(completeCreateVendorRequest)
        val result = Json.fromJson[CreateVendorRequest](json).asEither.value

        result mustEqual completeCreateVendorRequest
      }

      "must round-trip serialize and deserialize with only required fields" in {
        val json = Json.toJson(minimalCreateVendorRequest)
        val result = Json.fromJson[CreateVendorRequest](json).asEither.value

        result mustEqual minimalCreateVendorRequest
      }

      "must round-trip with mixed optional fields" in {
        val mixedCreateVendorRequest = CreateVendorRequest(
          stornId = "12345",
          returnResourceRef = "45678",
          title = Some("Mr"),
          forename1 = Some("A"),
          forename2 = None,
          name = "Test",
          houseNumber = None,
          addressLine1 = "Test Street",
          addressLine2 = Some("Apartment 5"),
          addressLine3 = None,
          addressLine4 = Some("District B"),
          postcode = Some("TE23 5TT"),
          isRepresentedByAgent = "YES"
        )

        val json = Json.toJson(mixedCreateVendorRequest)
        val result = Json.fromJson[CreateVendorRequest](json).asEither.value

        result mustEqual mixedCreateVendorRequest
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        completeCreateVendorRequest.stornId mustBe "12345"
        completeCreateVendorRequest.houseNumber mustBe Some("23")
        completeCreateVendorRequest.addressLine2 mustBe Some("Apartment 5")
      }

      "must create instance with only required fields" in {
        minimalCreateVendorRequest.stornId mustBe "12345"
        minimalCreateVendorRequest.houseNumber must not be defined
        minimalCreateVendorRequest.addressLine2 must not be defined
      }

      "must support equality" in {
        val vendorReturn1 = minimalCreateVendorRequest
        val vendorReturn2 = minimalCreateVendorRequest.copy()

        vendorReturn1 mustEqual vendorReturn2
      }

      "must support equality with all fields" in {
        val vendorReturn1 = completeCreateVendorRequest
        val vendorReturn2 = completeCreateVendorRequest.copy()

        vendorReturn1 mustEqual vendorReturn2
      }

      "must support copy with modifications" in {
        val modified = minimalCreateVendorRequest.copy(
          houseNumber = Some("99"),
          postcode = Some("AB12 3CD")
        )

        modified.houseNumber mustBe Some("99")
        modified.postcode mustBe Some("AB12 3CD")
        modified.stornId mustBe minimalCreateVendorRequest.stornId
      }

      "must not be equal when required fields differ" in {
        val vendorReturn1 = minimalCreateVendorRequest
        val vendorReturn2 = minimalCreateVendorRequest.copy(stornId = "DIFFERENT")

        vendorReturn1 must not equal vendorReturn2
      }

      "must not be equal when optional fields differ" in {
        val vendorReturn1 = minimalCreateVendorRequest
        val vendorReturn2 = minimalCreateVendorRequest.copy(houseNumber = Some("100"))

        vendorReturn1 must not equal vendorReturn2
      }
    }
  }

  "UpdateVendorRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[UpdateVendorRequest]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[UpdateVendorRequest](validUpdateVendorRequestJsonComplete).asEither.value

        result.stornId mustBe "12345"
        result.returnResourceRef mustBe "45678"
        result.title mustBe Some("Mr")
        result.forename1 mustBe Some("A")
        result.forename2 mustBe Some("B")
        result.name mustBe "Test"
        result.houseNumber mustBe Some("23")
        result.addressLine1 mustBe "Test Street"
        result.addressLine2 mustBe Some("Apartment 5")
        result.addressLine3 mustBe Some("Building A")
        result.addressLine4 mustBe Some("District B")
        result.postcode mustBe Some("TE23 5TT")
        result.isRepresentedByAgent mustBe "YES"
        result.vendorResourceRef mustBe "VRF-001"
        result.nextVendorId mustBe Some("VID-002")
      }

      "must deserialize valid JSON with only required fields" in {
        val result = Json.fromJson[UpdateVendorRequest](validUpdateVendorRequestJsonMinimal).asEither.value

        result.stornId mustBe "12345"
        result.returnResourceRef mustBe "45678"
        result.title must not be defined
        result.forename1 must not be defined
        result.forename2 must not be defined
        result.name mustBe "Test"
        result.houseNumber must not be defined
        result.addressLine1 mustBe "Test Street"
        result.addressLine2 must not be defined
        result.addressLine3 must not be defined
        result.addressLine4 must not be defined
        result.postcode must not be defined
        result.isRepresentedByAgent mustBe "YES"
        result.vendorResourceRef mustBe "VRF-001"
        result.nextVendorId must not be defined
      }

      "must deserialize JSON with null optional fields" in {
        val json = Json.obj(
          "stornId" -> "12345",
          "returnResourceRef" -> "45678",
          "title" -> JsNull,
          "forename1" -> JsNull,
          "forename2" -> JsNull,
          "name" -> "Test",
          "houseNumber" -> JsNull,
          "addressLine1" -> "Test Street",
          "addressLine2" -> JsNull,
          "addressLine3" -> JsNull,
          "addressLine4" -> JsNull,
          "postcode" -> JsNull,
          "isRepresentedByAgent" -> "YES",
          "vendorResourceRef" -> "VRF-001",
          "nextVendorId" -> JsNull
        )

        val result = Json.fromJson[UpdateVendorRequest](json).asEither.value

        result.houseNumber must not be defined
        result.addressLine2 must not be defined
        result.postcode must not be defined
        result.nextVendorId must not be defined
      }

      "must fail to deserialize when stornId is missing" in {
        val json = validUpdateVendorRequestJsonComplete - "stornId"

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        val json = validUpdateVendorRequestJsonComplete - "returnResourceRef"

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when name is missing" in {
        val json = validUpdateVendorRequestJsonComplete - "name"

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when addressLine1 is missing" in {
        val json = validUpdateVendorRequestJsonComplete - "addressLine1"

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when isRepresentedByAgent is missing" in {
        val json = validUpdateVendorRequestJsonComplete - "isRepresentedByAgent"

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when vendorResourceRef is missing" in {
        val json = validUpdateVendorRequestJsonComplete - "vendorResourceRef"

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when houseNumber has invalid type" in {
        val json = validUpdateVendorRequestJsonComplete ++ Json.obj("houseNumber" -> 123)

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when required field has invalid type" in {
        val json = validUpdateVendorRequestJsonComplete ++ Json.obj("stornId" -> 123)

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[UpdateVendorRequest](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[UpdateVendorRequest]]
      }

      "must serialize UpdateVendorRequest with all fields" in {
        val json = Json.toJson(completeUpdateVendorRequest)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "45678"
        (json \ "title").as[String] mustBe "Mr"
        (json \ "forename1").as[String] mustBe "A"
        (json \ "forename2").as[String] mustBe "B"
        (json \ "name").as[String] mustBe "Test"
        (json \ "houseNumber").asOpt[String] mustBe Some("23")
        (json \ "addressLine1").as[String] mustBe "Test Street"
        (json \ "addressLine2").asOpt[String] mustBe Some("Apartment 5")
        (json \ "addressLine3").asOpt[String] mustBe Some("Building A")
        (json \ "addressLine4").asOpt[String] mustBe Some("District B")
        (json \ "postcode").asOpt[String] mustBe Some("TE23 5TT")
        (json \ "isRepresentedByAgent").as[String] mustBe "YES"
        (json \ "vendorResourceRef").as[String] mustBe "VRF-001"
        (json \ "nextVendorId").asOpt[String] mustBe Some("VID-002")
      }

      "must serialize UpdateVendorRequest with only required fields" in {
        val json = Json.toJson(minimalUpdateVendorRequest)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "returnResourceRef").as[String] mustBe "45678"
        (json \ "name").as[String] mustBe "Test"
        (json \ "addressLine1").as[String] mustBe "Test Street"
        (json \ "isRepresentedByAgent").as[String] mustBe "YES"
        (json \ "vendorResourceRef").as[String] mustBe "VRF-001"
      }

      "must serialize None optional fields correctly" in {
        val json = Json.toJson(minimalUpdateVendorRequest)

        val deserialized = Json.fromJson[UpdateVendorRequest](json).asEither.value
        deserialized.houseNumber must not be defined
        deserialized.addressLine2 must not be defined
        deserialized.postcode must not be defined
        deserialized.nextVendorId must not be defined
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(completeUpdateVendorRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("stornId", "returnResourceRef", "title", "forename1", "forename2", "name", "houseNumber", "addressLine1", "addressLine2", "addressLine3", "addressLine4", "postcode", "isRepresentedByAgent", "vendorResourceRef", "nextVendorId")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[UpdateVendorRequest]]
      }

      "must round-trip serialize and deserialize with all fields" in {
        val json = Json.toJson(completeUpdateVendorRequest)
        val result = Json.fromJson[UpdateVendorRequest](json).asEither.value

        result mustEqual completeUpdateVendorRequest
      }

      "must round-trip serialize and deserialize with only required fields" in {
        val json = Json.toJson(minimalUpdateVendorRequest)
        val result = Json.fromJson[UpdateVendorRequest](json).asEither.value

        result mustEqual minimalUpdateVendorRequest
      }

      "must round-trip with mixed optional fields" in {
        val mixedUpdateVendorRequest = UpdateVendorRequest(
          stornId = "12345",
          returnResourceRef = "45678",
          title = Some("Mr"),
          forename1 = Some("A"),
          forename2 = None,
          name = "Test",
          houseNumber = None,
          addressLine1 = "Test Street",
          addressLine2 = Some("Apartment 5"),
          addressLine3 = None,
          addressLine4 = Some("District B"),
          postcode = Some("TE23 5TT"),
          isRepresentedByAgent = "YES",
          vendorResourceRef = "VRF-001",
          nextVendorId = Some("VID-002")
        )

        val json = Json.toJson(mixedUpdateVendorRequest)
        val result = Json.fromJson[UpdateVendorRequest](json).asEither.value

        result mustEqual mixedUpdateVendorRequest
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        completeUpdateVendorRequest.stornId mustBe "12345"
        completeUpdateVendorRequest.houseNumber mustBe Some("23")
        completeUpdateVendorRequest.addressLine2 mustBe Some("Apartment 5")
        completeUpdateVendorRequest.vendorResourceRef mustBe "VRF-001"
        completeUpdateVendorRequest.nextVendorId mustBe Some("VID-002")
      }

      "must create instance with only required fields" in {
        minimalUpdateVendorRequest.stornId mustBe "12345"
        minimalUpdateVendorRequest.houseNumber must not be defined
        minimalUpdateVendorRequest.addressLine2 must not be defined
        minimalUpdateVendorRequest.nextVendorId must not be defined
      }

      "must support equality" in {
        val vendorRequest1 = minimalUpdateVendorRequest
        val vendorRequest2 = minimalUpdateVendorRequest.copy()

        vendorRequest1 mustEqual vendorRequest2
      }

      "must support equality with all fields" in {
        val vendorRequest1 = completeUpdateVendorRequest
        val vendorRequest2 = completeUpdateVendorRequest.copy()

        vendorRequest1 mustEqual vendorRequest2
      }

      "must support copy with modifications" in {
        val modified = minimalUpdateVendorRequest.copy(
          houseNumber = Some("99"),
          postcode = Some("AB12 3CD"),
          nextVendorId = Some("VID-003")
        )

        modified.houseNumber mustBe Some("99")
        modified.postcode mustBe Some("AB12 3CD")
        modified.nextVendorId mustBe Some("VID-003")
        modified.stornId mustBe minimalUpdateVendorRequest.stornId
      }

      "must not be equal when required fields differ" in {
        val vendorRequest1 = minimalUpdateVendorRequest
        val vendorRequest2 = minimalUpdateVendorRequest.copy(stornId = "DIFFERENT")

        vendorRequest1 must not equal vendorRequest2
      }

      "must not be equal when optional fields differ" in {
        val vendorRequest1 = minimalUpdateVendorRequest
        val vendorRequest2 = minimalUpdateVendorRequest.copy(houseNumber = Some("100"))

        vendorRequest1 must not equal vendorRequest2
      }
    }
  }

  "UpdateVendorReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[UpdateVendorReturn]]
      }

      "must deserialize valid JSON with updated true" in {
        val result = Json.fromJson[UpdateVendorReturn](validUpdateVendorReturnJsonTrue).asEither.value

        result.updated mustBe true
      }

      "must deserialize valid JSON with updated false" in {
        val result = Json.fromJson[UpdateVendorReturn](validUpdateVendorReturnJsonFalse).asEither.value

        result.updated mustBe false
      }

      "must fail to deserialize when updated is missing" in {
        val json = Json.obj()

        val result = Json.fromJson[UpdateVendorReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when updated has invalid type" in {
        val json = Json.obj("updated" -> "invalid")

        val result = Json.fromJson[UpdateVendorReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[UpdateVendorReturn](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[UpdateVendorReturn]]
      }

      "must serialize UpdateVendorReturn with updated true" in {
        val json = Json.toJson(updateVendorReturnTrue)

        (json \ "updated").as[Boolean] mustBe true
      }

      "must serialize UpdateVendorReturn with updated false" in {
        val json = Json.toJson(updateVendorReturnFalse)

        (json \ "updated").as[Boolean] mustBe false
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(updateVendorReturnTrue)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("updated")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[UpdateVendorReturn]]
      }

      "must round-trip serialize and deserialize with updated true" in {
        val json = Json.toJson(updateVendorReturnTrue)
        val result = Json.fromJson[UpdateVendorReturn](json).asEither.value

        result mustEqual updateVendorReturnTrue
      }

      "must round-trip serialize and deserialize with updated false" in {
        val json = Json.toJson(updateVendorReturnFalse)
        val result = Json.fromJson[UpdateVendorReturn](json).asEither.value

        result mustEqual updateVendorReturnFalse
      }
    }

    "case class" - {

      "must create instance with updated true" in {
        updateVendorReturnTrue.updated mustBe true
      }

      "must create instance with updated false" in {
        updateVendorReturnFalse.updated mustBe false
      }

      "must support equality" in {
        val vendorReturn1 = updateVendorReturnTrue
        val vendorReturn2 = updateVendorReturnTrue.copy()

        vendorReturn1 mustEqual vendorReturn2
      }

      "must support copy with modifications" in {
        val modified = updateVendorReturnTrue.copy(updated = false)

        modified.updated mustBe false
      }

      "must not be equal when fields differ" in {
        val vendorReturn1 = updateVendorReturnTrue
        val vendorReturn2 = updateVendorReturnFalse

        vendorReturn1 must not equal vendorReturn2
      }
    }
  }
  "DeleteVendorRequest" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[DeleteVendorRequest]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[DeleteVendorRequest](validDeleteVendorRequestJson).asEither.value

        result.storn mustBe "12345"
        result.vendorResourceRef mustBe "VRF-001"
        result.returnResourceRef mustBe "VID-001"
      }

      "must fail to deserialize when storn is missing" in {
        val json = validDeleteVendorRequestJson - "storn"

        val result = Json.fromJson[DeleteVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when vendorResourceRef is missing" in {
        val json = validDeleteVendorRequestJson - "vendorResourceRef"

        val result = Json.fromJson[DeleteVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef is missing" in {
        val json = validDeleteVendorRequestJson - "returnResourceRef"

        val result = Json.fromJson[DeleteVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when storn has invalid type" in {
        val json = validDeleteVendorRequestJson ++ Json.obj("storn" -> 123)

        val result = Json.fromJson[DeleteVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when vendorResourceRef has invalid type" in {
        val json = validDeleteVendorRequestJson ++ Json.obj("vendorResourceRef" -> true)

        val result = Json.fromJson[DeleteVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when returnResourceRef has invalid type" in {
        val json = validDeleteVendorRequestJson ++ Json.obj("returnResourceRef" -> 456)

        val result = Json.fromJson[DeleteVendorRequest](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[DeleteVendorRequest](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[DeleteVendorRequest]]
      }

      "must serialize DeleteVendorRequest with all fields" in {
        val json = Json.toJson(deleteVendorRequest)

        (json \ "storn").as[String] mustBe "12345"
        (json \ "vendorResourceRef").as[String] mustBe "VRF-001"
        (json \ "returnResourceRef").as[String] mustBe "VID-001"
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(deleteVendorRequest)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("storn", "vendorResourceRef", "returnResourceRef")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[DeleteVendorRequest]]
      }

      "must round-trip serialize and deserialize" in {
        val json = Json.toJson(deleteVendorRequest)
        val result = Json.fromJson[DeleteVendorRequest](json).asEither.value

        result mustEqual deleteVendorRequest
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        deleteVendorRequest.storn mustBe "12345"
        deleteVendorRequest.vendorResourceRef mustBe "VRF-001"
        deleteVendorRequest.returnResourceRef mustBe "VID-001"
      }

      "must support equality" in {
        val vendorRequest1 = deleteVendorRequest
        val vendorRequest2 = deleteVendorRequest.copy()

        vendorRequest1 mustEqual vendorRequest2
      }

      "must support copy with modifications" in {
        val modified = deleteVendorRequest.copy(storn = "54321")

        modified.storn mustBe "54321"
        modified.vendorResourceRef mustBe deleteVendorRequest.vendorResourceRef
        modified.returnResourceRef mustBe deleteVendorRequest.returnResourceRef
      }

      "must not be equal when fields differ" in {
        val vendorRequest1 = deleteVendorRequest
        val vendorRequest2 = deleteVendorRequest.copy(returnResourceRef = "VID-002")

        vendorRequest1 must not equal vendorRequest2
      }

      "must not be equal when multiple fields differ" in {
        val vendorRequest1 = deleteVendorRequest
        val vendorRequest2 = deleteVendorRequest.copy(
          storn = "99999",
          vendorResourceRef = "VRF-999"
        )

        vendorRequest1 must not equal vendorRequest2
      }
    }
  }

  "DeleteVendorReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[DeleteVendorReturn]]
      }

      "must deserialize valid JSON with deleted true" in {
        val result = Json.fromJson[DeleteVendorReturn](validDeleteVendorReturnJsonTrue).asEither.value

        result.deleted mustBe true
      }

      "must deserialize valid JSON with deleted false" in {
        val result = Json.fromJson[DeleteVendorReturn](validDeleteVendorReturnJsonFalse).asEither.value

        result.deleted mustBe false
      }

      "must fail to deserialize when deleted is missing" in {
        val json = Json.obj()

        val result = Json.fromJson[DeleteVendorReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when deleted has invalid type" in {
        val json = Json.obj("deleted" -> "invalid")

        val result = Json.fromJson[DeleteVendorReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[DeleteVendorReturn](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[DeleteVendorReturn]]
      }

      "must serialize DeleteVendorReturn with deleted true" in {
        val json = Json.toJson(deleteVendorReturnTrue)

        (json \ "deleted").as[Boolean] mustBe true
      }

      "must serialize DeleteVendorReturn with deleted false" in {
        val json = Json.toJson(deleteVendorReturnFalse)

        (json \ "deleted").as[Boolean] mustBe false
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(deleteVendorReturnTrue)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain("deleted")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[DeleteVendorReturn]]
      }

      "must round-trip serialize and deserialize with deleted true" in {
        val json = Json.toJson(deleteVendorReturnTrue)
        val result = Json.fromJson[DeleteVendorReturn](json).asEither.value

        result mustEqual deleteVendorReturnTrue
      }

      "must round-trip serialize and deserialize with deleted false" in {
        val json = Json.toJson(deleteVendorReturnFalse)
        val result = Json.fromJson[DeleteVendorReturn](json).asEither.value

        result mustEqual deleteVendorReturnFalse
      }
    }

    "case class" - {

      "must create instance with deleted true" in {
        deleteVendorReturnTrue.deleted mustBe true
      }

      "must create instance with deleted false" in {
        deleteVendorReturnFalse.deleted mustBe false
      }

      "must support equality" in {
        val vendorReturn1 = deleteVendorReturnTrue
        val vendorReturn2 = deleteVendorReturnTrue.copy()

        vendorReturn1 mustEqual vendorReturn2
      }

      "must support copy with modifications" in {
        val modified = deleteVendorReturnTrue.copy(deleted = false)

        modified.deleted mustBe false
      }

      "must not be equal when fields differ" in {
        val vendorReturn1 = deleteVendorReturnTrue
        val vendorReturn2 = deleteVendorReturnFalse

        vendorReturn1 must not equal vendorReturn2
      }
    }
  }

}