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

package uk.gov.hmrc.stampdutylandtax.models.agent

import models.agent.CreatePredefinedAgentRequest
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class CreatePredefinedAgentRequestSpec extends AnyWordSpec with Matchers {

  "CreatePredefinedAgentRequest (JSON)" should {
    "read and write a fully-populated with all optional and mandatory fields" in {
      val json = Json.parse (
        """
          |{
          | "storn": "STORN12345",
          | "agentName": "123456",
          | "addressLine1":"High Street",
          | "addressLine2":"Kensington",
          | "addressLine3":"London",
          | "addressLine4":"Greater London",
          | "postcode":"SW1A 1AA",
          | "phone":"1233456",
          | "email":"hello@email.com"
          |}
          |""".stripMargin
      )

      val model = json.as[CreatePredefinedAgentRequest]
      model.storn mustBe "STORN12345"
      model.agentName mustBe "123456"
      model.addressLine1 mustBe Some("High Street")
      model.addressLine2 mustBe Some("Kensington")
      model.addressLine3 mustBe Some("London")
      model.addressLine4 mustBe Some("Greater London")
      model.postcode mustBe Some("SW1A 1AA")
      model.phone mustBe Some("1233456")
      model.email mustBe Some("hello@email.com")
    }
    "read and write a partially-populated with all mandatory fields " in {
      val json = Json.parse (
        """
          |{
          | "storn": "STORN12345",
          | "agentName": "John Snow"
          |}
          |""".stripMargin
      )

      val model = json.as[CreatePredefinedAgentRequest]
      model.storn mustBe "STORN12345"
      model.agentName mustBe "John Snow"
      model.addressLine1 mustBe None
      model.addressLine2 mustBe None
      model.addressLine3 mustBe None
      model.addressLine4 mustBe None
      model.postcode mustBe None
      model.phone mustBe None
      model.email mustBe None
    }
  }

}
