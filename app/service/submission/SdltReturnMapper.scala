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

package service.submission

import service.submission.Normalise.*

import scala.xml.{Elem, NodeSeq}

import models.filing._

object SdltReturnMapper:

  private val SdltNs = "http://www.govtalk.gov.uk/taxation/SDLT/6"

  private val ZeroMoney = "0.00"
  
  def toSdltElement(fullReturn: FullReturn): Elem =
    transactionDescription(fullReturn) match
      case "F" => buildFreehold(fullReturn)
      case "L" => buildLease(fullReturn)
      case "O" => buildOther(fullReturn)
      case "A" => buildAssignment(fullReturn)
      case other =>
        throw new IllegalArgumentException(s"Unknown TransactionDescription '$other'; expected one of F/L/O/A")
  
  private def buildFreehold(fr: FullReturn): Elem =
    <SDLT xmlns={SdltNs}>
      {transactionDetails(fr, leased = false)}
      {landDetail(fr, leased = false)}
      {vendorDetails(fr)}
      {purchaserDetails(fr)}
      {supplementarySections(fr, leased = false)}
      {sdlt4Sections(fr, leased = false)}
    </SDLT>

  private def buildLease(fr: FullReturn): Elem =
    <SDLT xmlns={SdltNs}>
      {transactionDetails(fr, leased = true)}
      {landDetail(fr, leased = true)}
      {vendorDetails(fr)}
      {purchaserDetails(fr)}
      {supplementarySections(fr, leased = true)}
      {sdlt4Sections(fr, leased = true)}
    </SDLT>

  private def buildOther(fr: FullReturn): Elem = buildFreehold(fr)

  private def buildAssignment(fr: FullReturn): Elem = buildLease(fr)
  
  private def transactionDetails(fr: FullReturn, leased: Boolean): Elem =
    val tx = fr.transaction.getOrElse(emptyTransaction)
    <TransactionDetails>
      <TransactionDescription>{transactionDescription(fr)}</TransactionDescription>
      <EffectiveDate>{isoDate(tx.effectiveDate).getOrElse("")}</EffectiveDate>
      {restrictionsAffecting(tx)}
      {contractDate(tx)}
      {landExchanged(tx)}
      <PursuantToOption>{yesNo(tx.isPursuantToPreviousOption).getOrElse("no")}</PursuantToOption>
      {taxCalculation(fr, leased)}
    </TransactionDetails>

  private def restrictionsAffecting(tx: Transaction): Elem =
    val apply = yesNo(tx.restrictionsAffectInterest).getOrElse("no")
    val details: NodeSeq = nonBlank(tx.restrictionDetails)
      .map(d => <Details>{d}</Details>: NodeSeq).getOrElse(NodeSeq.Empty)
    <RestrictionsAffecting Apply={apply}>{details}</RestrictionsAffecting>

  private def contractDate(tx: Transaction): NodeSeq =
    isoDate(tx.contractDate).map(d => <ContractDate>{d}</ContractDate>: NodeSeq).getOrElse(NodeSeq.Empty)

  private def landExchanged(tx: Transaction): Elem =
    val exchanged = yesNo(tx.isLandExchanged).getOrElse("no")
    val address: NodeSeq =
      if exchanged == "yes" then exchangedAddress(tx).map(a => a: NodeSeq).getOrElse(NodeSeq.Empty)
      else NodeSeq.Empty
    <LandExchanged Exchanged={exchanged}>{address}</LandExchanged>

  private def exchangedAddress(tx: Transaction): Option[Elem] =
    val lines = Seq(tx.exchangedLandAddress1, tx.exchangedLandAddress2, tx.exchangedLandAddress3, tx.exchangedLandAddress4).flatMap(nonBlank)
    if lines.isEmpty && isBlank(tx.exchangedLandPostcode) && isBlank(tx.exchangedLandHouseNumber) then None
    else Some(<Address>{addressBody(tx.exchangedLandPostcode, tx.exchangedLandHouseNumber, lines)}</Address>)
  
  private def taxCalculation(fr: FullReturn, leased: Boolean): Elem =
    val tx          = fr.transaction.getOrElse(emptyTransaction)
    val taxCalc     = fr.taxCalculation
    val amountPaid  = taxCalc.flatMap(t => moneyFromString(t.amountPaid)).getOrElse(ZeroMoney)
    val includesPen = taxCalc.flatMap(t => yesNo(t.includesPenalty)).getOrElse("no")
    <TaxCalculation>
      {claimingRelief(tx)}
      {if leased then NodeSeq.Empty else consideration(tx)}
      {linkedTransaction(tx)}
      <TotalDue>{taxCalc.flatMap(t => moneyFromString(t.taxDue)).getOrElse(ZeroMoney)}</TotalDue>
      <AmountPaid IncludesPenalties={includesPen}>{amountPaid}</AmountPaid>
    </TaxCalculation>

  private def claimingRelief(tx: Transaction): Elem =
    val claiming = yesNo(tx.claimingRelief).getOrElse("no")
    val children: NodeSeq =
      nonBlank(tx.reliefReason).map(r => <Reason>{r}</Reason>: NodeSeq).getOrElse(NodeSeq.Empty) ++
        nonBlank(tx.reliefSchemeNumber).map(s => <SchemeNumber>{s}</SchemeNumber>: NodeSeq).getOrElse(NodeSeq.Empty) ++
        tx.reliefAmount.flatMap(toMoney).map(a => <ChargeableAmount>
          {money(a)}
        </ChargeableAmount>: NodeSeq).getOrElse(NodeSeq.Empty)
    <ClaimingRelief Claiming={claiming}>{children}</ClaimingRelief>

  private def consideration(tx: Transaction): Elem =
    val total = tx.totalConsideration.flatMap(toMoney).map(bd => money(bd)).getOrElse(ZeroMoney)
    val vat = tx.considerationVAT.flatMap(toMoney)
      .map(v => <VATamount>
        {money(v)}
      </VATamount>: NodeSeq).getOrElse(NodeSeq.Empty)
    <Consideration>
      <TotalConsideration>
        {total}
      </TotalConsideration>{vat}{considerationFormCodes(tx)}
    </Consideration>

  private val considerationFormCodeLookup: Seq[(Transaction => Option[String], String)] = Seq(
    (_.considerationCash, "30"),
    (_.considerationDebt, "31"),
    (_.considerationBuild, "32"),
    (_.considerationEmploy, "33"),
    (_.considerationOther, "34"),
    (_.considerationSharesQTD, "35"),
    (_.considerationSharesUNQTD, "36"),
    (_.considerationLand, "37"),
    (_.considerationServices, "38"),
    (_.considerationContingent, "39")
  )

  private def considerationFormCodes(tx: Transaction): NodeSeq =
    considerationFormCodeLookup
      .collect { case (accessor, code) if isYes(accessor(tx)) => code }
      .take(4)
      .foldLeft(NodeSeq.Empty)((acc, code) => acc ++ <FormCode>
        {code}
      </FormCode>)

  private def linkedTransaction(tx: Transaction): Elem =
    val isLinked = yesNo(tx.isLinked).getOrElse("no")
    val total: NodeSeq = tx.totalConsiderationLinked
      .flatMap(toMoney).map(t => <LinkedTotal>{money(t)}</LinkedTotal>: NodeSeq).getOrElse(NodeSeq.Empty)
    <LinkedTransaction IsLinked={isLinked}>{total}</LinkedTransaction>
  
  private def landDetail(fr: FullReturn, leased: Boolean): Elem =
    val lands           = fr.land.getOrElse(Nil)
    val first           = lands.headOption.getOrElse(emptyLand)
    val additionalLands = lands.drop(1)
    val certificateForEach: String =
      fr.returnInfo.flatMap(_.landCertForEachProp).flatMap(s => yesNo(Option(s)))
        .filter(v => v == "yes" || v == "no").getOrElse("no")
    <LandDetail>
      <NumberOfProperties>{lands.size.max(1)}</NumberOfProperties>
      <CertificateForEach>{certificateForEach}</CertificateForEach>
      {if leased then landDetailLeaseDetails(fr) else NodeSeq.Empty}
      {property(first)}
      {if leased then NodeSeq.Empty else additionalLands.flatMap(additionalProperty)}
    </LandDetail>

  private def landDetailLeaseDetails(fr: FullReturn): NodeSeq =
    fr.lease.map { lease =>
      val taxCalc          = fr.taxCalculation
      val totalPremiumTax  = taxCalc.flatMap(t => moneyFromString(t.taxDuePremium)).getOrElse(ZeroMoney)
      val totalNpvTax      = taxCalc.flatMap(t => moneyFromString(t.taxDueNPV)).getOrElse(ZeroMoney)
      <LeaseDetails>
        {nonBlank(lease.leaseType).map(t => <LeaseType>{t}</LeaseType>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {isoDate(lease.contractStartDate).map(d => <StartDate>{d}</StartDate>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {isoDate(lease.contractEndDate).map(d => <EndDate>{d}</EndDate>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {nonBlank(lease.rentFreePeriod).map(p => <RentFreePeriod>{p}</RentFreePeriod>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {startingRent(lease, zeroAmounts = false)}
        {moneyFromString(lease.totalPremiumPayable).map(v => <PremiumPaid>{v}</PremiumPaid>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {moneyFromString(lease.netPresentValue).map(v => <NetPresentValue>{v}</NetPresentValue>: NodeSeq).getOrElse(NodeSeq.Empty)}
        <TotalPremiumTax>{totalPremiumTax}</TotalPremiumTax>
        <TotalNPVtax>{totalNpvTax}</TotalNPVtax>
      </LeaseDetails>: NodeSeq
    }.getOrElse(NodeSeq.Empty)
  
  private def startingRent(lease: Lease, zeroAmounts: Boolean): NodeSeq =
    val rent  =
      if zeroAmounts then <RentPayable>{ZeroMoney}</RentPayable>: NodeSeq
      else moneyFromString(lease.startingRent).map(v => <RentPayable>{v}</RentPayable>: NodeSeq).getOrElse(NodeSeq.Empty)
    val end   = isoDate(lease.startingRentEndDate).map(d => <EndDate>{d}</EndDate>: NodeSeq).getOrElse(NodeSeq.Empty)
    val later = yesNo(lease.laterRentKnown).map(v => <LaterRentKnown>{v}</LaterRentKnown>: NodeSeq).getOrElse(NodeSeq.Empty)
    val vat   =
      if zeroAmounts then <VATamount>{ZeroMoney}</VATamount>: NodeSeq
      else moneyFromString(lease.VATAmount).map(v => <VATamount>{v}</VATamount>: NodeSeq).getOrElse(NodeSeq.Empty)
    if rent.isEmpty && end.isEmpty && later.isEmpty && vat.isEmpty then NodeSeq.Empty
    else <StartingRent>{rent ++ end ++ later ++ vat}</StartingRent>

  private def property(land: Land): Elem =
    <Property>
      <PropertyType>{land.propertyType.getOrElse("01")}</PropertyType>
      {addressOfLand(land)}
      <LAnumber>{land.localAuthorityNumber.getOrElse("0000")}</LAnumber>
      {nonBlank(land.titleNumber).map(t => <TitleNumber>{t}</TitleNumber>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(land.NLPGUPRN).map(n => <NLPGUPRN>{n}</NLPGUPRN>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {landArea(land)}
      <PlanSubmitted>{yesNo(land.willSendPlanByPost).getOrElse("no")}</PlanSubmitted>
      <InterestTransfered>{land.interestCreatedTransferred.map(_.trim.take(2)).getOrElse("OT")}</InterestTransfered>
    </Property>

  private def additionalProperty(land: Land): NodeSeq =
    <AdditionalProperty>
      <PropertyType>{land.propertyType.getOrElse("01")}</PropertyType>
      <LAnumber>{land.localAuthorityNumber.getOrElse("0000")}</LAnumber>
      {nonBlank(land.titleNumber).map(t => <TitleNumber>{t}</TitleNumber>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(land.NLPGUPRN).map(n => <NLPGUPRN>{n}</NLPGUPRN>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {longAddressOfLand(land)}
      {landArea(land)}
      <PlanSubmitted>{yesNo(land.willSendPlanByPost).getOrElse("no")}</PlanSubmitted>
      <InterestTransfered>{land.interestCreatedTransferred.map(_.trim.take(2)).getOrElse("OT")}</InterestTransfered>
      {yesNo(land.mineralRights).map(v => <MineralRights>{v}</MineralRights>: NodeSeq).getOrElse(NodeSeq.Empty)}
    </AdditionalProperty>

  private def addressOfLand(land: Land): Elem =
    val lines = Seq(land.address1, land.address2, land.address3, land.address4).flatMap(nonBlank)
    <AddressOfLand AddressExtended="no">{addressBody(land.postcode, land.houseNumber, lines)}</AddressOfLand>

  private def longAddressOfLand(land: Land): Elem =
    val lines = Seq(land.address1, land.address2, land.address3, land.address4).flatMap(nonBlank)
    <AddressOfLand>{addressBody(land.postcode, land.houseNumber, lines)}</AddressOfLand>

  private def landArea(land: Land): NodeSeq =
    nonBlank(land.landArea).map { area =>
      val unit = nonBlank(land.areaUnit).getOrElse("SquareMetres")
      <LandArea Unit={unit}>{area}</LandArea>: NodeSeq
    }.getOrElse(NodeSeq.Empty)
  
  private def vendorDetails(fr: FullReturn): Elem =
    val vendors     = fr.vendor.getOrElse(Nil)
    val primary     = vendors.headOption.getOrElse(emptyVendor)
    val additional  = vendors.lift(1)
    val sdlt2       = vendors.drop(2)
    val vendorAgent = if isYes(primary.isRepresentedByAgent) then agentOf(fr, "VENDOR") else None
    <VendorDetails>
      <NumberOfVendors>{vendors.size.max(1)}</NumberOfVendors>
      {primaryVendor(primary)}
      {vendorAgent.map(vendorAgentDetails).getOrElse(NodeSeq.Empty)}
      {additional.map(additionalVendor).getOrElse(NodeSeq.Empty)}
      {sdlt2.flatMap(sdlt2Vendor)}
    </VendorDetails>

  private def primaryVendor(v: Vendor): Elem =
    <Vendor>
      {vendorName(v)}
      {vendorAddress(v)}
    </Vendor>

  private def additionalVendor(v: Vendor): NodeSeq =
    <AdditionalVendor>
      {vendorName(v)}
      {vendorAddress(v)}
    </AdditionalVendor>

  private def sdlt2Vendor(v: Vendor): NodeSeq =
    <SDLT2Vendor>
      {vendorName(v)}
      {vendorAddress(v)}
    </SDLT2Vendor>

  private def vendorName(v: Vendor): Elem =
    <Name>
      {nonBlank(v.title).map(t => <Title>{t}</Title>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(v.forename1).map(f => <Forename>{f}</Forename>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(v.forename2).map(f => <Forename>{f}</Forename>: NodeSeq).getOrElse(NodeSeq.Empty)}
      <CompanyOrSurname>{v.name.getOrElse("")}</CompanyOrSurname>
    </Name>

  private def vendorAddress(v: Vendor): Elem =
    val lines = Seq(v.address1, v.address2, v.address3, v.address4).flatMap(nonBlank)
    <Address>{addressBody(v.postcode, v.houseNumber, lines)}</Address>

  private def vendorAgentDetails(agent: ReturnAgent): NodeSeq =
    <AgentDetails>
      {nonBlank(agent.name).map(n => <Name>{n}</Name>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {agentAddress(agent)}
      {nonBlank(agent.email).map(e => <EmailAddress>{e}</EmailAddress>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(agent.reference).map(r => <Reference>{r}</Reference>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(agent.phone).map(p => <Telephone>{p}</Telephone>: NodeSeq).getOrElse(NodeSeq.Empty)}
    </AgentDetails>

  private def agentOf(fr: FullReturn, agentType: String): Option[ReturnAgent] =
    fr.returnAgent.getOrElse(Nil).find(_.agentType.exists(_.equalsIgnoreCase(agentType)))

  private def agentAddress(agent: ReturnAgent): NodeSeq =
    val lines = Seq(agent.address1, agent.address2, agent.address3, agent.address4).flatMap(nonBlank)
    if lines.isEmpty && isBlank(agent.postcode) && isBlank(agent.houseNumber) then NodeSeq.Empty
    else <Address>{addressBody(agent.postcode, agent.houseNumber, lines)}</Address>
  
  private def purchaserDetails(fr: FullReturn): Elem =
    val purchasers = fr.purchaser.getOrElse(Nil)
    val primary    = purchasers.headOption.getOrElse(emptyPurchaser)
    val additional = purchasers.lift(1)
    val sdlt2      = purchasers.drop(2)
    val residency  = fr.residency
    <PurchaserDetails>
      <NumberOfPurchasers>{purchasers.size.max(1)}</NumberOfPurchasers>
      {residency.flatMap(r => yesNo(r.isNonUkResidents)).map(v => <ResidencyStatus>{v}</ResidencyStatus>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {residency.flatMap(r => yesNo(r.isCloseCompany)).map(v => <CloseCompanyStatus>{v}</CloseCompanyStatus>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {residency.flatMap(r => yesNo(r.isCrownRelief)).map(v => <CrownEmployeeRelief>{v}</CrownEmployeeRelief>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {primaryPurchaser(primary, fr)}
      {additional.map(additionalPurchaser).getOrElse(NodeSeq.Empty)}
      {sdlt2.flatMap(sdlt2Purchaser)}
    </PurchaserDetails>

  private def primaryPurchaser(p: Purchaser, fr: FullReturn): Elem =
    val representedByAgent = isYes(p.isRepresentedByAgent)
    val purchaserAgent     = if representedByAgent then agentOf(fr, "PURCHASER") else None
    <Purchaser>
      {purchaserName(p)}
      {purchaserAddress(p)}
      {nonBlank(p.nino).map(n => <NINO>{n}</NINO>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {isoDate(p.dateOfBirth).map(d => <DateOfBirth>{d}</DateOfBirth>: NodeSeq).getOrElse(NodeSeq.Empty)}
      <Trustee>{yesNo(p.isTrustee).getOrElse("no")}</Trustee>
      {nonBlank(p.phone).map(ph => <Telephone>{ph}</Telephone>: NodeSeq).getOrElse(NodeSeq.Empty)}
      <VendorConnected>{yesNo(p.isConnectedToVendor).getOrElse("no")}</VendorConnected>
      <CertificateAddress>Property</CertificateAddress>
      {authoriseAgent(representedByAgent, purchaserAgent)}
      {purchaserAgent.map(purchaserAgentDetails).getOrElse(NodeSeq.Empty)}
    </Purchaser>

  private def additionalPurchaser(p: Purchaser): NodeSeq =
    <AdditionalPurchaser>
      {purchaserName(p)}
      {purchaserAddress(p)}
      <Trustee>{yesNo(p.isTrustee).getOrElse("no")}</Trustee>
    </AdditionalPurchaser>

  private def sdlt2Purchaser(p: Purchaser): NodeSeq =
    <SDLT2Purchaser>
      {purchaserName(p)}
      {purchaserAddress(p)}
      <Trustee>{yesNo(p.isTrustee).getOrElse("no")}</Trustee>
      <VendorConnected>{yesNo(p.isConnectedToVendor).getOrElse("no")}</VendorConnected>
    </SDLT2Purchaser>

  private def purchaserName(p: Purchaser): Elem =
    val surname = if p.isCompany.exists(_.equalsIgnoreCase("YES")) then p.companyName.getOrElse("") else p.surname.getOrElse("")
    <Name>
      {nonBlank(p.title).map(t => <Title>{t}</Title>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(p.forename1).map(f => <Forename>{f}</Forename>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(p.forename2).map(f => <Forename>{f}</Forename>: NodeSeq).getOrElse(NodeSeq.Empty)}
      <CompanyOrSurname>{surname}</CompanyOrSurname>
    </Name>

  private def purchaserAddress(p: Purchaser): Elem =
    val lines = Seq(p.address1, p.address2, p.address3, p.address4).flatMap(nonBlank)
    <Address>{addressBody(p.postcode, p.houseNumber, lines)}</Address>

  private def authoriseAgent(representedByAgent: Boolean, agent: Option[ReturnAgent]): NodeSeq =
    if !representedByAgent then <AuthoriseAgent>no</AuthoriseAgent>
    else agent.flatMap(a => yesNo(a.isAuthorised)).map(v => <AuthoriseAgent>{v}</AuthoriseAgent>: NodeSeq).getOrElse(NodeSeq.Empty)

  private def purchaserAgentDetails(agent: ReturnAgent): NodeSeq =
    <AgentDetails>
      {nonBlank(agent.name).map(n => <Name>{n}</Name>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {agentAddress(agent)}
      {nonBlank(agent.reference).map(r => <Reference>{r}</Reference>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(agent.phone).map(ph => <Telephone>{ph}</Telephone>: NodeSeq).getOrElse(NodeSeq.Empty)}
    </AgentDetails>
  
  private def supplementarySections(fr: FullReturn, leased: Boolean): Elem =
    val nP = fr.purchaser.getOrElse(Nil).size
    val nV = fr.vendor.getOrElse(Nil).size
    val nL = fr.land.getOrElse(Nil).size
    val sdlt2 = math.max(0, nP - 2) + math.max(0, nV - 2)
    val sdlt3 = if leased then 0 else math.max(0, nL - 1)
    val sdlt4 = if leased then leaseSdlt4Count(fr) else (if triggered(fr) then 1 else 0)
    <SupplementarySections>
      <SDLT2Count>{sdlt2}</SDLT2Count>
      <SDLT3Count>{sdlt3}</SDLT3Count>
      <SDLT4Count>{sdlt4}</SDLT4Count>
    </SupplementarySections>

  private def leaseSdlt4Count(fr: FullReturn): Int =
    val additional = math.max(0, fr.land.getOrElse(Nil).size - 1)
    if additional > 0 then additional
    else if triggered(fr) then 1
    else 0
  
  private def sdlt4Sections(fr: FullReturn, leased: Boolean): NodeSeq =
    if leased then
      val additionalLands = fr.land.getOrElse(Nil).drop(1)
      if additionalLands.isEmpty then
        if triggered(fr) then
          <SDLT4>{additionalTransactionDetailsFull(fr)}</SDLT4>
        else NodeSeq.Empty
      else
        val firstAdd = additionalLands.head
        val tail     = additionalLands.tail
        val firstBlock: NodeSeq =
          <SDLT4>
            {additionalTransactionDetailsFull(fr)}
            {aboutTheLease(fr, firstAdd)}
          </SDLT4>
        val tailBlocks: NodeSeq =
          tail.flatMap(land =>
            <SDLT4>
              {additionalTransactionDetailsStripped(fr, land)}
              {aboutTheLease(fr, land)}
            </SDLT4>: NodeSeq
          )
        firstBlock ++ tailBlocks
    else if triggered(fr) then
      <SDLT4>{additionalTransactionDetailsFull(fr)}</SDLT4>
    else
      NodeSeq.Empty
  
  private def triggered(fr: FullReturn): Boolean =
    val tx = fr.transaction.getOrElse(emptyTransaction)
    val p  = fr.purchaser.flatMap(_.headOption).getOrElse(emptyPurchaser)
    val cd = fr.companyDetails.getOrElse(emptyCompanyDetails)
    val l  = fr.lease.isDefined
    isYes(tx.postTransRulingApplied) ||
      isYes(tx.agreedToDeferPayment) ||
      isYes(tx.isDependantOnFutureEvent) ||
      Seq(tx.usedAsFactory, tx.usedAsHotel, tx.usedAsIndustrial, tx.usedAsOffice,
        tx.usedAsOther, tx.usedAsShop, tx.usedAsWarehouse).exists(isYes) ||
      (l && anyNonBlank(Seq(cd.VATReference, cd.UTR, p.registrationNumber, p.placeOfRegistration)))
  
  private def additionalTransactionDetailsFull(fr: FullReturn): Elem =
    val tx = fr.transaction.getOrElse(emptyTransaction)
    <AdditionalTransactionDetails>
      {totalConsiderationWithFlags(tx)}
      {propertyUse(tx)}
      {triggerFields(tx)}
      {purchaserVatReferenceNumber(fr)}
      {purchaserCompanyDetails(fr)}
      {purchaserDescriptions(fr)}
    </AdditionalTransactionDetails>
  
  private def additionalTransactionDetailsStripped(fr: FullReturn, land: Land): Elem =
    val tx = fr.transaction.getOrElse(emptyTransaction)
    <AdditionalTransactionDetails>
      {triggerFields(tx)}
      {yesNo(land.mineralRights).map(v => <MineralRights>{v}</MineralRights>: NodeSeq).getOrElse(NodeSeq.Empty)}
    </AdditionalTransactionDetails>

  private def triggerFields(tx: Transaction): NodeSeq =
    nonBlank(tx.postTransRulingApplied).flatMap(s => yesNo(Option(s))).map(v => <PostTransactionRuling>{v}</PostTransactionRuling>: NodeSeq).getOrElse(NodeSeq.Empty) ++
      postTransactionRulingFollowed(tx) ++
      nonBlank(tx.isDependantOnFutureEvent).flatMap(s => yesNo(Option(s))).map(v => <ConsiderationDependentOnFutureEvents>{v}</ConsiderationDependentOnFutureEvents>: NodeSeq).getOrElse(NodeSeq.Empty) ++
      nonBlank(tx.agreedToDeferPayment).flatMap(s => yesNo(Option(s))).map(v => <DeferredPayment>{v}</DeferredPayment>: NodeSeq).getOrElse(NodeSeq.Empty)
  
  private def postTransactionRulingFollowed(tx: Transaction): NodeSeq =
    nonBlank(tx.postTransRulingFollowed).flatMap { raw =>
        raw.trim.toUpperCase match
          case "YES"                 => Some("yes")
          case "NO"                  => Some("no")
          case "RULINGNOTRECEIVED"   => Some("RulingNotReceived")
          case "RULING_NOT_RECEIVED" => Some("RulingNotReceived")
          case _                     => None
      }.map(v => <PostTransactionRulingFollowed>{v}</PostTransactionRulingFollowed>: NodeSeq)
      .getOrElse(NodeSeq.Empty)

  private def totalConsiderationWithFlags(tx: Transaction): NodeSeq =
    tx.totalConsiderationBusiness.map { value =>
      <TotalConsideration
      Stock={yesNo(tx.includesStock).getOrElse("no")}
      Goodwill={yesNo(tx.includesGoodwill).getOrElse("no")}
      Other={yesNo(tx.includesOther).getOrElse("no")}
      ChattelsAndMoveables={yesNo(tx.includesChattel).getOrElse("no")}
      >{money(toMoney(value).getOrElse(BigDecimal(0)))}</TotalConsideration>: NodeSeq
    }.getOrElse(NodeSeq.Empty)

  private def propertyUse(tx: Transaction): NodeSeq =
    val children: NodeSeq =
      flag(tx.usedAsOffice,     "Office") ++
        flag(tx.usedAsHotel,      "Hotel") ++
        flag(tx.usedAsShop,       "Shop") ++
        flag(tx.usedAsWarehouse,  "Warehouse") ++
        flag(tx.usedAsFactory,    "Factory") ++
        flag(tx.usedAsOther,      "Other") ++
        flag(tx.usedAsIndustrial, "OtherIndustrialUnit")
    if children.isEmpty then NodeSeq.Empty else <PropertyUse>{children}</PropertyUse>

  private def flag(value: Option[String], elementName: String): NodeSeq =
    if isYes(value) then
      scala.xml.Elem(null, elementName, scala.xml.Null, scala.xml.TopScope, minimizeEmpty = true, scala.xml.Text("yes"))
    else NodeSeq.Empty
  
  private def purchaserVatReferenceNumber(fr: FullReturn): NodeSeq =
    fr.companyDetails.flatMap(cd => nonBlank(cd.VATReference))
      .map { raw =>
        val normalised = raw.trim.replaceAll("\\s+", "").stripPrefix("GB").stripPrefix("gb")
        <PurchaserVATreferenceNumber>{normalised}</PurchaserVATreferenceNumber>: NodeSeq
      }.getOrElse(NodeSeq.Empty)
  
  private def purchaserCompanyDetails(fr: FullReturn): NodeSeq =
    val cd             = fr.companyDetails
    val firstPurchaser = fr.purchaser.flatMap(_.headOption)
    val taxRef         = cd.flatMap(c => nonBlank(c.UTR))
      .map(u => <TaxReferenceNumber>{u}</TaxReferenceNumber>: NodeSeq).getOrElse(NodeSeq.Empty)
    val regNumber      = firstPurchaser.flatMap(p => nonBlank(p.registrationNumber))
      .map(r => <CompanyRegisteredNumber>{r}</CompanyRegisteredNumber>: NodeSeq).getOrElse(NodeSeq.Empty)
    val regPlace       = firstPurchaser.flatMap(p => nonBlank(p.placeOfRegistration))
      .map(p => <PlaceOfRegistration>{p}</PlaceOfRegistration>: NodeSeq).getOrElse(NodeSeq.Empty)
    val children       = taxRef ++ regNumber ++ regPlace
    if children.isEmpty then NodeSeq.Empty
    else <PurchaserCompanyDetails>{children}</PurchaserCompanyDetails>


  private val companyTypeCodes: Seq[(CompanyDetails => Option[String], String)] = Seq(
    (_.companyTypeBuilder, "01"),
    (_.companyTypeSoletrader, "02"),
    (_.companyTypeIndividual, "03"),
    (_.companyTypePartnership, "04"),
    (_.companyTypeLocalauth, "05"),
    (_.companyTypeCentgov, "06"),
    (_.companyTypePubliccorp, "07"),
    (_.companyTypeProperty, "08"),
    (_.companyTypeBank, "09"),
    (_.companyTypeBuildsoc, "10"),
    (_.companyTypeInsurance, "11"),
    (_.companyTypePensionfund, "12"),
    (_.companyTypeOtherfinancial, "13"),
    (_.companyTypeOthercompany, "14"),
    (_.companyTypeOthercharity, "15")
  )

  private def purchaserDescriptions(fr: FullReturn): NodeSeq =
    fr.companyDetails.map { cd =>
      companyTypeCodes
        .collect { case (accessor, code) if isYes(accessor(cd)) => code }
        .take(4)
        .foldLeft(NodeSeq.Empty)((acc, code) => acc ++ <PurchaserDescription>
          {code}
        </PurchaserDescription>)
    }.getOrElse(NodeSeq.Empty)
    
  private def aboutTheLease(fr: FullReturn, land: Land): NodeSeq =
    <AboutTheLease>
      {aboutTheLeaseProperty(land)}
      {aboutTheLeaseDetails(fr)}
    </AboutTheLease>

  private def aboutTheLeaseProperty(land: Land): Elem =
    <Property>
      <PropertyType>{land.propertyType.getOrElse("01")}</PropertyType>
      {longAddressOfLand(land)}
      {nonBlank(land.localAuthorityNumber).map(n => <LAnumber>{n}</LAnumber>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(land.titleNumber).map(t => <TitleNumber>{t}</TitleNumber>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(land.NLPGUPRN).map(n => <NLPGUPRN>{n}</NLPGUPRN>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {landArea(land)}
      {yesNo(land.willSendPlanByPost).map(v => <PlanSubmitted>{v}</PlanSubmitted>: NodeSeq).getOrElse(NodeSeq.Empty)}
      {nonBlank(land.interestCreatedTransferred).map(i => <InterestTransfered>{i.trim.take(2)}</InterestTransfered>: NodeSeq).getOrElse(NodeSeq.Empty)}
    </Property>
  
  private def aboutTheLeaseDetails(fr: FullReturn): NodeSeq =
    fr.lease.map { lease =>
      <LeaseDetails>
        {nonBlank(lease.leaseType).map(t => <LeaseType>{t}</LeaseType>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {isoDate(lease.contractStartDate).map(d => <StartDate>{d}</StartDate>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {isoDate(lease.contractEndDate).map(d => <EndDate>{d}</EndDate>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {nonBlank(lease.rentFreePeriod).map(p => <RentFreePeriod>{p}</RentFreePeriod>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {startingRent(lease, zeroAmounts = false)}
        {nonBlank(lease.totalPremiumPayable).map(v => <PremiumPaid>{money(BigDecimal(v))}</PremiumPaid>: NodeSeq).getOrElse(NodeSeq.Empty)}
        {nonBlank(lease.netPresentValue).map(v => <NetPresentValue>{money(BigDecimal(v))}</NetPresentValue>: NodeSeq).getOrElse(NodeSeq.Empty)}
      </LeaseDetails>: NodeSeq
    }.getOrElse(NodeSeq.Empty)

  private def addressBody(postcode: Option[String], houseNumber: Option[String], lines: Seq[String]): NodeSeq =
    nonBlank(postcode).map(p => <PostCode>{p}</PostCode>: NodeSeq).getOrElse(NodeSeq.Empty) ++
      nonBlank(houseNumber).map(h => <HouseNumber>{h}</HouseNumber>: NodeSeq).getOrElse(NodeSeq.Empty) ++
      lines.foldLeft(NodeSeq.Empty)((acc, l) => acc ++ <Line>{l}</Line>)
  
  private def transactionDescription(fr: FullReturn): String =
    fr.transaction.flatMap(_.transactionDescription).map(_.trim.toUpperCase).getOrElse("F")

  private def emptyTransaction: Transaction       = Transaction()
  private def emptyLand: Land                     = Land()
  private def emptyVendor: Vendor                 = Vendor()
  private def emptyPurchaser: Purchaser           = Purchaser()
  private def emptyCompanyDetails: CompanyDetails = CompanyDetails()