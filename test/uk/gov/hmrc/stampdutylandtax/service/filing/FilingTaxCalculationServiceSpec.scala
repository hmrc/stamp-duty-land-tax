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
import service.filing.FilingTaxCalculationService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class FilingTaxCalculationServiceSpec extends SpecBase {

  private def mkUpdateTaxCalculationRequest(
                                             stornId: String = "STORN12345",
                                             returnResourceRef: String = "100001"
                                           ): UpdateTaxCalculationRequest =
    UpdateTaxCalculationRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
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

  private def mkUpdateTaxCalculationReturn(updated: Boolean = true): UpdateTaxCalculationReturn =
    UpdateTaxCalculationReturn(updated = updated)

  "FilingTaxCalculationService updateTaxCalculation" - {

    "must delegate to connector (happy path)" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new FilingTaxCalculationService(connector)
      val request: UpdateTaxCalculationRequest   = mkUpdateTaxCalculationRequest()
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTaxCalculation(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn()))

      val result: UpdateTaxCalculationReturn = service.updateTaxCalculation(request).futureValue
      result mustBe mkUpdateTaxCalculationReturn()

      verify(connector).updateTaxCalculation(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return updated=false when connector reports no rows updated" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new FilingTaxCalculationService(connector)
      val request: UpdateTaxCalculationRequest   = mkUpdateTaxCalculationRequest()
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTaxCalculation(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn(updated = false)))

      val result: UpdateTaxCalculationReturn = service.updateTaxCalculation(request).futureValue
      result.updated mustBe false

      verify(connector).updateTaxCalculation(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new FilingTaxCalculationService(connector)
      val request: UpdateTaxCalculationRequest   = mkUpdateTaxCalculationRequest()
      val boom                                   = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTaxCalculation(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateTaxCalculation(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateTaxCalculation(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal update with no optional fields" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new FilingTaxCalculationService(connector)
      val request: UpdateTaxCalculationRequest   = UpdateTaxCalculationRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001"
      )
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTaxCalculation(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn()))

      val result: UpdateTaxCalculationReturn = service.updateTaxCalculation(request).futureValue
      result mustBe mkUpdateTaxCalculationReturn()

      verify(connector).updateTaxCalculation(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle partial update" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new FilingTaxCalculationService(connector)
      val request: UpdateTaxCalculationRequest   = UpdateTaxCalculationRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        amountPaid = Some("3000"),
        taxDue = Some("3000"),
        calcTaxDue = Some("3000"),
        honestyDeclaration = Some("YES")
      )
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTaxCalculation(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn()))

      val result: UpdateTaxCalculationReturn = service.updateTaxCalculation(request).futureValue
      result mustBe mkUpdateTaxCalculationReturn()

      verify(connector).updateTaxCalculation(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new FilingTaxCalculationService(connector)
      val request: UpdateTaxCalculationRequest   = mkUpdateTaxCalculationRequest()
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTaxCalculation(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateTaxCalculationReturn()))

      service.updateTaxCalculation(request).futureValue

      verify(connector, times(1)).updateTaxCalculation(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new FilingTaxCalculationService(connector)
      val request: UpdateTaxCalculationRequest   = mkUpdateTaxCalculationRequest()
      val boom                                   = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateTaxCalculation(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateTaxCalculation(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateTaxCalculation(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}
