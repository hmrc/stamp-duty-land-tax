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

import models.agent.UpdatePredefinedAgentResponse
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class UpdatePredefinedAgentResponseSpec extends AnyWordSpec with Matchers {

  "UpdatePredefinedAgentResponse (JSON)" should {
    "read and write a fully-populated with all mandatory fields" in {
      val json = Json.parse(
        """
          |{
          | "updated": true
          |}
          |""".stripMargin
      )

      val model = json.as[UpdatePredefinedAgentResponse]
      model.updated mustBe true
    }
  }

}
