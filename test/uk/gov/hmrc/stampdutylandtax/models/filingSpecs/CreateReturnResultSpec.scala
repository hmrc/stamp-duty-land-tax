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

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json, Reads, Writes}
import models.filing._

class CreateReturnResultSpec
    extends AnyFreeSpec
    with Matchers
    with EitherValues {

  "CreateReturnResultSpec" - {

    def validCreateReturnResultJson: JsObject =
      Json.obj("returnResourceRef" -> "12345")

    def inValidCreateReturnResultJson: JsObject =
      Json.obj("returnResourceRef" -> true)

    def validCreateReturnResult = CreateReturnResult("12345")

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[CreateReturnResult]]
      }

      "must deserialize valid JSON" in {
        val result = Json
          .fromJson[CreateReturnResult](validCreateReturnResultJson)
          .asEither
          .value

        result mustBe CreateReturnResult("12345")
      }

      "must fail when field has wrong type" in {
        val result = Json
          .fromJson[CreateReturnResult](inValidCreateReturnResultJson)
          .asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[CreateReturnResult]]
      }

      "must serialize" in {
        val json = Json.toJson(validCreateReturnResult)

        (json \ "returnResourceRef").as[String] mustBe "12345"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val json = Json.toJson(validCreateReturnResult)
        val result = Json.fromJson[CreateReturnResult](json).asEither.value

        result mustEqual validCreateReturnResult
      }
    }
  }
}
