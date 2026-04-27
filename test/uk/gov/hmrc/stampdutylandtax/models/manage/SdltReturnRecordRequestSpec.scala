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

import models.manage.SdltReturnRecordRequest
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class SdltReturnRecordRequestSpec extends AnyWordSpec with Matchers {
  "SdltReturnRecordRequest" should {
    "read and write fully populated with all optional and mandatory fields " in {
      val json = Json.parse(s"""
           |{
           |"storn":"123456",
           |"status": "IN-PROGRESS",
           |"deletionFlag":false,
           |"pageType":"testValue1",
           |"pageNumber":"1"
           |}
           |""".stripMargin)

      val model = json.as[SdltReturnRecordRequest]
      model.storn mustBe "123456"
      model.status mustBe Some("IN-PROGRESS")
      model.deletionFlag mustBe false
      model.pageType mustBe Some("testValue1")
      model.pageNumber mustBe Some("1")
    }

    "read and write fully populated with all mandatory fields only" in {
      val json = Json.parse(s"""
           |{
           |"storn":"123456",
           |"deletionFlag":false
           |}
           |""".stripMargin)

      val model = json.as[SdltReturnRecordRequest]
      model.storn mustBe "123456"
      model.status mustBe None
      model.deletionFlag mustBe false
      model.pageType mustBe None
      model.pageNumber mustBe None

    }
  }

}
