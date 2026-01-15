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

package itutil

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.{Application, Environment, Mode}

trait ApplicationWithWiremock
  extends AnyWordSpec
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  lazy val wireMock = new WireMock

  val extraConfig: Map[String, Any] = {
    Map[String, Any](
      "microservice.services.auth.host"                -> WireMockConstants.stubHost,
      "microservice.services.auth.port"                -> WireMockConstants.stubPort,
      "microservice.services.chris.host"               -> WireMockConstants.stubHost,
      "microservice.services.chris.port"               -> WireMockConstants.stubPort,
      "microservice.services.rds-datacache-proxy.host" -> WireMockConstants.stubHost,
      "microservice.services.rds-datacache-proxy.port" -> WireMockConstants.stubPort,
      "microservice.services.formp-proxy.host"         -> WireMockConstants.stubHost,
      "microservice.services.formp-proxy.port"         -> WireMockConstants.stubPort,
      "microservice.services.stamp-duty-land-tax-stub.port" -> WireMockConstants.stubPort,
      "microservice.services.stamp-duty-land-tax-stub.host" -> WireMockConstants.stubHost
    )
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(extraConfig)
    .build()

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  override protected def beforeAll(): Unit =
    wireMock.start()
    super.beforeAll()

  override def beforeEach(): Unit =
    wireMock.resetAll()
    super.beforeEach()

  override def afterAll(): Unit = {
    wireMock.stop()
    super.afterAll()
  }

  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  private val postAuthoriseUrl = "/auth/authorise"

  private val allEnrolmentsJson: JsObject = Json.obj(
    "allEnrolments" ->
      Json.obj(
        "key" -> "IR-SDLT-ORG",
        "identifiers" -> Seq(
          Json.obj(
            "key" -> "NINO",
            "value" -> ""
          )
        ),
        "state" -> "Activated",
        "confidenceLevel" -> 200
      )

  )

  private val authoriseBodyWithOrgEnrolmentsRetrieval: String =
    """{
      |  "authorise": [{"confidenceLevel": 200}],
      |  "retrieve": ["allEnrolments"],
      |  "credId": "credId",
      |  "individualEnrolments":{"sa":"1111111111"},
      |  "allEnrolments": [
      |               {
      |                 "key":"IR-SDLT-ORG",
      |                 "identifiers": [
      |                    {
      |                       "key":"IR-SDLT-ORG",
      |                       "value": "value"
      |                    }
      |                   ],
      |                "state": "Activated"
      |              }
      |  ],
      |  "affinityGroup": "Organisation",
      |  "credentialRole": "User",
      |  "internalId": "internalId"
      |}""".stripMargin



  def stubAuthorised(): Unit = {
    stubPost(postAuthoriseUrl, Status.OK, authoriseBodyWithOrgEnrolmentsRetrieval )
  }

  def stubUnauthorised(): Unit = {
    stubPost(postAuthoriseUrl, Status.UNAUTHORIZED, "{}")
  }

}