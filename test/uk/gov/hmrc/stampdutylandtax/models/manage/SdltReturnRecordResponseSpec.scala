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

import models.manage.SdltReturnRecordResponse
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class SdltReturnRecordResponseSpec extends AnyWordSpec with Matchers {

  "SdltReturnRecordResponse" should {
    "return and write with all fields " in {
      val json = Json.parse(s"""
           |{
           |"returnSummaryCount": 2,
           |"returnSummaryList": [
           |{
           |"returnReference":"123457",
           |"utrn":"UTRN123",
           |"status": "IN-PROGRESS",
           |"dateSubmitted":"2026-01-03",
           |"purchaserName":"John Doe1",
           |"address":"SW1XLE",
           |"agentReference":"REF12345"
           |},
           |{
           |"returnReference":"12356",
           |"utrn":"UTRN123",
           |"status": "DUE-FOR-DELETION",
           |"dateSubmitted":"2026-01-02",
           |"purchaserName":"John Doe",
           |"address":"SW1XNE",
           |"agentReference":"REF008"
           |}
           |]
           |}
           |""".stripMargin)

      val model = json.as[SdltReturnRecordResponse]

      model.returnSummaryCount mustBe Some(2)
      model.returnSummaryList.size mustBe 2
      model.returnSummaryList.head.returnReference mustBe "123457"
      model.returnSummaryList(1).returnReference mustBe "12356"

    }
  }

}
