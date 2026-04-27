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
import service.filing.VendorReturnsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class VendorReturnsServiceSpec extends SpecBase {

  private def mkCreateVendorRequest(
      stornId: String = "STORN12345",
      returnResourceRef: String = "RRF-2024-001"
  ): CreateVendorRequest =
    CreateVendorRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      title = Some("Mr"),
      forename1 = Some("John"),
      forename2 = Some("Paul"),
      name = "Smith",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Apartment 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = Some("TE23 5TT"),
      isRepresentedByAgent = "YES"
    )

  private def mkCreateVendorReturn(
      vendorResourceRef: String = "VRF-001",
      vendorId: String = "VID-001"
  ): CreateVendorReturn =
    CreateVendorReturn(
      vendorResourceRef = vendorResourceRef,
      vendorId = vendorId
    )

  private def mkUpdateVendorRequest(
      stornId: String = "STORN12345",
      returnResourceRef: String = "RRF-2024-001",
      vendorResourceRef: String = "VRF-001"
  ): UpdateVendorRequest =
    UpdateVendorRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      title = Some("Mr"),
      forename1 = Some("John"),
      forename2 = Some("Paul"),
      name = "Smith Updated",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Apartment 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = Some("TE23 5TT"),
      isRepresentedByAgent = "YES",
      vendorResourceRef = vendorResourceRef,
      nextVendorId = Some("VID-002")
    )

  private def mkUpdateVendorReturn(
      updated: Boolean = true
  ): UpdateVendorReturn =
    UpdateVendorReturn(updated = updated)

  private def mkDeleteVendorRequest(
      storn: String = "STORN12345",
      vendorResourceRef: String = "VRF-001",
      returnResourceRef: String = "VID-001"
  ): DeleteVendorRequest =
    DeleteVendorRequest(
      storn = storn,
      vendorResourceRef = vendorResourceRef,
      returnResourceRef = returnResourceRef
    )

  private def mkDeleteVendorReturn(
      deleted: Boolean = true
  ): DeleteVendorReturn =
    DeleteVendorReturn(deleted = deleted)

  "VendorReturnsService createVendor" - {

    "must delegate to connector (happy path)" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: CreateVendorRequest = mkCreateVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateVendorReturn()))

      val result: CreateVendorReturn = service.createVendor(request).futureValue
      result mustBe mkCreateVendorReturn()

      verify(connector).createVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: CreateVendorRequest =
        mkCreateVendorRequest("STORN11111", "RRF-001")
      val request2: CreateVendorRequest =
        mkCreateVendorRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(mkCreateVendorReturn("VRF-001", "VID-001"))
        )
      when(connector.createVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(mkCreateVendorReturn("VRF-002", "VID-002"))
        )

      service.createVendor(request1).futureValue mustBe mkCreateVendorReturn(
        "VRF-001",
        "VID-001"
      )
      service.createVendor(request2).futureValue mustBe mkCreateVendorReturn(
        "VRF-002",
        "VID-002"
      )

      verify(connector).createVendor(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createVendor(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: CreateVendorRequest = mkCreateVendorRequest()
      val boom = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createVendor(request).failed.futureValue
      ex mustBe boom

      verify(connector).createVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: CreateVendorRequest = mkCreateVendorRequest().copy(
        title = None,
        forename1 = None,
        forename2 = None,
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateVendorReturn()))

      val result: CreateVendorReturn = service.createVendor(request).futureValue
      result mustBe mkCreateVendorReturn()

      verify(connector).createVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle request with all optional fields populated" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: CreateVendorRequest = mkCreateVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateVendorReturn()))

      val result: CreateVendorReturn = service.createVendor(request).futureValue
      result mustBe mkCreateVendorReturn()

      verify(connector).createVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different isRepresentedByAgent values" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val yesRequest: CreateVendorRequest =
        mkCreateVendorRequest().copy(isRepresentedByAgent = "YES")
      val noRequest: CreateVendorRequest =
        mkCreateVendorRequest().copy(isRepresentedByAgent = "NO")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(yesRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateVendorReturn()))
      when(connector.createVendor(eqTo(noRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateVendorReturn()))

      service.createVendor(yesRequest).futureValue mustBe mkCreateVendorReturn()
      service.createVendor(noRequest).futureValue mustBe mkCreateVendorReturn()

      verify(connector).createVendor(eqTo(yesRequest))(any[HeaderCarrier])
      verify(connector).createVendor(eqTo(noRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: CreateVendorRequest = mkCreateVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateVendorReturn()))

      service.createVendor(request).futureValue

      verify(connector, times(1)).createVendor(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: CreateVendorRequest =
        mkCreateVendorRequest("STORN11111", "RRF-001")
      val request2: CreateVendorRequest =
        mkCreateVendorRequest("STORN22222", "RRF-002")
      val request3: CreateVendorRequest =
        mkCreateVendorRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(mkCreateVendorReturn("VRF-001", "VID-001"))
        )
      when(connector.createVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(mkCreateVendorReturn("VRF-002", "VID-002"))
        )
      when(connector.createVendor(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(mkCreateVendorReturn("VRF-003", "VID-003"))
        )

      service.createVendor(request1).futureValue mustBe mkCreateVendorReturn(
        "VRF-001",
        "VID-001"
      )
      service.createVendor(request2).futureValue mustBe mkCreateVendorReturn(
        "VRF-002",
        "VID-002"
      )
      service.createVendor(request3).futureValue mustBe mkCreateVendorReturn(
        "VRF-003",
        "VID-003"
      )

      verify(connector, times(3)).createVendor(any[CreateVendorRequest])(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: CreateVendorRequest = mkCreateVendorRequest()
      val boom = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createVendor(request).failed.futureValue
      ex mustBe boom

      verify(connector).createVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different stornId formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: CreateVendorRequest = mkCreateVendorRequest("STORN12345")
      val request2: CreateVendorRequest = mkCreateVendorRequest("STORN-ABC-123")
      val request3: CreateVendorRequest = mkCreateVendorRequest("12345678")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(mkCreateVendorReturn("VRF-001", "VID-001"))
        )
      when(connector.createVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(mkCreateVendorReturn("VRF-002", "VID-002"))
        )
      when(connector.createVendor(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(mkCreateVendorReturn("VRF-003", "VID-003"))
        )

      service.createVendor(request1).futureValue mustBe mkCreateVendorReturn(
        "VRF-001",
        "VID-001"
      )
      service.createVendor(request2).futureValue mustBe mkCreateVendorReturn(
        "VRF-002",
        "VID-002"
      )
      service.createVendor(request3).futureValue mustBe mkCreateVendorReturn(
        "VRF-003",
        "VID-003"
      )

      verify(connector).createVendor(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createVendor(eqTo(request2))(any[HeaderCarrier])
      verify(connector).createVendor(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "VendorReturnsService updateVendor" - {

    "must delegate to connector (happy path)" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: UpdateVendorRequest = mkUpdateVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn()))

      val result: UpdateVendorReturn = service.updateVendor(request).futureValue
      result mustBe mkUpdateVendorReturn()

      verify(connector).updateVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: UpdateVendorRequest =
        mkUpdateVendorRequest("STORN11111", "RRF-001", "VRF-001")
      val request2: UpdateVendorRequest =
        mkUpdateVendorRequest("STORN22222", "RRF-002", "VRF-002")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn(true)))
      when(connector.updateVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn(true)))

      service.updateVendor(request1).futureValue mustBe mkUpdateVendorReturn(
        true
      )
      service.updateVendor(request2).futureValue mustBe mkUpdateVendorReturn(
        true
      )

      verify(connector).updateVendor(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateVendor(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: UpdateVendorRequest = mkUpdateVendorRequest()
      val boom = UpstreamErrorResponse("Not found", 404)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateVendor(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: UpdateVendorRequest = mkUpdateVendorRequest().copy(
        title = None,
        forename1 = None,
        forename2 = None,
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        nextVendorId = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn()))

      val result: UpdateVendorReturn = service.updateVendor(request).futureValue
      result mustBe mkUpdateVendorReturn()

      verify(connector).updateVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle request with nextVendorId populated" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: UpdateVendorRequest =
        mkUpdateVendorRequest().copy(nextVendorId = Some("VID-999"))
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn()))

      val result: UpdateVendorReturn = service.updateVendor(request).futureValue
      result mustBe mkUpdateVendorReturn()

      verify(connector).updateVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: UpdateVendorRequest = mkUpdateVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn()))

      service.updateVendor(request).futureValue

      verify(connector, times(1)).updateVendor(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: UpdateVendorRequest =
        mkUpdateVendorRequest("STORN11111", "RRF-001", "VRF-001")
      val request2: UpdateVendorRequest =
        mkUpdateVendorRequest("STORN22222", "RRF-002", "VRF-002")
      val request3: UpdateVendorRequest =
        mkUpdateVendorRequest("STORN33333", "RRF-003", "VRF-003")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn(true)))
      when(connector.updateVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn(true)))
      when(connector.updateVendor(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn(true)))

      service.updateVendor(request1).futureValue mustBe mkUpdateVendorReturn(
        true
      )
      service.updateVendor(request2).futureValue mustBe mkUpdateVendorReturn(
        true
      )
      service.updateVendor(request3).futureValue mustBe mkUpdateVendorReturn(
        true
      )

      verify(connector, times(3)).updateVendor(any[UpdateVendorRequest])(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: UpdateVendorRequest = mkUpdateVendorRequest()
      val boom = new RuntimeException("Connection timeout")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateVendor(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update result with false status" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: UpdateVendorRequest = mkUpdateVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateVendorReturn(false)))

      val result: UpdateVendorReturn = service.updateVendor(request).futureValue
      result mustBe mkUpdateVendorReturn(false)
      result.updated mustBe false

      verify(connector).updateVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "VendorReturnsService deleteVendor" - {

    "must delegate to connector (happy path)" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: DeleteVendorRequest = mkDeleteVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn()))

      val result: DeleteVendorReturn = service.deleteVendor(request).futureValue
      result mustBe mkDeleteVendorReturn()

      verify(connector).deleteVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN11111", "VRF-001", "VID-001")
      val request2: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN22222", "VRF-002", "VID-002")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))

      service.deleteVendor(request1).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request2).futureValue mustBe mkDeleteVendorReturn(
        true
      )

      verify(connector).deleteVendor(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteVendor(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: DeleteVendorRequest = mkDeleteVendorRequest()
      val boom = UpstreamErrorResponse("Internal Server Error", 500)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteVendor(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: DeleteVendorRequest = mkDeleteVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn()))

      service.deleteVendor(request).futureValue

      verify(connector, times(1)).deleteVendor(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN11111", "VRF-001", "VID-001")
      val request2: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN22222", "VRF-002", "VID-002")
      val request3: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN33333", "VRF-003", "VID-003")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))

      service.deleteVendor(request1).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request2).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request3).futureValue mustBe mkDeleteVendorReturn(
        true
      )

      verify(connector, times(3)).deleteVendor(any[DeleteVendorRequest])(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: DeleteVendorRequest = mkDeleteVendorRequest()
      val boom = new RuntimeException("Network error")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteVendor(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle delete result with false status" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request: DeleteVendorRequest = mkDeleteVendorRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(false)))

      val result: DeleteVendorReturn = service.deleteVendor(request).futureValue
      result mustBe mkDeleteVendorReturn(false)
      result.deleted mustBe false

      verify(connector).deleteVendor(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN12345", "VRF-001", "VID-001")
      val request2: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN-ABC-123", "VRF-001", "VID-001")
      val request3: DeleteVendorRequest =
        mkDeleteVendorRequest("12345678", "VRF-001", "VID-001")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))

      service.deleteVendor(request1).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request2).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request3).futureValue mustBe mkDeleteVendorReturn(
        true
      )

      verify(connector).deleteVendor(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteVendor(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteVendor(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different vendorResourceRef formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN12345", "VRF-001", "VID-001")
      val request2: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN12345", "123456", "VID-001")
      val request3: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN12345", "ABC-123-XYZ", "VID-001")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))

      service.deleteVendor(request1).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request2).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request3).futureValue mustBe mkDeleteVendorReturn(
        true
      )

      verify(connector).deleteVendor(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteVendor(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteVendor(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different vendorId formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new VendorReturnsService(connector)
      val request1: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN12345", "VRF-001", "VID-001")
      val request2: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN12345", "VRF-001", "VID-ABC-123")
      val request3: DeleteVendorRequest =
        mkDeleteVendorRequest("STORN12345", "VRF-001", "12345678")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteVendor(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))
      when(connector.deleteVendor(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteVendorReturn(true)))

      service.deleteVendor(request1).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request2).futureValue mustBe mkDeleteVendorReturn(
        true
      )
      service.deleteVendor(request3).futureValue mustBe mkDeleteVendorReturn(
        true
      )

      verify(connector).deleteVendor(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteVendor(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteVendor(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}
