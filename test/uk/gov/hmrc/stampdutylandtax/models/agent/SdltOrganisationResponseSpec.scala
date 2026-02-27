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

import models.agent.{CreatedAgent, SdltOrganisationResponse}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class SdltOrganisationResponseSpec extends AnyWordSpec with Matchers {

  "SdltOrganisationResponse (JSON)" should {
    "read and write a fully-populated with all optional fields" in {
      val json = Json.parse (
        """
          |{
          | "storn": "STORN12345",
          | "version": "123456",
          | "isReturnUser": "John Snow",
          | "doNotDisplayWelcomePage": "12345",
          | "agents": [
          | {
          | "storn": "STORN001",
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
          | "dxAddress":"12345",
          | "agentResourceReference":"agentReference"
          |},
          | {
          | "storn": "STORN003",
          | "agentId": "123456",
          | "name": "TestUserName",
          | "houseNumber": "12345",
          | "address1":"High Street",
          | "address2":"Kensington",
          | "address3":"London",
          | "address4":"Greater London",
          | "postcode":"MW12 1AA",
          | "phone":"1233456",
          | "email":"hello@email2.com",
          | "dxAddress":"12345",
          | "agentResourceReference":"agentReference"
          | }
          | ]
          |}
          |""".stripMargin
      )

      val model = json.as[SdltOrganisationResponse]
      model.storn mustBe "STORN12345"
      model.version mustBe Some("123456")
      model.isReturnUser mustBe Some("John Snow")
      model.doNotDisplayWelcomePage mustBe Some("12345")
      model.agents.size mustBe  2
    }
    "read and write a partially-populated with mandatory fields" in {
      val json = Json.parse(
        """
          |{
          | "storn": "STORN12345",
          | "agents":[
          | {
          | "storn": "STORN001",
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
          | "dxAddress":"12345",
          | "agentResourceReference":"agentReference"
          |   }
          | ]
          |}
          |""".stripMargin
      )

      val model = json.as[SdltOrganisationResponse]
      model.storn mustBe "STORN12345"
      model.version mustBe None
      model.isReturnUser mustBe None
      model.agents.size mustBe 1
    }
  }

}
