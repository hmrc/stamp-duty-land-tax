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

package models.filing

import play.api.libs.json._

case class SdltOrganisation(
    isReturnUser: Option[String] = None,
    doNotDisplayWelcomePage: Option[String] = None,
    storn: Option[String] = None,
    version: Option[String] = None
)

object SdltOrganisation {
  implicit val format: OFormat[SdltOrganisation] = Json.format[SdltOrganisation]
}

case class ReturnInfo(
    returnID: Option[String] = None,
    storn: Option[String] = None,
    purchaserCounter: Option[String] = None,
    vendorCounter: Option[String] = None,
    landCounter: Option[String] = None,
    purgeDate: Option[String] = None,
    version: Option[String] = None,
    mainPurchaserID: Option[String] = None,
    mainVendorID: Option[String] = None,
    mainLandID: Option[String] = None,
    IRMarkGenerated: Option[String] = None,
    landCertForEachProp: Option[String] = None,
    returnResourceRef: Option[String] = None,
    declaration: Option[String] = None,
    status: Option[String] = None
)

object ReturnInfo {
  implicit val format: OFormat[ReturnInfo] = Json.format[ReturnInfo]
}

case class Purchaser(
    purchaserID: Option[String] = None,
    returnID: Option[String] = None,
    isCompany: Option[String] = None,
    isTrustee: Option[String] = None,
    isConnectedToVendor: Option[String] = None,
    isRepresentedByAgent: Option[String] = None,
    title: Option[String] = None,
    surname: Option[String] = None,
    forename1: Option[String] = None,
    forename2: Option[String] = None,
    companyName: Option[String] = None,
    houseNumber: Option[String] = None,
    address1: Option[String] = None,
    address2: Option[String] = None,
    address3: Option[String] = None,
    address4: Option[String] = None,
    postcode: Option[String] = None,
    phone: Option[String] = None,
    nino: Option[String] = None,
    purchaserResourceRef: Option[String] = None,
    nextPurchaserID: Option[String] = None,
    lMigrated: Option[String] = None,
    createDate: Option[String] = None,
    lastUpdateDate: Option[String] = None,
    isUkCompany: Option[String] = None,
    hasNino: Option[String] = None,
    dateOfBirth: Option[String] = None,
    registrationNumber: Option[String] = None,
    placeOfRegistration: Option[String] = None
)

object Purchaser {
  private val baseFormat: OFormat[Purchaser] = Json.format[Purchaser]

  private def normalizeYesNo(value: Option[String]): Option[String] =
    value.map(_.toUpperCase)

  implicit val format: OFormat[Purchaser] = new OFormat[Purchaser] {
    override def reads(json: JsValue): JsResult[Purchaser] = {
      baseFormat.reads(json).map { purchaser =>
        purchaser.copy(
          isCompany = normalizeYesNo(purchaser.isCompany),
          isTrustee = normalizeYesNo(purchaser.isTrustee),
          isConnectedToVendor = normalizeYesNo(purchaser.isConnectedToVendor),
          isRepresentedByAgent = normalizeYesNo(purchaser.isRepresentedByAgent),
          isUkCompany = normalizeYesNo(purchaser.isUkCompany),
          hasNino = normalizeYesNo(purchaser.hasNino)
        )
      }
    }

    override def writes(purchaser: Purchaser): JsObject = {
      baseFormat.writes(purchaser)
    }
  }
}

case class CompanyDetails(
    companyDetailsID: Option[String] = None,
    returnID: Option[String] = None,
    purchaserID: Option[String] = None,
    UTR: Option[String] = None,
    VATReference: Option[String] = None,
    companyTypeBank: Option[String] = None,
    companyTypeBuilder: Option[String] = None,
    companyTypeBuildsoc: Option[String] = None,
    companyTypeCentgov: Option[String] = None,
    companyTypeIndividual: Option[String] = None,
    companyTypeInsurance: Option[String] = None,
    companyTypeLocalauth: Option[String] = None,
    companyTypeOthercharity: Option[String] = None,
    companyTypeOthercompany: Option[String] = None,
    companyTypeOtherfinancial: Option[String] = None,
    companyTypePartnership: Option[String] = None,
    companyTypeProperty: Option[String] = None,
    companyTypePubliccorp: Option[String] = None,
    companyTypeSoletrader: Option[String] = None,
    companyTypePensionfund: Option[String] = None
)

