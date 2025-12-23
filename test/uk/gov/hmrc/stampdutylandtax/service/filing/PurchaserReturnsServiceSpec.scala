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

package uk.gov.hmrc.stampdutylandtax.service.filing

import base.SpecBase
import connectors.FilingFormpProxyConnector
import models.filing.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import service.filing.PurchaserReturnsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class PurchaserReturnsServiceSpec extends SpecBase {

  private def mkCreatePurchaserRequest(
                                        stornId: String = "STORN12345",
                                        returnResourceRef: String = "RRF-2024-001"
                                      ): CreatePurchaserRequest =
    CreatePurchaserRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      isCompany = "NO",
      isTrustee = "NO",
      isConnectedToVendor = "NO",
      isRepresentedByAgent = "YES",
      title = Some("Mr"),
      surname = Some("Jones"),
      forename1 = Some("David"),
      forename2 = Some("Michael"),
      companyName = None,
      houseNumber = Some("25"),
      address1 = "Park Avenue",
      address2 = Some("Flat 3"),
      address3 = Some("Central District"),
      address4 = Some("London"),
      postcode = Some("SW1A 2AA"),
      phone = Some("02012345678"),
      nino = Some("AB123456C"),
      isUkCompany = None,
      hasNino = Some("YES"),
      dateOfBirth = Some("1980-01-15"),
      registrationNumber = None,
      placeOfRegistration = None
    )

  private def mkCreatePurchaserReturn(
                                       purchaserResourceRef: String = "PRF-001",
                                       purchaserId: String = "PID-001"
                                     ): CreatePurchaserReturn =
    CreatePurchaserReturn(
      purchaserResourceRef = purchaserResourceRef,
      purchaserId = purchaserId
    )

  private def mkUpdatePurchaserRequest(
                                        stornId: String = "STORN12345",
                                        returnResourceRef: String = "RRF-2024-001",
                                        purchaserResourceRef: String = "PRF-001"
                                      ): UpdatePurchaserRequest =
    UpdatePurchaserRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      purchaserResourceRef = purchaserResourceRef,
      isCompany = "NO",
      isTrustee = "NO",
      isConnectedToVendor = "NO",
      isRepresentedByAgent = "YES",
      title = Some("Mr"),
      surname = Some("Jones Updated"),
      forename1 = Some("David"),
      forename2 = Some("Michael"),
      companyName = None,
      houseNumber = Some("25"),
      address1 = "Park Avenue",
      address2 = Some("Flat 3"),
      address3 = Some("Central District"),
      address4 = Some("London"),
      postcode = Some("SW1A 2AA"),
      phone = Some("02012345678"),
      nino = Some("AB123456C"),
      nextPurchaserId = Some("PID-002"),
      isUkCompany = None,
      hasNino = Some("YES"),
      dateOfBirth = Some("1980-01-15"),
      registrationNumber = None,
      placeOfRegistration = None
    )

  private def mkUpdatePurchaserReturn(updated: Boolean = true): UpdatePurchaserReturn =
    UpdatePurchaserReturn(updated = updated)

  private def mkDeletePurchaserRequest(
                                        storn: String = "STORN12345",
                                        purchaserResourceRef: String = "PRF-001",
                                        returnResourceRef: String = "RRF-2024-001"
                                      ): DeletePurchaserRequest =
    DeletePurchaserRequest(
      storn = storn,
      purchaserResourceRef = purchaserResourceRef,
      returnResourceRef = returnResourceRef
    )

  private def mkDeletePurchaserReturn(deleted: Boolean = true): DeletePurchaserReturn =
    DeletePurchaserReturn(deleted = deleted)

  private def mkCreateCompanyDetailsRequest(
                                             stornId: String = "STORN12345",
                                             returnResourceRef: String = "RRF-2024-001",
                                             purchaserResourceRef: String = "PRF-001"
                                           ): CreateCompanyDetailsRequest =
    CreateCompanyDetailsRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      purchaserResourceRef = purchaserResourceRef,
      utr = Some("1234567890"),
      vatReference = Some("GB123456789"),
      compTypeBank = Some("YES"),
      compTypeBuilder = Some("NO"),
      compTypeBuildsoc = Some("NO"),
      compTypeCentgov = Some("NO"),
      compTypeIndividual = Some("NO"),
      compTypeInsurance = Some("NO"),
      compTypeLocalauth = Some("NO"),
      compTypeOcharity = Some("NO"),
      compTypeOcompany = Some("NO"),
      compTypeOfinancial = Some("NO"),
      compTypePartship = Some("NO"),
      compTypeProperty = Some("NO"),
      compTypePubliccorp = Some("NO"),
      compTypeSoletrader = Some("NO"),
      compTypePenfund = Some("NO")
    )

  private def mkCreateCompanyDetailsReturn(
                                            companyDetailsId: String = "CID-001"
                                          ): CreateCompanyDetailsReturn =
    CreateCompanyDetailsReturn(companyDetailsId = companyDetailsId)

  private def mkUpdateCompanyDetailsRequest(
                                             stornId: String = "STORN12345",
                                             returnResourceRef: String = "RRF-2024-001",
                                             purchaserResourceRef: String = "PRF-001"
                                           ): UpdateCompanyDetailsRequest =
    UpdateCompanyDetailsRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      purchaserResourceRef = purchaserResourceRef,
      utr = Some("9876543210"),
      vatReference = Some("GB987654321"),
      compTypeBank = Some("NO"),
      compTypeBuilder = Some("YES"),
      compTypeBuildsoc = Some("NO"),
      compTypeCentgov = Some("NO"),
      compTypeIndividual = Some("NO"),
      compTypeInsurance = Some("NO"),
      compTypeLocalauth = Some("NO"),
      compTypeOcharity = Some("NO"),
      compTypeOcompany = Some("NO"),
      compTypeOfinancial = Some("NO"),
      compTypePartship = Some("NO"),
      compTypeProperty = Some("NO"),
      compTypePubliccorp = Some("NO"),
      compTypeSoletrader = Some("NO"),
      compTypePenfund = Some("NO")
    )

  private def mkUpdateCompanyDetailsReturn(updated: Boolean = true): UpdateCompanyDetailsReturn =
    UpdateCompanyDetailsReturn(updated = updated)

  private def mkDeleteCompanyDetailsRequest(
                                             storn: String = "STORN12345",
                                             returnResourceRef: String = "RRF-2024-001"
                                           ): DeleteCompanyDetailsRequest =
    DeleteCompanyDetailsRequest(
      storn = storn,
      returnResourceRef = returnResourceRef
    )

  private def mkDeleteCompanyDetailsReturn(deleted: Boolean = true): DeleteCompanyDetailsReturn =
    DeleteCompanyDetailsReturn(deleted = deleted)

  "PurchaserReturnsService createPurchaser" - {

    "must delegate to connector (happy path)" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: CreatePurchaserRequest    = mkCreatePurchaserRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn()))

      val result: CreatePurchaserReturn = service.createPurchaser(request).futureValue
      result mustBe mkCreatePurchaserReturn()

      verify(connector).createPurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new PurchaserReturnsService(connector)
      val request1: CreatePurchaserRequest    = mkCreatePurchaserRequest("STORN11111", "RRF-001")
      val request2: CreatePurchaserRequest    = mkCreatePurchaserRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn("PRF-001", "PID-001")))
      when(connector.createPurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn("PRF-002", "PID-002")))

      service.createPurchaser(request1).futureValue mustBe mkCreatePurchaserReturn("PRF-001", "PID-001")
      service.createPurchaser(request2).futureValue mustBe mkCreatePurchaserReturn("PRF-002", "PID-002")

      verify(connector).createPurchaser(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createPurchaser(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: CreatePurchaserRequest  = mkCreatePurchaserRequest()
      val boom                          = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createPurchaser(request).failed.futureValue
      ex mustBe boom

      verify(connector).createPurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: CreatePurchaserRequest  = mkCreatePurchaserRequest().copy(
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        houseNumber = None,
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = None,
        phone = None,
        nino = None,
        hasNino = None,
        dateOfBirth = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn()))

      val result: CreatePurchaserReturn = service.createPurchaser(request).futureValue
      result mustBe mkCreatePurchaserReturn()

      verify(connector).createPurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle company purchaser request" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: CreatePurchaserRequest  = mkCreatePurchaserRequest().copy(
        isCompany = "YES",
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = Some("XYZ Properties Ltd"),
        nino = None,
        hasNino = None,
        dateOfBirth = None,
        isUkCompany = Some("YES"),
        registrationNumber = Some("12345678"),
        placeOfRegistration = Some("England and Wales")
      )
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn()))

      val result: CreatePurchaserReturn = service.createPurchaser(request).futureValue
      result mustBe mkCreatePurchaserReturn()

      verify(connector).createPurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different flag combinations" in {
      val connector                            = mock[FilingFormpProxyConnector]
      val service                              = new PurchaserReturnsService(connector)
      val trusteeRequest: CreatePurchaserRequest    = mkCreatePurchaserRequest().copy(isTrustee = "YES")
      val connectedRequest: CreatePurchaserRequest  = mkCreatePurchaserRequest().copy(isConnectedToVendor = "YES")
      val noAgentRequest: CreatePurchaserRequest    = mkCreatePurchaserRequest().copy(isRepresentedByAgent = "NO")
      implicit val hc: HeaderCarrier           = HeaderCarrier()

      when(connector.createPurchaser(eqTo(trusteeRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn()))
      when(connector.createPurchaser(eqTo(connectedRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn()))
      when(connector.createPurchaser(eqTo(noAgentRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn()))

      service.createPurchaser(trusteeRequest).futureValue mustBe mkCreatePurchaserReturn()
      service.createPurchaser(connectedRequest).futureValue mustBe mkCreatePurchaserReturn()
      service.createPurchaser(noAgentRequest).futureValue mustBe mkCreatePurchaserReturn()

      verify(connector).createPurchaser(eqTo(trusteeRequest))(any[HeaderCarrier])
      verify(connector).createPurchaser(eqTo(connectedRequest))(any[HeaderCarrier])
      verify(connector).createPurchaser(eqTo(noAgentRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: CreatePurchaserRequest  = mkCreatePurchaserRequest()
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn()))

      service.createPurchaser(request).futureValue

      verify(connector, times(1)).createPurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new PurchaserReturnsService(connector)
      val request1: CreatePurchaserRequest  = mkCreatePurchaserRequest("STORN11111", "RRF-001")
      val request2: CreatePurchaserRequest  = mkCreatePurchaserRequest("STORN22222", "RRF-002")
      val request3: CreatePurchaserRequest  = mkCreatePurchaserRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn("PRF-001", "PID-001")))
      when(connector.createPurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn("PRF-002", "PID-002")))
      when(connector.createPurchaser(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn("PRF-003", "PID-003")))

      service.createPurchaser(request1).futureValue mustBe mkCreatePurchaserReturn("PRF-001", "PID-001")
      service.createPurchaser(request2).futureValue mustBe mkCreatePurchaserReturn("PRF-002", "PID-002")
      service.createPurchaser(request3).futureValue mustBe mkCreatePurchaserReturn("PRF-003", "PID-003")

      verify(connector, times(3)).createPurchaser(any[CreatePurchaserRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: CreatePurchaserRequest  = mkCreatePurchaserRequest()
      val boom                          = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createPurchaser(request).failed.futureValue
      ex mustBe boom

      verify(connector).createPurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different stornId formats" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new PurchaserReturnsService(connector)
      val request1: CreatePurchaserRequest  = mkCreatePurchaserRequest("STORN12345")
      val request2: CreatePurchaserRequest  = mkCreatePurchaserRequest("STORN-ABC-123")
      val request3: CreatePurchaserRequest  = mkCreatePurchaserRequest("12345678")
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.createPurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn("PRF-001", "PID-001")))
      when(connector.createPurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn("PRF-002", "PID-002")))
      when(connector.createPurchaser(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreatePurchaserReturn("PRF-003", "PID-003")))

      service.createPurchaser(request1).futureValue mustBe mkCreatePurchaserReturn("PRF-001", "PID-001")
      service.createPurchaser(request2).futureValue mustBe mkCreatePurchaserReturn("PRF-002", "PID-002")
      service.createPurchaser(request3).futureValue mustBe mkCreatePurchaserReturn("PRF-003", "PID-003")

      verify(connector).createPurchaser(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createPurchaser(eqTo(request2))(any[HeaderCarrier])
      verify(connector).createPurchaser(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "PurchaserReturnsService updatePurchaser" - {

    "must delegate to connector (happy path)" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: UpdatePurchaserRequest    = mkUpdatePurchaserRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn()))

      val result: UpdatePurchaserReturn = service.updatePurchaser(request).futureValue
      result mustBe mkUpdatePurchaserReturn()

      verify(connector).updatePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new PurchaserReturnsService(connector)
      val request1: UpdatePurchaserRequest    = mkUpdatePurchaserRequest("STORN11111", "RRF-001", "PRF-001")
      val request2: UpdatePurchaserRequest    = mkUpdatePurchaserRequest("STORN22222", "RRF-002", "PRF-002")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn(true)))
      when(connector.updatePurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn(true)))

      service.updatePurchaser(request1).futureValue mustBe mkUpdatePurchaserReturn(true)
      service.updatePurchaser(request2).futureValue mustBe mkUpdatePurchaserReturn(true)

      verify(connector).updatePurchaser(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updatePurchaser(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: UpdatePurchaserRequest  = mkUpdatePurchaserRequest()
      val boom                          = UpstreamErrorResponse("Not found", 404)
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updatePurchaser(request).failed.futureValue
      ex mustBe boom

      verify(connector).updatePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: UpdatePurchaserRequest  = mkUpdatePurchaserRequest().copy(
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        houseNumber = None,
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = None,
        phone = None,
        nino = None,
        nextPurchaserId = None,
        hasNino = None,
        dateOfBirth = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn()))

      val result: UpdatePurchaserReturn = service.updatePurchaser(request).futureValue
      result mustBe mkUpdatePurchaserReturn()

      verify(connector).updatePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle request with nextPurchaserId populated" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: UpdatePurchaserRequest  = mkUpdatePurchaserRequest().copy(nextPurchaserId = Some("PID-999"))
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn()))

      val result: UpdatePurchaserReturn = service.updatePurchaser(request).futureValue
      result mustBe mkUpdatePurchaserReturn()

      verify(connector).updatePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: UpdatePurchaserRequest  = mkUpdatePurchaserRequest()
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn()))

      service.updatePurchaser(request).futureValue

      verify(connector, times(1)).updatePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new PurchaserReturnsService(connector)
      val request1: UpdatePurchaserRequest  = mkUpdatePurchaserRequest("STORN11111", "RRF-001", "PRF-001")
      val request2: UpdatePurchaserRequest  = mkUpdatePurchaserRequest("STORN22222", "RRF-002", "PRF-002")
      val request3: UpdatePurchaserRequest  = mkUpdatePurchaserRequest("STORN33333", "RRF-003", "PRF-003")
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn(true)))
      when(connector.updatePurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn(true)))
      when(connector.updatePurchaser(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn(true)))

      service.updatePurchaser(request1).futureValue mustBe mkUpdatePurchaserReturn(true)
      service.updatePurchaser(request2).futureValue mustBe mkUpdatePurchaserReturn(true)
      service.updatePurchaser(request3).futureValue mustBe mkUpdatePurchaserReturn(true)

      verify(connector, times(3)).updatePurchaser(any[UpdatePurchaserRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: UpdatePurchaserRequest  = mkUpdatePurchaserRequest()
      val boom                          = new RuntimeException("Connection timeout")
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updatePurchaser(request).failed.futureValue
      ex mustBe boom

      verify(connector).updatePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update result with false status" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: UpdatePurchaserRequest  = mkUpdatePurchaserRequest()
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.updatePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdatePurchaserReturn(false)))

      val result: UpdatePurchaserReturn = service.updatePurchaser(request).futureValue
      result mustBe mkUpdatePurchaserReturn(false)
      result.updated mustBe false

      verify(connector).updatePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "PurchaserReturnsService deletePurchaser" - {

    "must delegate to connector (happy path)" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: DeletePurchaserRequest    = mkDeletePurchaserRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn()))

      val result: DeletePurchaserReturn = service.deletePurchaser(request).futureValue
      result mustBe mkDeletePurchaserReturn()

      verify(connector).deletePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new PurchaserReturnsService(connector)
      val request1: DeletePurchaserRequest    = mkDeletePurchaserRequest("STORN11111", "PRF-001", "RRF-001")
      val request2: DeletePurchaserRequest    = mkDeletePurchaserRequest("STORN22222", "PRF-002", "RRF-002")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))

      service.deletePurchaser(request1).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request2).futureValue mustBe mkDeletePurchaserReturn(true)

      verify(connector).deletePurchaser(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deletePurchaser(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: DeletePurchaserRequest  = mkDeletePurchaserRequest()
      val boom                          = UpstreamErrorResponse("Internal Server Error", 500)
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deletePurchaser(request).failed.futureValue
      ex mustBe boom

      verify(connector).deletePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: DeletePurchaserRequest  = mkDeletePurchaserRequest()
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn()))

      service.deletePurchaser(request).futureValue

      verify(connector, times(1)).deletePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new PurchaserReturnsService(connector)
      val request1: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN11111", "PRF-001", "RRF-001")
      val request2: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN22222", "PRF-002", "RRF-002")
      val request3: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN33333", "PRF-003", "RRF-003")
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))

      service.deletePurchaser(request1).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request2).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request3).futureValue mustBe mkDeletePurchaserReturn(true)

      verify(connector, times(3)).deletePurchaser(any[DeletePurchaserRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: DeletePurchaserRequest  = mkDeletePurchaserRequest()
      val boom                          = new RuntimeException("Network error")
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deletePurchaser(request).failed.futureValue
      ex mustBe boom

      verify(connector).deletePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle delete result with false status" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new PurchaserReturnsService(connector)
      val request: DeletePurchaserRequest  = mkDeletePurchaserRequest()
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(false)))

      val result: DeletePurchaserReturn = service.deletePurchaser(request).futureValue
      result mustBe mkDeletePurchaserReturn(false)
      result.deleted mustBe false

      verify(connector).deletePurchaser(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new PurchaserReturnsService(connector)
      val request1: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN12345", "PRF-001", "RRF-001")
      val request2: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN-ABC-123", "PRF-001", "RRF-001")
      val request3: DeletePurchaserRequest  = mkDeletePurchaserRequest("12345678", "PRF-001", "RRF-001")
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))

      service.deletePurchaser(request1).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request2).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request3).futureValue mustBe mkDeletePurchaserReturn(true)

      verify(connector).deletePurchaser(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deletePurchaser(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deletePurchaser(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different purchaserResourceRef formats" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new PurchaserReturnsService(connector)
      val request1: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN12345", "PRF-001", "RRF-001")
      val request2: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN12345", "123456", "RRF-001")
      val request3: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN12345", "ABC-123-XYZ", "RRF-001")
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))

      service.deletePurchaser(request1).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request2).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request3).futureValue mustBe mkDeletePurchaserReturn(true)

      verify(connector).deletePurchaser(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deletePurchaser(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deletePurchaser(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different returnResourceRef formats" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new PurchaserReturnsService(connector)
      val request1: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN12345", "PRF-001", "RRF-001")
      val request2: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN12345", "PRF-001", "RRF-ABC-123")
      val request3: DeletePurchaserRequest  = mkDeletePurchaserRequest("STORN12345", "PRF-001", "12345678")
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.deletePurchaser(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))
      when(connector.deletePurchaser(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeletePurchaserReturn(true)))

      service.deletePurchaser(request1).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request2).futureValue mustBe mkDeletePurchaserReturn(true)
      service.deletePurchaser(request3).futureValue mustBe mkDeletePurchaserReturn(true)

      verify(connector).deletePurchaser(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deletePurchaser(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deletePurchaser(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "PurchaserReturnsService createCompanyDetails" - {

    "must delegate to connector (happy path)" in {
      val connector                               = mock[FilingFormpProxyConnector]
      val service                                 = new PurchaserReturnsService(connector)
      val request: CreateCompanyDetailsRequest    = mkCreateCompanyDetailsRequest()
      implicit val hc: HeaderCarrier              = HeaderCarrier()

      when(connector.createCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn()))

      val result: CreateCompanyDetailsReturn = service.createCompanyDetails(request).futureValue
      result mustBe mkCreateCompanyDetailsReturn()

      verify(connector).createCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                                = mock[FilingFormpProxyConnector]
      val service                                  = new PurchaserReturnsService(connector)
      val request1: CreateCompanyDetailsRequest    = mkCreateCompanyDetailsRequest("STORN11111", "RRF-001", "PRF-001")
      val request2: CreateCompanyDetailsRequest    = mkCreateCompanyDetailsRequest("STORN22222", "RRF-002", "PRF-002")
      implicit val hc: HeaderCarrier               = HeaderCarrier()

      when(connector.createCompanyDetails(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn("CID-001")))
      when(connector.createCompanyDetails(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn("CID-002")))

      service.createCompanyDetails(request1).futureValue mustBe mkCreateCompanyDetailsReturn("CID-001")
      service.createCompanyDetails(request2).futureValue mustBe mkCreateCompanyDetailsReturn("CID-002")

      verify(connector).createCompanyDetails(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createCompanyDetails(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: CreateCompanyDetailsRequest  = mkCreateCompanyDetailsRequest()
      val boom                               = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.createCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(connector).createCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: CreateCompanyDetailsRequest  = mkCreateCompanyDetailsRequest().copy(
        utr = None,
        vatReference = None,
        compTypeBank = None,
        compTypeBuilder = None,
        compTypeBuildsoc = None,
        compTypeCentgov = None,
        compTypeIndividual = None,
        compTypeInsurance = None,
        compTypeLocalauth = None,
        compTypeOcharity = None,
        compTypeOcompany = None,
        compTypeOfinancial = None,
        compTypePartship = None,
        compTypeProperty = None,
        compTypePubliccorp = None,
        compTypeSoletrader = None,
        compTypePenfund = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn()))

      val result: CreateCompanyDetailsReturn = service.createCompanyDetails(request).futureValue
      result mustBe mkCreateCompanyDetailsReturn()

      verify(connector).createCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different company type combinations" in {
      val connector                                     = mock[FilingFormpProxyConnector]
      val service                                       = new PurchaserReturnsService(connector)
      val propertyRequest: CreateCompanyDetailsRequest  = mkCreateCompanyDetailsRequest().copy(
        compTypeBank = Some("NO"),
        compTypeProperty = Some("YES")
      )
      val charityRequest: CreateCompanyDetailsRequest   = mkCreateCompanyDetailsRequest().copy(
        compTypeBank = Some("NO"),
        compTypeOcharity = Some("YES")
      )
      val partnershipRequest: CreateCompanyDetailsRequest = mkCreateCompanyDetailsRequest().copy(
        compTypeBank = Some("NO"),
        compTypePartship = Some("YES")
      )
      implicit val hc: HeaderCarrier                    = HeaderCarrier()

      when(connector.createCompanyDetails(eqTo(propertyRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn()))
      when(connector.createCompanyDetails(eqTo(charityRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn()))
      when(connector.createCompanyDetails(eqTo(partnershipRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn()))

      service.createCompanyDetails(propertyRequest).futureValue mustBe mkCreateCompanyDetailsReturn()
      service.createCompanyDetails(charityRequest).futureValue mustBe mkCreateCompanyDetailsReturn()
      service.createCompanyDetails(partnershipRequest).futureValue mustBe mkCreateCompanyDetailsReturn()

      verify(connector).createCompanyDetails(eqTo(propertyRequest))(any[HeaderCarrier])
      verify(connector).createCompanyDetails(eqTo(charityRequest))(any[HeaderCarrier])
      verify(connector).createCompanyDetails(eqTo(partnershipRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: CreateCompanyDetailsRequest  = mkCreateCompanyDetailsRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.createCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn()))

      service.createCompanyDetails(request).futureValue

      verify(connector, times(1)).createCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new PurchaserReturnsService(connector)
      val request1: CreateCompanyDetailsRequest  = mkCreateCompanyDetailsRequest("STORN11111", "RRF-001", "PRF-001")
      val request2: CreateCompanyDetailsRequest  = mkCreateCompanyDetailsRequest("STORN22222", "RRF-002", "PRF-002")
      val request3: CreateCompanyDetailsRequest  = mkCreateCompanyDetailsRequest("STORN33333", "RRF-003", "PRF-003")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.createCompanyDetails(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn("CID-001")))
      when(connector.createCompanyDetails(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn("CID-002")))
      when(connector.createCompanyDetails(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateCompanyDetailsReturn("CID-003")))

      service.createCompanyDetails(request1).futureValue mustBe mkCreateCompanyDetailsReturn("CID-001")
      service.createCompanyDetails(request2).futureValue mustBe mkCreateCompanyDetailsReturn("CID-002")
      service.createCompanyDetails(request3).futureValue mustBe mkCreateCompanyDetailsReturn("CID-003")

      verify(connector, times(3)).createCompanyDetails(any[CreateCompanyDetailsRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: CreateCompanyDetailsRequest  = mkCreateCompanyDetailsRequest()
      val boom                               = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.createCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(connector).createCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "PurchaserReturnsService updateCompanyDetails" - {

    "must delegate to connector (happy path)" in {
      val connector                               = mock[FilingFormpProxyConnector]
      val service                                 = new PurchaserReturnsService(connector)
      val request: UpdateCompanyDetailsRequest    = mkUpdateCompanyDetailsRequest()
      implicit val hc: HeaderCarrier              = HeaderCarrier()

      when(connector.updateCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateCompanyDetailsReturn()))

      val result: UpdateCompanyDetailsReturn = service.updateCompanyDetails(request).futureValue
      result mustBe mkUpdateCompanyDetailsReturn()

      verify(connector).updateCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                                = mock[FilingFormpProxyConnector]
      val service                                  = new PurchaserReturnsService(connector)
      val request1: UpdateCompanyDetailsRequest    = mkUpdateCompanyDetailsRequest("STORN11111", "RRF-001", "PRF-001")
      val request2: UpdateCompanyDetailsRequest    = mkUpdateCompanyDetailsRequest("STORN22222", "RRF-002", "PRF-002")
      implicit val hc: HeaderCarrier               = HeaderCarrier()

      when(connector.updateCompanyDetails(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateCompanyDetailsReturn(true)))
      when(connector.updateCompanyDetails(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateCompanyDetailsReturn(true)))

      service.updateCompanyDetails(request1).futureValue mustBe mkUpdateCompanyDetailsReturn(true)
      service.updateCompanyDetails(request2).futureValue mustBe mkUpdateCompanyDetailsReturn(true)

      verify(connector).updateCompanyDetails(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateCompanyDetails(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: UpdateCompanyDetailsRequest  = mkUpdateCompanyDetailsRequest()
      val boom                               = UpstreamErrorResponse("Not found", 404)
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: UpdateCompanyDetailsRequest  = mkUpdateCompanyDetailsRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateCompanyDetailsReturn()))

      service.updateCompanyDetails(request).futureValue

      verify(connector, times(1)).updateCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new PurchaserReturnsService(connector)
      val request1: UpdateCompanyDetailsRequest  = mkUpdateCompanyDetailsRequest("STORN11111", "RRF-001", "PRF-001")
      val request2: UpdateCompanyDetailsRequest  = mkUpdateCompanyDetailsRequest("STORN22222", "RRF-002", "PRF-002")
      val request3: UpdateCompanyDetailsRequest  = mkUpdateCompanyDetailsRequest("STORN33333", "RRF-003", "PRF-003")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.updateCompanyDetails(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateCompanyDetailsReturn(true)))
      when(connector.updateCompanyDetails(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateCompanyDetailsReturn(true)))
      when(connector.updateCompanyDetails(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateCompanyDetailsReturn(true)))

      service.updateCompanyDetails(request1).futureValue mustBe mkUpdateCompanyDetailsReturn(true)
      service.updateCompanyDetails(request2).futureValue mustBe mkUpdateCompanyDetailsReturn(true)
      service.updateCompanyDetails(request3).futureValue mustBe mkUpdateCompanyDetailsReturn(true)

      verify(connector, times(3)).updateCompanyDetails(any[UpdateCompanyDetailsRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: UpdateCompanyDetailsRequest  = mkUpdateCompanyDetailsRequest()
      val boom                               = new RuntimeException("Connection timeout")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update result with false status" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: UpdateCompanyDetailsRequest  = mkUpdateCompanyDetailsRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateCompanyDetailsReturn(false)))

      val result: UpdateCompanyDetailsReturn = service.updateCompanyDetails(request).futureValue
      result mustBe mkUpdateCompanyDetailsReturn(false)
      result.updated mustBe false

      verify(connector).updateCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "PurchaserReturnsService deleteCompanyDetails" - {

    "must delegate to connector (happy path)" in {
      val connector                               = mock[FilingFormpProxyConnector]
      val service                                 = new PurchaserReturnsService(connector)
      val request: DeleteCompanyDetailsRequest    = mkDeleteCompanyDetailsRequest()
      implicit val hc: HeaderCarrier              = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn()))

      val result: DeleteCompanyDetailsReturn = service.deleteCompanyDetails(request).futureValue
      result mustBe mkDeleteCompanyDetailsReturn()

      verify(connector).deleteCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                                = mock[FilingFormpProxyConnector]
      val service                                  = new PurchaserReturnsService(connector)
      val request1: DeleteCompanyDetailsRequest    = mkDeleteCompanyDetailsRequest("STORN11111", "RRF-001")
      val request2: DeleteCompanyDetailsRequest    = mkDeleteCompanyDetailsRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier               = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))
      when(connector.deleteCompanyDetails(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))

      service.deleteCompanyDetails(request1).futureValue mustBe mkDeleteCompanyDetailsReturn(true)
      service.deleteCompanyDetails(request2).futureValue mustBe mkDeleteCompanyDetailsReturn(true)

      verify(connector).deleteCompanyDetails(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteCompanyDetails(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest()
      val boom                               = UpstreamErrorResponse("Internal Server Error", 500)
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn()))

      service.deleteCompanyDetails(request).futureValue

      verify(connector, times(1)).deleteCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new PurchaserReturnsService(connector)
      val request1: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("STORN11111", "RRF-001")
      val request2: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("STORN22222", "RRF-002")
      val request3: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))
      when(connector.deleteCompanyDetails(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))
      when(connector.deleteCompanyDetails(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))

      service.deleteCompanyDetails(request1).futureValue mustBe mkDeleteCompanyDetailsReturn(true)
      service.deleteCompanyDetails(request2).futureValue mustBe mkDeleteCompanyDetailsReturn(true)
      service.deleteCompanyDetails(request3).futureValue mustBe mkDeleteCompanyDetailsReturn(true)

      verify(connector, times(3)).deleteCompanyDetails(any[DeleteCompanyDetailsRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest()
      val boom                               = new RuntimeException("Network error")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle delete result with false status" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new PurchaserReturnsService(connector)
      val request: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(false)))

      val result: DeleteCompanyDetailsReturn = service.deleteCompanyDetails(request).futureValue
      result mustBe mkDeleteCompanyDetailsReturn(false)
      result.deleted mustBe false

      verify(connector).deleteCompanyDetails(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new PurchaserReturnsService(connector)
      val request1: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("STORN12345", "RRF-001")
      val request2: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("STORN-ABC-123", "RRF-001")
      val request3: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("12345678", "RRF-001")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))
      when(connector.deleteCompanyDetails(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))
      when(connector.deleteCompanyDetails(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))

      service.deleteCompanyDetails(request1).futureValue mustBe mkDeleteCompanyDetailsReturn(true)
      service.deleteCompanyDetails(request2).futureValue mustBe mkDeleteCompanyDetailsReturn(true)
      service.deleteCompanyDetails(request3).futureValue mustBe mkDeleteCompanyDetailsReturn(true)

      verify(connector).deleteCompanyDetails(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteCompanyDetails(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteCompanyDetails(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different returnResourceRef formats" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new PurchaserReturnsService(connector)
      val request1: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("STORN12345", "RRF-001")
      val request2: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("STORN12345", "123456")
      val request3: DeleteCompanyDetailsRequest  = mkDeleteCompanyDetailsRequest("STORN12345", "ABC-123-XYZ")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.deleteCompanyDetails(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))
      when(connector.deleteCompanyDetails(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))
      when(connector.deleteCompanyDetails(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteCompanyDetailsReturn(true)))

      service.deleteCompanyDetails(request1).futureValue mustBe mkDeleteCompanyDetailsReturn(true)
      service.deleteCompanyDetails(request2).futureValue mustBe mkDeleteCompanyDetailsReturn(true)
      service.deleteCompanyDetails(request3).futureValue mustBe mkDeleteCompanyDetailsReturn(true)

      verify(connector).deleteCompanyDetails(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteCompanyDetails(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteCompanyDetails(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}