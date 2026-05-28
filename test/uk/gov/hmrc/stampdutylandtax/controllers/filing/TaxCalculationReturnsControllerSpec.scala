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

package uk.gov.hmrc.stampdutylandtax.controllers.filing

import base.SpecBase
import models.filing.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import service.filing.TaxCalculationReturnsService
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}

class TaxCalculationReturnsControllerSpec extends SpecBase {

  "TaxCalculationReturnsController" - {
    "POST /update/tax-calculation (updateTaxCalcInfo)" - {

      "return OK with update response when service returns successfully" in new BaseSetup {
        when(mockTaxCalcReturnsService.updateTaxCalculationInfo(eqTo(testUpdateReturnRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testUpdateReturnResponse))

        val result: Future[Result] = controller.updateTaxCalculation()(fakeRequest.withBody(Json.toJson(testUpdateReturnRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testUpdateReturnResponse)
        verify(mockTaxCalcReturnsService).updateTaxCalculationInfo(eqTo(testUpdateReturnRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateTaxCalculation()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "100001"
        )
        val result: Future[Result] = controller.updateTaxCalculation()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "stornId" -> "STORN12345"
        )
        val result: Future[Result] = controller.updateTaxCalculation()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when all fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.updateTaxCalculation()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockTaxCalcReturnsService.updateTaxCalculationInfo(any[UpdateTaxCalculationRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateTaxCalculation()(fakeRequest.withBody(Json.toJson(testUpdateReturnRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(mockTaxCalcReturnsService.updateTaxCalculationInfo(any[UpdateTaxCalculationRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateTaxCalculation()(fakeRequest.withBody(Json.toJson(testUpdateReturnRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }
  }

  private trait BaseSetup {
    val mockTaxCalcReturnsService: TaxCalculationReturnsService = mock[TaxCalculationReturnsService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new TaxCalculationReturnsController(cc, mockTaxCalcReturnsService, fakeIdentifierAction)


    val testUpdateReturnRequest: UpdateTaxCalculationRequest = UpdateTaxCalculationRequest(
      stornId = "STORN12345",
      returnResourceRef = "100001",
      amountPaid = Some("2000"),
      includesPenalty = Some("YES"),
      taxDue = Some("8000"),
      calcPenaltyDue = Some("500"),
      calcTaxDue = Some("8000"),
      calcTaxRate1 = Some("3"),
      calcTaxRate2 = Some("7"),
      calcTotalTaxPenaltyDue = Some("8500"),
      calcTotalNpvTax = Some("1000"),
      calcTotalPremiumTax = Some("7500"),
      taxDuePremium = Some("7500"),
      taxDueNpv = Some("1000"),
      honestyDeclaration = Some("YES")
    )

    val testUpdateReturnResponse: UpdateTaxCalculationReturn = UpdateTaxCalculationReturn(
      updated = true
    )
  }
}