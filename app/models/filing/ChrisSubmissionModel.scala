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

import play.api.libs.json.*

enum ChrisDeleteResponse:
  case Deleted(correlationId: Option[String], raw: String)
  case NotFound(correlationId: Option[String], raw: String)
  case Errored(errors: Seq[GovTalkError], correlationId: Option[String], raw: String)
  case TransportError(message: String, raw: String = "")

enum SubmissionStatus(val value: String):
  case Pending  extends SubmissionStatus("PENDING")
  case Accepted extends SubmissionStatus("ACCEPTED")
  case Errored  extends SubmissionStatus("ERROR")
  case Failed   extends SubmissionStatus("FAILED")

object SubmissionStatus:
  def fromValue(s: String): Option[SubmissionStatus] =
    values.find(_.value.equalsIgnoreCase(s.trim))

  given Format[SubmissionStatus] = new Format[SubmissionStatus]:
    def reads(json: JsValue): JsResult[SubmissionStatus] = json match
      case JsString(s) =>
        SubmissionStatus.fromValue(s) match
          case Some(value) => JsSuccess(value)
          case None        => JsError(s"unknown SubmissionStatus: $s")
      case other => JsError(s"expected JsString for SubmissionStatus, got $other")
    def writes(s: SubmissionStatus): JsValue = JsString(s.value)


final case class GovTalkError(
                               raisedBy: String,
                               number: Option[String],
                               errorType: String,
                               text: Option[String],
                               location: Option[String]
                             ):
  def fromGateway: Boolean    = raisedBy.equalsIgnoreCase("gateway")
  def fromDepartment: Boolean = raisedBy.equalsIgnoreCase("department")

  def isBusiness: Boolean = errorType.equalsIgnoreCase("business") || number.contains("3001")

  def isFatal: Boolean = errorType.equalsIgnoreCase("fatal")

object GovTalkError:
  given Format[GovTalkError] = Json.format[GovTalkError]

sealed trait ChrisResponse:
  def rawXml: String

  def toStatus: SubmissionStatus = this match
    case ChrisResponse.Completed(Some(_), _, _, _, _) => SubmissionStatus.Accepted
    case ChrisResponse.Completed(None, _, _, _, _)    => SubmissionStatus.Failed
    case _: ChrisResponse.Acknowledged                => SubmissionStatus.Pending
    case e: ChrisResponse.Errored                     =>
      if e.isBusinessReject then SubmissionStatus.Errored else SubmissionStatus.Failed
    case _: ChrisResponse.TransportError              => SubmissionStatus.Failed

object ChrisResponse:
  final case class Completed(
                              utrn: Option[String],
                              receivedIrMark: Option[String],
                              correlationId: Option[String],
                              responseEndPoint: Option[String],
                              rawXml: String
                            ) extends ChrisResponse

  final case class Errored(
                            errors: Seq[GovTalkError],
                            correlationId: Option[String],
                            responseEndPoint: Option[String],
                            rawXml: String
                          ) extends ChrisResponse:

    def isBusinessReject: Boolean = errors.exists(_.isBusiness)

    def fieldErrors: Seq[GovTalkError] =
      val located = errors.filter(_.location.isDefined)
      if located.nonEmpty then located else errors

  final case class Acknowledged(
                                 correlationId: Option[String],
                                 pollIntervalSeconds: Option[Int],
                                 responseEndPoint: Option[String],
                                 rawXml: String
                               ) extends ChrisResponse

  final case class TransportError(message: String, rawXml: String = "<transport-error/>") extends ChrisResponse


final case class SubmissionError(
                                  code: Option[String],
                                  message: String,
                                  location: Option[String] = None
                                )

object SubmissionError:
  given Format[SubmissionError] = Json.format[SubmissionError]

  def fromGovTalk(e: GovTalkError): SubmissionError =
    SubmissionError(
      code     = e.number,
      message  = e.text.getOrElse("Submission rejected"),
      location = e.location
    )

sealed trait SubmissionResponse:
  def returnId: String

