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

package uk.gov.hmrc.stampdutylandtax.models.manage

import models.manage.ReturnSummary
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.LocalDate

class ReturnSummarySpec extends AnyWordSpec with Matchers {

  "ReturnSummarySpec" should {
    "read and write fully populated with all optional and mandatory fields " in {
      val json = Json.parse(
        s"""
           |{
           |"returnReference":"123456",
           |"utrn":"UTRN123",
           |"status": "IN-PROGRESS",
           |"dateSubmitted":"2026-01-02",
           |"purchaserName":"John Doe",
           |"address":"SW1XLE",
           |"agentReference":"REF12345"
           |}
           |""".stripMargin)

      val model = json.as[ReturnSummary]
      model.returnReference mustBe "123456"
      model.utrn mustBe Some("UTRN123")
      model.status mustBe Some("IN-PROGRESS")
      model.dateSubmitted mustBe Some(LocalDate.parse("2026-01-02"))
      model.purchaserName mustBe "John Doe"
      model.address mustBe "SW1XLE"
      model.agentReference mustBe Some("REF12345")
    }

    "read and write fully populated with all mandatory fields only" in {
      val json = Json.parse(
        s"""
           |{
           |"returnReference": "123456",
           |"status":"IN-PROGRESS",
           |"purchaserName":"John Doe",
           |"address":"SW1XLE"
           |}
           |""".stripMargin)

      val model = json.as[ReturnSummary]
      model.returnReference mustBe "123456"
      model.utrn mustBe None
      model.status mustBe Some("IN-PROGRESS")
      model.dateSubmitted mustBe None
      model.purchaserName mustBe "John Doe"
      model.address mustBe "SW1XLE"
      model.agentReference mustBe None

    }
  }

}
