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

package models.submission

import play.api.libs.json.{Json, OFormat}

case class CreateSubmissionRequest(
                                    storn: String,
                                    returnResourceRef: String,
                                    email: String
                                  )

object CreateSubmissionRequest {
  implicit val format: OFormat[CreateSubmissionRequest] = Json.format[CreateSubmissionRequest]
}

case class SubmissionUpdate(
                             IRMarkRecieved: Option[String],
                             utrn: Option[String],
                             email: Option[String],
                             submissionRequestDate: Option[String],
                             acceptedDate: Option[String],
                             submittableStatus: Option[String],
                             govTalkErrorCode: Option[String],
                             govTalkErrorType: Option[String],
                             govTalkErrorMessage: Option[String],
                             IRMarkSent: Option[String]
                           )

object SubmissionUpdate {
  implicit val format: OFormat[SubmissionUpdate] = Json.format[SubmissionUpdate]

}


case class UpdateSubmissionRequest(
                                    storn: String,
                                    returnResourceRef: String,
                                    submission: SubmissionUpdate
                                  )

object UpdateSubmissionRequest {
  implicit val format: OFormat[UpdateSubmissionRequest] = Json.format[UpdateSubmissionRequest]
}

case class SubmissionErrorDetail(
                                  position: String,
                                  errorMessage: String
                                )

object SubmissionErrorDetail {
  implicit val format: OFormat[SubmissionErrorDetail] = Json.format[SubmissionErrorDetail]
}

case class CreateSubmissionErrorDetailRequest(
                                               storn: String,
                                               returnResourceRef: String,
                                               submissionErrorDetails: SubmissionErrorDetail
                                             )

object CreateSubmissionErrorDetailRequest {
  implicit val format: OFormat[CreateSubmissionErrorDetailRequest] = Json.format[CreateSubmissionErrorDetailRequest]
}

case class DeleteSubmissionErrorDetailRequest(
                                               storn: String,
                                               returnResourceRef: String
                                             )

object DeleteSubmissionErrorDetailRequest {
  implicit val format: OFormat[DeleteSubmissionErrorDetailRequest] = Json.format[DeleteSubmissionErrorDetailRequest]
}

case class CreateSubmissionReturn(success: Boolean)

object CreateSubmissionReturn {
  implicit val format: OFormat[CreateSubmissionReturn] = Json.format[CreateSubmissionReturn]
}

case class UpdateSubmissionReturn(success: Boolean)

object UpdateSubmissionReturn {
  implicit val format: OFormat[UpdateSubmissionReturn] = Json.format[UpdateSubmissionReturn]
}

case class CreateSubmissionErrorDetailReturn(success: Boolean)

object CreateSubmissionErrorDetailReturn {
  implicit val format: OFormat[CreateSubmissionErrorDetailReturn] = Json.format[CreateSubmissionErrorDetailReturn]
}

case class DeleteSubmissionErrorDetailReturn(success: Boolean)

object DeleteSubmissionErrorDetailReturn {
  implicit val format: OFormat[DeleteSubmissionErrorDetailReturn] = Json.format[DeleteSubmissionErrorDetailReturn]
}

