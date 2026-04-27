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

case class CreateLandRequest(
    stornId: String,
    returnResourceRef: String,
    propertyType: String,
    interestTransferredCreated: String,
    houseNumber: Option[String] = None,
    addressLine1: String,
    addressLine2: Option[String] = None,
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postcode: Option[String] = None,
    landArea: Option[String] = None,
    areaUnit: Option[String] = None,
    localAuthorityNumber: Option[String] = None,
    mineralRights: Option[String] = None,
    nlpgUprn: Option[String] = None,
    willSendPlansByPost: Option[String] = None,
    titleNumber: Option[String] = None
)

object CreateLandRequest {
  implicit val format: OFormat[CreateLandRequest] =
    Json.format[CreateLandRequest]
}

case class CreateLandReturn(
    landResourceRef: String,
    landId: String
)

object CreateLandReturn {
  implicit val format: OFormat[CreateLandReturn] = Json.format[CreateLandReturn]
}

case class UpdateLandRequest(
    stornId: String,
    returnResourceRef: String,
    landResourceRef: String,
    propertyType: String,
    interestTransferredCreated: String,
    houseNumber: Option[String] = None,
    addressLine1: String,
    addressLine2: Option[String] = None,
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postcode: Option[String] = None,
    landArea: Option[String] = None,
    areaUnit: Option[String] = None,
    localAuthorityNumber: Option[String] = None,
    mineralRights: Option[String] = None,
    nlpgUprn: Option[String] = None,
    willSendPlansByPost: Option[String] = None,
    titleNumber: Option[String] = None,
    nextLandId: Option[String] = None
)

object UpdateLandRequest {
  implicit val format: OFormat[UpdateLandRequest] =
    Json.format[UpdateLandRequest]
}

case class UpdateLandReturn(
    updated: Boolean
)

object UpdateLandReturn {
  implicit val format: OFormat[UpdateLandReturn] = Json.format[UpdateLandReturn]
}

case class DeleteLandRequest(
    storn: String,
    returnResourceRef: String,
    landResourceRef: String
)

object DeleteLandRequest {
  implicit val format: OFormat[DeleteLandRequest] =
    Json.format[DeleteLandRequest]
}

case class DeleteLandReturn(
    deleted: Boolean
)

object DeleteLandReturn {
  implicit val format: OFormat[DeleteLandReturn] = Json.format[DeleteLandReturn]
}
