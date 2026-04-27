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

import models.filing._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

class UpdateReturnModelsSpec extends AnyFreeSpec with Matchers {

  "UpdateReturnRequest" - {

    "must serialize to JSON correctly with Y values" in {
      val request = UpdateReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        mainPurchaserID = Some("1"),
        mainVendorID = Some("1"),
        mainLandID = Some("1"),
        IRMarkGenerated = Some("IRMark123456"),
        landCertForEachProp = Some("YES"),
        declaration = Some("YES")
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "mainPurchaserID").as[String] mustBe "1"
      (json \ "mainVendorID").as[String] mustBe "1"
      (json \ "mainLandID").as[String] mustBe "1"
      (json \ "IRMarkGenerated").as[String] mustBe "IRMark123456"
      (json \ "landCertForEachProp").as[String] mustBe "YES"
      (json \ "declaration").as[String] mustBe "YES"
    }

    "must serialize to JSON correctly with N values" in {
      val request = UpdateReturnRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        mainPurchaserID = Some("5"),
        mainVendorID = Some("3"),
        mainLandID = Some("7"),
        IRMarkGenerated = Some("IRMark999999"),
        landCertForEachProp = Some("NO"),
        declaration = Some("NO")
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "mainPurchaserID").as[String] mustBe "5"
      (json \ "mainVendorID").as[String] mustBe "3"
      (json \ "mainLandID").as[String] mustBe "7"
      (json \ "IRMarkGenerated").as[String] mustBe "IRMark999999"
      (json \ "landCertForEachProp").as[String] mustBe "NO"
      (json \ "declaration").as[String] mustBe "NO"
    }

    "must serialize to JSON correctly with mixed values" in {
      val request = UpdateReturnRequest(
        storn = "STORN88888",
        returnResourceRef = "100003",
        mainPurchaserID = Some("10"),
        mainVendorID = Some("20"),
        mainLandID = Some("30"),
        IRMarkGenerated = Some("IRMark888888"),
        landCertForEachProp = Some("YES"),
        declaration = Some("NO")
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN88888"
      (json \ "returnResourceRef").as[String] mustBe "100003"
      (json \ "mainPurchaserID").as[String] mustBe "10"
      (json \ "mainVendorID").as[String] mustBe "20"
      (json \ "mainLandID").as[String] mustBe "30"
      (json \ "IRMarkGenerated").as[String] mustBe "IRMark888888"
      (json \ "landCertForEachProp").as[String] mustBe "YES"
      (json \ "declaration").as[String] mustBe "NO"
    }

    "must deserialize from JSON correctly with Y values" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "returnResourceRef" -> "100001",
        "mainPurchaserID" -> "1",
        "mainVendorID" -> "1",
        "mainLandID" -> "1",
        "IRMarkGenerated" -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.mainPurchaserID mustBe Some("1")
      request.mainVendorID mustBe Some("1")
      request.mainLandID mustBe Some("1")
      request.IRMarkGenerated mustBe Some("IRMark123456")
      request.landCertForEachProp mustBe Some("YES")
      request.declaration mustBe Some("YES")
    }

    "must deserialize from JSON correctly with N values" in {
      val json = Json.obj(
        "storn" -> "STORN99999",
        "returnResourceRef" -> "100002",
        "mainPurchaserID" -> "5",
        "mainVendorID" -> "3",
        "mainLandID" -> "7",
        "IRMarkGenerated" -> "IRMark999999",
        "landCertForEachProp" -> "NO",
        "declaration" -> "NO"
      )

      val result = json.validate[UpdateReturnRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN99999"
      request.returnResourceRef mustBe "100002"
      request.mainPurchaserID mustBe Some("5")
      request.mainVendorID mustBe Some("3")
      request.mainLandID mustBe Some("7")
      request.IRMarkGenerated mustBe Some("IRMark999999")
      request.landCertForEachProp mustBe Some("NO")
      request.declaration mustBe Some("NO")
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "mainPurchaserID" -> "1",
        "mainVendorID" -> "1",
        "mainLandID" -> "1",
        "IRMarkGenerated" -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "mainPurchaserID" -> "1",
        "mainVendorID" -> "1",
        "mainLandID" -> "1",
        "IRMarkGenerated" -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe true
    }

    "must pass to deserialize when mainPurchaserID is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "returnResourceRef" -> "100001",
        "mainVendorID" -> "1",
        "mainLandID" -> "1",
        "IRMarkGenerated" -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe false
    }

    "must pass to deserialize when mainVendorID is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "returnResourceRef" -> "100001",
        "mainPurchaserID" -> "1",
        "mainLandID" -> "1",
        "IRMarkGenerated" -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe false
    }

    "must pass to deserialize when mainLandID is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "returnResourceRef" -> "100001",
        "mainPurchaserID" -> "1",
        "mainVendorID" -> "1",
        "IRMarkGenerated" -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe false
    }

    "must pass to deserialize when IRMarkGenerated is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "returnResourceRef" -> "100001",
        "mainPurchaserID" -> "1",
        "mainVendorID" -> "1",
        "mainLandID" -> "1",
        "landCertForEachProp" -> "YES",
        "declaration" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe false
    }

    "must pass to deserialize when landCertForEachProp is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "returnResourceRef" -> "100001",
        "mainPurchaserID" -> "1",
        "mainVendorID" -> "1",
        "mainLandID" -> "1",
        "IRMarkGenerated" -> "IRMark123456",
        "declaration" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe false
    }

    "must pass to deserialize when declaration is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "returnResourceRef" -> "100001",
        "mainPurchaserID" -> "1",
        "mainVendorID" -> "1",
        "mainLandID" -> "1",
        "IRMarkGenerated" -> "IRMark123456",
        "landCertForEachProp" -> "YES"
      )

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe false
    }

    "must fail to deserialize when all fields are missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateReturnRequest]

      result.isError mustBe true
    }

    "must handle various IRMark formats" in {
      val request1 = UpdateReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        mainPurchaserID = Some("1"),
        mainVendorID = Some("1"),
        mainLandID = Some("1"),
        IRMarkGenerated = Some("123456789012"),
        landCertForEachProp = Some("YES"),
        declaration = Some("YES")
      )

      val json1 = Json.toJson(request1)
      (json1 \ "IRMarkGenerated").as[String] mustBe "123456789012"

      val request2 = UpdateReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        mainPurchaserID = Some("1"),
        mainVendorID = Some("1"),
        mainLandID = Some("1"),
        IRMarkGenerated = Some("IRMark-ABC-123"),
        landCertForEachProp = Some("YES"),
        declaration = Some("YES")
      )

      val json2 = Json.toJson(request2)
      (json2 \ "IRMarkGenerated").as[String] mustBe "IRMark-ABC-123"
    }
  }

  "UpdateReturnReturn" - {

    "must serialize to JSON correctly with updated=true" in {
      val response = UpdateReturnReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly with updated=false" in {
      val response = UpdateReturnReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly with updated=true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateReturnReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must deserialize from JSON correctly with updated=false" in {
      val json = Json.obj("updated" -> false)

      val result = json.validate[UpdateReturnReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe false
    }

    "must fail to deserialize when updated is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateReturnReturn]

      result.isError mustBe true
    }

    "must fail to deserialize when updated is not a boolean" in {
      val json = Json.obj("updated" -> "true")

      val result = json.validate[UpdateReturnReturn]

      result.isError mustBe true
    }

    "must fail to deserialize when updated is a number" in {
      val json = Json.obj("updated" -> 1)

      val result = json.validate[UpdateReturnReturn]

      result.isError mustBe true
    }
  }

  "UpdateReturnRequest and UpdateReturnReturn" - {

    "must round-trip serialize and deserialize correctly" in {
      val request = UpdateReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        mainPurchaserID = Some("1"),
        mainVendorID = Some("1"),
        mainLandID = Some("1"),
        IRMarkGenerated = Some("IRMark123456"),
        landCertForEachProp = Some("YES"),
        declaration = Some("YES")
      )

      val requestJson = Json.toJson(request)
      val requestResult = requestJson.validate[UpdateReturnRequest]

      requestResult mustBe a[JsSuccess[_]]
      requestResult.get mustBe request

      val response = UpdateReturnReturn(updated = true)

      val responseJson = Json.toJson(response)
      val responseResult = responseJson.validate[UpdateReturnReturn]

      responseResult mustBe a[JsSuccess[_]]
      responseResult.get mustBe response
    }

    "must handle typical update scenario with Y values" in {
      val request = UpdateReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        mainPurchaserID = Some("1"),
        mainVendorID = Some("1"),
        mainLandID = Some("1"),
        IRMarkGenerated = Some("IRMark123456"),
        landCertForEachProp = Some("YES"),
        declaration = Some("YES")
      )

      val requestJson = Json.toJson(request)
      (requestJson \ "landCertForEachProp").as[String] mustBe "YES"
      (requestJson \ "declaration").as[String] mustBe "YES"

      val response = UpdateReturnReturn(updated = true)

      val responseJson = Json.toJson(response)
      (responseJson \ "updated").as[Boolean] mustBe true
    }

    "must handle typical update scenario with N values" in {
      val request = UpdateReturnRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        mainPurchaserID = Some("5"),
        mainVendorID = Some("3"),
        mainLandID = Some("7"),
        IRMarkGenerated = Some("IRMark999999"),
        landCertForEachProp = Some("NO"),
        declaration = Some("NO")
      )

      val requestJson = Json.toJson(request)
      (requestJson \ "landCertForEachProp").as[String] mustBe "NO"
      (requestJson \ "declaration").as[String] mustBe "NO"

      val response = UpdateReturnReturn(updated = true)

      val responseJson = Json.toJson(response)
      (responseJson \ "updated").as[Boolean] mustBe true
    }

    "must handle update scenario with different IDs" in {
      val request = UpdateReturnRequest(
        storn = "STORN88888",
        returnResourceRef = "100003",
        mainPurchaserID = Some("10"),
        mainVendorID = Some("20"),
        mainLandID = Some("30"),
        IRMarkGenerated = Some("IRMark888888"),
        landCertForEachProp = Some("YES"),
        declaration = Some("YES")
      )

      val requestJson = Json.toJson(request)
      (requestJson \ "mainPurchaserID").as[String] mustBe "10"
      (requestJson \ "mainVendorID").as[String] mustBe "20"
      (requestJson \ "mainLandID").as[String] mustBe "30"

      val response = UpdateReturnReturn(updated = true)

      val responseJson = Json.toJson(response)
      (responseJson \ "updated").as[Boolean] mustBe true
    }
  }
}
