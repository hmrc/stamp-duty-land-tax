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

case class LeasePayload(
  isAnnualRentOver1000: Option[String],
  contractEndDate: Option[String],
  contractStartDate: Option[String],
  leaseType: Option[String],
  netPresentValue: Option[String],
  totalPremiumPayable: Option[String],
  rentFreePeriod: Option[String],
  startingRent: Option[String],
  startingRentEndDate: Option[String],
  laterRentKnown: Option[String],
  vatAmount: Option[String]
)

object LeasePayload {
  implicit val format: OFormat[LeasePayload] = Json.format[LeasePayload]
}

case class CreateLeaseRequest(
  stornId: String,
  returnResourceRef: String,
  lease: LeasePayload
)

object CreateLeaseRequest {
  implicit val format: OFormat[CreateLeaseRequest] = Json.format[CreateLeaseRequest]
}

case class CreateLeaseReturn(created: Boolean)

object CreateLeaseReturn {
  implicit val format: OFormat[CreateLeaseReturn] = Json.format[CreateLeaseReturn]
}

case class UpdateLeaseRequest(
  stornId: String,
  returnResourceRef: String,
  lease: LeasePayload
)

object UpdateLeaseRequest {
  implicit val format: OFormat[UpdateLeaseRequest] = Json.format[UpdateLeaseRequest]
}

case class UpdateLeaseReturn(updated: Boolean)

object UpdateLeaseReturn {
  implicit val format: OFormat[UpdateLeaseReturn] = Json.format[UpdateLeaseReturn]
}

case class DeleteLeaseRequest(
  storn: String,
  returnResourceRef: String
)

object DeleteLeaseRequest {
  implicit val format: OFormat[DeleteLeaseRequest] = Json.format[DeleteLeaseRequest]
}

case class DeleteLeaseReturn(deleted: Boolean)

object DeleteLeaseReturn {
  implicit val format: OFormat[DeleteLeaseReturn] = Json.format[DeleteLeaseReturn]
}
