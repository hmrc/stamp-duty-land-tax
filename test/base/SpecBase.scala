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

package base

import models.agent.{AgentDetailsRequest, AgentDetailsResponse, SubmitAgentDetailsResponse}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, PlayBodyParsers}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.stampdutylandtax.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import play.api.inject.bind

import scala.concurrent.ExecutionContext

trait   SpecBase
  extends AnyFreeSpec
    with Matchers
    with DefaultAwaitTimeout
    with ScalaFutures
    with FakeApplicationFactory
    with BaseOneAppPerSuite
    with MockitoSugar 
    with BeforeAndAfterEach{

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host" -> "localhost",
        "microservice.services.auth.port" -> 11111,
        "microservice.services.rds-datacache-proxy.host" -> "localhost",
        "microservice.services.rds-datacache-proxy.port" -> 11111
      )
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction] // TODO: is that required at all ???
      )
      .build()


  val testAgentDetailsRequest: AgentDetailsRequest = AgentDetailsRequest(
    agentName = "64Acme Property Agents Ltd",
    addressLine1 = "Zoo Lane",
    addressLine2 = Some("Westminster"),
    addressLine3 = "London",
    addressLine4 = None,
    postcode = Some("SW1A 2AA"),
    phone = Some("02079460000"),
    email = "test@example.com"
  )

  val testAgentDetailsAfterCreation: AgentDetailsResponse = AgentDetailsResponse(
    agentReferenceNumber = "ARN001",
    agentName = "64 Acme Property Agents Ltd",
    agentId = Some("AGT001"),
    addressLine1 = Some("Zoo Lane"),
    addressLine2 = Some("Westminster"),
    addressLine3 = Some("London"),
    addressLine4 = None,
    postcode = Some("SW1A 2AA"),
    phone = Some("02079460000"),
    email = Some("test@example.com")
  )

  val testAgentDetailsList: List[AgentDetailsResponse] = List(
    AgentDetailsResponse(
      agentReferenceNumber = "ARN001",
      agentName = "64 Acme Property Agents Ltd",
      agentId = Some("AGT001"),
      addressLine1 = Some("Zoo Lane"),
      addressLine2 = Some("Westminster"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      postcode = Some("SW1A 2AA"),
      phone = Some("02079460000"),
      email = Some("test@example.com")
    ),
    AgentDetailsResponse(
      agentReferenceNumber = "ARN001",
      agentName = "12B BrightHomes Estates",
      agentId = Some("AGT001"),
      addressLine1 = Some("Maple Street"),
      addressLine2 = Some("Camden"),
      addressLine3 = Some("London"),
      addressLine4 = Some("Greater London"),
      postcode = Some("NW1 5LE"),
      phone = Some("02071234567"),
      email = Some("info@brighthomes.co.uk")
    )
  )

  val testAgentDetailsSuccessResponse: SubmitAgentDetailsResponse = SubmitAgentDetailsResponse(
    agentResourceRef = "some-id"
  )

  val cc: ControllerComponents = stubControllerComponents()
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val bodyParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  val fakeIdentifierAction: FakeIdentifierAction = app.injector.instanceOf[FakeIdentifierAction]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = cc.executionContext

}