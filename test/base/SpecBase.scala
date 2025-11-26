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

import models.agent.*
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
import uk.gov.hmrc.stampdutylandtax.controllers.actions.FakeIdentifierAction

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
      .build()

  val testAgentDetailsRequest: CreatePredefinedAgentRequest = CreatePredefinedAgentRequest(
    storn = "STN001",
    agentName = "64Acme Property Agents Ltd",
    addressLine1 = Some("Zoo Lane"),
    addressLine2 = Some("Westminster"),
    addressLine3 = Some("London"),
    addressLine4 = None,
    postcode = Some("SW1A 2AA"),
    phone = Some("02079460000"),
    email = Some("test@example.com")
  )

  val testAgentDetailsAfterCreation: CreatedAgent = CreatedAgent(
    agentId                = Some("AGT001"),
    storn                  = Some("STN001"),
    name                   = Some("64 Acme Property Agents Ltd"),
    houseNumber            = None,
    address1               = Some("Zoo Lane"),
    address2               = Some("Westminster"),
    address3               = Some("London"),
    address4               = None,
    postcode               = Some("SW1A 2AA"),
    phone                  = Some("02079460000"),
    email                  = Some("test@example.com"),
    dxAddress              = None,
    agentResourceReference = Some("ARN001")
  )

  val testAgentDetailsList: List[CreatedAgent] = List(
    CreatedAgent(
      agentId                = Some("AGT001"),
      storn                  = Some("STN001"),
      name                   = Some("64 Acme Property Agents Ltd"),
      houseNumber            = None,
      address1               = Some("Zoo Lane"),
      address2               = Some("Westminster"),
      address3               = Some("London"),
      address4               = None,
      postcode               = Some("SW1A 2AA"),
      phone                  = Some("02079460000"),
      email                  = Some("test@example.com"),
      dxAddress              = None,
      agentResourceReference = Some("ARN001")
    ),
    CreatedAgent(
      agentId                = Some("AGT001"),
      storn                  = Some("STN001"),
      name                   = Some("12B BrightHomes Estates"),
      houseNumber            = None,
      address1               = Some("Maple Street"),
      address2               = Some("Camden"),
      address3               = Some("London"),
      address4               = Some("Greater London"),
      postcode               = Some("NW1 5LE"),
      phone                  = Some("02071234567"),
      email                  = Some("info@brighthomes.co.uk"),
      dxAddress              = None,
      agentResourceReference = Some("ARN001")
    )
  )

  val testAgentDetailsSuccessResponse: CreatePredefinedAgentResponse = CreatePredefinedAgentResponse(
    agentResourceRef = "some-id",
    agentId = "4567"
  )


  val testDeletePredefinedAgentRequest: DeletePredefinedAgentRequest = DeletePredefinedAgentRequest(
    storn = "STN001",
    agentReferenceNumber = "100001"
  )

  //Update Agent Details Fixtures
  val testUpdateAgentDetailsRequest: UpdateAgentDetailsRequest = UpdateAgentDetailsRequest(
    agentReferenceNumber = "ARN001",
    storn = "STN001",
    name = "64 Acme Property Agents Ltd",
    houseNumber = None,
    addressLine1 = Some("Zoo Lane"),
    addressLine2 = Some("Westminster"),
    addressLine3 = Some("London"),
    addressLine4 = None,
    postcode = Some("SW1A 2AA"),
    phone = Some("02079460000"),
    email = Some("test@example.com")
  )

  val cc: ControllerComponents = stubControllerComponents()
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val bodyParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  val fakeIdentifierAction: FakeIdentifierAction = app.injector.instanceOf[FakeIdentifierAction]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = cc.executionContext

}