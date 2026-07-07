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

package uk.gov.hmrc.stampdutylandtax.models.submission

import models.submission._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class CreateSubmissionModelsSpec extends AnyWordSpec with Matchers {

  "CreateSubmissionRequest" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"storn":"STORN-1",
           |"returnResourceRef":"RRR-1",
           |"email":"user@example.com"
           |}
           |""".stripMargin)

      val model = json.as[CreateSubmissionRequest]
      model.storn mustBe "STORN-1"
      model.returnResourceRef mustBe "RRR-1"
      model.email mustBe Some("user@example.com")

      Json.toJson(model).as[CreateSubmissionRequest] mustBe model
    }
  }

  "SubmissionUpdate" should {
    "read and write fully populated with all optional fields present" in {
      val json = Json.parse(
        s"""
           |{
           |"IRMarkRecieved":"MARK-IN",
           |"utrn":"UTRN123",
           |"email":"user@example.com",
           |"submissionRequestDate":"2026-01-02T09:00:00Z",
           |"acceptedDate":"2026-01-02T09:05:00Z",
           |"submittableStatus":"SUBMITTABLE",
           |"govTalkErrorCode":"1000",
           |"govTalkErrorType":"business",
           |"govTalkErrorMessage":"Something went wrong",
           |"IRMarkSent":"MARK-OUT"
           |}
           |""".stripMargin)

      val model = json.as[SubmissionUpdate]
      model.IRMarkRecieved mustBe Some("MARK-IN")
      model.utrn mustBe Some("UTRN123")
      model.email mustBe Some("user@example.com")
      model.submissionRequestDate mustBe Some("2026-01-02T09:00:00Z")
      model.acceptedDate mustBe Some("2026-01-02T09:05:00Z")
      model.submittableStatus mustBe Some("SUBMITTABLE")
      model.govTalkErrorCode mustBe Some("1000")
      model.govTalkErrorType mustBe Some("business")
      model.govTalkErrorMessage mustBe Some("Something went wrong")
      model.IRMarkSent mustBe Some("MARK-OUT")

      Json.toJson(model).as[SubmissionUpdate] mustBe model
    }

    "read and write with all optional fields absent" in {
      val json = Json.parse("{}")

      val model = json.as[SubmissionUpdate]
      model.IRMarkRecieved mustBe None
      model.utrn mustBe None
      model.email mustBe None
      model.submissionRequestDate mustBe None
      model.acceptedDate mustBe None
      model.submittableStatus mustBe None
      model.govTalkErrorCode mustBe None
      model.govTalkErrorType mustBe None
      model.govTalkErrorMessage mustBe None
      model.IRMarkSent mustBe None

      Json.toJson(model) mustBe Json.obj()
      Json.toJson(model).as[SubmissionUpdate] mustBe model
    }
  }

  "UpdateSubmissionRequest" should {
    "read and write fully populated with all nested optional fields present" in {
      val json = Json.parse(
        s"""
           |{
           |"storn":"STORN-1",
           |"returnResourceRef":"RRR-1",
           |"submission":{
           |  "IRMarkRecieved":"MARK-IN",
           |  "utrn":"UTRN123",
           |  "email":"user@example.com",
           |  "submissionRequestDate":"2026-01-02T09:00:00Z",
           |  "acceptedDate":"2026-01-02T09:05:00Z",
           |  "submittableStatus":"SUBMITTABLE",
           |  "govTalkErrorCode":"1000",
           |  "govTalkErrorType":"business",
           |  "govTalkErrorMessage":"Something went wrong",
           |  "IRMarkSent":"MARK-OUT"
           |}
           |}
           |""".stripMargin)

      val model = json.as[UpdateSubmissionRequest]
      model.storn mustBe "STORN-1"
      model.returnResourceRef mustBe "RRR-1"
      model.submission.IRMarkRecieved mustBe Some("MARK-IN")
      model.submission.utrn mustBe Some("UTRN123")
      model.submission.email mustBe Some("user@example.com")
      model.submission.submissionRequestDate mustBe Some("2026-01-02T09:00:00Z")
      model.submission.acceptedDate mustBe Some("2026-01-02T09:05:00Z")
      model.submission.submittableStatus mustBe Some("SUBMITTABLE")
      model.submission.govTalkErrorCode mustBe Some("1000")
      model.submission.govTalkErrorType mustBe Some("business")
      model.submission.govTalkErrorMessage mustBe Some("Something went wrong")
      model.submission.IRMarkSent mustBe Some("MARK-OUT")

      Json.toJson(model).as[UpdateSubmissionRequest] mustBe model
    }

    "read and write with the nested submission empty" in {
      val json = Json.parse(
        s"""
           |{
           |"storn":"STORN-1",
           |"returnResourceRef":"RRR-1",
           |"submission":{}
           |}
           |""".stripMargin)

      val model = json.as[UpdateSubmissionRequest]
      model.storn mustBe "STORN-1"
      model.returnResourceRef mustBe "RRR-1"
      model.submission.IRMarkRecieved mustBe None
      model.submission.utrn mustBe None
      model.submission.email mustBe None
      model.submission.submissionRequestDate mustBe None
      model.submission.acceptedDate mustBe None
      model.submission.submittableStatus mustBe None
      model.submission.govTalkErrorCode mustBe None
      model.submission.govTalkErrorType mustBe None
      model.submission.govTalkErrorMessage mustBe None
      model.submission.IRMarkSent mustBe None

      Json.toJson(model).as[UpdateSubmissionRequest] mustBe model
    }
  }

  "SubmissionErrorDetail" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"position":"1",
           |"errorMessage":"Invalid value"
           |}
           |""".stripMargin)

      val model = json.as[SubmissionErrorDetail]
      model.position mustBe "1"
      model.errorMessage mustBe "Invalid value"

      Json.toJson(model).as[SubmissionErrorDetail] mustBe model
    }
  }

  "CreateSubmissionErrorDetailRequest" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"storn":"STORN-1",
           |"returnResourceRef":"RRR-1",
           |"submissionErrorDetails":{
           |  "position":"1",
           |  "errorMessage":"Invalid value"
           |}
           |}
           |""".stripMargin)

      val model = json.as[CreateSubmissionErrorDetailRequest]
      model.storn mustBe "STORN-1"
      model.returnResourceRef mustBe "RRR-1"
      model.submissionErrorDetails.position mustBe "1"
      model.submissionErrorDetails.errorMessage mustBe "Invalid value"

      Json.toJson(model).as[CreateSubmissionErrorDetailRequest] mustBe model
    }
  }

  "DeleteSubmissionErrorDetailRequest" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"storn":"STORN-1",
           |"returnResourceRef":"RRR-1"
           |}
           |""".stripMargin)

      val model = json.as[DeleteSubmissionErrorDetailRequest]
      model.storn mustBe "STORN-1"
      model.returnResourceRef mustBe "RRR-1"

      Json.toJson(model).as[DeleteSubmissionErrorDetailRequest] mustBe model
    }
  }

  "CreateSubmissionReturn" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"success": true
           |}
           |""".stripMargin)

      val model = json.as[CreateSubmissionReturn]
      model.success mustBe true

      Json.toJson(model).as[CreateSubmissionReturn] mustBe model
    }
  }

  "UpdateSubmissionReturn" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"success": true
           |}
           |""".stripMargin)

      val model = json.as[UpdateSubmissionReturn]
      model.success mustBe true

      Json.toJson(model).as[UpdateSubmissionReturn] mustBe model
    }
  }

  "CreateSubmissionErrorDetailReturn" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"success": true
           |}
           |""".stripMargin)

      val model = json.as[CreateSubmissionErrorDetailReturn]
      model.success mustBe true

      Json.toJson(model).as[CreateSubmissionErrorDetailReturn] mustBe model
    }
  }

  "DeleteSubmissionErrorDetailReturn" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"success": true
           |}
           |""".stripMargin)

      val model = json.as[DeleteSubmissionErrorDetailReturn]
      model.success mustBe true

      Json.toJson(model).as[DeleteSubmissionErrorDetailReturn] mustBe model
    }
  }

}