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

import base.BaseSpec
import itutil.ApplicationWithWiremock
import models.filing.*
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.http.Status.{CREATED, FORBIDDEN}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class VendorReturnsControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val createVendorUrl = s"$servicePrefix/filing/create/vendor"
  lazy val updateVendorUrl = s"$servicePrefix/filing/update/vendor"
  lazy val deleteVendorUrl = s"$servicePrefix/filing/delete/vendor "

  def stubCreateVendorResponse(): Unit = {
    stubPost("/formp-proxy/filing/create/vendor", Status.CREATED,
      Json.toJson(CreateVendorReturn(vendorResourceRef = "ref", vendorId = "vendorId")).toString)
  }

  def stubUpdateVendorResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/vendor", Status.CREATED,
      Json.toJson(UpdateVendorReturn(updated = true)).toString)
  }

  def stubDeleteVendorResponse(): Unit = {
    stubPost("/formp-proxy/filing/delete/vendor", Status.CREATED,
      Json.toJson(DeleteVendorReturn(deleted = true)).toString)
  }

  "VendorReturns" should {

    "call createVendor" when {

      "return a 403:Forbidden:: unauthorised request" in {
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

      "return a 201:Created:: authorised request" in {
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

    "call updateVendor" when {

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdateVendorResponse()
        val jsonBody = Json.toJson(
          UpdateVendorRequest(stornId = "storn", returnResourceRef = "ref",
            title = None, forename1 = None, forename2 = None, name = "name",
            houseNumber = None, addressLine1 = "address1", addressLine2 = None,
            addressLine3 = None, addressLine4 = None,
            postcode = None, isRepresentedByAgent = "isRep", vendorResourceRef = "ref",
            nextVendorId = None))

        val result = wsClient.url(updateVendorUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 201:Created:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdateVendorResponse()
        val jsonBody = Json.toJson(
          UpdateVendorRequest(stornId = "storn", returnResourceRef = "ref",
            title = None, forename1 = None, forename2 = None, name = "name",
            houseNumber = None, addressLine1 = "address1", addressLine2 = None,
            addressLine3 = None, addressLine4 = None,
            postcode = None, isRepresentedByAgent = "isRep", vendorResourceRef = "ref",
            nextVendorId = None))

        val result = wsClient.url(updateVendorUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

    }

    "call deleteVendor" when {

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubDeleteVendorResponse()
        val jsonBody = Json.toJson(
          DeleteVendorRequest(storn = "storn", vendorResourceRef = "ref", returnResourceRef = "ref"))

        val result = wsClient.url(deleteVendorUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 201:Created:: authorised request" in {
        stubAuthorisedAsActivated()
        stubDeleteVendorResponse()
        val jsonBody = Json.toJson(
          DeleteVendorRequest(storn = "storn", vendorResourceRef = "ref", returnResourceRef = "ref"))

        val result = wsClient.url(deleteVendorUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

    }

  }

}