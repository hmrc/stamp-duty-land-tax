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

package models.purge

import play.api.libs.json.{Json, OFormat, OWrites}

import java.time.LocalDate

case class GetReturnsForPurgeRequest(purgeDate: LocalDate)
object GetReturnsForPurgeRequest {
  implicit val writes: OWrites[GetReturnsForPurgeRequest] = Json.writes[GetReturnsForPurgeRequest]
}

case class ReturnForPurge(storn: String, returnResourceRef: String, status: String)
object ReturnForPurge {
  implicit val format: OFormat[ReturnForPurge] = Json.format[ReturnForPurge]
}

case class ReturnsForPurgeResponse(returnsForPurge: List[ReturnForPurge])
object ReturnsForPurgeResponse {
  implicit val format: OFormat[ReturnsForPurgeResponse] = Json.format[ReturnsForPurgeResponse]
}

case class DeleteReturnRequest(storn: String, returnResourceRef: String)
object DeleteReturnRequest {
  implicit val writes: OWrites[DeleteReturnRequest] = Json.writes[DeleteReturnRequest]
}

case class DeleteReturnResponse(deleted: Boolean)
object DeleteReturnResponse {
  implicit val format: OFormat[DeleteReturnResponse] = Json.format[DeleteReturnResponse]
}
