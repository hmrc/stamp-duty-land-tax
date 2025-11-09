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

package uk.gov.hmrc.stampdutylandtax.models.filingSpecs

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import models.filing._

class GetReturnByRefRequestSpec extends AnyWordSpec with Matchers {

  "GetReturnByRefRequest (JSON)" should {

    "read and write a valid object with both fields" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": "100001",
          |  "storn": "STORN12345"
          |}
        """.stripMargin
      )

      val model = json.as[GetReturnByRefRequest]
      model.returnResourceRef mustBe "100001"
      model.storn mustBe "STORN12345"

      Json.toJson(model) mustBe json
    }

    "read and write with numeric returnResourceRef" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": "999999",
          |  "storn": "STORN99999"
          |}
        """.stripMargin
      )

      val model = json.as[GetReturnByRefRequest]
      model.returnResourceRef mustBe "999999"
      model.storn mustBe "STORN99999"

      Json.toJson(model) mustBe json
    }

    "read and write with alphanumeric returnResourceRef" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": "REF-ABC-123",
          |  "storn": "STORN-XYZ-789"
          |}
        """.stripMargin
      )

      val model = json.as[GetReturnByRefRequest]
      model.returnResourceRef mustBe "REF-ABC-123"
      model.storn mustBe "STORN-XYZ-789"

      Json.toJson(model) mustBe json
    }

    "fail to read when returnResourceRef is missing" in {
      val json = Json.parse(
        """
          |{
          |  "storn": "STORN12345"
          |}
        """.stripMargin
      )

      json.validate[GetReturnByRefRequest] mustBe a[JsError]
    }

    "fail to read when storn is missing" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": "100001"
          |}
        """.stripMargin
      )

      json.validate[GetReturnByRefRequest] mustBe a[JsError]
    }

    "fail to read when both fields are missing" in {
      val json = Json.parse(
        """
          |{
          |}
        """.stripMargin
      )

      json.validate[GetReturnByRefRequest] mustBe a[JsError]
    }

    "fail to read when returnResourceRef is null" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": null,
          |  "storn": "STORN12345"
          |}
        """.stripMargin
      )

      json.validate[GetReturnByRefRequest] mustBe a[JsError]
    }

    "fail to read when storn is null" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": "100001",
          |  "storn": null
          |}
        """.stripMargin
      )

      json.validate[GetReturnByRefRequest] mustBe a[JsError]
    }

    "fail to read when returnResourceRef is not a string" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": 12345,
          |  "storn": "STORN12345"
          |}
        """.stripMargin
      )

      json.validate[GetReturnByRefRequest] mustBe a[JsError]
    }

    "fail to read when storn is not a string" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": "100001",
          |  "storn": 12345
          |}
        """.stripMargin
      )

      json.validate[GetReturnByRefRequest] mustBe a[JsError]
    }

    "handle empty strings for both fields (though validation may reject elsewhere)" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": "",
          |  "storn": ""
          |}
        """.stripMargin
      )

      val result = json.validate[GetReturnByRefRequest]
      result mustBe a[JsSuccess[_]]
      val model  = result.get
      model.returnResourceRef mustBe ""
      model.storn mustBe ""
    }

    "handle very long strings" in {
      val longRef   = "R" * 500
      val longStorn = "S" * 500
      val json      = Json.parse(
        s"""
           |{
           |  "returnResourceRef": "$longRef",
           |  "storn": "$longStorn"
           |}
        """.stripMargin
      )

      val model = json.as[GetReturnByRefRequest]
      model.returnResourceRef mustBe longRef
      model.storn mustBe longStorn
    }

    "handle special characters in fields" in {
      val json = Json.parse(
        """
          |{
          |  "returnResourceRef": "REF-123_ABC.v2",
          |  "storn": "STORN/2025-01"
          |}
        """.stripMargin
      )

      val model = json.as[GetReturnByRefRequest]
      model.returnResourceRef mustBe "REF-123_ABC.v2"
      model.storn mustBe "STORN/2025-01"
    }

    "round-trip serialize and deserialize preserving all data" in {
      val original = GetReturnByRefRequest(
        returnResourceRef = "100999",
        storn = "STORN88888"
      )

      val json         = Json.toJson(original)
      val deserialized = json.as[GetReturnByRefRequest]

      deserialized mustBe original
    }
  }
}