object CompanyDetails {
  implicit val format: OFormat[CompanyDetails] = Json.format[CompanyDetails]
}

case class Vendor(
    vendorID: Option[String] = None,
    returnID: Option[String] = None,
    title: Option[String] = None,
    forename1: Option[String] = None,
    forename2: Option[String] = None,
    name: Option[String] = None,
    houseNumber: Option[String] = None,
    address1: Option[String] = None,
    address2: Option[String] = None,
    address3: Option[String] = None,
    address4: Option[String] = None,
    postcode: Option[String] = None,
    isRepresentedByAgent: Option[String] = None,
    vendorResourceRef: Option[String] = None,
    nextVendorID: Option[String] = None,
    lastUpdateDate: Option[String] = None
)

object Vendor {
  private val baseFormat: OFormat[Vendor] = Json.format[Vendor]

  private def normalizeYesNo(value: Option[String]): Option[String] =
    value.map(_.toUpperCase)

  implicit val format: OFormat[Vendor] = new OFormat[Vendor] {
    override def reads(json: JsValue): JsResult[Vendor] = {
      baseFormat.reads(json).map { vendor =>
        vendor.copy(
          isRepresentedByAgent = normalizeYesNo(vendor.isRepresentedByAgent)
        )
      }
    }

    override def writes(vendor: Vendor): JsObject = {
      baseFormat.writes(vendor)
    }
  }
}

case class Land(
    landID: Option[String] = None,
    returnID: Option[String] = None,
    propertyType: Option[String] = None,
    interestCreatedTransferred: Option[String] = None,
    houseNumber: Option[String] = None,
    address1: Option[String] = None,
    address2: Option[String] = None,
    address3: Option[String] = None,
    address4: Option[String] = None,
    postcode: Option[String] = None,
    landArea: Option[String] = None,
    areaUnit: Option[String] = None,
    localAuthorityNumber: Option[String] = None,
    mineralRights: Option[String] = None,
    NLPGUPRN: Option[String] = None,
    willSendPlanByPost: Option[String] = None,
    titleNumber: Option[String] = None,
    landResourceRef: Option[String] = None,
    nextLandID: Option[String] = None,
    DARPostcode: Option[String] = None,
    lastUpdateDate: Option[String] = None
)

object Land {
  implicit val format: OFormat[Land] = Json.format[Land]
}

case class Transaction(
    transactionID: Option[String] = None,
    returnID: Option[String] = None,
    claimingRelief: Option[String] = None,
    reliefAmount: Option[BigDecimal] = None,
    reliefReason: Option[String] = None,
    reliefSchemeNumber: Option[String] = None,
    isLinked: Option[String] = None,
    totalConsiderationLinked: Option[BigDecimal] = None,
    totalConsideration: Option[BigDecimal] = None,
    considerationBuild: Option[BigDecimal] = None,
    considerationCash: Option[BigDecimal] = None,
    considerationContingent: Option[BigDecimal] = None,
    considerationDebt: Option[BigDecimal] = None,
    considerationEmploy: Option[BigDecimal] = None,
    considerationOther: Option[BigDecimal] = None,
    considerationLand: Option[BigDecimal] = None,
    considerationServices: Option[BigDecimal] = None,
    considerationSharesQTD: Option[BigDecimal] = None,
    considerationSharesUNQTD: Option[BigDecimal] = None,
    considerationVAT: Option[BigDecimal] = None,
    includesChattel: Option[String] = None,
    includesGoodwill: Option[String] = None,
    includesOther: Option[String] = None,
    includesStock: Option[String] = None,
    usedAsFactory: Option[String] = None,
    usedAsHotel: Option[String] = None,
    usedAsIndustrial: Option[String] = None,
    usedAsOffice: Option[String] = None,
    usedAsOther: Option[String] = None,
    usedAsShop: Option[String] = None,
    usedAsWarehouse: Option[String] = None,
    contractDate: Option[String] = None,
    isDependantOnFutureEvent: Option[String] = None,
    transactionDescription: Option[String] = None,
    newTransactionDescription: Option[String] = None,
    effectiveDate: Option[String] = None,
    isLandExchanged: Option[String] = None,
    exchangedLandHouseNumber: Option[String] = None,
    exchangedLandAddress1: Option[String] = None,
    exchangedLandAddress2: Option[String] = None,
    exchangedLandAddress3: Option[String] = None,
    exchangedLandAddress4: Option[String] = None,
    exchangedLandPostcode: Option[String] = None,
    agreedToDeferPayment: Option[String] = None,
    postTransRulingApplied: Option[String] = None,
    isPursuantToPreviousOption: Option[String] = None,
    restrictionsAffectInterest: Option[String] = None,
    restrictionDetails: Option[String] = None,
    postTransRulingFollowed: Option[String] = None,
    isPartOfSaleOfBusiness: Option[String] = None,
    totalConsiderationBusiness: Option[BigDecimal] = None
)

