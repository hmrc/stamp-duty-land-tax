package controllers

import base.BaseSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.{FORBIDDEN}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}

class ManageAgentsControllerISpec extends BaseSpec with GuiceOneServerPerSuite {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure()
    .build()

  lazy val calculateUrl = s"http://localhost:$port/stamp-duty-land-tax/manage-agents/get-sdlt-organisation?storn=1001"

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  "Organisation" should {

    "call get-sdlt-organisation" when {

      "return a 404:Forbidden when no auth in scope" in {
        def request: WSResponse = ws.url(calculateUrl).get()

        request.status shouldBe FORBIDDEN
      }
    }
  }
}
