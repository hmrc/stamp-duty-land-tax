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

package uk.gov.hmrc.stampdutylandtax.models.filingSpecs

import models.filing.CreateReturnRequest
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import models.filing._

class CreateReturnRequestSpec extends AnyWordSpec with Matchers {

  "CreateReturnRequest (JSON)" should {

    "read and write a fully-populated object with all optional fields" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN12345",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "Smith",
          |  "houseNumber": 42,
          |  "addressLine1": "High Street",
          |  "addressLine2": "Kensington",
          |  "addressLine3": "London",
          |  "addressLine4": "Greater London",
          |  "postcode": "SW1A 1AA",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.stornId mustBe "STORN12345"
      model.purchaserIsCompany mustBe "NO"
      model.surNameOrCompanyName mustBe "Smith"
      model.houseNumber mustBe Some(42)
      model.addressLine1 mustBe "High Street"
      model.addressLine2 mustBe Some("Kensington")
      model.addressLine3 mustBe Some("London")
      model.addressLine4 mustBe Some("Greater London")
      model.postcode mustBe Some("SW1A 1AA")
      model.transactionType mustBe "O"

    }

    "read and write an object with minimal fields (no optional values)" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN99999",
          |  "purchaserIsCompany": "Company",
          |  "surNameOrCompanyName": "ABC Property Ltd",
          |  "addressLine1": "Business Park",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.stornId mustBe "STORN99999"
      model.purchaserIsCompany mustBe "YES"
      model.surNameOrCompanyName mustBe "ABC Property Ltd"
      model.houseNumber mustBe None
      model.addressLine1 mustBe "Business Park"
      model.addressLine2 mustBe None
      model.addressLine3 mustBe None
      model.addressLine4 mustBe None
      model.postcode mustBe None
      model.transactionType mustBe "O"

    }

    "handle conversion for Individual to N" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN99999",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "ABC Property Ltd",
          |  "addressLine1": "Business Park",
          |  "transactionType": "otherTransaction"
          |}
            """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.purchaserIsCompany mustBe "NO"
    }

    "handle conversion for Company to Y" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN99999",
          |  "purchaserIsCompany": "Company",
          |  "surNameOrCompanyName": "ABC Property Ltd",
          |  "addressLine1": "Business Park",
          |  "transactionType": "otherTransaction"
          |}
                """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.purchaserIsCompany mustBe "YES"
    }

    "handle conversion for Transaction Type conveyanceTransfer to F" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN99999",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "ABC Property Ltd",
          |  "addressLine1": "Business Park",
          |  "transactionType": "conveyanceTransfer"
          |}
                    """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.transactionType mustBe "F"
    }

    "handle conversion for Transaction Type grantOfLease to L" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN99999",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "ABC Property Ltd",
          |  "addressLine1": "Business Park",
          |  "transactionType": "grantOfLease"
          |}
                        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.transactionType mustBe "L"
    }

    "handle conversion for Transaction Type conveyanceTransferLease to A" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN99999",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "ABC Property Ltd",
          |  "addressLine1": "Business Park",
          |  "transactionType": "conveyanceTransferLease"
          |}
                            """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.transactionType mustBe "A"
    }

    "handle conversion for Transaction Type otherTransaction to O" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN99999",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "ABC Property Ltd",
          |  "addressLine1": "Business Park",
          |  "transactionType": "otherTransaction"
          |}
                                """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.transactionType mustBe "O"
    }

    "read object with explicit null values for optional fields" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN88888",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "Johnson",
          |  "houseNumber": null,
          |  "addressLine1": "Oak Street",
          |  "addressLine2": null,
          |  "addressLine3": null,
          |  "addressLine4": null,
          |  "postcode": null,
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.stornId mustBe "STORN88888"
      model.houseNumber mustBe None
      model.addressLine2 mustBe None
      model.addressLine3 mustBe None
      model.addressLine4 mustBe None
      model.postcode mustBe None
    }

    "write object with None optional fields as missing JSON fields (not null)" in {
      val model = CreateReturnRequest(
        stornId = "STORN77777",
        purchaserIsCompany = "Company",
        surNameOrCompanyName = "Test Corp",
        houseNumber = None,
        addressLine1 = "Main Road",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        transactionType = "NON_otherTransaction"
      )

      val json = Json.toJson(model)
      (json \ "stornId").as[String] mustBe "STORN77777"
      (json \ "houseNumber").toOption mustBe None
      (json \ "addressLine2").toOption mustBe None
      (json \ "addressLine3").toOption mustBe None
      (json \ "addressLine4").toOption mustBe None
      (json \ "postcode").toOption mustBe None
    }

    "fail to read when stornId is missing" in {
      val json = Json.parse(
        """
          |{
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "Smith",
          |  "addressLine1": "High Street",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      json.validate[CreateReturnRequest] mustBe a[JsError]
    }

    "fail to read when purchaserIsCompany is missing" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN12345",
          |  "surNameOrCompanyName": "Smith",
          |  "addressLine1": "High Street",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      json.validate[CreateReturnRequest] mustBe a[JsError]
    }

    "fail to read when surNameOrCompanyName is missing" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN12345",
          |  "purchaserIsCompany": "Individual",
          |  "addressLine1": "High Street",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      json.validate[CreateReturnRequest] mustBe a[JsError]
    }

    "fail to read when addressLine1 is missing" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN12345",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "Smith",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      json.validate[CreateReturnRequest] mustBe a[JsError]
    }

    "fail to read when transactionType is missing" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN12345",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "Smith",
          |  "addressLine1": "High Street"
          |}
        """.stripMargin
      )

      json.validate[CreateReturnRequest] mustBe a[JsError]
    }

    "fail to read when houseNumber is not a number" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN12345",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "Smith",
          |  "houseNumber": "not-a-number",
          |  "addressLine1": "High Street",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      json.validate[CreateReturnRequest] mustBe a[JsError]
    }

    "successfully read with company purchaser" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN55555",
          |  "purchaserIsCompany": "Company",
          |  "surNameOrCompanyName": "Global Property Holdings Ltd",
          |  "houseNumber": 250,
          |  "addressLine1": "Corporate Drive",
          |  "addressLine2": "Business Quarter",
          |  "addressLine3": "Birmingham",
          |  "postcode": "B1 1AA",
          |  "transactionType": "NON_otherTransaction"
          |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.purchaserIsCompany mustBe "YES"
      model.surNameOrCompanyName mustBe "Global Property Holdings Ltd"
      model.transactionType mustBe "NON_otherTransaction"
    }

    "successfully read with individual purchaser" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN44444",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "Williams",
          |  "houseNumber": 7,
          |  "addressLine1": "Elm Avenue",
          |  "addressLine2": "Cambridge",
          |  "postcode": "CB1 1AA",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.purchaserIsCompany mustBe "NO"
      model.surNameOrCompanyName mustBe "Williams"
      model.transactionType mustBe "O"
    }

    "handle very long address lines" in {
      val longAddress = "A" * 200
      val json = Json.parse(
        s"""
           |{
           |  "stornId": "STORN33333",
           |  "purchaserIsCompany": "Individual",
           |  "surNameOrCompanyName": "LongAddressPerson",
           |  "addressLine1": "$longAddress",
           |  "addressLine2": "$longAddress",
           |  "addressLine3": "$longAddress",
           |  "transactionType": "otherTransaction"
           |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.addressLine1 mustBe longAddress
      model.addressLine2 mustBe Some(longAddress)
      model.addressLine3 mustBe Some(longAddress)
    }

    "handle zero as a valid house number" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN22222",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "ZeroHouse",
          |  "houseNumber": 0,
          |  "addressLine1": "Unnamed Building",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.houseNumber mustBe Some(0)
    }

    "handle negative house number" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN11111",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "NegativeHouse",
          |  "houseNumber": -1,
          |  "addressLine1": "Basement Flat",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.houseNumber mustBe Some(-1)
    }

    "handle special characters in string fields" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "STORN-123-ABC",
          |  "purchaserIsCompany": "Individual",
          |  "surNameOrCompanyName": "O'Brien-Smith & Sons",
          |  "houseNumber": 42,
          |  "addressLine1": "High St. (North)",
          |  "addressLine2": "St. Mary's District",
          |  "postcode": "SW1A 1AA",
          |  "transactionType": "otherTransaction"
          |}
        """.stripMargin
      )

      val model = json.as[CreateReturnRequest]
      model.stornId mustBe "STORN-123-ABC"
      model.surNameOrCompanyName mustBe "O'Brien-Smith & Sons"
      model.addressLine1 mustBe "High St. (North)"
      model.addressLine2 mustBe Some("St. Mary's District")
    }

    "handle empty strings in required fields (though validation may reject these elsewhere)" in {
      val json = Json.parse(
        """
          |{
          |  "stornId": "",
          |  "purchaserIsCompany": "",
          |  "surNameOrCompanyName": "",
          |  "addressLine1": "",
          |  "transactionType": ""
          |}
        """.stripMargin
      )

      val result = json.validate[CreateReturnRequest]
      result mustBe a[JsSuccess[_]]
      val model = result.get
      model.stornId mustBe ""
      model.purchaserIsCompany mustBe ""
      model.surNameOrCompanyName mustBe ""
      model.addressLine1 mustBe ""
      model.transactionType mustBe ""
    }
  }
}