object Transaction {
  implicit val format: OFormat[Transaction] = Json.format[Transaction]
}

case class ReturnAgent(
    returnAgentID: Option[String] = None,
    returnID: Option[String] = None,
    agentType: Option[String] = None,
    name: Option[String] = None,
    houseNumber: Option[String] = None,
    address1: Option[String] = None,
    address2: Option[String] = None,
    address3: Option[String] = None,
    address4: Option[String] = None,
    postcode: Option[String] = None,
    phone: Option[String] = None,
    email: Option[String] = None,
    DXAddress: Option[String] = None,
    reference: Option[String] = None,
    isAuthorised: Option[String] = None
)

object ReturnAgent {
  implicit val format: OFormat[ReturnAgent] = Json.format[ReturnAgent]
}

case class Agent(
    agentId: Option[String] = None,
    storn: Option[String] = None,
    name: Option[String] = None,
    houseNumber: Option[String] = None,
    address1: Option[String] = None,
    address2: Option[String] = None,
    address3: Option[String] = None,
    address4: Option[String] = None,
    postcode: Option[String] = None,
    phone: Option[String] = None,
    email: Option[String] = None,
    dxAddress: Option[String] = None,
    agentResourceReference: Option[String] = None
)

object Agent {
  implicit val format: OFormat[Agent] = Json.format[Agent]
}

case class Lease(
    leaseID: Option[String] = None,
    returnID: Option[String] = None,
    isAnnualRentOver1000: Option[String] = None,
    breakClauseType: Option[String] = None,
    breakClauseDate: Option[String] = None,
    leaseContReservedRent: Option[String] = None,
    contractEndDate: Option[String] = None,
    contractStartDate: Option[String] = None,
    firstReviewDate: Option[String] = None,
    leaseType: Option[String] = None,
    marketRent: Option[String] = None,
    netPresentValue: Option[String] = None,
    optionToRenew: Option[String] = None,
    totalPremiumPayable: Option[String] = None,
    rentChargeDate: Option[String] = None,
    rentFreePeriod: Option[String] = None,
    reviewClauseType: Option[String] = None,
    rentReviewFrequency: Option[String] = None,
    serviceCharge: Option[String] = None,
    serviceChargeFrequency: Option[String] = None,
    startingRent: Option[String] = None,
    startingRentEndDate: Option[String] = None,
    laterRentKnown: Option[String] = None,
    termsSurrendered: Option[String] = None,
    considToLndlrdBuild: Option[String] = None,
    considToLndlrdContin: Option[String] = None,
    considToLndlrdDebt: Option[String] = None,
    considToLndlrdEmploy: Option[String] = None,
    considToLndlrdOther: Option[String] = None,
    considToLndlrdLand: Option[String] = None,
    considToLndlrdServices: Option[String] = None,
    considToLndlrdSharedQTD: Option[String] = None,
    considToLndlrdSharedUNQTD: Option[String] = None,
    considToTenantBuild: Option[String] = None,
    considToTenantContin: Option[String] = None,
    considToTenantEmploy: Option[String] = None,
    considToTenantOther: Option[String] = None,
    considToTenantLand: Option[String] = None,
    considToTenantServices: Option[String] = None,
    considToTenantSharesQTD: Option[String] = None,
    considToTenantSharesUNQTD: Option[String] = None,
    turnoverRent: Option[String] = None,
    unasertainableRent: Option[String] = None,
    VATAmount: Option[String] = None
)

