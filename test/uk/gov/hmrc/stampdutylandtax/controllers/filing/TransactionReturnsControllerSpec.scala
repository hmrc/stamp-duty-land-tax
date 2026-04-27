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

package uk.gov.hmrc.stampdutylandtax.controllers.filing

import base.SpecBase
import models.filing.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import service.filing.TransactionReturnsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TransactionReturnsControllerSpec extends SpecBase {

  "TransactionReturnsController" - {

    "POST /update-transaction (updateTransaction)" - {

      "return CREATED with update response when service returns successfully" in new BaseSetup {
        when(
          mockTransactionReturnsService.updateTransaction(
            eqTo(testUpdateTransactionRequest)
          )(any[HeaderCarrier])
        )
          .thenReturn(Future.successful(testUpdateTransactionReturn))

        val result: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.toJson(testUpdateTransactionRequest))
        )

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testUpdateTransactionReturn)
        verify(mockTransactionReturnsService).updateTransaction(
          eqTo(testUpdateTransactionRequest)
        )(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.obj("invalid" -> "data"))
        )

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] =
          controller.updateTransaction()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when storn is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "returnResourceRef" -> "RRF-2024-001",
          "transaction" -> Json.obj()
        )
        val result: Future[Result] =
          controller.updateTransaction()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when returnResourceRef is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "transaction" -> Json.obj()
        )
        val result: Future[Result] =
          controller.updateTransaction()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return BAD_REQUEST when transaction payload is missing" in new BaseSetup {
        val invalidRequest: JsObject = Json.obj(
          "storn" -> "STORN12345",
          "returnResourceRef" -> "RRF-2024-001"
        )
        val result: Future[Result] =
          controller.updateTransaction()(fakeRequest.withBody(invalidRequest))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(
          mockTransactionReturnsService.updateTransaction(
            any[UpdateTransactionRequest]
          )(any[HeaderCarrier])
        )
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.toJson(testUpdateTransactionRequest))
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 when service fails with exception" in new BaseSetup {
        when(
          mockTransactionReturnsService.updateTransaction(
            any[UpdateTransactionRequest]
          )(any[HeaderCarrier])
        )
          .thenReturn(Future.failed(new Exception("Service failure")))

        val result: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.toJson(testUpdateTransactionRequest))
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "handle updated false response" in new BaseSetup {
        when(
          mockTransactionReturnsService.updateTransaction(
            eqTo(testUpdateTransactionRequest)
          )(any[HeaderCarrier])
        )
          .thenReturn(
            Future.successful(UpdateTransactionReturn(updated = false))
          )

        val result: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.toJson(testUpdateTransactionRequest))
        )

        status(result) mustBe CREATED
        (contentAsJson(result) \ "updated").as[Boolean] mustBe false
      }

      "handle valid payload with all optional transaction fields" in new BaseSetup {
        val completeRequest = testUpdateTransactionRequest.copy(
          transaction = testTransactionPayload.copy(
            claimingRelief = Some("YES"),
            totalConsider = Some("200000"),
            effectiveDate = Some("2024-02-01"),
            contractDate = Some("2024-01-15"),
            isLandExchanged = Some("NO")
          )
        )
        when(
          mockTransactionReturnsService.updateTransaction(
            eqTo(completeRequest)
          )(any[HeaderCarrier])
        )
          .thenReturn(Future.successful(testUpdateTransactionReturn))

        val result: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.toJson(completeRequest))
        )

        status(result) mustBe CREATED
        verify(mockTransactionReturnsService).updateTransaction(
          eqTo(completeRequest)
        )(any[HeaderCarrier])
      }

      "handle different storn formats" in new BaseSetup {
        val request1 = testUpdateTransactionRequest.copy(storn = "STORN12345")
        val request2 =
          testUpdateTransactionRequest.copy(storn = "STORN-ABC-123")
        val request3 = testUpdateTransactionRequest.copy(storn = "12345678")

        when(
          mockTransactionReturnsService.updateTransaction(
            any[UpdateTransactionRequest]
          )(any[HeaderCarrier])
        )
          .thenReturn(Future.successful(testUpdateTransactionReturn))

        val result1: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.toJson(request1))
        )
        val result2: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.toJson(request2))
        )
        val result3: Future[Result] = controller.updateTransaction()(
          fakeRequest.withBody(Json.toJson(request3))
        )

        status(result1) mustBe CREATED
        status(result2) mustBe CREATED
        status(result3) mustBe CREATED
      }
    }
  }

  private trait BaseSetup {
    val mockTransactionReturnsService: TransactionReturnsService =
      mock[TransactionReturnsService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new TransactionReturnsController(
      cc,
      mockTransactionReturnsService,
      fakeIdentifierAction
    )

    val testTransactionPayload: TransactionPayload = TransactionPayload()

    val testUpdateTransactionRequest: UpdateTransactionRequest =
      UpdateTransactionRequest(
        storn = "STORN12345",
        returnResourceRef = "RRF-2024-001",
        transaction = testTransactionPayload
      )

    val testUpdateTransactionReturn: UpdateTransactionReturn =
      UpdateTransactionReturn(
        updated = true
      )
  }
}
