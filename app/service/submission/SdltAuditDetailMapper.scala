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

import com.google.inject.{ImplementedBy, Singleton}
import play.api.libs.json.*
import models.filing.*

@ImplementedBy(classOf[SdltAuditDetailMapperImpl])
trait SdltAuditDetailMapper:
  def submissionDetail(fullReturn: FullReturn): JsObject

@Singleton
class SdltAuditDetailMapperImpl extends SdltAuditDetailMapper:

  def submissionDetail(fullReturn: FullReturn): JsObject =
    prune(
      obj(
        "transactionDetails"    -> Some(transactionDetails(fullReturn)),
        "leaseDetails"          -> fullReturn.lease.map(leaseDetails(_, fullReturn.taxCalculation)),
        "landDetails"           -> fullReturn.land.map(landDetails(_, fullReturn.returnInfo)),
        "vendorDetails"         -> fullReturn.vendor.map(vs => JsArray(vs.map(vendor))),
        "vendorAgentDetails"    -> agentOf(fullReturn, "VENDOR"),
        "purchaserDetails"      -> fullReturn.purchaser.map(ps => purchasers(ps, fullReturn.companyDetails)),
        "purchaserAgentDetails" -> agentOf(fullReturn, "PURCHASER"),
        "residencyDetails"      -> fullReturn.residency.map(residency)
      )
    )
  
  private def transactionDetails(fr: FullReturn): JsObject =
    val t   = fr.transaction
    val tax = fr.taxCalculation
    prune(
      obj(
        "transactionDescription"           -> t.flatMap(_.transactionDescription).map(JsString.apply),
        "effectiveDate"                    -> t.flatMap(_.effectiveDate).map(JsString.apply),
        "restrictionsAffectingTransaction" -> t.flatMap(_.restrictionDetails).map(JsString.apply),
        "contractDate"                     -> t.flatMap(_.contractDate).map(JsString.apply),
        "landExchangedAddress"             -> exchangedAddress(t),
        "persuantToOption"                 -> t.flatMap(_.isPursuantToPreviousOption).map(bool),
        "propertyUse"                      -> propertyUse(t),
        "reliefDetails"                    -> reliefDetails(t),
        "postTransactionRuling"            -> t.flatMap(_.postTransRulingApplied).map(bool),
        "postTransactionRulingFollowed"    -> t.flatMap(_.postTransRulingFollowed).map(JsString.apply),
        "considerationDependentOnFutureEvents" -> t.flatMap(_.isDependantOnFutureEvent).map(bool),
        "deferredPayment"                  -> t.flatMap(_.agreedToDeferPayment).map(bool),
        "considerationDetails"             -> considerationDetails(t),
        "businessSaleDetails"              -> businessSaleDetails(t),
        "linkedTransactionTotalConsideration" -> t.flatMap(_.totalConsiderationLinked).flatMap(num),
        "totalDue"                         -> tax.flatMap(_.taxDue).flatMap(num),
        "amountPaid"                       -> tax.flatMap(_.amountPaid).flatMap(num),
        "amountPaidIncludesPenalties"      -> tax.flatMap(_.includesPenalty).map(bool)
      )
    )

  private def exchangedAddress(t: Option[Transaction]): Option[JsObject] =
    t.filter(_.isLandExchanged.exists(isTrue)).map { tx =>
      prune(obj(
        "addressLine1" -> tx.exchangedLandHouseNumber.map(JsString.apply),
        "addressLine2" -> tx.exchangedLandAddress1.map(JsString.apply),
        "addressLine3" -> tx.exchangedLandAddress2.map(JsString.apply),
        "addressLine4" -> tx.exchangedLandAddress3.map(JsString.apply),
        "postCode"     -> tx.exchangedLandPostcode.map(JsString.apply)
      ))
    }

  private def propertyUse(t: Option[Transaction]): Option[JsArray] =
    t.map { tx =>
      val uses = Seq(
        "office"                -> tx.usedAsOffice,
        "hotel"                 -> tx.usedAsHotel,
        "shop"                  -> tx.usedAsShop,
        "warehouse"             -> tx.usedAsWarehouse,
        "factory"               -> tx.usedAsFactory,
        "other industrial unit" -> tx.usedAsIndustrial,
        "other"                 -> tx.usedAsOther
      ).collect { case (label, v) if v.exists(isTrue) => JsString(label) }
      JsArray(uses)
    }.filter(_.value.nonEmpty)

  private def reliefDetails(t: Option[Transaction]): Option[JsObject] =
    t.filter(_.claimingRelief.exists(isTrue)).map { tx =>
      prune(obj(
        "reason"                            -> tx.reliefReason.map(JsString.apply),
        "constructionIndustrySchemeNumber"  -> tx.reliefSchemeNumber.map(JsString.apply),
        "chargeableAmount"                  -> tx.reliefAmount.flatMap(num)
      ))
    }

  private def considerationDetails(t: Option[Transaction]): Option[JsObject] =
    t.map { tx =>
      prune(obj(
        "totalConsideration"  -> tx.totalConsideration.flatMap(num),
        "vatAmount"           -> tx.considerationVAT.flatMap(num),
        "formOfConsideration" -> formOfConsideration(tx)
      ))
    }.filter(o => (o \ "totalConsideration").toOption.isDefined)

  private def formOfConsideration(tx: Transaction): Option[JsArray] =
    def nonZero(s: Option[String]): Boolean =
      s.flatMap(v => scala.util.Try(BigDecimal(v.trim)).toOption).exists(_ != BigDecimal(0))

    val forms = Seq(
      "30 - Cash"              -> tx.considerationCash,
      "31 - Debt"              -> tx.considerationDebt,
      "32 - Building Works"    -> tx.considerationBuild,
      "33 - Employment"        -> tx.considerationEmploy,
      "34 - Other"             -> tx.considerationOther,
      "35 - Land"              -> tx.considerationLand,
      "36 - Services"          -> tx.considerationServices,
      "37 - Shares (quoted)"   -> tx.considerationSharesQTD,
      "38 - Shares (unquoted)" -> tx.considerationSharesUNQTD,
      "39 - Contingent"        -> tx.considerationContingent
    ).collect { case (label, amt) if nonZero(amt) => JsString(label) }.take(4)

    Option.when(forms.nonEmpty)(JsArray(forms))

  private def businessSaleDetails(t: Option[Transaction]): Option[JsObject] =
    t.filter(_.isPartOfSaleOfBusiness.exists(isTrue)).map { tx =>
      val items = Seq(
        "stock"                 -> tx.includesStock,
        "goodwill"              -> tx.includesGoodwill,
        "chattels and moveables"-> tx.includesChattel,
        "other"                 -> tx.includesOther
      ).collect { case (label, v) if v.exists(isTrue) => JsString(label) }
      prune(obj(
        "totalConsiderationOfItems" -> tx.totalConsiderationBusiness.flatMap(num),
        "itemsIncludedInSale"       -> Option.when(items.nonEmpty)(JsArray(items))
      ))
    }
  
  private def leaseDetails(l: Lease, tax: Option[TaxCalculation]): JsObject =
    prune(obj(
      "leaseType"              -> l.leaseType.map(JsString.apply),
      "leaseStartDate"         -> l.contractStartDate.map(JsString.apply),
      "leaseEndDate"           -> l.contractEndDate.map(JsString.apply),
      "monthsRentFree"         -> l.rentFreePeriod.flatMap(num),
      "startingRentPayable"    -> l.startingRent.flatMap(num),
      "startingRentEndDate"    -> l.startingRentEndDate.map(JsString.apply),
      "laterRentKnown"         -> l.laterRentKnown.map(bool),
      "vatAmountOnStartingRent"-> l.VATAmount.flatMap(num),
      "premiumPaid"            -> l.totalPremiumPayable.flatMap(num),
      "netPresentValue"        -> l.netPresentValue.flatMap(num),
      "totalPremiumTax"        -> tax.flatMap(_.calcTotalPremiumTax).flatMap(num),
      "totalNetPresentValueTax"-> tax.flatMap(_.calcTotalNPVTax).flatMap(num)
    ))


  private def landDetails(lands: Seq[Land], info: Option[ReturnInfo]): JsObject =
    prune(obj(
      "certificateForEach" -> info.flatMap(_.landCertForEachProp).map(bool),
      "propertyDetails"    -> Some(JsArray(lands.map(property)))
    ))

  private def property(l: Land): JsObject =
    prune(obj(
      "propertyType"          -> l.propertyType.map(JsString.apply),   // code-prefixed, passed through
      "address"               -> address(l.postcode, l.houseNumber, l.address1, l.address2, l.address3),
      "localAuthorityNumber"  -> l.localAuthorityNumber.map(JsString.apply),
      "titleNumber"           -> l.titleNumber.map(JsString.apply),
      "landAreaUnits"         -> l.areaUnit.map(JsString.apply),
      "landArea"              -> l.landArea.flatMap(num),
      "planSubmitted"         -> l.willSendPlanByPost.map(v => JsBoolean(!isTrue(v))), // "will send by post" => not submitted with return
      "interestTransferredOrCreated" -> l.interestCreatedTransferred.map(JsString.apply), // code-prefixed
      "mineralRights"         -> l.mineralRights.map(bool)
    ))


  private def vendor(v: Vendor): JsObject =
    prune(obj(
      "firstName"        -> v.forename1.map(JsString.apply),
      "secondName"       -> v.forename2.map(JsString.apply),
      "companyOrSurname" -> v.name.map(JsString.apply),
      "address"          -> address(v.postcode, v.houseNumber, v.address1, v.address2, v.address3)
    ))

  private def purchasers(ps: Seq[Purchaser], company: Option[CompanyDetails]): JsArray =
    JsArray(ps.zipWithIndex.map { case (p, idx) =>
      purchaser(p, if idx == 0 then company else None, firstPurchaser = idx == 0)
    })

  private def purchaser(p: Purchaser, company: Option[CompanyDetails], firstPurchaser: Boolean): JsObject =
    prune(obj(
      "firstName"        -> p.forename1.map(JsString.apply),
      "secondName"       -> p.forename2.map(JsString.apply),
      "companyOrSurname" -> p.surname.orElse(p.companyName).map(JsString.apply),
      "address"          -> address(p.postcode, p.houseNumber, p.address1, p.address2, p.address3),
      "purchaserCompanyDetails" -> (if firstPurchaser then purchaserCompany(p, company) else None),
      "nino"             -> (if firstPurchaser then p.nino.map(JsString.apply) else None),
      "dateOfBirth"      -> (if firstPurchaser then p.dateOfBirth.map(JsString.apply) else None),
      "actingAsTrustee"  -> p.isTrustee.map(bool),
      "phoneNumber"      -> p.phone.map(JsString.apply),
      "connectedToVendor"-> p.isConnectedToVendor.map(bool)
    ))

  private def purchaserCompany(p: Purchaser, company: Option[CompanyDetails]): Option[JsObject] =
    val o = prune(obj(
      "vatReferenceNumber"     -> company.flatMap(_.VATReference).map(JsString.apply),
      "taxReferenceNumber"     -> company.flatMap(_.UTR).map(JsString.apply),
      "companyRegisteredNumber"-> p.registrationNumber.map(JsString.apply),
      "placeOfRegistration"    -> p.placeOfRegistration.map(JsString.apply)
    ))
    Option.when(o.fields.nonEmpty)(o)
  
  private def agentOf(fr: FullReturn, agentType: String): Option[JsObject] =
    fr.returnAgent.flatMap(_.find(_.agentType.exists(_.equalsIgnoreCase(agentType)))).map { a =>
      prune(obj(
        "name"        -> a.name.map(JsString.apply),
        "address"     -> address(a.postcode, a.houseNumber, a.address1, a.address2, a.address3),
        "emailAddress"-> a.email.map(JsString.apply),
        "returnReference" -> a.reference.map(JsString.apply),
        "phoneNumber" -> a.phone.map(JsString.apply),
        "authorisedForCorrespondence" -> (if agentType.equalsIgnoreCase("PURCHASER") then a.isAuthorised.map(bool) else None)
      ))
    }


  
  private def residency(r: Residency): JsObject =
    prune(obj(
      "residencyStatus"    -> r.isNonUkResidents.map(bool),
      "closeCompanyStatus" -> r.isCloseCompany.map(bool),
      "crownEmployeeRelief"-> r.isCrownRelief.map(bool)
    ))


  
  private def address(postcode: Option[String], line1: Option[String],
                      line2: Option[String], line3: Option[String], line4: Option[String]): Option[JsObject] =
    val o = prune(obj(
      "postCode"     -> postcode.map(JsString.apply),
      "addressLine1" -> line1.map(JsString.apply),
      "addressLine2" -> line2.map(JsString.apply),
      "addressLine3" -> line3.map(JsString.apply),
      "addressLine4" -> line4.map(JsString.apply)
    ))
    Option.when(o.fields.nonEmpty)(o)

  private def obj(fields: (String, Option[JsValue])*): JsObject =
    JsObject(fields.collect { case (k, Some(v)) => k -> v })

  private def bool(s: String): JsBoolean = JsBoolean(isTrue(s))

  private def isTrue(s: String): Boolean =
    val v = s.trim.toUpperCase
    v == "YES" || v == "Y" || v == "TRUE" || v == "1"

  private def num(s: String): Option[JsNumber] =
    scala.util.Try(BigDecimal(s.trim)).toOption.map(JsNumber.apply)

  
  private def prune(o: JsObject): JsObject =
    JsObject(o.fields.flatMap {
      case (_, JsNull)                          => None
      case (k, sub: JsObject) =>
        val p = prune(sub)
        Option.when(p.fields.nonEmpty)(k -> p)
      case (k, arr: JsArray) if arr.value.isEmpty => None
      case (k, v)                               => Some(k -> v)
    })