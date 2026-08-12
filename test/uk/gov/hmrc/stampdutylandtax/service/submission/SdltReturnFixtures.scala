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

import models.filing.*

object SdltReturnFixtures:

  private def spellOut(i: Int): String = i match
    case 1  => "One"
    case 2  => "Two"
    case 3  => "Three"
    case 4  => "Four"
    case 5  => "Five"
    case 6  => "Six"
    case 7  => "Seven"
    case 8  => "Eight"
    case 9  => "Nine"
    case _  => "X"


  def buildVendor(i: Int, withForenames: Boolean = false): Vendor =
    Vendor(
      vendorID    = Some(s"V$i"),
      title       = if withForenames then Some("Mr") else None,
      forename1   = if withForenames then Some(s"Test ${spellOut(i)}") else None,
      name        = Some(s"Vendor$i Ltd"),
      houseNumber = Some(s"${(i % 99) + 1}"),
      address1    = Some(s"Vendor Road $i"),
      postcode    = Some("SE1 0NZ")
    )

  def buildPurchaser(i: Int, isCompany: Boolean = false, isTrustee: Boolean = false): Purchaser =
    Purchaser(
      purchaserID         = Some(s"P$i"),
      title               = if !isCompany then Some("Mr") else None,
      surname             = if !isCompany then Some(s"Purchaser$i") else None,
      companyName         = if isCompany  then Some(s"Buyer$i Ltd") else None,
      isCompany           = Some(if isCompany then "YES" else "NO"),
      isTrustee           = Some(if isTrustee then "YES" else "NO"),
      isConnectedToVendor = Some("NO"),
      houseNumber         = Some(s"${(i % 99) + 1}"),
      address1            = Some(s"Purchaser Road $i"),
      postcode            = Some("SE1 0NZ")
    )

  def buildLand(i: Int, propertyType: String = "02", interest: String = "FG"): Land =
    Land(
      landID                     = Some(s"L$i"),
      propertyType               = Some(propertyType),
      interestCreatedTransferred = Some(interest),
      houseNumber                = Some(s"${(i % 99) + 1}"),
      address1                   = Some(s"Land Road $i"),
      postcode                   = Some("SE1 0NZ"),
      localAuthorityNumber       = Some("1250"),
      willSendPlanByPost         = Some("no"),
      mineralRights              = Some("no")
    )

  val baselineFreeholdTransaction: Transaction = Transaction(
    transactionDescription     = Some("F"),
    effectiveDate              = Some("16/05/2026"),
    restrictionsAffectInterest = Some("no"),
    isLandExchanged            = Some("no"),
    isPursuantToPreviousOption = Some("no"),
    claimingRelief             = Some("no"),
    totalConsideration         = Some("481516.00"),
    isLinked                   = Some("no")
  )

  val baselineLeaseTransaction: Transaction =
    baselineFreeholdTransaction.copy(transactionDescription = Some("L"),
      totalConsiderationBusiness = Some("481516.00") )

  val baselineTaxCalculation: TaxCalculation = TaxCalculation(
    amountPaid      = Some("400.00"),
    includesPenalty = Some("no"),
    taxDue          = Some("60000.00")
  )

  val baselineLease: Lease = Lease(
    leaseType            = Some("M"),
    contractStartDate    = Some("01/06/2026"),
    contractEndDate      = Some("31/05/2046"),
    rentFreePeriod       = Some("06"),
    startingRent         = Some("24000.00"),
    startingRentEndDate  = Some("31/05/2027"),
    laterRentKnown       = Some("no"),
    VATAmount            = Some("4800.00"),
    totalPremiumPayable  = Some("50000.00"),
    netPresentValue      = Some("480000.00")
  )


  def freeholdReturn(vendors: Int, purchasers: Int, lands: Int): FullReturn =
    FullReturn(
      stornId           = Some("1142344344"),
      returnResourceRef = Some("R1"),
      returnInfo        = Some(ReturnInfo(returnID = Some("R1"))),
      purchaser         = Some((1 to purchasers).map(i => buildPurchaser(i))),
      vendor            = Some((1 to vendors).map(i => buildVendor(i))),
      land              = Some((1 to lands).map(i => buildLand(i))),
      transaction       = Some(baselineFreeholdTransaction),
      taxCalculation    = Some(baselineTaxCalculation)
    )

  def leaseReturn(vendors: Int, purchasers: Int, lands: Int): FullReturn =
    freeholdReturn(vendors, purchasers, lands).copy(
      transaction = Some(baselineLeaseTransaction),
      lease       = Some(baselineLease)
    )

  def sdlt4FreeholdReturn(vendors: Int, purchasers: Int, lands: Int): FullReturn =
    val sdlt4Tx = baselineFreeholdTransaction.copy(
      usedAsOffice              = Some("YES"),
      usedAsShop                = Some("YES"),
      postTransRulingApplied    = Some("YES"),
      postTransRulingFollowed   = Some("YES"),
      isDependantOnFutureEvent  = Some("YES"),
      agreedToDeferPayment      = Some("YES"),
      includesStock             = Some("YES"),
      totalConsiderationBusiness = Some("481516.00"),
      includesGoodwill          = Some("NO"),
      includesOther             = Some("NO"),
      includesChattel           = Some("YES")
    )
    freeholdReturn(vendors, purchasers, lands).copy(transaction = Some(sdlt4Tx))

  def sdlt4LeaseholdReturn(): FullReturn =
    val purchaser = buildPurchaser(1, true).copy(
      registrationNumber = Some("123456789"),
      placeOfRegistration = Some("England and Wales")
    )

    FullReturn(
      stornId = Some("1142344344"),
      returnResourceRef = Some("R1"),
      returnInfo = Some(ReturnInfo(returnID = Some("R1"))),
      transaction = Some(baselineLeaseTransaction),
      taxCalculation = Some(baselineTaxCalculation),
      purchaser = Some(Seq(purchaser)),
      vendor = Some(Seq(buildVendor(1, withForenames = true))),
      land = Some(Seq(buildLand(1))),
      lease = Some(baselineLease),
      companyDetails = Some(CompanyDetails(
        UTR = Some("1234567890"),
        VATReference = Some("GB123456789"),
        companyTypeBank = Some("YES"),
        companyTypePartnership = Some("YES")
      ))
    )

  def richFreeholdReturn(): FullReturn =
    val tx = baselineFreeholdTransaction.copy(
      restrictionsAffectInterest = Some("YES"),
      restrictionDetails         = Some("restrictive covenants in place"),
      contractDate               = Some("01/04/2026"),
      isLandExchanged            = Some("YES"),
      exchangedLandPostcode      = Some("M4 2AH"),
      exchangedLandHouseNumber   = Some("12"),
      exchangedLandAddress1      = Some("Exchanged Lane"),
      isPursuantToPreviousOption = Some("YES"),
      claimingRelief             = Some("YES"),
      reliefReason               = Some("27"),
      reliefSchemeNumber         = Some("SCH-2026-001"),
      reliefAmount               = Some("15000.00"),
      isLinked                   = Some("YES"),
      totalConsiderationLinked   = Some("576543.00"),
      considerationVAT           = Some("96303.00")
    )

    val primaryPurchaser = buildPurchaser(1).copy(
      nino                 = Some("AB686456D"),
      dateOfBirth          = Some("12/08/1980"),
      phone                = Some("01234567890"),
      isConnectedToVendor  = Some("YES"),
      isTrustee            = Some("YES"),
      isRepresentedByAgent = Some("YES")
    )

    val agent = ReturnAgent(
      returnAgentID     = Some("A1"),
      agentType   = Some("PURCHASER"),
      name        = Some("Agent Smith"),
      houseNumber = Some("2"),
      address1    = Some("Vendor Agent Gardens"),
      postcode    = Some("M4 2AH"),
      email       = Some("agent@smith.test"),
      reference   = Some("M852147/JL/KL"),
      phone       = Some("01952 123456"),
      isAuthorised = Some("YES")
    )

    FullReturn(
      stornId           = Some("1142344344"),
      returnResourceRef = Some("R1"),
      returnInfo        = Some(ReturnInfo(returnID = Some("R1"))),
      transaction       = Some(tx),
      taxCalculation    = Some(baselineTaxCalculation),
      purchaser         = Some(Seq(primaryPurchaser, buildPurchaser(2, isCompany = true))),
      vendor            = Some(Seq(buildVendor(1, withForenames = true))),
      land              = Some(Seq(buildLand(1))),
      returnAgent       = Some(Seq(agent)),
      residency         = Some(Residency(
        isNonUkResidents = Some("NO"),
        isCloseCompany   = Some("NO"),
        isCrownRelief    = Some("NO")
      )),
      companyDetails    = Some(CompanyDetails(
        UTR = Some("1234567890"),
        VATReference = Some("GB123456789")
      ))
    )
 
  def leaseReturnTriggered(): FullReturn =
    val triggeredLeaseTx = baselineLeaseTransaction.copy(
      postTransRulingApplied   = Some("YES"),
      postTransRulingFollowed  = Some("YES"),
      isDependantOnFutureEvent = Some("YES"),
      agreedToDeferPayment     = Some("YES"),
      includesStock            = Some("YES"),
      includesGoodwill         = Some("NO"),
      includesOther            = Some("NO"),
      includesChattel          = Some("YES")
    )
    leaseReturn(vendors = 1, purchasers = 1, lands = 3).copy(
      transaction = Some(triggeredLeaseTx)
    )
  
  def companyPurchaserFreehold(): FullReturn =
    val triggeredTx = baselineFreeholdTransaction.copy(
      postTransRulingApplied   = Some("YES"),
      postTransRulingFollowed  = Some("YES"),
      isDependantOnFutureEvent = Some("YES"),
      agreedToDeferPayment     = Some("YES"),
      includesStock            = Some("YES"),
      includesGoodwill         = Some("YES"),
      includesOther            = Some("YES"),
      includesChattel          = Some("YES")
    )

    val companyPurchaser = buildPurchaser(1, isCompany = true).copy(
      registrationNumber  = Some("CR-123456"),
      placeOfRegistration = Some("England and Wales"),
      isConnectedToVendor = Some("NO"),
      isTrustee           = Some("NO")
    )

    FullReturn(
      stornId           = Some("1142344344"),
      returnResourceRef = Some("R1"),
      returnInfo        = Some(ReturnInfo(returnID = Some("R1"))),
      transaction       = Some(triggeredTx),
      taxCalculation    = Some(baselineTaxCalculation),
      purchaser         = Some(Seq(companyPurchaser)),
      vendor            = Some(Seq(buildVendor(1))),
      land              = Some(Seq(buildLand(1))),
      companyDetails    = Some(CompanyDetails(
        UTR          = Some("1234567890"),
        VATReference = Some("GB123456789")
      ))
    )
  
  def individualPurchaserLease(): FullReturn =
    val individualPurchaser = buildPurchaser(1, isCompany = false).copy(
      nino        = Some("AB686456D"),
      dateOfBirth = Some("15/03/1985"),
      phone       = Some("07123456789"),
      forename1   = Some("Scott")
    )

    leaseReturn(vendors = 1, purchasers = 1, lands = 2).copy(
      purchaser = Some(Seq(individualPurchaser))
    )