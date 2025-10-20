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

package models

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Json, Reads, Writes, __}

case class AgentDetails(
                         storn        : String,
                         name         : String,
                         houseNumber  : String,
                         addressLine1 : String,
                         addressLine2 : Option[String],
                         addressLine3 : String,
                         addressLine4 : Option[String],
                         postcode     : Option[String],
                         phoneNumber  : String,
                         emailAddress : String,
                         agentId      : String,
                         isAuthorised : BigInt
                       )

object AgentDetails {
  implicit val writes: Writes[AgentDetails] = Json.writes[AgentDetails]
  implicit val reads: Reads[AgentDetails] = (
      (__ \ "p_storn")          .read[String]         and
      (__ \ "p_name")           .read[String]         and
      (__ \ "p_house_number")   .read[String]         and
      (__ \ "p_address_1")      .read[String]         and
      (__ \ "p_address_2")      .readNullable[String] and
      (__ \ "p_address_3")      .read[String]         and
      (__ \ "p_address_4")      .readNullable[String] and
      (__ \ "p_postcode")       .readNullable[String] and
      (__ \ "p_phone")          .read[String]         and
      (__ \ "p_email")          .read[String]         and
      (__ \ "p_reference")      .read[String]         and
      (__ \ "p_is_authorised")  .read[BigInt]
    )(AgentDetails.apply _)
}
