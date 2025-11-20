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

case class CreateReturnAgentRequest(
                                     stornId: String,
                                     returnResourceRef: String,
                                     agentType: String,
                                     name: String,
                                     houseNumber: Option[String] = None,
                                     addressLine1: String,
                                     addressLine2: Option[String] = None,
                                     addressLine3: Option[String] = None,
                                     addressLine4: Option[String] = None,
                                     postcode: String,
                                     phoneNumber: Option[String] = None,
                                     email: Option[String] = None,
                                     agentReference: Option[String] = None,
                                     isAuthorised: Option[String] = None
                                   )

object CreateReturnAgentRequest {
  implicit val format: OFormat[CreateReturnAgentRequest] = Json.format[CreateReturnAgentRequest]
}

case class CreateReturnAgentReturn(
                                    returnAgentID: String
                                  )

object CreateReturnAgentReturn {
  implicit val format: OFormat[CreateReturnAgentReturn] = Json.format[CreateReturnAgentReturn]
}


case class UpdateReturnAgentRequest(
                                     stornId: String,
                                     returnResourceRef: String,
                                     agentType: String,
                                     name: String,
                                     houseNumber: Option[String] = None,
                                     addressLine1: String,
                                     addressLine2: Option[String] = None,
                                     addressLine3: Option[String] = None,
                                     addressLine4: Option[String] = None,
                                     postcode: String,
                                     phoneNumber: Option[String] = None,
                                     email: Option[String] = None,
                                     agentReference: Option[String] = None,
                                     isAuthorised: Option[String] = None
                                   )

object UpdateReturnAgentRequest {
  implicit val format: OFormat[UpdateReturnAgentRequest] = Json.format[UpdateReturnAgentRequest]
}

case class UpdateReturnAgentReturn(
                                    updated: Boolean
                                  )

object UpdateReturnAgentReturn {
  implicit val format: OFormat[UpdateReturnAgentReturn] = Json.format[UpdateReturnAgentReturn]
}

case class DeleteReturnAgentRequest(
                                     storn: String,
                                     returnResourceRef: String,
                                     agentType: String
                                   )

object DeleteReturnAgentRequest {
  implicit val format: OFormat[DeleteReturnAgentRequest] = Json.format[DeleteReturnAgentRequest]
}

case class DeleteReturnAgentReturn(
                                    deleted: Boolean
                                  )

object DeleteReturnAgentReturn {
  implicit val format: OFormat[DeleteReturnAgentReturn] = Json.format[DeleteReturnAgentReturn]
}
