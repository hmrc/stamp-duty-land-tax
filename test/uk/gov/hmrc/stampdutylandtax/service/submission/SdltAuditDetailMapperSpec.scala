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

package uk.gov.hmrc.stampdutylandtax.service.submission

import models.filing.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import service.submission.*
import SdltReturnFixtures.*

class SdltAuditDetailMapperSpec extends AnyWordSpec with Matchers:

  private val mapper = new SdltAuditDetailMapperImpl()
  
  private def td(tx: Transaction): JsLookupResult =
    mapper.submissionDetail(freeholdReturn(1, 1, 1).copy(transaction = Some(tx))) \ "transactionDetails"

  private val baseTx = baselineFreeholdTransaction
  
  "SdltAuditDetailMapper transactionDetails landExchangedAddress" should {

    "emit the exchanged-land address when the land is exchanged" in {
      val tx = baseTx.copy(
        isLandExchanged          = Some("YES"),
        exchangedLandHouseNumber = Some("12"),
        exchangedLandAddress1    = Some("Exchange Street"),
        exchangedLandPostcode    = Some("M4 2AH")
      )
      val addr = td(tx) \ "landExchangedAddress"
      (addr \ "addressLine1").as[String] shouldBe "12"
      (addr \ "addressLine2").as[String] shouldBe "Exchange Street"
      (addr \ "postCode").as[String]     shouldBe "M4 2AH"
    }

    "omit the exchanged-land address when the land is not exchanged" in {
      val tx = baseTx.copy(isLandExchanged = Some("NO"), exchangedLandPostcode = Some("M4 2AH"))
      (td(tx) \ "landExchangedAddress").toOption shouldBe None
    }
  }

  "SdltAuditDetailMapper transactionDetails boolean flags" should {

    "encode postTransactionRuling as true for truthy inputs" in {
      (td(baseTx.copy(postTransRulingApplied = Some("YES"))) \ "postTransactionRuling").as[Boolean] shouldBe true
      (td(baseTx.copy(postTransRulingApplied = Some("1")))   \ "postTransactionRuling").as[Boolean] shouldBe true
      (td(baseTx.copy(postTransRulingApplied = Some("true")))\ "postTransactionRuling").as[Boolean] shouldBe true
    }

    "encode postTransactionRuling as false for non-truthy inputs" in {
      (td(baseTx.copy(postTransRulingApplied = Some("no"))) \ "postTransactionRuling").as[Boolean] shouldBe false
    }

    "pass postTransactionRulingFollowed through verbatim (no yes/no normalisation, unlike the XML mapper)" in {
      (td(baseTx.copy(postTransRulingFollowed = Some("YES"))) \ "postTransactionRulingFollowed").as[String] shouldBe "YES"
      (td(baseTx.copy(postTransRulingFollowed = Some("RulingNotReceived"))) \ "postTransactionRulingFollowed").as[String] shouldBe "RulingNotReceived"
    }
  }

  "SdltAuditDetailMapper transactionDetails dates" should {

    "pass effectiveDate and contractDate through verbatim, without ISO normalisation" in {
      val tx = baseTx.copy(effectiveDate = Some("15/01/2026"), contractDate = Some("01/04/2026"))
      (td(tx) \ "effectiveDate").as[String] shouldBe "15/01/2026"
      (td(tx) \ "contractDate").as[String]  shouldBe "01/04/2026"
    }
  }

  "SdltAuditDetailMapper transactionDetails propertyUse" should {

    "list only the truthy property uses" in {
      val tx = baseTx.copy(
        usedAsOffice     = Some("YES"),
        usedAsShop       = Some("YES"),
        usedAsHotel      = Some("NO"),
        usedAsWarehouse  = None,
        usedAsFactory    = Some("false"),
        usedAsIndustrial = None,
        usedAsOther      = None
      )
      val uses = (td(tx) \ "propertyUse").as[Seq[String]]
      uses should contain allOf ("office", "shop")
      uses should contain noneOf ("hotel", "warehouse", "factory")
    }

    "omit propertyUse entirely when nothing is truthy" in {
      val tx = baseTx.copy(
        usedAsOffice = Some("NO"), usedAsShop = Some("NO"), usedAsHotel = Some("NO"),
        usedAsWarehouse = Some("NO"), usedAsFactory = Some("NO"),
        usedAsIndustrial = Some("NO"), usedAsOther = Some("NO")
      )
      (td(tx) \ "propertyUse").toOption shouldBe None
    }
  }

  "SdltAuditDetailMapper transactionDetails reliefDetails" should {

    "emit relief details when claiming relief" in {
      val tx = baseTx.copy(
        claimingRelief     = Some("YES"),
        reliefReason       = Some("27"),
        reliefSchemeNumber = Some("SCH-2026-001"),
        reliefAmount       = Some("15000.00")
      )
      val rd = td(tx) \ "reliefDetails"
      (rd \ "reason").as[String]                           shouldBe "27"
      (rd \ "constructionIndustrySchemeNumber").as[String] shouldBe "SCH-2026-001"
      (rd \ "chargeableAmount").as[BigDecimal]             shouldBe BigDecimal("15000.00")
    }

    "omit relief details when not claiming relief" in {
      val tx = baseTx.copy(claimingRelief = Some("NO"), reliefReason = Some("27"))
      (td(tx) \ "reliefDetails").toOption shouldBe None
    }
  }

  "SdltAuditDetailMapper transactionDetails considerationDetails" should {

    "emit consideration details when a total consideration is present" in {
      val tx = baseTx.copy(totalConsideration = Some("100000.00"), considerationVAT = Some("20000.00"))
      val cd = td(tx) \ "considerationDetails"
      (cd \ "totalConsideration").as[BigDecimal] shouldBe BigDecimal("100000.00")
      (cd \ "vatAmount").as[BigDecimal]          shouldBe BigDecimal("20000.00")
    }

    "omit consideration details when there is no total consideration, even if VAT is set" in {
      val tx = baseTx.copy(totalConsideration = None, considerationVAT = Some("20000.00"))
      (td(tx) \ "considerationDetails").toOption shouldBe None
    }

    "cap formOfConsideration at four entries, in schema order, excluding zero amounts" in {
      val tx = baseTx.copy(
        totalConsideration       = Some("100000.00"),
        considerationCash        = Some("1.00"),
        considerationDebt        = Some("2.00"),
        considerationBuild       = Some("3.00"),
        considerationEmploy      = Some("4.00"),
        considerationOther       = Some("5.00"),
        considerationLand        = Some("0.00"),
        considerationServices    = Some("0.00"),
        considerationSharesQTD   = Some("0.00"),
        considerationSharesUNQTD = Some("0.00"),
        considerationContingent  = Some("0.00")
      )
      val forms = (td(tx) \ "considerationDetails" \ "formOfConsideration").as[Seq[String]]
      forms shouldBe Seq("30 - Cash", "31 - Debt", "32 - Building Works", "33 - Employment")
    }
  }

  "SdltAuditDetailMapper transactionDetails businessSaleDetails" should {

    "emit business sale details, listing only the included items, when part of a business sale" in {
      val tx = baseTx.copy(
        isPartOfSaleOfBusiness     = Some("YES"),
        includesStock              = Some("YES"),
        includesChattel            = Some("YES"),
        includesGoodwill           = Some("NO"),
        includesOther              = None,
        totalConsiderationBusiness = Some("5000.00")
      )
      val bsd = td(tx) \ "businessSaleDetails"
      (bsd \ "totalConsiderationOfItems").as[BigDecimal] shouldBe BigDecimal("5000.00")
      val items = (bsd \ "itemsIncludedInSale").as[Seq[String]]
      items should contain allOf ("stock", "chattels and moveables")
      items should contain noneOf ("goodwill", "other")
    }

    "omit business sale details when not part of a business sale" in {
      val tx = baseTx.copy(isPartOfSaleOfBusiness = Some("NO"), includesStock = Some("YES"))
      (td(tx) \ "businessSaleDetails").toOption shouldBe None
    }
  }
  
  "SdltAuditDetailMapper on a rich freehold return" should {

    val rich = mapper.submissionDetail(richFreeholdReturn())

    "map the distinctive transaction fields through from source" in {
      (rich \ "transactionDetails" \ "restrictionsAffectingTransaction").as[String] shouldBe "restrictive covenants in place"
      (rich \ "transactionDetails" \ "contractDate").as[String]                     shouldBe "01/04/2026"
      (rich \ "transactionDetails" \ "landExchangedAddress" \ "postCode").as[String] shouldBe "M4 2AH"
      (rich \ "transactionDetails" \ "linkedTransactionTotalConsideration").as[BigDecimal] shouldBe BigDecimal("576543.00")
      (rich \ "transactionDetails" \ "considerationDetails" \ "vatAmount").as[BigDecimal]  shouldBe BigDecimal("96303.00")
      (rich \ "transactionDetails" \ "reliefDetails" \ "constructionIndustrySchemeNumber").as[String] shouldBe "SCH-2026-001"
    }

    "emit purchaserDetails as an array, applying first-purchaser-only fields to index 0 only" in {
      val purchasers = (rich \ "purchaserDetails").as[Seq[JsValue]]
      purchasers.size shouldBe 2

      (purchasers.head \ "nino").as[String]            shouldBe "AB686456D"
      (purchasers.head \ "dateOfBirth").as[String]     shouldBe "12/08/1980"
      (purchasers.head \ "phoneNumber").as[String]     shouldBe "01234567890"
      (purchasers.head \ "actingAsTrustee").as[Boolean]   shouldBe true
      (purchasers.head \ "connectedToVendor").as[Boolean] shouldBe true

      (purchasers(1) \ "companyOrSurname").as[String]         shouldBe "Buyer2 Ltd"
      (purchasers(1) \ "nino").toOption                       shouldBe None
      (purchasers(1) \ "dateOfBirth").toOption                shouldBe None
      (purchasers(1) \ "purchaserCompanyDetails").toOption    shouldBe None
    }

    "emit residency details" in {
      (rich \ "residencyDetails" \ "residencyStatus").as[Boolean]     shouldBe false
      (rich \ "residencyDetails" \ "closeCompanyStatus").as[Boolean]  shouldBe false
      (rich \ "residencyDetails" \ "crownEmployeeRelief").as[Boolean] shouldBe false
    }
  }

  "SdltAuditDetailMapper on a company-purchaser freehold" should {

    val company = mapper.submissionDetail(companyPurchaserFreehold())

    "attach the company details to the first purchaser" in {
      val pcd = (company \ "purchaserDetails").as[Seq[JsValue]].head \ "purchaserCompanyDetails"
      (pcd \ "taxReferenceNumber").as[String]      shouldBe "1234567890"
      (pcd \ "companyRegisteredNumber").as[String] shouldBe "CR-123456"
      (pcd \ "placeOfRegistration").as[String]     shouldBe "England and Wales"
    }

    "omit NINO and DateOfBirth for a company purchaser" in {
      val first = (company \ "purchaserDetails").as[Seq[JsValue]].head
      (first \ "nino").toOption        shouldBe None
      (first \ "dateOfBirth").toOption shouldBe None
    }
  }

  "SdltAuditDetailMapper on a lease return" should {

    val lease = mapper.submissionDetail(leaseReturn(1, 1, 1))

    "emit leaseDetails with the source dates and amounts" in {
      (lease \ "leaseDetails" \ "leaseStartDate").as[String]    shouldBe "01/06/2026"
      (lease \ "leaseDetails" \ "premiumPaid").as[BigDecimal]   shouldBe BigDecimal("50000.00")
      (lease \ "leaseDetails" \ "netPresentValue").as[BigDecimal] shouldBe BigDecimal("480000.00")
    }
  }

  "SdltAuditDetailMapper prune behaviour" should {

    val freehold = mapper.submissionDetail(freeholdReturn(1, 1, 1))

    "omit the leaseDetails section for a non-lease return" in {
      (freehold \ "leaseDetails").toOption shouldBe None
    }

    "emit one entry per party in the vendor and purchaser arrays" in {
      (freehold \ "vendorDetails").as[Seq[JsValue]].size    shouldBe 1
      (freehold \ "purchaserDetails").as[Seq[JsValue]].size shouldBe 1
    }
  }