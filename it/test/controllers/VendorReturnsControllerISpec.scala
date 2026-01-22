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

package controllers

//class  {

import base.BaseSpec
import itutil.ApplicationWithWiremock
import models.filing.{CreateVendorRequest, CreateVendorReturn, ReturnVersionUpdateRequest, ReturnVersionUpdateReturn}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.http.Status.{CREATED, FORBIDDEN}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class VendorReturnsControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val createVendorUrl = s"$servicePrefix/filing/create/vendor"

  def stubCreateVendorResponse(): Unit = {
    stubPost("/formp-proxy/filing/create/vendor", Status.CREATED,
      Json.toJson(CreateVendorReturn(vendorResourceRef = "ref", vendorId = "vendorId")).toString)
  }

  "VendorReturns" should {

    "call createVendor" when {

      "return a 403:Forbidden:: authorised request" in {
        stubUnauthorised()
        stubCreateVendorResponse()
        val jsonBody = Json.toJson(
          CreateVendorRequest(stornId = "storn", returnResourceRef = "ref",
            title = None, forename1 = None, forename2 = None, name = "name",
            houseNumber = None, addressLine1 = "address1", addressLine2 = None,
            addressLine3 = None, addressLine4 = None,
            postcode = None, isRepresentedByAgent = "isRep"))

        val result = wsClient.url(createVendorUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAsActivated()
        stubCreateVendorResponse()
        val jsonBody = Json.toJson(
          CreateVendorRequest(stornId = "storn", returnResourceRef = "ref",
            title = None, forename1 = None, forename2 = None, name = "name",
            houseNumber = None, addressLine1 = "address1", addressLine2 = None,
            addressLine3 = None, addressLine4 = None,
            postcode = None, isRepresentedByAgent = "isRep"))

        val result = wsClient.url(createVendorUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

    }
  }

}