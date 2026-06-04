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
import service.filing.LeaseReturnsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class LeaseReturnsServiceSpec extends SpecBase {

  private def mkLeasePayload(
                              isAnnualRentOver1000: Option[String] = Some("true"),
                              contractEndDate: Option[String] = Some("2026-01-01"),
                              contractStartDate: Option[String] = Some("2025-01-01"),
                              leaseType: Option[String] = Some("leaseType"),
                              netPresentValue: Option[String] = Some("1000"),
                              totalPremiumPayable: Option[String] = Some("500"),
                              rentFreePeriod: Option[String] = Some("0"),
                              startingRent: Option[String] = Some("100"),
                              startingRentEndDate: Option[String] = Some("2025-12-31"),
                              laterRentKnown: Option[String] = Some("false"),
                              vatAmount: Option[String] = Some("20")
                            ): LeasePayload =
    LeasePayload(
      isAnnualRentOver1000 = isAnnualRentOver1000,
      contractEndDate = contractEndDate,
      contractStartDate = contractStartDate,
      leaseType = leaseType,
      netPresentValue = netPresentValue,
      totalPremiumPayable = totalPremiumPayable,
      rentFreePeriod = rentFreePeriod,
      startingRent = startingRent,
      startingRentEndDate = startingRentEndDate,
      laterRentKnown = laterRentKnown,
      vatAmount = vatAmount
    )

  private def mkCreateLeaseRequest(
                                    stornId: String = "STORN12345",
                                    returnResourceRef: String = "RRF-2024-001",
                                    lease: LeasePayload = mkLeasePayload()
                                  ): CreateLeaseRequest =
    CreateLeaseRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      lease = lease
    )

  private def mkCreateLeaseReturn(created: Boolean = true): CreateLeaseReturn =
    CreateLeaseReturn(created = created)

  private def mkUpdateLeaseRequest(
                                    stornId: String = "STORN12345",
                                    returnResourceRef: String = "RRF-2024-001",
                                    lease: LeasePayload = mkLeasePayload()
                                  ): UpdateLeaseRequest =
    UpdateLeaseRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      lease = lease
    )

  private def mkUpdateLeaseReturn(updated: Boolean = true): UpdateLeaseReturn =
    UpdateLeaseReturn(updated = updated)

  private def mkDeleteLeaseRequest(
                                    storn: String = "STORN12345",
                                    returnResourceRef: String = "RRF-2024-001"
                                  ): DeleteLeaseRequest =
    DeleteLeaseRequest(
      storn = storn,
      returnResourceRef = returnResourceRef
    )

  private def mkDeleteLeaseReturn(deleted: Boolean = true): DeleteLeaseReturn =
    DeleteLeaseReturn(deleted = deleted)

  "LeaseReturnsService createLease" - {

    "must delegate to connector (happy path)" in {
      val connector                  = mock[FilingFormpProxyConnector]
      val service                    = new LeaseReturnsService(connector)
      val request: CreateLeaseRequest = mkCreateLeaseRequest()
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.createLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn()))

      val result: CreateLeaseReturn = service.createLease(request).futureValue
      result mustBe mkCreateLeaseReturn()

      verify(connector).createLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new LeaseReturnsService(connector)
      val request1: CreateLeaseRequest = mkCreateLeaseRequest("STORN11111", "RRF-001")
      val request2: CreateLeaseRequest = mkCreateLeaseRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.createLease(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn(created = true)))
      when(connector.createLease(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn(created = false)))

      service.createLease(request1).futureValue mustBe mkCreateLeaseReturn(created = true)
      service.createLease(request2).futureValue mustBe mkCreateLeaseReturn(created = false)

      verify(connector).createLease(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createLease(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: CreateLeaseRequest = mkCreateLeaseRequest()
      val boom                        = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.createLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createLease(request).failed.futureValue
      ex mustBe boom

      verify(connector).createLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: CreateLeaseRequest = mkCreateLeaseRequest()
      val boom                        = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.createLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createLease(request).failed.futureValue
      ex mustBe boom

      verify(connector).createLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle a lease payload with no optional fields populated" in {
      val connector = mock[FilingFormpProxyConnector]
      val service   = new LeaseReturnsService(connector)
      val request: CreateLeaseRequest = mkCreateLeaseRequest(
        lease = mkLeasePayload(
          isAnnualRentOver1000 = None,
          contractEndDate = None,
          contractStartDate = None,
          leaseType = None,
          netPresentValue = None,
          totalPremiumPayable = None,
          rentFreePeriod = None,
          startingRent = None,
          startingRentEndDate = None,
          laterRentKnown = None,
          vatAmount = None
        )
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn()))

      service.createLease(request).futureValue mustBe mkCreateLeaseReturn()

      verify(connector).createLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle a fully populated lease payload" in {
      val connector = mock[FilingFormpProxyConnector]
      val service   = new LeaseReturnsService(connector)
      val request: CreateLeaseRequest = mkCreateLeaseRequest(lease = mkLeasePayload())
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.createLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn()))

      service.createLease(request).futureValue mustBe mkCreateLeaseReturn()

      verify(connector).createLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: CreateLeaseRequest = mkCreateLeaseRequest()
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.createLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn()))

      service.createLease(request).futureValue

      verify(connector, times(1)).createLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new LeaseReturnsService(connector)
      val request1: CreateLeaseRequest = mkCreateLeaseRequest("STORN11111", "RRF-001")
      val request2: CreateLeaseRequest = mkCreateLeaseRequest("STORN22222", "RRF-002")
      val request3: CreateLeaseRequest = mkCreateLeaseRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.createLease(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn()))
      when(connector.createLease(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn()))
      when(connector.createLease(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLeaseReturn()))

      service.createLease(request1).futureValue mustBe mkCreateLeaseReturn()
      service.createLease(request2).futureValue mustBe mkCreateLeaseReturn()
      service.createLease(request3).futureValue mustBe mkCreateLeaseReturn()

      verify(connector, times(3)).createLease(any[CreateLeaseRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "LeaseReturnsService updateLease" - {

    "must delegate to connector (happy path)" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: UpdateLeaseRequest = mkUpdateLeaseRequest()
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.updateLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLeaseReturn()))

      val result: UpdateLeaseReturn = service.updateLease(request).futureValue
      result mustBe mkUpdateLeaseReturn()

      verify(connector).updateLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new LeaseReturnsService(connector)
      val request1: UpdateLeaseRequest = mkUpdateLeaseRequest("STORN11111", "RRF-001")
      val request2: UpdateLeaseRequest = mkUpdateLeaseRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.updateLease(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLeaseReturn(updated = true)))
      when(connector.updateLease(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLeaseReturn(updated = false)))

      service.updateLease(request1).futureValue mustBe mkUpdateLeaseReturn(updated = true)
      service.updateLease(request2).futureValue mustBe mkUpdateLeaseReturn(updated = false)

      verify(connector).updateLease(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateLease(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: UpdateLeaseRequest = mkUpdateLeaseRequest()
      val boom                        = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.updateLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateLease(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: UpdateLeaseRequest = mkUpdateLeaseRequest()
      val boom                        = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.updateLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateLease(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle a lease payload with no optional fields populated" in {
      val connector = mock[FilingFormpProxyConnector]
      val service   = new LeaseReturnsService(connector)
      val request: UpdateLeaseRequest = mkUpdateLeaseRequest(
        lease = mkLeasePayload(
          isAnnualRentOver1000 = None,
          contractEndDate = None,
          contractStartDate = None,
          leaseType = None,
          netPresentValue = None,
          totalPremiumPayable = None,
          rentFreePeriod = None,
          startingRent = None,
          startingRentEndDate = None,
          laterRentKnown = None,
          vatAmount = None
        )
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLeaseReturn()))

      service.updateLease(request).futureValue mustBe mkUpdateLeaseReturn()

      verify(connector).updateLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: UpdateLeaseRequest = mkUpdateLeaseRequest()
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.updateLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLeaseReturn()))

      service.updateLease(request).futureValue

      verify(connector, times(1)).updateLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new LeaseReturnsService(connector)
      val request1: UpdateLeaseRequest = mkUpdateLeaseRequest("STORN11111", "RRF-001")
      val request2: UpdateLeaseRequest = mkUpdateLeaseRequest("STORN22222", "RRF-002")
      val request3: UpdateLeaseRequest = mkUpdateLeaseRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.updateLease(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLeaseReturn()))
      when(connector.updateLease(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLeaseReturn()))
      when(connector.updateLease(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLeaseReturn()))

      service.updateLease(request1).futureValue mustBe mkUpdateLeaseReturn()
      service.updateLease(request2).futureValue mustBe mkUpdateLeaseReturn()
      service.updateLease(request3).futureValue mustBe mkUpdateLeaseReturn()

      verify(connector, times(3)).updateLease(any[UpdateLeaseRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "LeaseReturnsService deleteLease" - {

    "must delegate to connector (happy path)" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: DeleteLeaseRequest = mkDeleteLeaseRequest()
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.deleteLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn()))

      val result: DeleteLeaseReturn = service.deleteLease(request).futureValue
      result mustBe mkDeleteLeaseReturn()

      verify(connector).deleteLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new LeaseReturnsService(connector)
      val request1: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN11111", "RRF-001")
      val request2: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.deleteLease(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn(deleted = true)))
      when(connector.deleteLease(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn(deleted = false)))

      service.deleteLease(request1).futureValue mustBe mkDeleteLeaseReturn(deleted = true)
      service.deleteLease(request2).futureValue mustBe mkDeleteLeaseReturn(deleted = false)

      verify(connector).deleteLease(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteLease(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: DeleteLeaseRequest = mkDeleteLeaseRequest()
      val boom                        = UpstreamErrorResponse("Not found", 404)
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.deleteLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteLease(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: DeleteLeaseRequest = mkDeleteLeaseRequest()
      val boom                        = new RuntimeException("Connection timeout")
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.deleteLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteLease(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new LeaseReturnsService(connector)
      val request1: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN123456", "RRF-2024-001")
      val request2: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN-ABC-123", "RRF-2024-001")
      val request3: DeleteLeaseRequest = mkDeleteLeaseRequest("12345678", "RRF-2024-001")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.deleteLease(any[DeleteLeaseRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn()))

      service.deleteLease(request1).futureValue mustBe mkDeleteLeaseReturn()
      service.deleteLease(request2).futureValue mustBe mkDeleteLeaseReturn()
      service.deleteLease(request3).futureValue mustBe mkDeleteLeaseReturn()

      verify(connector).deleteLease(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteLease(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteLease(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different returnResourceRef formats" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new LeaseReturnsService(connector)
      val request1: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN12345", "100001")
      val request2: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN12345", "RRF-2024-001")
      val request3: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN12345", "ABC-123-XYZ")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.deleteLease(any[DeleteLeaseRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn()))

      service.deleteLease(request1).futureValue mustBe mkDeleteLeaseReturn()
      service.deleteLease(request2).futureValue mustBe mkDeleteLeaseReturn()
      service.deleteLease(request3).futureValue mustBe mkDeleteLeaseReturn()

      verify(connector).deleteLease(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteLease(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteLease(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                   = mock[FilingFormpProxyConnector]
      val service                     = new LeaseReturnsService(connector)
      val request: DeleteLeaseRequest = mkDeleteLeaseRequest()
      implicit val hc: HeaderCarrier  = HeaderCarrier()

      when(connector.deleteLease(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn()))

      service.deleteLease(request).futureValue

      verify(connector, times(1)).deleteLease(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new LeaseReturnsService(connector)
      val request1: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN11111", "RRF-001")
      val request2: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN22222", "RRF-002")
      val request3: DeleteLeaseRequest = mkDeleteLeaseRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.deleteLease(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn()))
      when(connector.deleteLease(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn()))
      when(connector.deleteLease(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLeaseReturn()))

      service.deleteLease(request1).futureValue mustBe mkDeleteLeaseReturn()
      service.deleteLease(request2).futureValue mustBe mkDeleteLeaseReturn()
      service.deleteLease(request3).futureValue mustBe mkDeleteLeaseReturn()

      verify(connector, times(3)).deleteLease(any[DeleteLeaseRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}