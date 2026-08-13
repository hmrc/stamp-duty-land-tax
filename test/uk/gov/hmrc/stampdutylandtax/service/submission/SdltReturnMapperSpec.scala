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

package uk.gov.hmrc.stampdutylandtax.service.submission

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.submission.*
import models.filing.*
import SdltReturnFixtures.*

import java.nio.file.{Files, Paths}
import scala.xml.{Elem, PrettyPrinter}

class SdltReturnMapperSpec extends AnyWordSpec with Matchers:

  private val validator = new SchemaValidator()

  "Mapper on a LEASE at scale (49/49/99)" should {

    val sdlt = SdltReturnMapper.toSdltElement(leaseReturn(vendors = 49, purchasers = 49, lands = 99))

    "set TransactionDescription = 'L'" in {
      (sdlt \\ "TransactionDescription").text.trim shouldBe "L"
    }

    "emit a LandDetail/LeaseDetails block with the lease's dates and amounts" in {
      val ld = sdlt \\ "LandDetail" \ "LeaseDetails"
      ld.size shouldBe 1
      (ld \ "StartDate").text.trim       shouldBe "2026-06-01"
      (ld \ "EndDate").text.trim         shouldBe "2046-05-31"
      (ld \ "PremiumPaid").text.trim     shouldBe "50000.00"
      (ld \ "NetPresentValue").text.trim shouldBe "480000.00"
    }

    "omit AdditionalProperty under LandDetail — additional lands live in SDLT4/AboutTheLease for lease returns" in {
      (sdlt \\ "LandDetail" \ "AdditionalProperty") shouldBe empty
    }

    "flip SupplementarySections counts: SDLT3=0, SDLT4=lands-1" in {
      (sdlt \\ "SDLT2Count").text.trim shouldBe "94"
      (sdlt \\ "SDLT3Count").text.trim shouldBe "0"
      (sdlt \\ "SDLT4Count").text.trim shouldBe "98"
    }

    "emit one SDLT4 block per additional land — 98 in total at 99 lands" in {
      (sdlt \\ "SDLT4").size shouldBe 98
    }

    "emit AboutTheLease in every SDLT4 block, each referencing a different additional land" in {
      val atls = sdlt \\ "SDLT4" \ "AboutTheLease"
      atls.size shouldBe 98

      val firstProperty = (atls.head \ "Property")
      (firstProperty \ "AddressOfLand" \ "Line").text.trim should include("Land Road 2")
      (firstProperty \ "PropertyType").text.trim shouldBe "02"
    }

    "give every SDLT4/AboutTheLease/LeaseDetails the LeaseType M and source-driven monetary values (no force-zeroing)" in {
      val leaseDetails = sdlt \\ "SDLT4" \ "AboutTheLease" \ "LeaseDetails"
      leaseDetails.size shouldBe 98
      val first = leaseDetails.head
      (first \ "LeaseType").text.trim       shouldBe "M"
      (first \ "PremiumPaid").text.trim     shouldBe "50000.00"
      (first \ "NetPresentValue").text.trim shouldBe "480000.00"
      (first \ "TotalPremiumTax")           shouldBe empty
      (first \ "TotalNPVtax")               shouldBe empty
    }

    "emit a stripped AdditionalTransactionDetails on SDLT4 blocks beyond the first (no TotalConsideration)" in {
      val sdlt4Blocks = sdlt \\ "SDLT4"
      (sdlt4Blocks.head \ "AdditionalTransactionDetails" \ "TotalConsideration").size shouldBe 1
      (sdlt4Blocks(1)   \ "AdditionalTransactionDetails" \ "TotalConsideration") shouldBe empty
      (sdlt4Blocks.last \ "AdditionalTransactionDetails" \ "TotalConsideration") shouldBe empty
    }

    "omit the Consideration block from TaxCalculation for lease returns" in {
      (sdlt \\ "TaxCalculation" \ "Consideration") shouldBe empty
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-lease.xml") }
  }

  "Mapper on a return with every optional field populated" should {

    val sdlt = SdltReturnMapper.toSdltElement(richFreeholdReturn())

    "emit RestrictionsAffecting with Details" in {
      val ra = sdlt \\ "RestrictionsAffecting"
      (ra \@ "Apply") shouldBe "yes"
      (ra \ "Details").text.trim shouldBe "restrictive covenants in place"
    }

    "emit ContractDate, LandExchanged with Address, ClaimingRelief, LinkedTransaction, VAT" in {
      (sdlt \\ "ContractDate").text.trim shouldBe "2026-04-01"

      val le = sdlt \\ "LandExchanged"
      (le \@ "Exchanged") shouldBe "yes"
      (le \ "Address" \ "PostCode").text.trim shouldBe "M4 2AH"

      val cr = sdlt \\ "ClaimingRelief"
      (cr \@ "Claiming") shouldBe "yes"
      (cr \ "Reason").text.trim           shouldBe "27"
      (cr \ "SchemeNumber").text.trim     shouldBe "SCH-2026-001"
      (cr \ "ChargeableAmount").text.trim shouldBe "15000.00"

      val lt = sdlt \\ "LinkedTransaction"
      (lt \@ "IsLinked") shouldBe "yes"
      (lt \ "LinkedTotal").text.trim shouldBe "576543.00"
      (sdlt \\ "Consideration" \ "VATamount").text.trim shouldBe "96303.00"
    }

    "emit NINO, DateOfBirth, Telephone, Trustee, VendorConnected on the primary purchaser" in {
      val p = (sdlt \\ "PurchaserDetails" \ "Purchaser").head
      (p \ "NINO").text.trim            shouldBe "AB686456D"
      (p \ "DateOfBirth").text.trim     shouldBe "1980-08-12"
      (p \ "Telephone").text.trim       shouldBe "01234567890"
      (p \ "Trustee").text.trim         shouldBe "yes"
      (p \ "VendorConnected").text.trim shouldBe "yes"
    }

    "use companyName for a company purchaser's CompanyOrSurname" in {
      val ap = (sdlt \\ "PurchaserDetails" \ "AdditionalPurchaser").head
      (ap \ "Name" \ "CompanyOrSurname").text.trim shouldBe "Buyer2 Ltd"
    }

    "emit purchaser AgentDetails when the agent is authorised" in {
      val purchaser = (sdlt \\ "PurchaserDetails" \ "Purchaser").head
      (purchaser \ "AuthoriseAgent").text.trim shouldBe "yes"
      (purchaser \ "AgentDetails" \ "Name").text.trim shouldBe "Agent Smith"
    }

    "emit Residency flags when residency block is present" in {
      val pd = sdlt \\ "PurchaserDetails"
      (pd \ "ResidencyStatus").text.trim     shouldBe "no"
      (pd \ "CloseCompanyStatus").text.trim  shouldBe "no"
      (pd \ "CrownEmployeeRelief").text.trim shouldBe "no"
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-rich.xml") }
  }

  "Mapper on a non-leased return that triggers SDLT4 due to transaction answers" should {

    val sdlt = SdltReturnMapper.toSdltElement(sdlt4FreeholdReturn(vendors = 1, purchasers = 1, lands = 1))

    "set SDLT4Count = 1 when sdlt4-warranting flags are set on a non-leased return" in {
      (sdlt \\ "SDLT2Count").text.trim shouldBe "0"
      (sdlt \\ "SDLT3Count").text.trim shouldBe "0"
      (sdlt \\ "SDLT4Count").text.trim shouldBe "1"
    }

    "emit PropertyUse flags only for truthy options" in {
      val pu = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails" \ "PropertyUse"
      pu.size shouldBe 1
      (pu \ "Office").text.trim shouldBe "yes"
      (pu \ "Shop").text.trim   shouldBe "yes"
      (pu \ "Hotel")     shouldBe empty
      (pu \ "Warehouse") shouldBe empty
      (pu \ "Factory")   shouldBe empty
    }

    "emit includes-* attributes on the SDLT4 TotalConsideration" in {
      val tc = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails" \ "TotalConsideration"
      tc.size shouldBe 1
      (tc \@ "Stock")                shouldBe "yes"
      (tc \@ "Goodwill")             shouldBe "no"
      (tc \@ "Other")                shouldBe "no"
      (tc \@ "ChattelsAndMoveables") shouldBe "yes"
      tc.text.trim shouldBe "481516.00"
    }

    "emit PostTransactionRuling, ConsiderationDependentOnFutureEvents, DeferredPayment" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "PostTransactionRuling").text.trim                shouldBe "yes"
      (atd \ "PostTransactionRulingFollowed").text.trim        shouldBe "yes"
      (atd \ "ConsiderationDependentOnFutureEvents").text.trim shouldBe "yes"
      (atd \ "DeferredPayment").text.trim                      shouldBe "yes"
    }

    "omit PurchaserCompanyDetails entirely when SDLT4 fires for an individual purchaser (no company data)" in {
      val pcd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails" \ "PurchaserCompanyDetails"
      pcd shouldBe empty
    }

    "(TODO) produce no FormCode elements until the CONSIDERATION_* lookup is implemented" in {
      (sdlt \\ "Consideration" \ "FormCode") shouldBe empty
    }

    "produce no PurchaserDescription for the sdlt4 fixture (no company-type flags set)" in {
      (sdlt \\ "AdditionalTransactionDetails" \ "PurchaserDescription") shouldBe empty
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-sdlt4.xml") }
  }

  "Mapper on a non-leased return that triggers SDLT4 due to company details/purchaser answers" should {

    val sdlt = SdltReturnMapper.toSdltElement(sdlt4FreeholdReturnWithCompanyDetails())

    "set SDLT4Count = 1 when sdlt4-warranting flags are set on a non-leased return" in {
      (sdlt \\ "SDLT2Count").text.trim shouldBe "0"
      (sdlt \\ "SDLT3Count").text.trim shouldBe "0"
      (sdlt \\ "SDLT4Count").text.trim shouldBe "1"
    }

    "emit PurchaserVATreferenceNumber as the 9-digit body (GB prefix stripped to satisfy the 9-char schema limit)" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "PurchaserVATreferenceNumber").text.trim shouldBe "123456789"
    }

    "populate PurchaserCompanyDetails with TaxReferenceNumber and PlaceOfRegistration" in {
      val pcd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails" \ "PurchaserCompanyDetails"
      pcd.size shouldBe 1
      (pcd \ "TaxReferenceNumber").text.trim shouldBe "1234567890"
      (pcd \ "PlaceOfRegistration").text.trim shouldBe "England and Wales"
    }

    "emit PurchaserDescription with relevant codes" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "PurchaserDescription").size shouldBe 4
      (atd \ "PurchaserDescription").text.trim should include("01")
      (atd \ "PurchaserDescription").text.trim should include("05")
      (atd \ "PurchaserDescription").text.trim should include("08")
      (atd \ "PurchaserDescription").text.trim should include("12")
    }

    "validate against the SDLT/6 schema" in {
      assertValid(sdlt, "sdlt-sdlt4.xml")
    }
  }

  "Mapper on a triggered lease with 3 lands" should {

    val sdlt = SdltReturnMapper.toSdltElement(leaseReturnTriggered())

    "produce 2 SDLT4 blocks (lands - 1)" in {
      (sdlt \\ "SDLT4").size shouldBe 2
      (sdlt \\ "SDLT4Count").text.trim shouldBe "2"
    }

    "carry the FULL AdditionalTransactionDetails on SDLT4 #1: TotalConsideration + includes-* + trigger fields + no PurchaserCompanyDetails (no company data in fixture)" in {
      val atd1 = (sdlt \\ "SDLT4").head \ "AdditionalTransactionDetails"

      val tc = atd1 \ "TotalConsideration"
      tc.size shouldBe 1
      (tc \@ "Stock")                shouldBe "yes"
      (tc \@ "Goodwill")             shouldBe "no"
      (tc \@ "Other")                shouldBe "no"
      (tc \@ "ChattelsAndMoveables") shouldBe "yes"

      (atd1 \ "PostTransactionRuling").text.trim                shouldBe "yes"
      (atd1 \ "PostTransactionRulingFollowed").text.trim        shouldBe "yes"
      (atd1 \ "ConsiderationDependentOnFutureEvents").text.trim shouldBe "yes"
      (atd1 \ "DeferredPayment").text.trim                      shouldBe "yes"

      (atd1 \ "PurchaserCompanyDetails") shouldBe empty
    }

    "carry the STRIPPED AdditionalTransactionDetails on SDLT4 #2: trigger fields + MineralRights but no TotalConsideration / PurchaserCompanyDetails" in {
      val atd2 = (sdlt \\ "SDLT4")(1) \ "AdditionalTransactionDetails"

      (atd2 \ "PostTransactionRuling").text.trim                shouldBe "yes"
      (atd2 \ "PostTransactionRulingFollowed").text.trim        shouldBe "yes"
      (atd2 \ "ConsiderationDependentOnFutureEvents").text.trim shouldBe "yes"
      (atd2 \ "DeferredPayment").text.trim                      shouldBe "yes"
      (atd2 \ "MineralRights").text.trim shouldBe "no"
      (atd2 \ "TotalConsideration")     shouldBe empty
      (atd2 \ "PropertyUse")            shouldBe empty
      (atd2 \ "PurchaserCompanyDetails") shouldBe empty
    }

    "point each SDLT4/AboutTheLease at a different additional land" in {
      val atls = sdlt \\ "SDLT4" \ "AboutTheLease"
      atls.size shouldBe 2

      (atls.head \ "Property" \ "AddressOfLand" \ "Line").text.trim should include("Land Road 2")
      (atls(1)   \ "Property" \ "AddressOfLand" \ "Line").text.trim should include("Land Road 3")
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-lease-triggered.xml") }
  }

  "Mapper on a freehold with a COMPANY purchaser (UTR + VAT + registration + triggers)" should {

    val sdlt = SdltReturnMapper.toSdltElement(companyPurchaserFreehold())

    "render the primary Purchaser's CompanyOrSurname as the company name" in {
      val p = (sdlt \\ "PurchaserDetails" \ "Purchaser").head
      (p \ "Name" \ "CompanyOrSurname").text.trim shouldBe "Buyer1 Ltd"
      (p \ "Name" \ "Title")                      shouldBe empty
    }

    "omit NINO and DateOfBirth for a company purchaser" in {
      val p = (sdlt \\ "PurchaserDetails" \ "Purchaser").head
      (p \ "NINO")        shouldBe empty
      (p \ "DateOfBirth") shouldBe empty
    }

    "emit PurchaserVATreferenceNumber as the 9-digit body (GB prefix stripped to satisfy the 9-char schema limit)" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "PurchaserVATreferenceNumber").text.trim shouldBe "123456789"
    }

    "populate PurchaserCompanyDetails with TaxReferenceNumber, CompanyRegisteredNumber, PlaceOfRegistration" in {
      val pcd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails" \ "PurchaserCompanyDetails"
      pcd.size shouldBe 1
      (pcd \ "TaxReferenceNumber").text.trim       shouldBe "1234567890"
      (pcd \ "CompanyRegisteredNumber").text.trim  shouldBe "CR-123456"
      (pcd \ "PlaceOfRegistration").text.trim      shouldBe "England and Wales"
    }

    "fire SDLT4 with the full trigger field set" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "PostTransactionRuling").text.trim                shouldBe "yes"
      (atd \ "PostTransactionRulingFollowed").text.trim        shouldBe "yes"
      (atd \ "ConsiderationDependentOnFutureEvents").text.trim shouldBe "yes"
      (atd \ "DeferredPayment").text.trim                      shouldBe "yes"
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-company-freehold.xml") }
  }

  "Mapper on a leasehold with a COMPANY purchaser (UTR + VAT + registration + purchaser/company details triggers)" should {

    val sdlt = SdltReturnMapper.toSdltElement(sdlt4LeaseholdReturn())

    "emit PurchaserVATreferenceNumber as the 9-digit body (GB prefix stripped to satisfy the 9-char schema limit)" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "PurchaserVATreferenceNumber").text.trim shouldBe "123456789"
    }

    "populate PurchaserCompanyDetails with TaxReferenceNumber and PlaceOfRegistration" in {
      val pcd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails" \ "PurchaserCompanyDetails"
      pcd.size shouldBe 1
      (pcd \ "TaxReferenceNumber").text.trim shouldBe "1234567890"
      (pcd \ "PlaceOfRegistration").text.trim shouldBe "England and Wales"
    }

    "emit PurchaserDescription with relevant codes" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "PurchaserDescription").size shouldBe 2
      (atd \ "PurchaserDescription").text.trim should include("04")
      (atd \ "PurchaserDescription").text.trim should include("09")
    }

    "validate against the SDLT/6 schema" in {
      assertValid(sdlt, "sdlt-lease-triggered.xml")
    }
  }

  "Mapper on a lease with an INDIVIDUAL purchaser (NINO + DOB + 2 lands, no triggers)" should {

    val sdlt = SdltReturnMapper.toSdltElement(individualPurchaserLease())

    "render the primary Purchaser as an individual with NINO, DateOfBirth, and Telephone" in {
      val p = (sdlt \\ "PurchaserDetails" \ "Purchaser").head
      (p \ "Name" \ "Title").text.trim            shouldBe "Mr"
      (p \ "Name" \ "Forename").text.trim         shouldBe "Scott"
      (p \ "Name" \ "CompanyOrSurname").text.trim shouldBe "Purchaser1"
      (p \ "NINO").text.trim                      shouldBe "AB686456D"
      (p \ "DateOfBirth").text.trim               shouldBe "1985-03-15"
      (p \ "Telephone").text.trim                 shouldBe "07123456789"
    }

    "emit exactly 1 SDLT4 block (lands - 1 = 1)" in {
      (sdlt \\ "SDLT4").size shouldBe 1
      (sdlt \\ "SDLT4Count").text.trim shouldBe "1"
    }

    "give SDLT4 #1 the full ATD with TotalConsideration and omit PurchaserCompanyDetails (no company data in fixture)" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "TotalConsideration").size shouldBe 1
      (atd \ "PurchaserCompanyDetails") shouldBe empty
    }

    "omit trigger fields when no trigger flags are set" in {
      val atd = sdlt \\ "SDLT4" \ "AdditionalTransactionDetails"
      (atd \ "PostTransactionRuling")                shouldBe empty
      (atd \ "PostTransactionRulingFollowed")        shouldBe empty
      (atd \ "ConsiderationDependentOnFutureEvents") shouldBe empty
      (atd \ "DeferredPayment")                      shouldBe empty
    }

    "omit PurchaserVATreferenceNumber when no VAT reference is set" in {
      (sdlt \\ "SDLT4" \ "AdditionalTransactionDetails" \ "PurchaserVATreferenceNumber") shouldBe empty
    }

    "emit SDLT4/AboutTheLease/LeaseDetails with source-driven monetary fields (no force-zeroing)" in {
      val ld = sdlt \\ "SDLT4" \ "AboutTheLease" \ "LeaseDetails"
      ld.size shouldBe 1
      (ld \ "PremiumPaid").text.trim     shouldBe "50000.00"
      (ld \ "NetPresentValue").text.trim shouldBe "480000.00"
      // Source data has no tax fields → omitted (schema permits absence).
      (ld \ "TotalPremiumTax") shouldBe empty
      (ld \ "TotalNPVtax")     shouldBe empty
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-individual-lease.xml") }
  }

  "Mapper PostTransactionRulingFollowed handling" should {

    def withRulingFollowed(value: String): scala.xml.Elem =
      val tx = baselineFreeholdTransaction.copy(
        postTransRulingApplied  = Some("YES"),
        postTransRulingFollowed = Some(value)
      )
      SdltReturnMapper.toSdltElement(
        freeholdReturn(1, 1, 1).copy(transaction = Some(tx))
      )

    "emit 'yes' for YES" in {
      val sdlt = withRulingFollowed("YES")
      (sdlt \\ "PostTransactionRulingFollowed").text.trim shouldBe "yes"
    }

    "emit 'no' for NO" in {
      val sdlt = withRulingFollowed("NO")
      (sdlt \\ "PostTransactionRulingFollowed").text.trim shouldBe "no"
    }

    "emit 'RulingNotReceived' for the third enum value (preserving meaning vs yes/no)" in {
      val sdlt = withRulingFollowed("RulingNotReceived")
      (sdlt \\ "PostTransactionRulingFollowed").text.trim shouldBe "RulingNotReceived"
    }

    "also accept RULING_NOT_RECEIVED for snake-case input" in {
      val sdlt = withRulingFollowed("RULING_NOT_RECEIVED")
      (sdlt \\ "PostTransactionRulingFollowed").text.trim shouldBe "RulingNotReceived"
    }

    "omit the element for unrecognised values rather than silently mapping to 'no'" in {
      val sdlt = withRulingFollowed("garbage")
      (sdlt \\ "PostTransactionRulingFollowed") shouldBe empty
    }
  }

  "Mapper company-type PurchaserDescription mapping" should {

    "map each company-type flag to its schema code" in {
      val cases: Seq[(CompanyDetails => CompanyDetails, String)] = Seq(
        (_.copy(companyTypeBuilder        = Some("YES")), "01"),
        (_.copy(companyTypeSoletrader     = Some("YES")), "02"),
        (_.copy(companyTypeIndividual     = Some("YES")), "03"),
        (_.copy(companyTypePartnership    = Some("YES")), "04"),
        (_.copy(companyTypeLocalauth      = Some("YES")), "05"),
        (_.copy(companyTypeCentgov        = Some("YES")), "06"),
        (_.copy(companyTypePubliccorp     = Some("YES")), "07"),
        (_.copy(companyTypeProperty       = Some("YES")), "08"),
        (_.copy(companyTypeBank           = Some("YES")), "09"),
        (_.copy(companyTypeBuildsoc       = Some("YES")), "10"),
        (_.copy(companyTypeInsurance      = Some("YES")), "11"),
        (_.copy(companyTypePensionfund    = Some("YES")), "12"),
        (_.copy(companyTypeOtherfinancial = Some("YES")), "13"),
        (_.copy(companyTypeOthercompany   = Some("YES")), "14"),
        (_.copy(companyTypeOthercharity   = Some("YES")), "15")
      )
      cases.foreach { (setFlag, code) =>
        val sdlt = companyTypeReturn(setFlag)
        (sdlt \\ "AdditionalTransactionDetails" \ "PurchaserDescription")
          .map(_.text.trim) shouldBe Seq(code)
      }
    }

    "emit at most 4 PurchaserDescription elements, keeping the first 4 in companyTypeCodes order" in {
      val sdlt = companyTypeReturn(_.copy(
        companyTypeBuilder     = Some("YES"),
        companyTypeSoletrader  = Some("YES"),
        companyTypeIndividual  = Some("YES"),
        companyTypePartnership = Some("YES"),
        companyTypeLocalauth   = Some("YES")
      ))
      (sdlt \\ "AdditionalTransactionDetails" \ "PurchaserDescription")
        .map(_.text.trim) shouldBe Seq("01", "02", "03", "04")
    }

    "order by companyTypeCodes declaration, not by which flags were set" in {
      val sdlt = companyTypeReturn(_.copy(
        companyTypeOthercharity = Some("YES"),
        companyTypeSoletrader   = Some("YES")
      ))
      (sdlt \\ "AdditionalTransactionDetails" \ "PurchaserDescription")
        .map(_.text.trim) shouldBe Seq("02", "15")
    }

    "mirror the production envelope shape (builder/partnership/centgov/insurance -> 01,04,06,11)" in {
      val sdlt = companyTypeReturn(_.copy(
        companyTypeBuilder     = Some("YES"),
        companyTypePartnership = Some("YES"),
        companyTypeCentgov     = Some("YES"),
        companyTypeInsurance   = Some("YES")
      ))
      (sdlt \\ "AdditionalTransactionDetails" \ "PurchaserDescription")
        .map(_.text.trim) shouldBe Seq("01", "04", "06", "11")
    }

    "emit only truthy flags (skip NO / blank / unset)" in {
      val sdlt = companyTypeReturn(_.copy(
        companyTypeBuilder     = Some("YES"),
        companyTypeSoletrader  = Some("NO"),
        companyTypeIndividual  = Some(""),
        companyTypePartnership = None
      ))
      (sdlt \\ "AdditionalTransactionDetails" \ "PurchaserDescription")
        .map(_.text.trim) shouldBe Seq("01")
    }

    "render PurchaserDescription as a direct child of AdditionalTransactionDetails, not under PurchaserCompanyDetails" in {
      val sdlt = companyTypeReturn(_.copy(companyTypeBuilder = Some("YES")))
      (sdlt \\ "AdditionalTransactionDetails" \ "PurchaserDescription").size shouldBe 1
      (sdlt \\ "PurchaserCompanyDetails" \ "PurchaserDescription")           shouldBe empty
    }

    "emit no PurchaserDescription when companyDetails has no type flags" in {
      val sdlt = companyTypeReturn(identity)
      (sdlt \\ "AdditionalTransactionDetails" \ "PurchaserDescription") shouldBe empty
    }

    "emit no PurchaserDescription when companyDetails is absent" in {
      val sdlt = SdltReturnMapper.toSdltElement(companyPurchaserFreehold().copy(companyDetails = None))
      (sdlt \\ "PurchaserDescription") shouldBe empty
    }

    "validate against the SDLT/6 schema with multiple company-type codes" in {
      val sdlt = companyTypeReturn(_.copy(
        companyTypeBuilder = Some("YES"),
        companyTypeBank    = Some("YES")
      ))
      assertValid(sdlt, "sdlt-company-descriptions.xml")
    }
  }

  "Mapper at 1/1/1 (minimum valid)" should {
    val sdlt = SdltReturnMapper.toSdltElement(freeholdReturn(1, 1, 1))

    "emit one primary of each party, no Additional, no SDLT2" in {
      (sdlt \\ "VendorDetails" \ "Vendor")                 .size shouldBe 1
      (sdlt \\ "VendorDetails" \ "AdditionalVendor")       shouldBe empty
      (sdlt \\ "VendorDetails" \ "SDLT2Vendor")            shouldBe empty
      (sdlt \\ "PurchaserDetails" \ "Purchaser")           .size shouldBe 1
      (sdlt \\ "PurchaserDetails" \ "AdditionalPurchaser") shouldBe empty
      (sdlt \\ "PurchaserDetails" \ "SDLT2Purchaser")      shouldBe empty
    }

    "produce zero supplementary counts" in {
      (sdlt \\ "SDLT2Count").text.trim shouldBe "0"
      (sdlt \\ "SDLT3Count").text.trim shouldBe "0"
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-bound-1.xml") }
  }

  "Mapper at 2/2/2 (Additional but no SDLT2)" should {
    val sdlt = SdltReturnMapper.toSdltElement(freeholdReturn(2, 2, 2))

    "emit 1 primary + 1 Additional, still no SDLT2" in {
      (sdlt \\ "VendorDetails" \ "AdditionalVendor")       .size shouldBe 1
      (sdlt \\ "VendorDetails" \ "SDLT2Vendor")            shouldBe empty
      (sdlt \\ "PurchaserDetails" \ "AdditionalPurchaser") .size shouldBe 1
      (sdlt \\ "PurchaserDetails" \ "SDLT2Purchaser")      shouldBe empty
      (sdlt \\ "SDLT2Count").text.trim shouldBe "0"
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-bound-2.xml") }
  }

  "Mapper at 3/3/3 (first SDLT2 entry)" should {
    val sdlt = SdltReturnMapper.toSdltElement(freeholdReturn(3, 3, 3))

    "emit 1 + 1 + 1 for each party and SDLT2Count = 2" in {
      (sdlt \\ "VendorDetails" \ "SDLT2Vendor")        .size shouldBe 1
      (sdlt \\ "PurchaserDetails" \ "SDLT2Purchaser")  .size shouldBe 1
      (sdlt \\ "SDLT2Count").text.trim shouldBe "2"
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-bound-3.xml") }
  }

  "Mapper at 52/51/99 (schema maximum: SDLT2Count = 99 exactly)" should {
    val sdlt = SdltReturnMapper.toSdltElement(freeholdReturn(vendors = 51, purchasers = 52, lands = 99))

    "hit the schema's hard SDLT2Count cap of 99" in {
      (sdlt \\ "PurchaserDetails" \ "SDLT2Purchaser").size shouldBe 50
      (sdlt \\ "VendorDetails"    \ "SDLT2Vendor")   .size shouldBe 49
      (sdlt \\ "SDLT2Count").text.trim shouldBe "99"
      (sdlt \\ "SDLT3Count").text.trim shouldBe "98"
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-bound-max.xml") }
  }

  "Mapper consideration FormCode mapping" should {

    "map each consideration flag to its FormCode" in {
      val cases: Seq[(Transaction => Transaction, String)] = Seq(
        (_.copy(considerationCash = Some("YES")), "30"),
        (_.copy(considerationDebt = Some("YES")), "31"),
        (_.copy(considerationBuild = Some("YES")), "32"),
        (_.copy(considerationEmploy = Some("YES")), "33"),
        (_.copy(considerationOther = Some("YES")), "34"),
        (_.copy(considerationSharesQTD = Some("YES")), "35"),
        (_.copy(considerationSharesUNQTD = Some("YES")), "36"),
        (_.copy(considerationLand = Some("YES")), "37"),
        (_.copy(considerationServices = Some("YES")), "38"),
        (_.copy(considerationContingent = Some("YES")), "39")
      )
      cases.foreach { (setFlag, code) =>
        val sdlt = considerationReturn(setFlag)
        (sdlt \\ "Consideration" \ "FormCode").map(_.text.trim) shouldBe Seq(code)
      }
    }

    "emit at most 4 FormCode elements, keeping the first 4 in lookup order" in {
      val sdlt = considerationReturn(_.copy(
        considerationCash = Some("YES"),
        considerationDebt = Some("YES"),
        considerationBuild = Some("YES"),
        considerationEmploy = Some("YES"),
        considerationOther = Some("YES")
      ))
      (sdlt \\ "Consideration" \ "FormCode").map(_.text.trim) shouldBe Seq("30", "31", "32", "33")
    }

    "order by lookup declaration, not by which flags were set" in {
      val sdlt = considerationReturn(_.copy(
        considerationContingent = Some("YES"),
        considerationCash = Some("YES")
      ))
      (sdlt \\ "Consideration" \ "FormCode").map(_.text.trim) shouldBe Seq("30", "39")
    }

    "mirror the TODO's documented envelope shape (cash/debt/employment/contingent -> 30,31,33,39)" in {
      val sdlt = considerationReturn(_.copy(
        considerationCash = Some("YES"),
        considerationDebt = Some("YES"),
        considerationEmploy = Some("YES"),
        considerationContingent = Some("YES")
      ))
      (sdlt \\ "Consideration" \ "FormCode").map(_.text.trim) shouldBe Seq("30", "31", "33", "39")
    }

    "emit only truthy flags (skip NO / blank / unset)" in {
      val sdlt = considerationReturn(_.copy(
        considerationCash = Some("YES"),
        considerationDebt = Some("NO"),
        considerationBuild = Some(""),
        considerationEmploy = None
      ))
      (sdlt \\ "Consideration" \ "FormCode").map(_.text.trim) shouldBe Seq("30")
    }

    "render FormCode after TotalConsideration, inside the Consideration block" in {
      val sdlt = considerationReturn(_.copy(considerationBuild = Some("YES")))
      val kids = (sdlt \\ "Consideration").head.child.collect { case e: scala.xml.Elem => e.label }
      kids.indexOf("FormCode") should be > kids.indexOf("TotalConsideration")
      (sdlt \\ "Consideration" \ "FormCode").map(_.text.trim) shouldBe Seq("32")
    }

    "emit no FormCode when no consideration flags are set" in {
      val sdlt = considerationReturn(identity)
      (sdlt \\ "Consideration" \ "FormCode") shouldBe empty
    }

    "validate against the SDLT/6 schema with multiple FormCodes" in {
      val sdlt = considerationReturn(_.copy(
        considerationCash = Some("YES"),
        considerationBuild = Some("YES")
      ))
      assertValid(sdlt, "sdlt-consideration-formcodes.xml")
    }
  }

  "Mapper CertificateForEach mapping" should {

    "default to no when landCertForEachProp was never answered" in {
      val sdlt = certificateForEachReturn(None)
      (sdlt \\ "LandDetail" \ "CertificateForEach").map(_.text.trim) shouldBe Seq("no")
    }

    "still emit it when the return has no returnInfo" in {
      val sdlt = SdltReturnMapper.toSdltElement(freeholdReturn(1, 1, 1).copy(returnInfo = None))
      (sdlt \\ "LandDetail" \ "CertificateForEach").map(_.text.trim) shouldBe Seq("no")
    }

    "map YES and Y to yes" in {
      Seq("YES", "yes", "Y", "y").foreach { stored =>
        (certificateForEachReturn(Some(stored)) \\ "CertificateForEach").text.trim shouldBe "yes"
      }
    }

    "map NO and N to no" in {
      Seq("NO", "no", "N", "n").foreach { stored =>
        (certificateForEachReturn(Some(stored)) \\ "CertificateForEach").text.trim shouldBe "no"
      }
    }

    "not pass through junk the schema would reject" in {
      Seq("true", "1", "rubbish").foreach { stored =>
        (certificateForEachReturn(Some(stored)) \\ "CertificateForEach").text.trim shouldBe "no"
      }
    }

    "treat blank as no" in {
      (certificateForEachReturn(Some("  ")) \\ "CertificateForEach").text.trim shouldBe "no"
    }

    "put CertificateForEach straight after NumberOfProperties" in {
      val kids = (certificateForEachReturn(Some("YES")) \\ "LandDetail").head.child
        .collect { case e: scala.xml.Elem => e.label }
      kids.indexOf("CertificateForEach") shouldBe kids.indexOf("NumberOfProperties") + 1
    }

    "emit it for leases too" in {
      val leased = leaseReturn(1, 1, 1)
      val sdlt   = SdltReturnMapper.toSdltElement(
        leased.copy(returnInfo = leased.returnInfo.map(_.copy(landCertForEachProp = Some("YES"))))
      )
      (sdlt \\ "LandDetail" \ "CertificateForEach").map(_.text.trim) shouldBe Seq("yes")
    }

    "validate against the SDLT/6 schema" in {
      assertValid(certificateForEachReturn(None), "sdlt-certificate-for-each.xml")
    }
  }

  "Mapper on a return carrying both a vendor and a purchaser agent" should {

    val sdlt = SdltReturnMapper.toSdltElement(twoAgentReturn)

    "give VendorDetails the vendor's own agent, not the purchaser's" in {
      val agent = (sdlt \\ "VendorDetails" \ "AgentDetails").head
      (agent \ "Name").text.trim         shouldBe "Agent One"
      (agent \ "Reference").text.trim    shouldBe "5435435"
      (agent \ "Telephone").text.trim    shouldBe "8757647647"
      (agent \ "EmailAddress").text.trim shouldBe "agentone@xyz.com"
    }

    "leave the purchaser's agent under Purchaser" in {
      val agent = (sdlt \\ "Purchaser" \ "AgentDetails").head
      (agent \ "Name").text.trim      shouldBe "Johnson & Co."
      (agent \ "Reference").text.trim shouldBe "ABCD123456789"
      (agent \ "Telephone").text.trim shouldBe "1122334455"
    }

    "carry EmailAddress on the vendor's agent only" in {
      (sdlt \\ "Purchaser" \ "AgentDetails" \ "EmailAddress") shouldBe empty
    }

    "read AuthoriseAgent off the purchaser agent, the only one that carries it" in {
      (sdlt \\ "Purchaser" \ "AuthoriseAgent").text.trim shouldBe "yes"
    }

    "not depend on the order FormP happens to return the agents in" in {
      val swapped = twoAgentReturn.copy(returnAgent = Some(Seq(agentOne, johnsonAndCo)))
      val out     = SdltReturnMapper.toSdltElement(swapped)
      (out \\ "VendorDetails" \ "AgentDetails" \ "Name").text.trim shouldBe "Agent One"
      (out \\ "Purchaser" \ "AgentDetails" \ "Name").text.trim     shouldBe "Johnson & Co."
    }

    "validate against the SDLT/6 schema" in { assertValid(sdlt, "sdlt-two-agents.xml") }
  }

  "Mapper on a purchaser agent not authorised for correspondence" should {

    val sdlt = SdltReturnMapper.toSdltElement(
      twoAgentReturn.copy(returnAgent = Some(Seq(johnsonAndCo.copy(isAuthorised = Some("no")), agentOne)))
    )

    "write AuthoriseAgent = no" in {
      (sdlt \\ "Purchaser" \ "AuthoriseAgent").text.trim shouldBe "no"
    }

    "still file the agent, which is not the same as having no agent at all" in {
      (sdlt \\ "Purchaser" \ "AgentDetails" \ "Name").text.trim shouldBe "Johnson & Co."
    }
  }

  "Mapper when a stale agent row outlives the represented-by-agent answer" should {

    "drop the vendor's AgentDetails once the vendor answers no" in {
      val answeredNo = twoAgentReturn.copy(vendor = Some(Seq(buildVendor(1).copy(isRepresentedByAgent = Some("no")))))
      (SdltReturnMapper.toSdltElement(answeredNo) \\ "VendorDetails" \ "AgentDetails") shouldBe empty
    }

    "drop it too when the vendor never answered at all" in {
      val neverAnswered = twoAgentReturn.copy(vendor = Some(Seq(buildVendor(1))))
      (SdltReturnMapper.toSdltElement(neverAnswered) \\ "VendorDetails" \ "AgentDetails") shouldBe empty
    }

    "fall back to AuthoriseAgent = no once the purchaser answers no" in {
      val answeredNo = twoAgentReturn.copy(purchaser = Some(Seq(buildPurchaser(1).copy(isRepresentedByAgent = Some("no")))))
      val sdlt       = SdltReturnMapper.toSdltElement(answeredNo)
      (sdlt \\ "Purchaser" \ "AuthoriseAgent").text.trim shouldBe "no"
      (sdlt \\ "Purchaser" \ "AgentDetails")             shouldBe empty
    }

    "treat a missing purchaser answer the same as no" in {
      val neverAnswered = twoAgentReturn.copy(purchaser = Some(Seq(buildPurchaser(1))))
      val sdlt          = SdltReturnMapper.toSdltElement(neverAnswered)
      (sdlt \\ "Purchaser" \ "AuthoriseAgent").text.trim shouldBe "no"
      (sdlt \\ "Purchaser" \ "AgentDetails")             shouldBe empty
    }

    "leave the other party's agent alone" in {
      val answeredNo = twoAgentReturn.copy(vendor = Some(Seq(buildVendor(1).copy(isRepresentedByAgent = Some("no")))))
      (SdltReturnMapper.toSdltElement(answeredNo) \\ "Purchaser" \ "AgentDetails" \ "Name").text.trim shouldBe "Johnson & Co."
    }
  }

  "Mapper on an agent with details missing" should {

    "omit AuthoriseAgent rather than defaulting it to no" in {
      val noAnswer = twoAgentReturn.copy(returnAgent = Some(Seq(johnsonAndCo.copy(isAuthorised = None), agentOne)))
      (SdltReturnMapper.toSdltElement(noAnswer) \\ "Purchaser" \ "AuthoriseAgent") shouldBe empty
    }

    "omit AuthoriseAgent when the purchaser said yes but no agent was ever saved" in {
      val noPurchaserAgent = twoAgentReturn.copy(returnAgent = Some(Seq(agentOne)))
      val sdlt             = SdltReturnMapper.toSdltElement(noPurchaserAgent)
      (sdlt \\ "Purchaser" \ "AuthoriseAgent") shouldBe empty
      (sdlt \\ "Purchaser" \ "AgentDetails")   shouldBe empty
    }

    "fail schema validation without AuthoriseAgent, so the return cannot be filed at all" in {
      val noPurchaserAgent = twoAgentReturn.copy(returnAgent = Some(Seq(agentOne)))
      validator.validateSdlt(SdltReturnMapper.toSdltElement(noPurchaserAgent)).isLeft shouldBe true
    }

    "omit Name rather than writing an empty one" in {
      val nameless = twoAgentReturn.copy(returnAgent = Some(Seq(johnsonAndCo, agentOne.copy(name = None))))
      (SdltReturnMapper.toSdltElement(nameless) \\ "VendorDetails" \ "AgentDetails" \ "Name") shouldBe empty
    }
  }

  private def johnsonAndCo = ReturnAgent(
    agentType    = Some("PURCHASER"),
    name         = Some("Johnson & Co."),
    address1     = Some("Apartment 8, Devell House"),
    address2     = Some("11 Rusholme Place"),
    address3     = Some("Manchester"),
    postcode     = Some("M14 5TG"),
    phone        = Some("1122334455"),
    email        = Some("johnson@xyz.com"),
    reference    = Some("ABCD123456789"),
    isAuthorised = Some("yes")
  )

  private def agentOne = ReturnAgent(
    agentType = Some("VENDOR"),
    name      = Some("Agent One"),
    address1  = Some("Apartment 11, Devell House"),
    address2  = Some("11 Rusholme Place"),
    address3  = Some("Manchester"),
    postcode  = Some("M14 5TG"),
    phone     = Some("8757647647"),
    email     = Some("agentone@xyz.com"),
    reference = Some("5435435")
  )

  private def twoAgentReturn: FullReturn =
    freeholdReturn(1, 1, 1).copy(
      vendor      = Some(Seq(buildVendor(1).copy(isRepresentedByAgent = Some("yes")))),
      purchaser   = Some(Seq(buildPurchaser(1).copy(isRepresentedByAgent = Some("yes")))),
      returnAgent = Some(Seq(johnsonAndCo, agentOne))
    )

  private def certificateForEachReturn(stored: Option[String]): Elem =
    val base = freeholdReturn(1, 1, 1)
    SdltReturnMapper.toSdltElement(
      base.copy(returnInfo = base.returnInfo.map(_.copy(landCertForEachProp = stored)))
    )

  private def companyTypeReturn(setFlags: CompanyDetails => CompanyDetails): Elem =
    val base    = companyPurchaserFreehold()
    val cleared = clearCompanyTypes(base.companyDetails.getOrElse(CompanyDetails()))
    SdltReturnMapper.toSdltElement(base.copy(companyDetails = Some(setFlags(cleared))))

  private def clearCompanyTypes(cd: CompanyDetails): CompanyDetails =
    cd.copy(
      companyTypeBuilder = None, companyTypeSoletrader = None, companyTypeIndividual = None,
      companyTypePartnership = None, companyTypeLocalauth = None, companyTypeCentgov = None,
      companyTypePubliccorp = None, companyTypeProperty = None, companyTypeBank = None,
      companyTypeBuildsoc = None, companyTypeInsurance = None, companyTypePensionfund = None,
      companyTypeOtherfinancial = None, companyTypeOthercompany = None, companyTypeOthercharity = None
    )

  private def considerationReturn(setFlags: Transaction => Transaction): Elem =
    val tx = clearConsiderationFlags(baselineFreeholdTransaction)
    SdltReturnMapper.toSdltElement(freeholdReturn(1, 1, 1).copy(transaction = Some(setFlags(tx))))

  private def clearConsiderationFlags(tx: Transaction): Transaction =
    tx.copy(
      considerationCash = None, considerationDebt = None, considerationBuild = None,
      considerationEmploy = None, considerationOther = None, considerationSharesQTD = None,
      considerationSharesUNQTD = None, considerationLand = None, considerationServices = None,
      considerationContingent = None
    )

  private def assertValid(sdlt: Elem, dumpName: String): Assertion =
    dumpForDiagnostics(sdlt, dumpName)
    validator.validateSdlt(sdlt) match
      case Right(_) => succeed
      case Left(errors) =>
        fail(s"Schema validation failed (${errors.size} error(s)):\n${errors.take(20).mkString("\n")}" +
          (if errors.size > 20 then s"\n... and ${errors.size - 20} more. Full XML at target/test-output/$dumpName" else ""))

  private def dumpForDiagnostics(sdlt: Elem, name: String): Unit =
    val outDir = Paths.get("target/test-output")
    Files.createDirectories(outDir)
    val pretty = new PrettyPrinter(width = 200, step = 2).format(sdlt)
    Files.writeString(outDir.resolve(name), pretty)