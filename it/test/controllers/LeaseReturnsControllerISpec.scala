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
import play.api.http.Status.{CREATED, FORBIDDEN, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class LeaseReturnsControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/stamp-duty-land-tax"
  lazy val createLease = s"$servicePrefix/filing/create/lease"
  lazy val updateLease = s"$servicePrefix/filing/update/lease"
  lazy val deleteLease = s"$servicePrefix/filing/delete/lease"

  val leasePayload: LeasePayload = LeasePayload(
    isAnnualRentOver1000 = Some("true"),
    contractEndDate = Some("2026-01-01"),
    contractStartDate = Some("2025-01-01"),
    leaseType = Some("leaseType"),
    netPresentValue = Some("1000"),
    totalPremiumPayable = Some("500"),
    rentFreePeriod = Some("0"),
    startingRent = Some("100"),
    startingRentEndDate = Some("2025-12-31"),
    laterRentKnown = Some("false"),
    vatAmount = Some("20")
  )

  def stubCreateLeaseResponse(): Unit = {
    stubPost("/formp-proxy/filing/create/lease", Status.OK,
      Json.toJson(CreateLeaseReturn(created = true)).toString)
  }

  def stubUpdateLeaseResponse(): Unit = {
    stubPost("/formp-proxy/filing/update/lease", Status.OK,
      Json.toJson(UpdateLeaseReturn(updated = true)).toString)
  }

  def stubDeleteLeaseResponse(): Unit = {
    stubPost("/formp-proxy/filing/delete/lease", Status.OK,
      Json.toJson(DeleteLeaseReturn(deleted = true)).toString)
  }


  "Lease Returns" should {

    "call CreateLease" when {

      "return a 201:CREATED:: authorised request" in {
        stubAuthorisedAsActivated()
        stubCreateLeaseResponse()
        val jsonBody = Json.toJson(
          CreateLeaseRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            lease = leasePayload
          )
        )
        val result = wsClient.url(createLease)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 201:CREATED:: authorised request with not yet activated enrollment" in {
        stubAuthorisedAsNotYetActivated()
        stubCreateLeaseResponse()
        val jsonBody = Json.toJson(
          CreateLeaseRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            lease = leasePayload
          )
        )
        val result = wsClient.url(createLease)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreateLeaseResponse()
        val jsonBody = Json.toJson(
          CreateLeaseRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            lease = leasePayload
          )
        )
        val result = wsClient.url(createLease)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call UpdateLease" when {

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAsActivated()
        stubUpdateLeaseResponse()
        val jsonBody = Json.toJson(
          UpdateLeaseRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            lease = leasePayload
          )
        )
        val result = wsClient.url(updateLease)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubUpdateLeaseResponse()
        val jsonBody = Json.toJson(
          UpdateLeaseRequest(
            stornId = "storn",
            returnResourceRef = "returnResourceRef",
            lease = leasePayload
          )
        )
        val result = wsClient.url(updateLease)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call DeleteLease" when {

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAsActivated()
        stubDeleteLeaseResponse()
        val jsonBody = Json.toJson(
          DeleteLeaseRequest(storn = "storn", returnResourceRef = "returnResourceRef")
        )
        val result = wsClient.url(deleteLease)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 200:OK:: authorised request with not yet activated enrollment" in {
        stubAuthorisedAsNotYetActivated()
        stubDeleteLeaseResponse()
        val jsonBody = Json.toJson(
          DeleteLeaseRequest(storn = "storn", returnResourceRef = "returnResourceRef")
        )
        val result = wsClient.url(deleteLease)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubDeleteLeaseResponse()
        val jsonBody = Json.toJson(
          DeleteLeaseRequest(storn = "storn", returnResourceRef = "returnResourceRef")
        )
        val result = wsClient.url(deleteLease)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

  }

}