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

import models.filing._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

class LeaseModelsSpec extends AnyFreeSpec with Matchers {

  "LeasePayload" - {

    "must serialize to JSON correctly with all fields populated" in {
      val payload = LeasePayload(
        isAnnualRentOver1000 = Some("true"),
        contractEndDate = Some("2026-01-01"),
        contractStartDate = Some("2025-01-01"),
        leaseType = Some("GRANTED"),
        netPresentValue = Some("1000"),
        totalPremiumPayable = Some("500"),
        rentFreePeriod = Some("0"),
        startingRent = Some("100"),
        startingRentEndDate = Some("2025-12-31"),
        laterRentKnown = Some("false"),
        vatAmount = Some("20")
      )

      val json = Json.toJson(payload)

      (json \ "isAnnualRentOver1000").as[String] mustBe "true"
      (json \ "contractEndDate").as[String] mustBe "2026-01-01"
      (json \ "contractStartDate").as[String] mustBe "2025-01-01"
      (json \ "leaseType").as[String] mustBe "GRANTED"
      (json \ "netPresentValue").as[String] mustBe "1000"
      (json \ "totalPremiumPayable").as[String] mustBe "500"
      (json \ "rentFreePeriod").as[String] mustBe "0"
      (json \ "startingRent").as[String] mustBe "100"
      (json \ "startingRentEndDate").as[String] mustBe "2025-12-31"
      (json \ "laterRentKnown").as[String] mustBe "false"
      (json \ "vatAmount").as[String] mustBe "20"
    }

    "must serialize to JSON correctly with no fields populated" in {
      val payload = LeasePayload(
        isAnnualRentOver1000 = None,
        contractEndDate = None,
        contractStartDate = None,
        leaseType = None,
        netPresentValue = None,
        totalPremiumPayable = None,
        rentFreePeriod = None,
        startingRent = None,
        startingRentEndDate = None,
        laterRentKnown = None,
        vatAmount = None
      )

      val json = Json.toJson(payload)

      (json \ "isAnnualRentOver1000").toOption mustBe None
      (json \ "leaseType").toOption mustBe None
      (json \ "netPresentValue").toOption mustBe None
      (json \ "vatAmount").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "isAnnualRentOver1000" -> "true",
        "contractEndDate"      -> "2026-01-01",
        "contractStartDate"    -> "2025-01-01",
        "leaseType"            -> "GRANTED",
        "netPresentValue"      -> "1000",
        "totalPremiumPayable"  -> "500",
        "rentFreePeriod"       -> "0",
        "startingRent"         -> "100",
        "startingRentEndDate"  -> "2025-12-31",
        "laterRentKnown"       -> "false",
        "vatAmount"            -> "20"
      )

      val result = json.validate[LeasePayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.isAnnualRentOver1000 mustBe Some("true")
      payload.contractEndDate mustBe Some("2026-01-01")
      payload.contractStartDate mustBe Some("2025-01-01")
      payload.leaseType mustBe Some("GRANTED")
      payload.netPresentValue mustBe Some("1000")
      payload.totalPremiumPayable mustBe Some("500")
      payload.rentFreePeriod mustBe Some("0")
      payload.startingRent mustBe Some("100")
      payload.startingRentEndDate mustBe Some("2025-12-31")
      payload.laterRentKnown mustBe Some("false")
      payload.vatAmount mustBe Some("20")
    }

    "must deserialize from an empty JSON object with all fields as None" in {
      val json = Json.obj()

      val result = json.validate[LeasePayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.isAnnualRentOver1000 mustBe None
      payload.contractEndDate mustBe None
      payload.contractStartDate mustBe None
      payload.leaseType mustBe None
      payload.netPresentValue mustBe None
      payload.totalPremiumPayable mustBe None
      payload.rentFreePeriod mustBe None
      payload.startingRent mustBe None
      payload.startingRentEndDate mustBe None
      payload.laterRentKnown mustBe None
      payload.vatAmount mustBe None
    }

    "must deserialize from JSON correctly with a partial set of fields" in {
      val json = Json.obj(
        "leaseType"       -> "ASSIGNED",
        "netPresentValue" -> "2500"
      )

      val result = json.validate[LeasePayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.leaseType mustBe Some("ASSIGNED")
      payload.netPresentValue mustBe Some("2500")
      payload.vatAmount mustBe None
      payload.contractStartDate mustBe None
    }
  }

  "CreateLeaseRequest" - {

    "must serialize to JSON correctly with a fully populated lease payload" in {
      val request = CreateLeaseRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        lease = LeasePayload(
          isAnnualRentOver1000 = Some("true"),
          contractEndDate = Some("2026-01-01"),
          contractStartDate = Some("2025-01-01"),
          leaseType = Some("GRANTED"),
          netPresentValue = Some("1000"),
          totalPremiumPayable = Some("500"),
          rentFreePeriod = Some("0"),
          startingRent = Some("100"),
          startingRentEndDate = Some("2025-12-31"),
          laterRentKnown = Some("false"),
          vatAmount = Some("20")
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "lease" \ "leaseType").as[String] mustBe "GRANTED"
      (json \ "lease" \ "netPresentValue").as[String] mustBe "1000"
      (json \ "lease" \ "vatAmount").as[String] mustBe "20"
    }

    "must serialize to JSON correctly with an empty lease payload" in {
      val request = CreateLeaseRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        lease = LeasePayload(None, None, None, None, None, None, None, None, None, None, None)
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "lease").toOption.isDefined mustBe true
      (json \ "lease" \ "leaseType").toOption mustBe None
    }

    "must deserialize from JSON correctly with a fully populated lease payload" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "lease" -> Json.obj(
          "isAnnualRentOver1000" -> "true",
          "contractEndDate"      -> "2026-01-01",
          "contractStartDate"    -> "2025-01-01",
          "leaseType"            -> "GRANTED",
          "netPresentValue"      -> "1000",
          "totalPremiumPayable"  -> "500",
          "rentFreePeriod"       -> "0",
          "startingRent"         -> "100",
          "startingRentEndDate"  -> "2025-12-31",
          "laterRentKnown"       -> "false",
          "vatAmount"            -> "20"
        )
      )

      val result = json.validate[CreateLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.lease.leaseType mustBe Some("GRANTED")
      request.lease.netPresentValue mustBe Some("1000")
      request.lease.vatAmount mustBe Some("20")
    }

    "must deserialize from JSON correctly with an empty lease object" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj()
      )

