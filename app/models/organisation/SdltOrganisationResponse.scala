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

package models.organisation

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess, JsValue, Json, OFormat, OWrites, Reads, __}

case class Agent(
                  agentReference : String,
                  name           : String,
                  agentId        : Option[String],
                  houseNumber    : Option[String],
                  addressLine1   : Option[String],
                  addressLine2   : Option[String],
                  addressLine3   : Option[String],
                  addressLine4   : Option[String],
                  postcode       : Option[String],
                  phone          : Option[String],
                  email          : Option[String]
                )

object Agent {
  implicit val reads: Reads[Agent] = (
    (__ \ "agentReference").read[JsValue].map {
      case JsNumber(n) if n.isWhole       => n.toBigInt.toString
      case JsNumber(_)                    => "0"
      case JsString(s) if s.trim.nonEmpty => s.trim
      case _                              => "0"
    } and
      (__ \ "name").read[String] and
      (__ \ "agentId").readNullable[String] and
      (__ \ "houseNumber").readNullable[String] and
      (__ \ "addressLine1").readNullable[String] and
      (__ \ "addressLine2").readNullable[String] and
      (__ \ "addressLine3").readNullable[String] and
      (__ \ "addressLine4").readNullable[String] and
      (__ \ "postcode").readNullable[String] and
      (__ \ "phone").readNullable[String] and
      (__ \ "email").readNullable[String]
    )(Agent.apply _)

  implicit val writes: OWrites[Agent] = OWrites { a =>
    Json.obj(
      "agentReference" -> (if (a.agentReference.trim.nonEmpty) a.agentReference.trim else "0"),
      "name"           -> a.name,
      "agentId"        -> a.agentId,
      "houseNumber"    -> a.houseNumber,
      "addressLine1"   -> a.addressLine1,
      "addressLine2"   -> a.addressLine2,
      "addressLine3"   -> a.addressLine3,
      "addressLine4"   -> a.addressLine4,
      "postcode"       -> a.postcode,
      "phone"          -> a.phone,
      "email"          -> a.email
    )
  }
}

case class SdltOrganisation(
                             storn                   : String,
                             version                 : Int,
                             isReturnUser            : String,
                             doNotDisplayWelcomePage : String,
                             agents                  : Seq[Agent]
                           )

object SdltOrganisation {
  implicit val format: OFormat[SdltOrganisation] = Json.format[SdltOrganisation]
}
