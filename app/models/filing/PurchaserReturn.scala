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

import play.api.libs.json.{Json, OFormat}

case class CreatePurchaserRequest(
                                   stornId: String,
                                   returnResourceRef: String,
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
                                   isUkCompany: Option[String] = None,
                                   hasNino: Option[String] = None,
                                   dateOfBirth: Option[String] = None,
                                   registrationNumber: Option[String] = None,
                                   placeOfRegistration: Option[String] = None
                                 )

object CreatePurchaserRequest {
  implicit val format: OFormat[CreatePurchaserRequest] = Json.format[CreatePurchaserRequest]
}

case class CreatePurchaserReturn(
                                  purchaserResourceRef: String,
                                  purchaserId: String
                                )

object CreatePurchaserReturn {
  implicit val format: OFormat[CreatePurchaserReturn] = Json.format[CreatePurchaserReturn]
}

case class UpdatePurchaserRequest(
                                   stornId: String,
                                   returnResourceRef: String,
                                   purchaserResourceRef: String,
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
                                   nextPurchaserId: Option[String] = None,
                                   isUkCompany: Option[String] = None,
                                   hasNino: Option[String] = None,
                                   dateOfBirth: Option[String] = None,
                                   registrationNumber: Option[String] = None,
                                   placeOfRegistration: Option[String] = None
                                 )

object UpdatePurchaserRequest {
  implicit val format: OFormat[UpdatePurchaserRequest] = Json.format[UpdatePurchaserRequest]
}

case class UpdatePurchaserReturn(
                                  updated: Boolean
                                )

object UpdatePurchaserReturn {
  implicit val format: OFormat[UpdatePurchaserReturn] = Json.format[UpdatePurchaserReturn]
}

case class DeletePurchaserRequest(
                                   storn: String,
                                   purchaserResourceRef: String,
                                   returnResourceRef: String
                                 )

object DeletePurchaserRequest {
  implicit val format: OFormat[DeletePurchaserRequest] = Json.format[DeletePurchaserRequest]
}

case class DeletePurchaserReturn(
                                  deleted: Boolean
                                )

object DeletePurchaserReturn {
  implicit val format: OFormat[DeletePurchaserReturn] = Json.format[DeletePurchaserReturn]
}

case class CreateCompanyDetailsRequest(
                                        stornId: String,
                                        returnResourceRef: String,
                                        purchaserResourceRef: String,
                                        utr: Option[String] = None,
                                        vatReference: Option[String] = None,
                                        compTypeBank: Option[String] = None,
                                        compTypeBuilder: Option[String] = None,
                                        compTypeBuildsoc: Option[String] = None,
                                        compTypeCentgov: Option[String] = None,
                                        compTypeIndividual: Option[String] = None,
                                        compTypeInsurance: Option[String] = None,
                                        compTypeLocalauth: Option[String] = None,
                                        compTypeOcharity: Option[String] = None,
                                        compTypeOcompany: Option[String] = None,
                                        compTypeOfinancial: Option[String] = None,
                                        compTypePartship: Option[String] = None,
                                        compTypeProperty: Option[String] = None,
                                        compTypePubliccorp: Option[String] = None,
                                        compTypeSoletrader: Option[String] = None,
                                        compTypePenfund: Option[String] = None
                                      )

object CreateCompanyDetailsRequest {
  implicit val format: OFormat[CreateCompanyDetailsRequest] = Json.format[CreateCompanyDetailsRequest]
}

case class CreateCompanyDetailsReturn(
                                       companyDetailsId: String
                                     )

object CreateCompanyDetailsReturn {
  implicit val format: OFormat[CreateCompanyDetailsReturn] = Json.format[CreateCompanyDetailsReturn]
}

case class UpdateCompanyDetailsRequest(
                                        stornId: String,
                                        returnResourceRef: String,
                                        purchaserResourceRef: String,
                                        utr: Option[String] = None,
                                        vatReference: Option[String] = None,
                                        compTypeBank: Option[String] = None,
                                        compTypeBuilder: Option[String] = None,
                                        compTypeBuildsoc: Option[String] = None,
                                        compTypeCentgov: Option[String] = None,
                                        compTypeIndividual: Option[String] = None,
                                        compTypeInsurance: Option[String] = None,
                                        compTypeLocalauth: Option[String] = None,
                                        compTypeOcharity: Option[String] = None,
                                        compTypeOcompany: Option[String] = None,
                                        compTypeOfinancial: Option[String] = None,
                                        compTypePartship: Option[String] = None,
                                        compTypeProperty: Option[String] = None,
                                        compTypePubliccorp: Option[String] = None,
                                        compTypeSoletrader: Option[String] = None,
                                        compTypePenfund: Option[String] = None
                                      )

object UpdateCompanyDetailsRequest {
  implicit val format: OFormat[UpdateCompanyDetailsRequest] = Json.format[UpdateCompanyDetailsRequest]
}

case class UpdateCompanyDetailsReturn(
                                       updated: Boolean
                                     )

object UpdateCompanyDetailsReturn {
  implicit val format: OFormat[UpdateCompanyDetailsReturn] = Json.format[UpdateCompanyDetailsReturn]
}

case class DeleteCompanyDetailsRequest(
                                        storn: String,
                                        returnResourceRef: String
                                      )

object DeleteCompanyDetailsRequest {
  implicit val format: OFormat[DeleteCompanyDetailsRequest] = Json.format[DeleteCompanyDetailsRequest]
}

case class DeleteCompanyDetailsReturn(
                                       deleted: Boolean
                                     )

object DeleteCompanyDetailsReturn {
  implicit val format: OFormat[DeleteCompanyDetailsReturn] = Json.format[DeleteCompanyDetailsReturn]
}