object SubmissionResponse:
  final case class Accepted(returnId: String, utrn: String) extends SubmissionResponse
  final case class Rejected(returnId: String, errors: Seq[SubmissionError]) extends SubmissionResponse

  private val acceptedFormat: OFormat[Accepted] = Json.format[Accepted]
  private val rejectedFormat: OFormat[Rejected] = Json.format[Rejected]

  given Format[SubmissionResponse] = new Format[SubmissionResponse]:
    def reads(json: JsValue): JsResult[SubmissionResponse] =
      (json \ "_type").asOpt[String] match
        case Some("accepted") => acceptedFormat.reads(json)
        case Some("rejected") => rejectedFormat.reads(json)
        case Some(other)      => JsError(s"unknown SubmissionResponse _type: $other")
        case None             => JsError("missing _type discriminator on SubmissionResponse")

    def writes(value: SubmissionResponse): JsValue = value match
      case a: Accepted => acceptedFormat.writes(a) ++ Json.obj("_type" -> "accepted")
      case r: Rejected => rejectedFormat.writes(r) ++ Json.obj("_type" -> "rejected")


final case class PersistedSubmission(
                                      returnId: String,
                                      storn: String,
                                      correlationId: String,
                                      status: SubmissionStatus,
                                      utrn: Option[String],
                                      govtalkErrors: Seq[GovTalkError]
                                    )

object PersistedSubmission:
  given Format[PersistedSubmission] = Json.format[PersistedSubmission]


enum UniversalStatus:
  case STARTED
  case VALIDATED
  case PENDING
  case ACCEPTED
  case SUBMITTED
  case SUBMITTED_NO_RECEIPT
  case DEPARTMENTAL_ERROR
  case FATAL_ERROR


object UniversalStatus:

  def fromString(in: String): Either[String, UniversalStatus] =
    in.toUpperCase() match
      case "STARTED"              => Right(UniversalStatus.STARTED)
      case "VALIDATED"            => Right(UniversalStatus.VALIDATED)
      case "ACCEPTED"             => Right(UniversalStatus.ACCEPTED)
      case "PENDING"              => Right(UniversalStatus.PENDING)
      case "SUBMITTED"            => Right(UniversalStatus.SUBMITTED)
      case "SUBMITTED_NO_RECEIPT" => Right(UniversalStatus.SUBMITTED_NO_RECEIPT)
      case "DEPARTMENTAL_ERROR"   => Right(UniversalStatus.DEPARTMENTAL_ERROR)
      case "FATAL_ERROR"          => Right(UniversalStatus.FATAL_ERROR)
      case status                 => Left(s"Unable to convert status: $status")

  private val ResetToStartedNumbers: Set[String] = Set("1000", "2005", "3000")

  def fromChrisResponse(resp: ChrisResponse, expectedIrMark: Option[String]): UniversalStatus =
    resp match
      case c: ChrisResponse.Completed =>
        if irMarkMatches(c.receivedIrMark, expectedIrMark) then UniversalStatus.SUBMITTED
        else UniversalStatus.SUBMITTED_NO_RECEIPT

      case _: ChrisResponse.Acknowledged =>
        UniversalStatus.ACCEPTED

      case e: ChrisResponse.Errored =>
        if isDepartmentalBusiness(e.errors) then UniversalStatus.DEPARTMENTAL_ERROR
        else if resetsToStarted(e.errors) then UniversalStatus.STARTED
        else UniversalStatus.FATAL_ERROR

      case t: ChrisResponse.TransportError =>
        if isTimeout(t.message) then UniversalStatus.STARTED
        else UniversalStatus.FATAL_ERROR

  private def irMarkMatches(received: Option[String], expected: Option[String]): Boolean =
    (received, expected) match
      case (Some(r), Some(e)) => r.trim.nonEmpty && r.trim.equalsIgnoreCase(e.trim)
      case _                  => false

  private def isDepartmentalBusiness(errors: Seq[GovTalkError]): Boolean =
    errors.exists { e =>
      e.number.contains("3001") &&
        e.raisedBy.equalsIgnoreCase("department") &&
        e.errorType.equalsIgnoreCase("business")
    }

  private def resetsToStarted(errors: Seq[GovTalkError]): Boolean =
    errors.exists(e => e.number.exists(ResetToStartedNumbers.contains))

  private def isTimeout(message: String): Boolean =
    val m = message.toLowerCase
    m.contains("timeout") || m.contains("timed out")



case class SubmitRequest(
                          email: Option[String] = None,
                          fullReturn: FullReturn
                        )

object SubmitRequest {
  implicit val reads: Reads[SubmitRequest] = Json.reads[SubmitRequest]
}