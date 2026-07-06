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

class LockReturnModelsSpec extends AnyWordSpec with Matchers {

  "LockReturnRequest" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"storn":"STORN-1",
           |"returnResourceRef":"RRR-1",
           |"version":2
           |}
           |""".stripMargin)

      val model = json.as[LockReturnRequest]
      model.storn mustBe "STORN-1"
      model.returnResourceRef mustBe "RRR-1"
      model.version mustBe 2

      Json.toJson(model).as[LockReturnRequest] mustBe model
    }
  }

  "LockReturnResponse" should {
    "read and write fully populated" in {
      val json = Json.parse(
        s"""
           |{
           |"success": true
           |}
           |""".stripMargin)

      val model = json.as[LockReturnResponse]
      model.success mustBe true

      Json.toJson(model).as[LockReturnResponse] mustBe model
    }
  }

}