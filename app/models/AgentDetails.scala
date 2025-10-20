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

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OWrites, Reads, Writes, __}

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
  implicit val reads: Reads[AgentDetails] = Json.reads[AgentDetails]
  implicit val writes: OWrites[AgentDetails] = (
      (__ \ "p_storn")          .write[String]         and
      (__ \ "p_name")           .write[String]         and
      (__ \ "p_house_number")   .write[String]         and
      (__ \ "p_address_1")      .write[String]         and
      (__ \ "p_address_2")      .writeNullable[String] and
      (__ \ "p_address_3")      .write[String]         and
      (__ \ "p_address_4")      .writeNullable[String] and
      (__ \ "p_postcode")       .writeNullable[String] and
      (__ \ "p_phone")          .write[String]         and
      (__ \ "p_email")          .write[String]         and
      (__ \ "p_reference")      .write[String]         and
      (__ \ "p_is_authorised")  .write[BigInt]
    ){ a =>
      ( a.storn
      , a.name
      , a.houseNumber
      , a.addressLine1
      , a.addressLine2
      , a.addressLine3
      , a.addressLine4
      , a.postcode
      , a.phoneNumber
      , a.emailAddress
      , a.agentId
      , a.isAuthorised
    )
  }
}
