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
import service.filing.TaxCalculationReturnsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class TaxCalculationReturnsServiceSpec extends SpecBase {

  private def mkUpdateRequest(
                               stornId: String = "STORN12345",
                               returnResourceRef: String = "100001",
                               amountPaid: Option[String] = Some("2000"),
                               includesPenalty: Option[String] = Some("YES"),
                               taxDue: Option[String] = Some("8000"),
                               calcPenaltyDue: Option[String] = Some("500"),
                               calcTaxDue: Option[String] = Some("8000"),
                               calcTaxRate1: Option[String] = Some("3"),
                               calcTaxRate2: Option[String] = Some("7"),
                               calcTotalTaxPenaltyDue: Option[String] = Some("8500"),
                               calcTotalNpvTax: Option[String] = Some("1000"),
                               calcTotalPremiumTax: Option[String] = Some("7500"),
                               taxDuePremium: Option[String] = Some("7500"),
                               taxDueNpv: Option[String] = Some("1000"),
                               honestyDeclaration: Option[String] = Some("YES")
                                        ): UpdateTaxCalculationRequest = UpdateTaxCalculationRequest(
    stornId = stornId,
    returnResourceRef = returnResourceRef,
    amountPaid = amountPaid,
    includesPenalty = includesPenalty,
    taxDue = taxDue,
    calcPenaltyDue = calcPenaltyDue,
    calcTaxDue = calcTaxDue,
    calcTaxRate1 = calcTaxRate1,
    calcTaxRate2 = calcTaxRate2,
    calcTotalTaxPenaltyDue = calcTotalTaxPenaltyDue,
    calcTotalNpvTax = calcTotalNpvTax,
    calcTotalPremiumTax = calcTotalPremiumTax,
    taxDuePremium = taxDuePremium,
    taxDueNpv = taxDueNpv,
    honestyDeclaration = honestyDeclaration
  )

  private def mkUpdateTaxCalculationReturn(updated: Boolean = true): UpdateTaxCalculationReturn =
    UpdateTaxCalculationReturn(updated = updated)

  "TaxCalculationReturnsService updateTaxCalc" - {

    "must delegate to connector (happy path)" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new TaxCalculationReturnsService(connector)
      val request: UpdateTaxCalculationRequest   = mkUpdateRequest()
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn()))

      val result = service.updateTaxCalculationInfo(request).futureValue
      result mustBe mkUpdateTaxCalculationReturn()

      verify(connector).updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TaxCalculationReturnsService(connector)
      val request: UpdateTaxCalculationRequest  = mkUpdateRequest()
      val boom                               = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateTaxCalculationInfo(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TaxCalculationReturnsService(connector)
      val request: UpdateTaxCalculationRequest  = mkUpdateRequest()
      val boom                               = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateTaxCalculationInfo(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TaxCalculationReturnsService(connector)
      val request: UpdateTaxCalculationRequest  = mkUpdateRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn()))

      service.updateTaxCalculationInfo(request).futureValue

      verify(connector, times(1)).updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update result with false status" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TaxCalculationReturnsService(connector)
      val request: UpdateTaxCalculationRequest  = mkUpdateRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn(false)))

      val result: UpdateTaxCalculationReturn = service.updateTaxCalculationInfo(request).futureValue
      result mustBe mkUpdateTaxCalculationReturn(false)
      result.updated mustBe false

      verify(connector).updateTaxCalculationInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                             = mock[FilingFormpProxyConnector]
      val service                               = new TaxCalculationReturnsService(connector)
      val request1: UpdateTaxCalculationRequest = mkUpdateRequest("STORN11111", "RRF-001")
      val request2: UpdateTaxCalculationRequest = mkUpdateRequest("STORN22222", "RRF-002")
      val request3: UpdateTaxCalculationRequest = mkUpdateRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTaxCalculationInfo(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn(true)))
      when(connector.updateTaxCalculationInfo(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn(true)))
      when(connector.updateTaxCalculationInfo(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn(true)))

      service.updateTaxCalculationInfo(request1).futureValue mustBe mkUpdateTaxCalculationReturn(true)
      service.updateTaxCalculationInfo(request2).futureValue mustBe mkUpdateTaxCalculationReturn(true)
      service.updateTaxCalculationInfo(request3).futureValue mustBe mkUpdateTaxCalculationReturn(true)

      verify(connector, times(3)).updateTaxCalculationInfo(any[UpdateTaxCalculationRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new TaxCalculationReturnsService(connector)
      val request1: UpdateTaxCalculationRequest = mkUpdateRequest("STORN12345")
      val request2: UpdateTaxCalculationRequest = mkUpdateRequest("STORN-ABC-123")
      val request3: UpdateTaxCalculationRequest = mkUpdateRequest("12345678")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateTaxCalculationInfo(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn(true)))
      when(connector.updateTaxCalculationInfo(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn(true)))
      when(connector.updateTaxCalculationInfo(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn(true)))

      service.updateTaxCalculationInfo(request1).futureValue mustBe mkUpdateTaxCalculationReturn(true)
      service.updateTaxCalculationInfo(request2).futureValue mustBe mkUpdateTaxCalculationReturn(true)
      service.updateTaxCalculationInfo(request3).futureValue mustBe mkUpdateTaxCalculationReturn(true)

      verify(connector).updateTaxCalculationInfo(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateTaxCalculationInfo(eqTo(request2))(any[HeaderCarrier])
      verify(connector).updateTaxCalculationInfo(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}