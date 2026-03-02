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

import models.agent.CreatedAgent
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class CreatedAgentSpec extends AnyWordSpec with Matchers {

  "CreatedAgent (JSON)" should {
    "read and write a fully-populated when all fields are present " in {
    val json = Json.parse (
        """
          |{
          | "storn": "STORN12345",
          | "agentId": "123456",
          | "name": "John Snow",
          | "houseNumber": "12345",
          | "address1":"High Street",
          | "address2":"Kensington",
          | "address3":"London",
          | "address4":"Greater London",
          | "postcode":"SW1A 1AA",
          | "phone":"1233456",
          | "email":"hello@email.com",
          | "dxAddress":"DX12345",
          | "agentResourceReference":"agentReference"
          |}
          |""".stripMargin
      )

      val model = json.as[CreatedAgent]
      model.storn mustBe Some("STORN12345")
      model.agentId mustBe Some("123456")
      model.name mustBe Some("John Snow")
      model.houseNumber mustBe Some("12345")
      model.address1 mustBe Some("High Street")
      model.address2 mustBe Some("Kensington")
      model.address3 mustBe Some("London")
      model.address4 mustBe Some("Greater London")
      model.postcode mustBe Some("SW1A 1AA")
      model.phone mustBe Some("1233456")
      model.email mustBe Some("hello@email.com")
      model.dxAddress mustBe Some("DX12345")
      model.agentResourceReference mustBe Some("agentReference")

    }
    "read and write a partially-populated with mandatory fields" in {
      val json = Json.parse (
        """
          |{
          | "storn": "STORN12345",
          | "agentId": "123456",
          | "name": "John Snow",
          | "houseNumber": "12345"
          |}
          |""".stripMargin
      )

      val model = json.as[CreatedAgent]
      model.storn mustBe Some("STORN12345")
      model.agentId mustBe Some("123456")
      model.name mustBe Some("John Snow")
      model.houseNumber mustBe Some("12345")
      model.address1 mustBe None
      model.address2 mustBe None
      model.address3 mustBe None
      model.address4 mustBe None
      model.postcode mustBe None
      model.phone mustBe None
      model.email mustBe None
      model.dxAddress mustBe None
      model.agentResourceReference mustBe None
    }
  }

}
