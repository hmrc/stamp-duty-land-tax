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

package uk.gov.hmrc.stampdutylandtax.models.filingSpecs

import models.filing.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class ChrisResponseModelsSpec extends AnyWordSpec with Matchers {
  
  private def govTalkError(
                            raisedBy: String = "Department",
                            number: Option[String] = Some("3001"),
                            errorType: String = "business",
                            text: Option[String] = Some("Keys do not match"),
                            location: Option[String] = None
                          ): GovTalkError = GovTalkError(raisedBy, number, errorType, text, location)

  private val departmentalBusiness: GovTalkError =
    govTalkError(raisedBy = "Department", number = Some("3001"), errorType = "business")

  private val gatewayFatal: GovTalkError =
    govTalkError(raisedBy = "Gateway", number = Some("1001"), errorType = "fatal", text = Some("Gateway down"))

  "SubmissionStatus" should {

    "expose the persisted string value for each case" in {
      SubmissionStatus.Pending.value  mustBe "PENDING"
      SubmissionStatus.Accepted.value mustBe "ACCEPTED"
      SubmissionStatus.Errored.value  mustBe "ERROR"
      SubmissionStatus.Failed.value   mustBe "FAILED"
    }

    "resolve fromValue case-insensitively and ignoring surrounding whitespace" in {
      SubmissionStatus.fromValue("pending")     mustBe Some(SubmissionStatus.Pending)
      SubmissionStatus.fromValue("  ACCEPTED ") mustBe Some(SubmissionStatus.Accepted)
      SubmissionStatus.fromValue("Error")       mustBe Some(SubmissionStatus.Errored)
    }

    "return None fromValue for an unknown status" in {
      SubmissionStatus.fromValue("NOPE") mustBe None
    }

    "round-trip through JSON" in {
      SubmissionStatus.values.foreach { s =>
        Json.toJson(s).as[SubmissionStatus] mustBe s
      }
    }

    "write as the persisted string value" in {
      Json.toJson(SubmissionStatus.Errored) mustBe JsString("ERROR")
    }

    "read a valid string value" in {
      JsString("FAILED").as[SubmissionStatus] mustBe SubmissionStatus.Failed
    }

    "fail to read an unknown string value" in {
      JsString("BOGUS").validate[SubmissionStatus] mustBe a[JsError]
    }

    "fail to read a non-string JSON value" in {
      JsNumber(1).validate[SubmissionStatus] mustBe a[JsError]
    }
  }

  "GovTalkError" should {

    "identify the source party case-insensitively" in {
      govTalkError(raisedBy = "GATEWAY").fromGateway       mustBe true
      govTalkError(raisedBy = "gateway").fromDepartment    mustBe false
      govTalkError(raisedBy = "Department").fromDepartment mustBe true
    }

    "treat a business type OR a 3001 number as a business error" in {
      govTalkError(errorType = "business", number = None).isBusiness       mustBe true
      govTalkError(errorType = "fatal", number = Some("3001")).isBusiness  mustBe true
      govTalkError(errorType = "fatal", number = Some("3000")).isBusiness  mustBe false
    }

    "identify fatal errors case-insensitively" in {
      govTalkError(errorType = "FATAL").isFatal    mustBe true
      govTalkError(errorType = "business").isFatal mustBe false
    }

    "round-trip through JSON" in {
      val e = govTalkError(location = Some("/IRenvelope[1]"))
      Json.toJson(e).as[GovTalkError] mustBe e
    }
  }

  "ChrisResponse.toStatus" should {

    "map a Completed with a UTRN to Accepted" in {
      ChrisResponse.Completed(Some("123456789MA"), Some("MARK"), Some("cid"), Some("url"), "<x/>").toStatus mustBe SubmissionStatus.Accepted
    }

    "map a Completed without a UTRN to Failed" in {
      ChrisResponse.Completed(None, Some("MARK"), Some("cid"), Some("url"), "<x/>").toStatus mustBe SubmissionStatus.Failed
    }

    "map an Acknowledged to Pending" in {
      ChrisResponse.Acknowledged(Some("cid"), Some(10), Some("url"), "<x/>").toStatus mustBe SubmissionStatus.Pending
    }

    "map an Errored with a business reject to Errored" in {
      ChrisResponse.Errored(Seq(departmentalBusiness), Some("cid"),Some("url"), "<x/>").toStatus mustBe SubmissionStatus.Errored
    }

    "map an Errored without a business reject to Failed" in {
      ChrisResponse.Errored(Seq(gatewayFatal), Some("cid"), Some("url"), "<x/>").toStatus mustBe SubmissionStatus.Failed
    }

    "map a TransportError to Failed" in {
      ChrisResponse.TransportError("boom").toStatus mustBe SubmissionStatus.Failed
    }
  }

  "ChrisResponse.Errored" should {

    "report a business reject when any error is a business error" in {
      ChrisResponse.Errored(Seq(gatewayFatal, departmentalBusiness), None,Some("url"), "<x/>").isBusinessReject mustBe true
    }

    "report no business reject when no error qualifies" in {
      ChrisResponse.Errored(Seq(gatewayFatal), None,Some("url"), "<x/>").isBusinessReject mustBe false
    }

    "return only located errors as fieldErrors when at least one has a Location" in {
      val located   = govTalkError(location = Some("/IRenvelope[1]/IRheader[1]"))
      val unlocated = gatewayFatal
      val errored   = ChrisResponse.Errored(Seq(located, unlocated), None,Some("url"), "<x/>")
      errored.fieldErrors mustBe Seq(located)
    }

    "fall back to all errors as fieldErrors when none has a Location" in {
      val errors  = Seq(gatewayFatal, departmentalBusiness)
      val errored = ChrisResponse.Errored(errors, None,Some("url"), "<x/>")
      errored.fieldErrors mustBe errors
    }
  }

  "ChrisResponse.TransportError" should {
    "default rawXml to a transport-error marker" in {
      ChrisResponse.TransportError("boom").rawXml mustBe "<transport-error/>"
    }
  }

  "SubmissionError" should {

    "round-trip through JSON" in {
      val e = SubmissionError(code = Some("3001"), message = "rejected", location = Some("/x"))
      Json.toJson(e).as[SubmissionError] mustBe e
    }

    "build from a GovTalkError, carrying code, text and location" in {
      val se = SubmissionError.fromGovTalk(govTalkError(number = Some("3001"), text = Some("bad"), location = Some("/x")))
      se.code     mustBe Some("3001")
      se.message  mustBe "bad"
      se.location mustBe Some("/x")
    }

    "fall back to a default message when the GovTalkError has no text" in {
      SubmissionError.fromGovTalk(govTalkError(text = None)).message mustBe "Submission rejected"
    }
  }

  "SubmissionResponse" should {

    "round-trip an Accepted through the discriminated JSON format" in {
      val a: SubmissionResponse = SubmissionResponse.Accepted("RRR-1", "123456789MA")
      val json = Json.toJson(a)
      (json \ "_type").as[String] mustBe "accepted"
      json.as[SubmissionResponse] mustBe a
    }

    "round-trip a Rejected through the discriminated JSON format" in {
      val r: SubmissionResponse =
        SubmissionResponse.Rejected("RRR-1", Seq(SubmissionError(Some("3001"), "rejected", Some("/x"))))
      val json = Json.toJson(r)
      (json \ "_type").as[String] mustBe "rejected"
      json.as[SubmissionResponse] mustBe r
    }

    "fail to read an unknown _type discriminator" in {
      Json.obj("_type" -> "mystery").validate[SubmissionResponse] mustBe a[JsError]
    }

    "fail to read when the _type discriminator is missing" in {
      Json.obj("returnId" -> "RRR-1").validate[SubmissionResponse] mustBe a[JsError]
    }
  }

  "PersistedSubmission" should {
    "round-trip through JSON with UTRN and errors present" in {
      val p = PersistedSubmission(
        returnId      = "RRR-1",
        storn         = "STORN-1",
        correlationId = "cid",
        status        = SubmissionStatus.Accepted,
        utrn          = Some("123456789MA"),
        govtalkErrors = Seq(departmentalBusiness)
      )
      Json.toJson(p).as[PersistedSubmission] mustBe p
    }

    "round-trip through JSON with UTRN absent and no errors" in {
      val p = PersistedSubmission("RRR-1", "STORN-1", "cid", SubmissionStatus.Pending, None, Nil)
      Json.toJson(p).as[PersistedSubmission] mustBe p
    }
  }

  "UniversalStatus.fromString" should {

    "resolve every known status case-insensitively" in {
      UniversalStatus.values.foreach { s =>
        UniversalStatus.fromString(s.toString.toLowerCase) mustBe Right(s)
      }
    }

    "reject an unknown status with a descriptive Left" in {
      UniversalStatus.fromString("NONSENSE") mustBe Left("Unable to convert status: NONSENSE")
    }
  }

  "UniversalStatus.fromChrisResponse" should {

    val completed = (received: Option[String]) =>
      ChrisResponse.Completed(Some("123456789MA"), received, Some("cid"),Some("url"), "<x/>")

    "map a Completed with a matching IRmark to SUBMITTED" in {
      UniversalStatus.fromChrisResponse(completed(Some("MARK-1")), Some("MARK-1")) mustBe UniversalStatus.SUBMITTED
    }

    "match the IRmark case-insensitively and ignoring whitespace" in {
      UniversalStatus.fromChrisResponse(completed(Some("  mark-1 ")), Some("MARK-1")) mustBe UniversalStatus.SUBMITTED
    }

    "map a Completed with a mismatched IRmark to SUBMITTED_NO_RECEIPT" in {
      UniversalStatus.fromChrisResponse(completed(Some("OTHER")), Some("MARK-1")) mustBe UniversalStatus.SUBMITTED_NO_RECEIPT
    }

    "map a Completed with no received IRmark to SUBMITTED_NO_RECEIPT" in {
      UniversalStatus.fromChrisResponse(completed(None), Some("MARK-1")) mustBe UniversalStatus.SUBMITTED_NO_RECEIPT
    }

    "map a Completed to SUBMITTED_NO_RECEIPT when we sent no IRmark" in {
      UniversalStatus.fromChrisResponse(completed(Some("MARK-1")), None) mustBe UniversalStatus.SUBMITTED_NO_RECEIPT
    }

    "treat a blank received IRmark as no receipt even if we sent a blank one" in {
      UniversalStatus.fromChrisResponse(completed(Some("   ")), Some("   ")) mustBe UniversalStatus.SUBMITTED_NO_RECEIPT
    }

    "map an Acknowledged to ACCEPTED" in {
      UniversalStatus.fromChrisResponse(ChrisResponse.Acknowledged(Some("cid"), Some(10),Some("url"), "<x/>"), None) mustBe UniversalStatus.ACCEPTED
    }

    "map a Department 3001 business error to DEPARTMENTAL_ERROR" in {
      val resp = ChrisResponse.Errored(Seq(departmentalBusiness), Some("cid"),Some("url"), "<x/>")
      UniversalStatus.fromChrisResponse(resp, None) mustBe UniversalStatus.DEPARTMENTAL_ERROR
    }

    "require raisedBy=Department, number=3001 and type=business together for DEPARTMENTAL_ERROR" in {
      // 3001 but raised by Gateway → not departmental-business → falls through to reset/fatal.
      val notDept = govTalkError(raisedBy = "Gateway", number = Some("3001"), errorType = "business")
      UniversalStatus.fromChrisResponse(ChrisResponse.Errored(Seq(notDept), None,Some("url"), "<x/>"), None) mustBe UniversalStatus.FATAL_ERROR
    }

    "map a resettable error number (1000, 2005, 3000) to STARTED" in {
      Seq("1000", "2005", "3000").foreach { n =>
        val resp = ChrisResponse.Errored(Seq(govTalkError(number = Some(n), errorType = "fatal", raisedBy = "Gateway")), None, Some("url"), "<x/>")
        UniversalStatus.fromChrisResponse(resp, None) mustBe UniversalStatus.STARTED
      }
    }

    "prefer DEPARTMENTAL_ERROR over STARTED when both a 3001 business and a resettable code are present" in {
      val reset = govTalkError(number = Some("2005"), errorType = "fatal", raisedBy = "Gateway")
      val resp  = ChrisResponse.Errored(Seq(reset, departmentalBusiness), None,Some("url"), "<x/>")
      UniversalStatus.fromChrisResponse(resp, None) mustBe UniversalStatus.DEPARTMENTAL_ERROR
    }

    "map any other error to FATAL_ERROR" in {
      val resp = ChrisResponse.Errored(Seq(gatewayFatal), None,Some("url"), "<x/>")
      UniversalStatus.fromChrisResponse(resp, None) mustBe UniversalStatus.FATAL_ERROR
    }

    "map an empty error list to FATAL_ERROR" in {
      UniversalStatus.fromChrisResponse(ChrisResponse.Errored(Nil, None,Some("url"), "<x/>"), None) mustBe UniversalStatus.FATAL_ERROR
    }

    "map a timeout TransportError to STARTED" in {
      UniversalStatus.fromChrisResponse(ChrisResponse.TransportError("Request timeout after 120s"), None) mustBe UniversalStatus.STARTED
      UniversalStatus.fromChrisResponse(ChrisResponse.TransportError("connection timed out"), None) mustBe UniversalStatus.STARTED
    }

    "map a non-timeout TransportError to FATAL_ERROR" in {
      UniversalStatus.fromChrisResponse(ChrisResponse.TransportError("NON-2xx status=500"), None) mustBe UniversalStatus.FATAL_ERROR
    }
  }

}