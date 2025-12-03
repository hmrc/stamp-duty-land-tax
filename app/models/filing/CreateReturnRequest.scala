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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Json, OFormat}
import play.api.libs.json.*

case class CreateReturnRequest(
                                stornId: String,
                                purchaserIsCompany: String,
                                surNameOrCompanyName: String,
                                houseNumber: Option[Int],
                                addressLine1: String,
                                addressLine2: Option[String],
                                addressLine3: Option[String],
                                addressLine4: Option[String],
                                postcode: Option[String],
                                transactionType: String
                              )

object CreateReturnRequest {

  implicit val reads: Reads[CreateReturnRequest] = (
    (JsPath \ "stornId").read[String] and
      (JsPath \ "purchaserIsCompany").read[String].map {
        case "Individual" => "NO"
        case "Company" => "YES"
        case other => other
      } and
      (JsPath \ "surNameOrCompanyName").read[String] and
      (JsPath \ "houseNumber").readNullable[Int] and
      (JsPath \ "addressLine1").read[String] and
      (JsPath \ "addressLine2").readNullable[String] and
      (JsPath \ "addressLine3").readNullable[String] and
      (JsPath \ "addressLine4").readNullable[String] and
      (JsPath \ "postcode").readNullable[String] and
      (JsPath \ "transactionType").read[String].map{
        case "conveyanceTransfer" => "F"
        case "grantOfLease" => "L"
        case "conveyanceTransferLease" => "A"
        case "otherTransaction" => "O"
        case other => other
      }
    )(CreateReturnRequest.apply _)

  implicit val writes: OWrites[CreateReturnRequest] = Json.writes[CreateReturnRequest]

  implicit val format: OFormat[CreateReturnRequest] = OFormat(reads, writes)
}

