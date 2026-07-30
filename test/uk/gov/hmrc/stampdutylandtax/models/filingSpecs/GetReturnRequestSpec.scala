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

class FullReturnSpec extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  private val validSdltOrganisationJson = Json.obj(
    "isReturnUser"            -> "true",
    "doNotDisplayWelcomePage" -> "yes",
    "storn"                   -> "STORN123456",
    "version"                 -> "1.0"
  )

  private val validReturnInfoJson = Json.obj(
    "returnID"          -> "RET123456789",
    "storn"             -> "STORN123456",
    "purchaserCounter"  -> "1",
    "vendorCounter"     -> "1",
    "landCounter"       -> "1",
    "version"           -> "1.0",
    "returnResourceRef" -> "RRF-2024-001",
    "status"            -> "SUBMITTED"
  )

  private val validPurchaserJson = Json.obj(
    "purchaserID" -> "PUR001",
    "returnID"    -> "RET123456789",
    "isCompany"   -> "yes",
    "surname"     -> "Smith",
    "forename1"   -> "John"
  )

  private val validVendorJson = Json.obj(
    "vendorID" -> "VEN001",
    "returnID" -> "RET123456789",
    "name"     -> "Johnson"
  )

  private val validCompanyDetailsJson = Json.obj(
    "companyDetailsID"      -> "CD001",
    "UTR"                   -> "1234567890",
    "companyTypeIndividual" -> "true"
  )

  private val validLandJson = Json.obj(
    "landID"       -> "LND001",
    "propertyType" -> "RESIDENTIAL",
    "postcode"     -> "NW1 6XE"
  )

  private val validTransactionJson = Json.obj(
    "transactionID"      -> "TXN001",
    "totalConsideration" -> "500000.00",
    "effectiveDate"      -> "2024-10-01"
  )

  private val validReturnAgentJson = Json.obj(
    "returnAgentID" -> "RA001",
    "name"          -> "Smith & Partners LLP",
    "agentType"     -> "SOLICITOR"
  )

  private val validAgentJson = Json.obj(
    "agentId" -> "AGT001",
    "name"    -> "Smith & Partners LLP"
  )

  private val validLeaseJson = Json.obj(
    "leaseID"              -> "LSE001",
    "isAnnualRentOver1000" -> "true",
    "leaseType"            -> "NEW"
  )

  private val validTaxCalculationJson = Json.obj(
    "taxCalculationID"   -> "TC001",
    "taxDue"             -> "15000.00",
    "honestyDeclaration" -> "true"
  )

  private val validSubmissionJson = Json.obj(
    "submissionID"     -> "SUB001",
    "storn"            -> "STORN123456",
    "submissionStatus" -> "ACCEPTED"
  )

  private val validSubmissionErrorDetailsJson = Json.obj(
    "errorDetailID" -> "ERR001",
    "errorMessage"  -> "Test error"
  )

  private val validResidencyJson = Json.obj(
    "residencyID"      -> "RES001",
    "isNonUkResidents" -> "yes"
  )

  private val validFullReturnJson = Json.obj(
    "stornId"           -> "STORN123456",
    "returnResourceRef" -> "RRF-2024-001",
    "sdltOrganisation"  -> validSdltOrganisationJson,
    "returnInfo"        -> validReturnInfoJson,
    "purchaser"         -> Json.arr(validPurchaserJson),
    "vendor"            -> Json.arr(validVendorJson)
  )

  private val validSdltOrganisation = Json.fromJson[SdltOrganisation](validSdltOrganisationJson).asOpt.get
  private val validReturnInfo       = Json.fromJson[ReturnInfo](validReturnInfoJson).asOpt.get
  private val validPurchaser        = Json.fromJson[Purchaser](validPurchaserJson).asOpt.get
  private val validVendor           = Json.fromJson[Vendor](validVendorJson).asOpt.get

  "SdltOrganisation" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[SdltOrganisation](validSdltOrganisationJson).asEither.value

        result.isReturnUser mustBe Some("true")
        result.storn mustBe Some("STORN123456")
        result.version mustBe Some("1.0")
      }

      "must deserialize empty JSON" in {
        val result = Json.fromJson[SdltOrganisation](Json.obj()).asEither.value

        result.isReturnUser must not be defined
        result.storn        must not be defined
      }
    }

    ".writes" - {

      "must serialize SdltOrganisation" in {
        val json = Json.toJson(validSdltOrganisation)

        (json \ "isReturnUser").as[String] mustBe "true"
        (json \ "storn").as[String] mustBe "STORN123456"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val json   = Json.toJson(validSdltOrganisation)
        val result = Json.fromJson[SdltOrganisation](json).asEither.value

        result mustEqual validSdltOrganisation
      }
    }
  }

  "ReturnInfo" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[ReturnInfo](validReturnInfoJson).asEither.value

        result.returnID mustBe Some("RET123456789")
        result.storn mustBe Some("STORN123456")
        result.status mustBe Some("SUBMITTED")
      }

      "must deserialize empty JSON" in {
        val result = Json.fromJson[ReturnInfo](Json.obj()).asEither.value

        result.returnID must not be defined
      }
    }

    ".writes" - {

      "must serialize ReturnInfo" in {
        val json = Json.toJson(validReturnInfo)

        (json \ "returnID").as[String] mustBe "RET123456789"
        (json \ "status").as[String] mustBe "SUBMITTED"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val json   = Json.toJson(validReturnInfo)
        val result = Json.fromJson[ReturnInfo](json).asEither.value

        result mustEqual validReturnInfo
      }
    }
  }

  "Purchaser" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[Purchaser](validPurchaserJson).asEither.value

        result.purchaserID mustBe Some("PUR001")
        result.isCompany mustBe Some("yes")
        result.surname mustBe Some("Smith")
      }

      "must deserialize empty JSON" in {
        val result = Json.fromJson[Purchaser](Json.obj()).asEither.value

        result.purchaserID must not be defined
      }

      "must fail when field has wrong type" in {
        val json   = Json.obj("purchaserID" -> 123)
        val result = Json.fromJson[Purchaser](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must serialize Purchaser" in {
        val json = Json.toJson(validPurchaser)

        (json \ "purchaserID").as[String] mustBe "PUR001"
        (json \ "surname").as[String] mustBe "Smith"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val json   = Json.toJson(validPurchaser)
        val result = Json.fromJson[Purchaser](json).asEither.value

        result mustEqual validPurchaser
      }
    }
  }

  "CompanyDetails" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[CompanyDetails](validCompanyDetailsJson).asEither.value

        result.companyDetailsID mustBe Some("CD001")
        result.UTR mustBe Some("1234567890")
      }
    }

    ".writes" - {

      "must serialize CompanyDetails" in {
        val companyDetails = Json.fromJson[CompanyDetails](validCompanyDetailsJson).asOpt.get
        val json           = Json.toJson(companyDetails)

        (json \ "companyDetailsID").as[String] mustBe "CD001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val companyDetails = Json.fromJson[CompanyDetails](validCompanyDetailsJson).asOpt.get
        val json           = Json.toJson(companyDetails)
        val result         = Json.fromJson[CompanyDetails](json).asEither.value

        result mustEqual companyDetails
      }
    }
  }

  "Vendor" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[Vendor](validVendorJson).asEither.value

        result.vendorID mustBe Some("VEN001")
        result.name mustBe Some("Johnson")
      }

      "must deserialize empty JSON" in {
        val result = Json.fromJson[Vendor](Json.obj()).asEither.value

        result.vendorID must not be defined
      }
    }

    ".writes" - {

      "must serialize Vendor" in {
        val json = Json.toJson(validVendor)

        (json \ "vendorID").as[String] mustBe "VEN001"
        (json \ "name").as[String] mustBe "Johnson"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val json   = Json.toJson(validVendor)
        val result = Json.fromJson[Vendor](json).asEither.value

        result mustEqual validVendor
      }
    }
  }

  "Land" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[Land](validLandJson).asEither.value

        result.landID mustBe Some("LND001")
        result.propertyType mustBe Some("RESIDENTIAL")
      }
    }

    ".writes" - {

      "must serialize Land" in {
        val land = Json.fromJson[Land](validLandJson).asOpt.get
        val json = Json.toJson(land)

        (json \ "landID").as[String] mustBe "LND001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val land   = Json.fromJson[Land](validLandJson).asOpt.get
        val json   = Json.toJson(land)
        val result = Json.fromJson[Land](json).asEither.value

        result mustEqual land
      }
    }
  }

  "Transaction" - {

    ".reads" - {

      "must deserialize valid JSON with String" in {
        val result = Json.fromJson[Transaction](validTransactionJson).asEither.value

        result.transactionID mustBe Some("TXN001")
        result.totalConsideration mustBe Some("500000.00")
      }
    }

    ".writes" - {

      "must serialize Transaction" in {
        val transaction = Json.fromJson[Transaction](validTransactionJson).asOpt.get
        val json        = Json.toJson(transaction)

        (json \ "transactionID").as[String] mustBe "TXN001"
        (json \ "totalConsideration").as[BigDecimal] mustBe BigDecimal("500000.00")
      }
    }

    ".formats" - {

      "must round-trip" in {
        val transaction = Json.fromJson[Transaction](validTransactionJson).asOpt.get
        val json        = Json.toJson(transaction)
        val result      = Json.fromJson[Transaction](json).asEither.value

        result mustEqual transaction
      }
    }
  }

  "ReturnAgent" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[ReturnAgent](validReturnAgentJson).asEither.value

        result.returnAgentID mustBe Some("RA001")
        result.name mustBe Some("Smith & Partners LLP")
      }
    }

    ".writes" - {

      "must serialize ReturnAgent" in {
        val returnAgent = Json.fromJson[ReturnAgent](validReturnAgentJson).asOpt.get
        val json        = Json.toJson(returnAgent)

        (json \ "returnAgentID").as[String] mustBe "RA001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val returnAgent = Json.fromJson[ReturnAgent](validReturnAgentJson).asOpt.get
        val json        = Json.toJson(returnAgent)
        val result      = Json.fromJson[ReturnAgent](json).asEither.value

        result mustEqual returnAgent
      }
    }
  }

  "Agent" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[Agent](validAgentJson).asEither.value

        result.agentId mustBe Some("AGT001")
        result.name mustBe Some("Smith & Partners LLP")
      }
    }

    ".writes" - {

      "must serialize Agent" in {
        val agent = Json.fromJson[Agent](validAgentJson).asOpt.get
        val json  = Json.toJson(agent)

        (json \ "agentId").as[String] mustBe "AGT001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val agent  = Json.fromJson[Agent](validAgentJson).asOpt.get
        val json   = Json.toJson(agent)
        val result = Json.fromJson[Agent](json).asEither.value

        result mustEqual agent
      }
    }
  }

  "Lease" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[Lease](validLeaseJson).asEither.value

        result.leaseID mustBe Some("LSE001")
        result.isAnnualRentOver1000 mustBe Some("true")
      }
    }

    ".writes" - {

      "must serialize Lease" in {
        val lease = Json.fromJson[Lease](validLeaseJson).asOpt.get
        val json  = Json.toJson(lease)

        (json \ "leaseID").as[String] mustBe "LSE001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val lease  = Json.fromJson[Lease](validLeaseJson).asOpt.get
        val json   = Json.toJson(lease)
        val result = Json.fromJson[Lease](json).asEither.value

        result mustEqual lease
      }
    }
  }

  "TaxCalculation" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[TaxCalculation](validTaxCalculationJson).asEither.value

        result.taxCalculationID mustBe Some("TC001")
        result.taxDue mustBe Some("15000.00")
      }
    }

    ".writes" - {

      "must serialize TaxCalculation" in {
        val taxCalculation = Json.fromJson[TaxCalculation](validTaxCalculationJson).asOpt.get
        val json           = Json.toJson(taxCalculation)

        (json \ "taxCalculationID").as[String] mustBe "TC001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val taxCalculation = Json.fromJson[TaxCalculation](validTaxCalculationJson).asOpt.get
        val json           = Json.toJson(taxCalculation)
        val result         = Json.fromJson[TaxCalculation](json).asEither.value

        result mustEqual taxCalculation
      }
    }
  }

  "Submission" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[Submission](validSubmissionJson).asEither.value

        result.submissionID mustBe Some("SUB001")
        result.submissionStatus mustBe Some("ACCEPTED")
      }
    }

    ".writes" - {

      "must serialize Submission" in {
        val submission = Json.fromJson[Submission](validSubmissionJson).asOpt.get
        val json       = Json.toJson(submission)

        (json \ "submissionID").as[String] mustBe "SUB001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val submission = Json.fromJson[Submission](validSubmissionJson).asOpt.get
        val json       = Json.toJson(submission)
        val result     = Json.fromJson[Submission](json).asEither.value

        result mustEqual submission
      }
    }
  }

  "SubmissionErrorDetails" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[SubmissionErrorDetails](validSubmissionErrorDetailsJson).asEither.value

        result.errorDetailID mustBe Some("ERR001")
        result.errorMessage mustBe Some("Test error")
      }
    }

    ".writes" - {

      "must serialize SubmissionErrorDetails" in {
        val errorDetails = Json.fromJson[SubmissionErrorDetails](validSubmissionErrorDetailsJson).asOpt.get
        val json         = Json.toJson(errorDetails)

        (json \ "errorDetailID").as[String] mustBe "ERR001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val errorDetails = Json.fromJson[SubmissionErrorDetails](validSubmissionErrorDetailsJson).asOpt.get
        val json         = Json.toJson(errorDetails)
        val result       = Json.fromJson[SubmissionErrorDetails](json).asEither.value

        result mustEqual errorDetails
      }
    }
  }

  "Residency" - {

    ".reads" - {

      "must deserialize valid JSON" in {
        val result = Json.fromJson[Residency](validResidencyJson).asEither.value

        result.residencyID mustBe Some("RES001")
        result.isNonUkResidents mustBe Some("yes")
      }
    }

    ".writes" - {

      "must serialize Residency" in {
        val residency = Json.fromJson[Residency](validResidencyJson).asOpt.get
        val json      = Json.toJson(residency)

        (json \ "residencyID").as[String] mustBe "RES001"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val residency = Json.fromJson[Residency](validResidencyJson).asOpt.get
        val json      = Json.toJson(residency)
        val result    = Json.fromJson[Residency](json).asEither.value

        result mustEqual residency
      }
    }
  }

  "FullReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[FullReturn]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[FullReturn](validFullReturnJson).asEither.value

        result.stornId mustBe Some("STORN123456")
        result.returnResourceRef mustBe Some("RRF-2024-001")
        result.sdltOrganisation mustBe defined
        result.returnInfo mustBe defined
        result.purchaser mustBe defined
        result.vendor mustBe defined
      }

      "must deserialize valid JSON with minimal fields" in {
        val json = Json.obj(
          "stornId" -> "STORN123456"
        )

        val result = Json.fromJson[FullReturn](json).asEither.value

        result.stornId mustBe Some("STORN123456")
        result.returnResourceRef must not be defined
        result.sdltOrganisation  must not be defined
      }

      "must deserialize valid JSON with None values" in {
        val json = Json.obj(
          "stornId"           -> JsNull,
          "returnResourceRef" -> JsNull,
          "sdltOrganisation"  -> JsNull
        )

        val result = Json.fromJson[FullReturn](json).asEither.value

        result.stornId           must not be defined
        result.returnResourceRef must not be defined
        result.sdltOrganisation  must not be defined
      }

      "must deserialize successfully when fields are missing and set to None" in {
        val json = Json.obj()

        val result = Json.fromJson[FullReturn](json).asEither.value

        result.stornId           must not be defined
        result.returnResourceRef must not be defined
        result.sdltOrganisation  must not be defined
        result.returnInfo        must not be defined
      }

      "must deserialize valid JSON with nested objects" in {
        val result = Json.fromJson[FullReturn](validFullReturnJson).asEither.value

        result.sdltOrganisation.value.storn mustBe Some("STORN123456")
        result.returnInfo.value.returnID mustBe Some("RET123456789")
      }

      "must deserialize valid JSON with sequences" in {
        val result = Json.fromJson[FullReturn](validFullReturnJson).asEither.value

        result.purchaser mustBe defined
        result.purchaser.value must have size 1
        result.purchaser.value.head.purchaserID mustBe Some("PUR001")

        result.vendor mustBe defined
        result.vendor.value must have size 1
        result.vendor.value.head.vendorID mustBe Some("VEN001")
      }

      "must fail to deserialize when nested object has invalid type for known field" in {
        val json = Json.obj(
          "stornId"          -> "STORN123456",
          "sdltOrganisation" -> Json.obj(
            "isReturnUser" -> 123
          )
        )

        val result = Json.fromJson[FullReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when sequence contains invalid objects" in {
        val json = Json.obj(
          "stornId"   -> "STORN123456",
          "purchaser" -> Json.arr(
            Json.obj("purchaserID" -> 123)
          )
        )

        val result = Json.fromJson[FullReturn](json).asEither

        result.isLeft mustBe true
      }

      "must deserialize with empty sequences" in {
        val json = Json.obj(
          "stornId"   -> "STORN123456",
          "purchaser" -> Json.arr(),
          "vendor"    -> Json.arr()
        )

        val result = Json.fromJson[FullReturn](json).asEither.value

        result.purchaser mustBe defined
        result.purchaser.value mustBe empty
        result.vendor mustBe defined
        result.vendor.value mustBe empty
      }

      "must deserialize with multiple items in sequences" in {
        val json = Json.obj(
          "stornId"   -> "STORN123456",
          "purchaser" -> Json.arr(validPurchaserJson, validPurchaserJson),
          "vendor"    -> Json.arr(validVendorJson, validVendorJson)
        )

        val result = Json.fromJson[FullReturn](json).asEither.value

        result.purchaser.value must have size 2
        result.vendor.value    must have size 2
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[FullReturn]]
      }

      "must serialize FullReturn with all fields" in {
        val fullReturn = FullReturn(
          stornId = Some("STORN123456"),
          returnResourceRef = Some("RRF-2024-001"),
          sdltOrganisation = Some(validSdltOrganisation),
          returnInfo = Some(validReturnInfo),
          purchaser = Some(Seq(validPurchaser)),
          vendor = Some(Seq(validVendor))
        )

        val json = Json.toJson(fullReturn)

        (json \ "stornId").as[String] mustBe "STORN123456"
        (json \ "returnResourceRef").as[String] mustBe "RRF-2024-001"
        (json \ "sdltOrganisation").isDefined mustBe true
        (json \ "returnInfo").isDefined mustBe true
        (json \ "purchaser").isDefined mustBe true
        (json \ "vendor").isDefined mustBe true
      }

      "must serialize FullReturn with None values" in {
        val fullReturn = FullReturn()

        val json = Json.toJson(fullReturn)

        json mustBe a[JsObject]
      }

      "must serialize FullReturn with Some nested objects" in {
        val fullReturn = FullReturn(
          stornId = Some("STORN123456"),
          sdltOrganisation = Some(validSdltOrganisation)
        )

        val json = Json.toJson(fullReturn)

        (json \ "stornId").as[String] mustBe "STORN123456"
        (json \ "sdltOrganisation").isDefined mustBe true
        (json \ "sdltOrganisation" \ "storn").as[String] mustBe "STORN123456"
      }

      "must serialize FullReturn with sequences" in {
        val fullReturn = FullReturn(
          purchaser = Some(Seq(validPurchaser, validPurchaser)),
          vendor = Some(Seq(validVendor))
        )

        val json = Json.toJson(fullReturn)

        (json \ "purchaser").as[JsArray].value must have size 2
        (json \ "vendor").as[JsArray].value    must have size 1
      }

      "must serialize FullReturn with empty sequences" in {
        val fullReturn = FullReturn(
          purchaser = Some(Seq.empty),
          vendor = Some(Seq.empty)
        )

        val json = Json.toJson(fullReturn)

        (json \ "purchaser").as[JsArray].value mustBe empty
        (json \ "vendor").as[JsArray].value mustBe empty
      }

      "must produce valid JSON structure" in {
        val fullReturn = FullReturn(
          stornId = Some("STORN123456"),
          returnResourceRef = Some("RRF-2024-001")
        )

        val json = Json.toJson(fullReturn)

        json mustBe a[JsObject]
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[FullReturn]]
      }

      "must serialize and deserialize with all fields" in {
        val fullReturn = FullReturn(
          stornId = Some("STORN123456"),
          returnResourceRef = Some("RRF-2024-001"),
          sdltOrganisation = Some(validSdltOrganisation),
          returnInfo = Some(validReturnInfo),
          purchaser = Some(Seq(validPurchaser)),
          vendor = Some(Seq(validVendor))
        )

        val json   = Json.toJson(fullReturn)
        val result = Json.fromJson[FullReturn](json).asEither.value

        result mustEqual fullReturn
      }

      "must serialize and deserialize with None values" in {
        val fullReturn = FullReturn()

        val json   = Json.toJson(fullReturn)
        val result = Json.fromJson[FullReturn](json).asEither.value

        result mustEqual fullReturn
      }

      "must serialize and deserialize with partial fields" in {
        val fullReturn = FullReturn(
          stornId = Some("STORN123456"),
          returnResourceRef = Some("RRF-2024-001")
        )

        val json   = Json.toJson(fullReturn)
        val result = Json.fromJson[FullReturn](json).asEither.value

        result mustEqual fullReturn
        result.stornId mustBe Some("STORN123456")
        result.returnResourceRef mustBe Some("RRF-2024-001")
        result.sdltOrganisation must not be defined
      }

      "must round-trip with sequences" in {
        val fullReturn = FullReturn(
          purchaser = Some(Seq(validPurchaser)),
          vendor = Some(Seq(validVendor))
        )

        val json   = Json.toJson(fullReturn)
        val result = Json.fromJson[FullReturn](json).asEither.value

        result mustEqual fullReturn
        result.purchaser.value must have size 1
        result.vendor.value    must have size 1
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        val fullReturn = FullReturn(
          stornId = Some("STORN123456"),
          returnResourceRef = Some("RRF-2024-001"),
          sdltOrganisation = Some(validSdltOrganisation),
          returnInfo = Some(validReturnInfo)
        )

        fullReturn.stornId mustBe Some("STORN123456")
        fullReturn.returnResourceRef mustBe Some("RRF-2024-001")
        fullReturn.sdltOrganisation mustBe Some(validSdltOrganisation)
        fullReturn.returnInfo mustBe Some(validReturnInfo)
      }

      "must create instance with None values" in {
        val fullReturn = FullReturn()

        fullReturn.stornId mustBe None
        fullReturn.returnResourceRef mustBe None
        fullReturn.sdltOrganisation mustBe None
      }

      "must support equality" in {
        val fullReturn1 = FullReturn()
        val fullReturn2 = FullReturn()

        fullReturn1 mustEqual fullReturn2
      }

      "must support equality with Some values" in {
        val fullReturn1 = FullReturn(
          stornId = Some("STORN123456"),
          returnResourceRef = Some("RRF-2024-001")
        )
        val fullReturn2 = FullReturn(
          stornId = Some("STORN123456"),
          returnResourceRef = Some("RRF-2024-001")
        )

        fullReturn1 mustEqual fullReturn2
      }

      "must support copy" in {
        val fullReturn1 = FullReturn()
        val fullReturn2 = fullReturn1.copy(
          stornId = Some("STORN123456"),
          returnResourceRef = Some("RRF-2024-001")
        )

        fullReturn2.stornId mustBe Some("STORN123456")
        fullReturn2.returnResourceRef mustBe Some("RRF-2024-001")
        fullReturn1.stornId           must not be defined
        fullReturn1.returnResourceRef must not be defined
      }

      "must not be equal when fields differ" in {
        val fullReturn1 = FullReturn(stornId = Some("STORN123456"))
        val fullReturn2 = FullReturn(stornId = Some("STORN789012"))

        fullReturn1 must not equal fullReturn2
      }

      "must support copy with nested objects" in {
        val fullReturn1 = FullReturn(stornId = Some("STORN123456"))
        val fullReturn2 = fullReturn1.copy(sdltOrganisation = Some(validSdltOrganisation))

        fullReturn2.sdltOrganisation mustBe defined
        fullReturn1.sdltOrganisation must not be defined
      }

      "must support copy with sequences" in {
        val fullReturn1 = FullReturn(stornId = Some("STORN123456"))
        val fullReturn2 = fullReturn1.copy(
          purchaser = Some(Seq(validPurchaser)),
          vendor = Some(Seq(validVendor))
        )

        fullReturn2.purchaser mustBe defined
        fullReturn2.vendor mustBe defined
        fullReturn1.purchaser must not be defined
        fullReturn1.vendor    must not be defined
      }
    }
  }
}
