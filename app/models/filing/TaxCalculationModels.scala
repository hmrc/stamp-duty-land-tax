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

package models.filing

import play.api.libs.json.{Json, OFormat}

case class UpdateTaxCalculationRequest(
  stornId: String,
  returnResourceRef: String,
  amountPaid: Option[String] = None,
  includesPenalty: Option[String] = None,
  taxDue: Option[String] = None,
  calcPenaltyDue: Option[String] = None,
  calcTaxDue: Option[String] = None,
  calcTaxRate1: Option[String] = None,
  calcTaxRate2: Option[String] = None,
  calcTotalTaxPenaltyDue: Option[String] = None,
  calcTotalNpvTax: Option[String] = None,
  calcTotalPremiumTax: Option[String] = None,
  taxDuePremium: Option[String] = None,
  taxDueNpv: Option[String] = None,
  honestyDeclaration: Option[String] = None
)

object UpdateTaxCalculationRequest {
  implicit val format: OFormat[UpdateTaxCalculationRequest] = Json.format[UpdateTaxCalculationRequest]
}

case class UpdateTaxCalculationReturn(
  updated: Boolean
)

object UpdateTaxCalculationReturn {
  implicit val format: OFormat[UpdateTaxCalculationReturn] = Json.format[UpdateTaxCalculationReturn]
}
