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

package uk.gov.hmrc.stampdutylandtax.service.filing

import base.SpecBase
import connectors.FilingFormpProxyConnector
import models.filing.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import service.filing.TransactionReturnsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class TransactionReturnsServiceSpec extends SpecBase {

  private def mkUpdateTransactionRequest(
                                          storn: String             = "STORN12345",
                                          returnResourceRef: String = "RRF-2024-001",
                                          transaction: TransactionPayload = TransactionPayload()
                                        ): UpdateTransactionRequest =
    UpdateTransactionRequest(
      storn             = storn,
      returnResourceRef = returnResourceRef,
      transaction       = transaction
    )

  private def mkUpdateTransactionReturn(updated: Boolean = true): UpdateTransactionReturn =
    UpdateTransactionReturn(updated = updated)

  "TransactionReturnsService updateTransaction" - {

    "must delegate to connector (happy path)" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new TransactionReturnsService(connector)
      val request: UpdateTransactionRequest      = mkUpdateTransactionRequest()
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn()))

      val result: UpdateTransactionReturn = service.updateTransaction(request).futureValue
      result mustBe mkUpdateTransactionReturn()

      verify(connector).updateTransaction(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new TransactionReturnsService(connector)
      val request1: UpdateTransactionRequest     = mkUpdateTransactionRequest("STORN11111", "RRF-001")
      val request2: UpdateTransactionRequest     = mkUpdateTransactionRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(true)))
      when(connector.updateTransaction(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(true)))

      service.updateTransaction(request1).futureValue mustBe mkUpdateTransactionReturn(true)
      service.updateTransaction(request2).futureValue mustBe mkUpdateTransactionReturn(true)

      verify(connector).updateTransaction(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateTransaction(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TransactionReturnsService(connector)
      val request: UpdateTransactionRequest  = mkUpdateTransactionRequest()
      val boom                               = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateTransaction(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateTransaction(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TransactionReturnsService(connector)
      val request: UpdateTransactionRequest  = mkUpdateTransactionRequest()
      val boom                               = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateTransaction(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateTransaction(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TransactionReturnsService(connector)
      val request: UpdateTransactionRequest  = mkUpdateTransactionRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn()))

      service.updateTransaction(request).futureValue

      verify(connector, times(1)).updateTransaction(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update result with false status" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TransactionReturnsService(connector)
      val request: UpdateTransactionRequest  = mkUpdateTransactionRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(false)))

      val result: UpdateTransactionReturn = service.updateTransaction(request).futureValue
      result mustBe mkUpdateTransactionReturn(false)
      result.updated mustBe false

      verify(connector).updateTransaction(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle request with complete transaction payload" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TransactionReturnsService(connector)
      val request: UpdateTransactionRequest  = mkUpdateTransactionRequest(
        transaction = TransactionPayload(
          claimingRelief  = Some("YES"),
          totalConsider   = Some("200000"),
          effectiveDate   = Some("2024-02-01"),
          contractDate    = Some("2024-01-15"),
          isLandExchanged = Some("NO")
        )
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn()))

      val result: UpdateTransactionReturn = service.updateTransaction(request).futureValue
      result mustBe mkUpdateTransactionReturn()

      verify(connector).updateTransaction(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TransactionReturnsService(connector)
      val request1: UpdateTransactionRequest = mkUpdateTransactionRequest("STORN11111", "RRF-001")
      val request2: UpdateTransactionRequest = mkUpdateTransactionRequest("STORN22222", "RRF-002")
      val request3: UpdateTransactionRequest = mkUpdateTransactionRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(true)))
      when(connector.updateTransaction(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(true)))
      when(connector.updateTransaction(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(true)))

      service.updateTransaction(request1).futureValue mustBe mkUpdateTransactionReturn(true)
      service.updateTransaction(request2).futureValue mustBe mkUpdateTransactionReturn(true)
      service.updateTransaction(request3).futureValue mustBe mkUpdateTransactionReturn(true)

      verify(connector, times(3)).updateTransaction(any[UpdateTransactionRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TransactionReturnsService(connector)
      val request1: UpdateTransactionRequest = mkUpdateTransactionRequest("STORN12345")
      val request2: UpdateTransactionRequest = mkUpdateTransactionRequest("STORN-ABC-123")
      val request3: UpdateTransactionRequest = mkUpdateTransactionRequest("12345678")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTransaction(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(true)))
      when(connector.updateTransaction(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(true)))
      when(connector.updateTransaction(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTransactionReturn(true)))

      service.updateTransaction(request1).futureValue mustBe mkUpdateTransactionReturn(true)
      service.updateTransaction(request2).futureValue mustBe mkUpdateTransactionReturn(true)
      service.updateTransaction(request3).futureValue mustBe mkUpdateTransactionReturn(true)

      verify(connector).updateTransaction(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateTransaction(eqTo(request2))(any[HeaderCarrier])
      verify(connector).updateTransaction(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}