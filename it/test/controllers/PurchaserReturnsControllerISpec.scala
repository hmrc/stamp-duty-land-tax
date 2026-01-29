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
import models.filing.{CreateCompanyDetailsRequest, CreateCompanyDetailsReturn, CreatePurchaserRequest, CreatePurchaserReturn, DeleteCompanyDetailsRequest, DeleteCompanyDetailsReturn, DeletePurchaserRequest, DeletePurchaserReturn, UpdateCompanyDetailsRequest, UpdateCompanyDetailsReturn, UpdatePurchaserRequest, UpdatePurchaserReturn}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.http.Status.{FORBIDDEN, CREATED}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class PurchaserReturnsControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val deletePurchaser = s"$servicePrefix/filing/delete/purchaser"
  lazy val createPurchaser = s"$servicePrefix/filing/create/purchaser"
  lazy val updatePurchaser = s"$servicePrefix/filing/update/purchaser"
  lazy val updateCompanyDetails = s"$servicePrefix/filing/update/company-details"
  lazy val createCompanyDetails = s"$servicePrefix/filing/create/company-details"
  lazy val deleteCompanyDetails = s"$servicePrefix/filing/delete/company-details"


  def stubDeletePurchaserResponse(): Unit = {
    stubPost("/formp-proxy/filing/delete/purchaser", Status.OK,
      Json.toJson(DeletePurchaserReturn(deleted = true)).toString)
  }

  def stubCreatePurchaserResponse(): Unit = {
    stubPost("/formp-proxy/filing/create/purchaser", Status.OK,
      Json.toJson(
        CreatePurchaserReturn(
          purchaserResourceRef = "purchaseRef", purchaserId = "purchaseId"
        )
      ).toString)
  }

  def stubUpdatePurchaserResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/purchaser", Status.OK,
      Json.toJson(
        UpdatePurchaserReturn(
          updated = true
        )
      ).toString)
  }

  def stubUpdateCompanyDetailsResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/company-details", Status.OK,
      Json.toJson(UpdateCompanyDetailsReturn(updated = true)).toString)
  }

  def stubCreateCompanyDetailsResponse(): Unit = {
    stubPost("/formp-proxy/filing/create/company-details", Status.OK,
      Json.toJson(
        CreateCompanyDetailsReturn(
          companyDetailsId = "companyId"
        )
      ).toString)
  }

  def stubDeleteCompanyDetailsResponse(): Unit = {
    stubPost("/formp-proxy/filing/delete/company-details", Status.OK,
      Json.toJson(
        DeleteCompanyDetailsReturn(
          deleted = true
        )
      ).toString)
  }

  "Purchaser Returns" should {

    "call DeletePurchaser" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubDeletePurchaserResponse()
        val jsonBody = Json.toJson(DeletePurchaserRequest(storn = "storn", purchaserResourceRef = "purchaseRef", returnResourceRef = "returnRef"))
        val result = wsClient.url(deletePurchaser)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 201:CREATED:: authorised request with not yet activated enrollment" in {
        stubAuthorisedAsNotYetActivated()
        stubDeletePurchaserResponse()
        val jsonBody = Json.toJson(DeletePurchaserRequest(storn = "storn", purchaserResourceRef = "purchaseRef", returnResourceRef = "returnRef"))
        val result = wsClient.url(deletePurchaser)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubDeletePurchaserResponse()
        val jsonBody = Json.toJson(DeletePurchaserRequest(storn = "storn", purchaserResourceRef = "purchaseRef", returnResourceRef = "returnRef"))
        val result = wsClient.url(deletePurchaser)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

    }

    "call CreatePurchaser" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubCreatePurchaserResponse()
        val jsonBody = Json.toJson(
          CreatePurchaserRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            isCompany = "true",
            isTrustee = "true",
            isConnectedToVendor = "true",
            isRepresentedByAgent = "true",
            address1 = "address1"
          )
        )
        val result = wsClient.url(createPurchaser)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 404:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreatePurchaserResponse()
        val jsonBody = Json.toJson(
          CreatePurchaserRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            isCompany = "true",
            isTrustee = "true",
            isConnectedToVendor = "true",
            isRepresentedByAgent = "true",
            address1 = "address1"
          )
        )
        val result = wsClient.url(createPurchaser)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call UpdatePurchaser" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdatePurchaserResponse()
        val jsonBody = Json.toJson(
          UpdatePurchaserRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            purchaserResourceRef = "purchaserResourceRef",
            isCompany = "true",
            isTrustee = "true",
            isConnectedToVendor = "true",
            isRepresentedByAgent = "true",
            address1 = "address1"
          )
        )
        val result = wsClient.url(updatePurchaser)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdatePurchaserResponse()
        val jsonBody = Json.toJson(
          UpdatePurchaserRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            purchaserResourceRef = "purchaserResourceRef",
            isCompany = "true",
            isTrustee = "true",
            isConnectedToVendor = "true",
            isRepresentedByAgent = "true",
            address1 = "address1"
          )
        )
        val result = wsClient.url(updatePurchaser)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

  }

  "Company Details returns" should {

    "call DeleteCompanyDetails" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubDeleteCompanyDetailsResponse()
        val jsonBody = Json.toJson(DeleteCompanyDetailsRequest(storn = "storn", returnResourceRef = "returnRef"))
        val result = wsClient.url(deleteCompanyDetails)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 201:CREATED:: authorised request with not yet activated enrollment" in {
        stubAuthorisedAsNotYetActivated()
        stubDeleteCompanyDetailsResponse()
        val jsonBody = Json.toJson(DeleteCompanyDetailsRequest(storn = "storn", returnResourceRef = "returnRef"))
        val result = wsClient.url(deleteCompanyDetails)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubDeleteCompanyDetailsResponse()
        val jsonBody = Json.toJson(DeleteCompanyDetailsRequest(storn = "storn", returnResourceRef = "returnRef"))
        val result = wsClient.url(deleteCompanyDetails)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

    }

    "call CreateCompanyDetails" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubCreateCompanyDetailsResponse()
        val jsonBody = Json.toJson(
          CreateCompanyDetailsRequest(
            stornId = "stornId",
            returnResourceRef = "returnResourceRef",
            purchaserResourceRef = "purchaserResourceRef"
          )
        )
        val result = wsClient.url(createCompanyDetails)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 404:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreateCompanyDetailsResponse()
        val jsonBody = Json.toJson(
          CreateCompanyDetailsRequest(
            stornId = "stornId",
            returnResourceRef = "returnResourceRef",
            purchaserResourceRef = "purchaserResourceRef"
          )
        )
        val result = wsClient.url(createCompanyDetails)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call UpdateCompanyDetails" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdateCompanyDetailsResponse()
        val jsonBody = Json.toJson(
          UpdateCompanyDetailsRequest(
            stornId = "stornId",
            returnResourceRef = "returnResourceRef",
            purchaserResourceRef = "purchaserResourceRef",
          )
        )
        val result = wsClient.url(updateCompanyDetails)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdateCompanyDetailsResponse()
        val jsonBody = Json.toJson(
          UpdateCompanyDetailsRequest(
            stornId = "stornId",
            returnResourceRef = "returnResourceRef",
            purchaserResourceRef = "purchaserResourceRef",
          )
        )
        val result = wsClient.url(updateCompanyDetails)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }
  }
}