      val result = json.validate[CreateLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.lease.leaseType mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj()
      )

      val result = json.validate[CreateLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId" -> "STORN12345",
        "lease"   -> Json.obj()
      )

      val result = json.validate[CreateLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field lease is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[CreateLeaseRequest]

      result.isError mustBe true
    }
  }

  "CreateLeaseReturn" - {

    "must serialize to JSON correctly when created is true" in {
      val response = CreateLeaseReturn(created = true)

      val json = Json.toJson(response)

      (json \ "created").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when created is false" in {
      val response = CreateLeaseReturn(created = false)

      val json = Json.toJson(response)

      (json \ "created").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when created is true" in {
      val json = Json.obj("created" -> true)

      val result = json.validate[CreateLeaseReturn]

      result mustBe a[JsSuccess[_]]
      result.get.created mustBe true
    }

    "must fail to deserialize when created field is missing" in {
      val json = Json.obj()

      val result = json.validate[CreateLeaseReturn]

      result.isError mustBe true
    }
  }

  "UpdateLeaseRequest" - {

    "must serialize to JSON correctly with a fully populated lease payload" in {
      val request = UpdateLeaseRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        lease = LeasePayload(
          isAnnualRentOver1000 = Some("true"),
          contractEndDate = Some("2026-06-01"),
          contractStartDate = Some("2025-06-01"),
          leaseType = Some("ASSIGNED"),
          netPresentValue = Some("2000"),
          totalPremiumPayable = Some("750"),
          rentFreePeriod = Some("3"),
          startingRent = Some("150"),
          startingRentEndDate = Some("2026-05-31"),
          laterRentKnown = Some("true"),
          vatAmount = Some("30")
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "lease" \ "leaseType").as[String] mustBe "ASSIGNED"
      (json \ "lease" \ "netPresentValue").as[String] mustBe "2000"
      (json \ "lease" \ "laterRentKnown").as[String] mustBe "true"
    }

    "must deserialize from JSON correctly with a fully populated lease payload" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "lease" -> Json.obj(
          "leaseType"       -> "ASSIGNED",
          "netPresentValue" -> "2000",
          "vatAmount"       -> "30"
        )
      )

      val result = json.validate[UpdateLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.lease.leaseType mustBe Some("ASSIGNED")
      request.lease.netPresentValue mustBe Some("2000")
      request.lease.vatAmount mustBe Some("30")
    }

    "must deserialize from JSON correctly with an empty lease object" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj()
      )

      val result = json.validate[UpdateLeaseRequest]

      result mustBe a[JsSuccess[_]]
      result.get.lease.leaseType mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj()
      )

      val result = json.validate[UpdateLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId" -> "STORN12345",
        "lease"   -> Json.obj()
      )

      val result = json.validate[UpdateLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field lease is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[UpdateLeaseRequest]

      result.isError mustBe true
    }
  }

  "UpdateLeaseReturn" - {

    "must serialize to JSON correctly when updated is true" in {
      val response = UpdateLeaseReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdateLeaseReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateLeaseReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateLeaseReturn]

      result.isError mustBe true
    }
  }

  "DeleteLeaseRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeleteLeaseRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345"
      )

      val result = json.validate[DeleteLeaseRequest]

      result.isError mustBe true
    }
  }

  "DeleteLeaseReturn" - {

    "must serialize to JSON correctly when deleted is true" in {
      val response = DeleteLeaseReturn(deleted = true)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when deleted is false" in {
      val response = DeleteLeaseReturn(deleted = false)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when deleted is true" in {
      val json = Json.obj("deleted" -> true)

      val result = json.validate[DeleteLeaseReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe true
    }

    "must fail to deserialize when deleted field is missing" in {
      val json = Json.obj()

      val result = json.validate[DeleteLeaseReturn]

      result.isError mustBe true
    }
  }
}