object Lease {
  implicit val format: OFormat[Lease] = Json.format[Lease]
}

case class TaxCalculation(
    taxCalculationID: Option[String] = None,
    returnID: Option[String] = None,
    amountPaid: Option[String] = None,
    includesPenalty: Option[String] = None,
    taxDue: Option[String] = None,
    taxDuePremium: Option[String] = None,
    taxDueNPV: Option[String] = None,
    calcPenaltyDue: Option[String] = None,
    calcTaxDue: Option[String] = None,
    calcTaxRate1: Option[String] = None,
    calcTaxRate2: Option[String] = None,
    calcTotalTaxPenaltyDue: Option[String] = None,
    calcTotalNPVTax: Option[String] = None,
    calcTotalPremiumTax: Option[String] = None,
    honestyDeclaration: Option[String] = None
)

object TaxCalculation {
  implicit val format: OFormat[TaxCalculation] = Json.format[TaxCalculation]
}

case class Submission(
    submissionID: Option[String] = None,
    returnID: Option[String] = None,
    storn: Option[String] = None,
    submissionStatus: Option[String] = None,
    govtalkMessageClass: Option[String] = None,
    UTRN: Option[String] = None,
    irmarkReceived: Option[String] = None,
    submissionReceipt: Option[String] = None,
    govtalkErrorCode: Option[String] = None,
    govtalkErrorType: Option[String] = None,
    govtalkErrorMessage: Option[String] = None,
    numPolls: Option[String] = None,
    createDate: Option[String] = None,
    lastUpdateDate: Option[String] = None,
    acceptedDate: Option[String] = None,
    submittedDate: Option[String] = None,
    email: Option[String] = None,
    submissionRequestDate: Option[String] = None,
    irmarkSent: Option[String] = None
)

object Submission {
  implicit val format: OFormat[Submission] = Json.format[Submission]
}

case class SubmissionErrorDetails(
    errorDetailID: Option[String] = None,
    returnID: Option[String] = None,
    position: Option[String] = None,
    errorMessage: Option[String] = None,
    storn: Option[String] = None,
    submissionID: Option[String] = None
)

object SubmissionErrorDetails {
  implicit val format: OFormat[SubmissionErrorDetails] =
    Json.format[SubmissionErrorDetails]
}

case class Residency(
    residencyID: Option[String] = None,
    isNonUkResidents: Option[String] = None,
    isCloseCompany: Option[String] = None,
    isCrownRelief: Option[String] = None
)

object Residency {
  implicit val format: OFormat[Residency] = Json.format[Residency]
}

case class GetReturnRequest(
    stornId: Option[String] = None,
    returnResourceRef: Option[String] = None,
    sdltOrganisation: Option[SdltOrganisation] = None,
    returnInfo: Option[ReturnInfo] = None,
    purchaser: Option[Seq[Purchaser]] = None,
    companyDetails: Option[CompanyDetails] = None,
    vendor: Option[Seq[Vendor]] = None,
    land: Option[Seq[Land]] = None,
    transaction: Option[Transaction] = None,
    returnAgent: Option[Seq[ReturnAgent]] = None,
    agent: Option[Seq[Agent]] = None,
    lease: Option[Lease] = None,
    taxCalculation: Option[TaxCalculation] = None,
    submission: Option[Submission] = None,
    submissionErrorDetails: Option[SubmissionErrorDetails] = None,
    residency: Option[Residency] = None
)

object GetReturnRequest {
  implicit val format: OFormat[GetReturnRequest] = Json.format[GetReturnRequest]
}
