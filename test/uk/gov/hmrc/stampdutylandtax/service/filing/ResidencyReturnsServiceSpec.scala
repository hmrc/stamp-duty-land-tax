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
import service.filing.ResidencyReturnsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class ResidencyReturnsServiceSpec extends SpecBase {

  private def mkResidencyPayload(
                                  isNonUkResidents: String = "NO",
                                  isCompany: String        = "NO",
                                  isCrownRelief: String    = "NO"
                                ): ResidencyPayload =
    ResidencyPayload(
      isNonUkResidents = isNonUkResidents,
      isCompany        = isCompany,
      isCrownRelief    = isCrownRelief
    )

  private def mkCreateResidencyRequest(
                                        stornId: String           = "STORN12345",
                                        returnResourceRef: String = "RRF-2024-001"
                                      ): CreateResidencyRequest =
    CreateResidencyRequest(
      stornId           = stornId,
      returnResourceRef = returnResourceRef,
      residency         = mkResidencyPayload()
    )

  private def mkCreateResidencyReturn(
                                       created: Boolean = true
                                     ): CreateResidencyReturn =
    CreateResidencyReturn(created = created)

  private def mkUpdateResidencyRequest(
                                        stornId: String           = "STORN12345",
                                        returnResourceRef: String = "RRF-2024-001"
                                      ): UpdateResidencyRequest =
    UpdateResidencyRequest(
      stornId           = stornId,
      returnResourceRef = returnResourceRef,
      residency         = mkResidencyPayload()
    )

  private def mkUpdateResidencyReturn(updated: Boolean = true): UpdateResidencyReturn =
    UpdateResidencyReturn(updated = updated)

  private def mkDeleteResidencyRequest(
                                        storn: String             = "STORN12345",
                                        returnResourceRef: String = "RRF-2024-001"
                                      ): DeleteResidencyRequest =
    DeleteResidencyRequest(
      storn             = storn,
      returnResourceRef = returnResourceRef
    )

  private def mkDeleteResidencyReturn(deleted: Boolean = true): DeleteResidencyReturn =
    DeleteResidencyReturn(deleted = deleted)

  "ResidencyReturnsService createResidency" - {

    "must delegate to connector (happy path)" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: CreateResidencyRequest = mkCreateResidencyRequest()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.createResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResidencyReturn()))

      val result: CreateResidencyReturn = service.createResidency(request).futureValue
      result mustBe mkCreateResidencyReturn()

      verify(connector).createResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                        = mock[FilingFormpProxyConnector]
      val service                          = new ResidencyReturnsService(connector)
      val request1: CreateResidencyRequest = mkCreateResidencyRequest("STORN11111", "RRF-001")
      val request2: CreateResidencyRequest = mkCreateResidencyRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier       = HeaderCarrier()

      when(connector.createResidency(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResidencyReturn(true)))
      when(connector.createResidency(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResidencyReturn(true)))

      service.createResidency(request1).futureValue mustBe mkCreateResidencyReturn(true)
      service.createResidency(request2).futureValue mustBe mkCreateResidencyReturn(true)

      verify(connector).createResidency(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createResidency(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: CreateResidencyRequest = mkCreateResidencyRequest()
      val boom                            = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.createResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createResidency(request).failed.futureValue
      ex mustBe boom

      verify(connector).createResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: CreateResidencyRequest = mkCreateResidencyRequest()
      val boom                            = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.createResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createResidency(request).failed.futureValue
      ex mustBe boom

      verify(connector).createResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: CreateResidencyRequest = mkCreateResidencyRequest()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.createResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResidencyReturn()))

      service.createResidency(request).futureValue

      verify(connector, times(1)).createResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different residency flag combinations" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new ResidencyReturnsService(connector)
      val nonUkRequest: CreateResidencyRequest   = mkCreateResidencyRequest().copy(residency = mkResidencyPayload(isNonUkResidents = "YES"))
      val companyRequest: CreateResidencyRequest = mkCreateResidencyRequest().copy(residency = mkResidencyPayload(isCompany = "YES"))
      val crownRequest: CreateResidencyRequest   = mkCreateResidencyRequest().copy(residency = mkResidencyPayload(isCrownRelief = "YES"))
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.createResidency(any[CreateResidencyRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResidencyReturn()))

      service.createResidency(nonUkRequest).futureValue mustBe mkCreateResidencyReturn()
      service.createResidency(companyRequest).futureValue mustBe mkCreateResidencyReturn()
      service.createResidency(crownRequest).futureValue mustBe mkCreateResidencyReturn()

      verify(connector, times(3)).createResidency(any[CreateResidencyRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                        = mock[FilingFormpProxyConnector]
      val service                          = new ResidencyReturnsService(connector)
      val request1: CreateResidencyRequest = mkCreateResidencyRequest("STORN11111", "RRF-001")
      val request2: CreateResidencyRequest = mkCreateResidencyRequest("STORN22222", "RRF-002")
      val request3: CreateResidencyRequest = mkCreateResidencyRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier       = HeaderCarrier()

      when(connector.createResidency(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResidencyReturn(true)))
      when(connector.createResidency(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResidencyReturn(true)))
      when(connector.createResidency(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResidencyReturn(true)))

      service.createResidency(request1).futureValue mustBe mkCreateResidencyReturn(true)
      service.createResidency(request2).futureValue mustBe mkCreateResidencyReturn(true)
      service.createResidency(request3).futureValue mustBe mkCreateResidencyReturn(true)

      verify(connector, times(3)).createResidency(any[CreateResidencyRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "ResidencyReturnsService updateResidency" - {

    "must delegate to connector (happy path)" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: UpdateResidencyRequest = mkUpdateResidencyRequest()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.updateResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn()))

      val result: UpdateResidencyReturn = service.updateResidency(request).futureValue
      result mustBe mkUpdateResidencyReturn()

      verify(connector).updateResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                        = mock[FilingFormpProxyConnector]
      val service                          = new ResidencyReturnsService(connector)
      val request1: UpdateResidencyRequest = mkUpdateResidencyRequest("STORN11111", "RRF-001")
      val request2: UpdateResidencyRequest = mkUpdateResidencyRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier       = HeaderCarrier()

      when(connector.updateResidency(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn(true)))
      when(connector.updateResidency(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn(true)))

      service.updateResidency(request1).futureValue mustBe mkUpdateResidencyReturn(true)
      service.updateResidency(request2).futureValue mustBe mkUpdateResidencyReturn(true)

      verify(connector).updateResidency(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateResidency(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: UpdateResidencyRequest = mkUpdateResidencyRequest()
      val boom                            = UpstreamErrorResponse("Not found", 404)
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.updateResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateResidency(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: UpdateResidencyRequest = mkUpdateResidencyRequest()
      val boom                            = new RuntimeException("Connection timeout")
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.updateResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateResidency(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: UpdateResidencyRequest = mkUpdateResidencyRequest()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.updateResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn()))

      service.updateResidency(request).futureValue

      verify(connector, times(1)).updateResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update result with false status" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: UpdateResidencyRequest = mkUpdateResidencyRequest()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.updateResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn(false)))

      val result: UpdateResidencyReturn = service.updateResidency(request).futureValue
      result mustBe mkUpdateResidencyReturn(false)
      result.updated mustBe false

      verify(connector).updateResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different residency flag combinations" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new ResidencyReturnsService(connector)
      val nonUkRequest: UpdateResidencyRequest   = mkUpdateResidencyRequest().copy(residency = mkResidencyPayload(isNonUkResidents = "YES"))
      val companyRequest: UpdateResidencyRequest = mkUpdateResidencyRequest().copy(residency = mkResidencyPayload(isCompany = "YES"))
      val crownRequest: UpdateResidencyRequest   = mkUpdateResidencyRequest().copy(residency = mkResidencyPayload(isCrownRelief = "YES"))
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.updateResidency(any[UpdateResidencyRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn()))

      service.updateResidency(nonUkRequest).futureValue mustBe mkUpdateResidencyReturn()
      service.updateResidency(companyRequest).futureValue mustBe mkUpdateResidencyReturn()
      service.updateResidency(crownRequest).futureValue mustBe mkUpdateResidencyReturn()

      verify(connector, times(3)).updateResidency(any[UpdateResidencyRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                        = mock[FilingFormpProxyConnector]
      val service                          = new ResidencyReturnsService(connector)
      val request1: UpdateResidencyRequest = mkUpdateResidencyRequest("STORN11111", "RRF-001")
      val request2: UpdateResidencyRequest = mkUpdateResidencyRequest("STORN22222", "RRF-002")
      val request3: UpdateResidencyRequest = mkUpdateResidencyRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier       = HeaderCarrier()

      when(connector.updateResidency(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn(true)))
      when(connector.updateResidency(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn(true)))
      when(connector.updateResidency(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateResidencyReturn(true)))

      service.updateResidency(request1).futureValue mustBe mkUpdateResidencyReturn(true)
      service.updateResidency(request2).futureValue mustBe mkUpdateResidencyReturn(true)
      service.updateResidency(request3).futureValue mustBe mkUpdateResidencyReturn(true)

      verify(connector, times(3)).updateResidency(any[UpdateResidencyRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "ResidencyReturnsService deleteResidency" - {

    "must delegate to connector (happy path)" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: DeleteResidencyRequest = mkDeleteResidencyRequest()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.deleteResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn()))

      val result: DeleteResidencyReturn = service.deleteResidency(request).futureValue
      result mustBe mkDeleteResidencyReturn()

      verify(connector).deleteResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                        = mock[FilingFormpProxyConnector]
      val service                          = new ResidencyReturnsService(connector)
      val request1: DeleteResidencyRequest = mkDeleteResidencyRequest("STORN11111", "RRF-001")
      val request2: DeleteResidencyRequest = mkDeleteResidencyRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier       = HeaderCarrier()

      when(connector.deleteResidency(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(true)))
      when(connector.deleteResidency(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(true)))

      service.deleteResidency(request1).futureValue mustBe mkDeleteResidencyReturn(true)
      service.deleteResidency(request2).futureValue mustBe mkDeleteResidencyReturn(true)

      verify(connector).deleteResidency(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteResidency(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: DeleteResidencyRequest = mkDeleteResidencyRequest()
      val boom                            = UpstreamErrorResponse("Internal Server Error", 500)
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.deleteResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteResidency(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: DeleteResidencyRequest = mkDeleteResidencyRequest()
      val boom                            = new RuntimeException("Network error")
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.deleteResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteResidency(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: DeleteResidencyRequest = mkDeleteResidencyRequest()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.deleteResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn()))

      service.deleteResidency(request).futureValue

      verify(connector, times(1)).deleteResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle delete result with false status" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new ResidencyReturnsService(connector)
      val request: DeleteResidencyRequest = mkDeleteResidencyRequest()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.deleteResidency(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(false)))

      val result: DeleteResidencyReturn = service.deleteResidency(request).futureValue
      result mustBe mkDeleteResidencyReturn(false)
      result.deleted mustBe false

      verify(connector).deleteResidency(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector                        = mock[FilingFormpProxyConnector]
      val service                          = new ResidencyReturnsService(connector)
      val request1: DeleteResidencyRequest = mkDeleteResidencyRequest("STORN12345",    "RRF-001")
      val request2: DeleteResidencyRequest = mkDeleteResidencyRequest("STORN-ABC-123", "RRF-001")
      val request3: DeleteResidencyRequest = mkDeleteResidencyRequest("12345678",      "RRF-001")
      implicit val hc: HeaderCarrier       = HeaderCarrier()

      when(connector.deleteResidency(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(true)))
      when(connector.deleteResidency(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(true)))
      when(connector.deleteResidency(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(true)))

      service.deleteResidency(request1).futureValue mustBe mkDeleteResidencyReturn(true)
      service.deleteResidency(request2).futureValue mustBe mkDeleteResidencyReturn(true)
      service.deleteResidency(request3).futureValue mustBe mkDeleteResidencyReturn(true)

      verify(connector).deleteResidency(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteResidency(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteResidency(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                        = mock[FilingFormpProxyConnector]
      val service                          = new ResidencyReturnsService(connector)
      val request1: DeleteResidencyRequest = mkDeleteResidencyRequest("STORN11111", "RRF-001")
      val request2: DeleteResidencyRequest = mkDeleteResidencyRequest("STORN22222", "RRF-002")
      val request3: DeleteResidencyRequest = mkDeleteResidencyRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier       = HeaderCarrier()

      when(connector.deleteResidency(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(true)))
      when(connector.deleteResidency(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(true)))
      when(connector.deleteResidency(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteResidencyReturn(true)))

      service.deleteResidency(request1).futureValue mustBe mkDeleteResidencyReturn(true)
      service.deleteResidency(request2).futureValue mustBe mkDeleteResidencyReturn(true)
      service.deleteResidency(request3).futureValue mustBe mkDeleteResidencyReturn(true)

      verify(connector, times(3)).deleteResidency(any[DeleteResidencyRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}