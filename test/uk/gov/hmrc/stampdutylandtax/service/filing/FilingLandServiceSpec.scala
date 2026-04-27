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
import service.filing.FilingLandService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class FilingLandServiceSpec extends SpecBase {

  private def mkCreateLandRequest(
      stornId: String = "STORN12345",
      returnResourceRef: String = "100001"
  ): CreateLandRequest =
    CreateLandRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      propertyType = "RESIDENTIAL",
      interestTransferredCreated = "FREEHOLD",
      houseNumber = Some("42"),
      addressLine1 = "High Street",
      addressLine2 = Some("Kensington"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      postcode = Some("SW1A 1AA"),
      landArea = Some("500"),
      areaUnit = Some("SQUARE_METERS"),
      localAuthorityNumber = Some("LA12345"),
      mineralRights = Some("YES"),
      nlpgUprn = Some("100012345678"),
      willSendPlansByPost = Some("NO"),
      titleNumber = Some("TN123456")
    )

  private def mkCreateLandReturn(
      landResourceRef: String = "100001",
      landId: String = "1"
  ): CreateLandReturn =
    CreateLandReturn(
      landResourceRef = landResourceRef,
      landId = landId
    )

  private def mkUpdateLandRequest(
      stornId: String = "STORN12345",
      returnResourceRef: String = "100001",
      landResourceRef: String = "100001"
  ): UpdateLandRequest =
    UpdateLandRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      landResourceRef = landResourceRef,
      propertyType = "RESIDENTIAL",
      interestTransferredCreated = "FREEHOLD",
      houseNumber = Some("42"),
      addressLine1 = "High Street",
      addressLine2 = Some("Kensington"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      postcode = Some("SW1A 1AA"),
      landArea = Some("500"),
      areaUnit = Some("SQUARE_METERS"),
      localAuthorityNumber = Some("LA12345"),
      mineralRights = Some("YES"),
      nlpgUprn = Some("100012345678"),
      willSendPlansByPost = Some("NO"),
      titleNumber = Some("TN123456"),
      nextLandId = Some("100002")
    )

  private def mkUpdateLandReturn(updated: Boolean = true): UpdateLandReturn =
    UpdateLandReturn(updated = updated)

  private def mkDeleteLandRequest(
      storn: String = "STORN12345",
      returnResourceRef: String = "100001",
      landResourceRef: String = "100001"
  ): DeleteLandRequest =
    DeleteLandRequest(
      storn = storn,
      returnResourceRef = returnResourceRef,
      landResourceRef = landResourceRef
    )

  private def mkDeleteLandReturn(deleted: Boolean = true): DeleteLandReturn =
    DeleteLandReturn(deleted = deleted)

  "FilingLandService createLand" - {

    "must delegate to connector (happy path)" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: CreateLandRequest = mkCreateLandRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))

      val result: CreateLandReturn = service.createLand(request).futureValue
      result mustBe mkCreateLandReturn()

      verify(connector).createLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request1: CreateLandRequest =
        mkCreateLandRequest("STORN11111", "100001")
      val request2: CreateLandRequest =
        mkCreateLandRequest("STORN22222", "100002")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn("100001", "1")))
      when(connector.createLand(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn("100002", "2")))

      service.createLand(request1).futureValue mustBe mkCreateLandReturn(
        "100001",
        "1"
      )
      service.createLand(request2).futureValue mustBe mkCreateLandReturn(
        "100002",
        "2"
      )

      verify(connector).createLand(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createLand(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: CreateLandRequest = mkCreateLandRequest()
      val boom = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createLand(request).failed.futureValue
      ex mustBe boom

      verify(connector).createLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle residential property requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: CreateLandRequest = mkCreateLandRequest().copy(
        propertyType = "RESIDENTIAL",
        interestTransferredCreated = "FREEHOLD"
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))

      val result: CreateLandReturn = service.createLand(request).futureValue
      result mustBe mkCreateLandReturn()

      verify(connector).createLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: CreateLandRequest = mkCreateLandRequest().copy(
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        landArea = None,
        areaUnit = None,
        localAuthorityNumber = None,
        mineralRights = None,
        nlpgUprn = None,
        willSendPlansByPost = None,
        titleNumber = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))

      val result: CreateLandReturn = service.createLand(request).futureValue
      result mustBe mkCreateLandReturn()

      verify(connector).createLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle request with all optional fields populated" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: CreateLandRequest = mkCreateLandRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))

      val result: CreateLandReturn = service.createLand(request).futureValue
      result mustBe mkCreateLandReturn()

      verify(connector).createLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different property types" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val residentialRequest: CreateLandRequest =
        mkCreateLandRequest().copy(propertyType = "RESIDENTIAL")
      val nonResidentialRequest: CreateLandRequest =
        mkCreateLandRequest().copy(propertyType = "NON_RESIDENTIAL")
      val mixedRequest: CreateLandRequest =
        mkCreateLandRequest().copy(propertyType = "MIXED")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(residentialRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))
      when(
        connector.createLand(eqTo(nonResidentialRequest))(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(mkCreateLandReturn()))
      when(connector.createLand(eqTo(mixedRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))

      service
        .createLand(residentialRequest)
        .futureValue mustBe mkCreateLandReturn()
      service
        .createLand(nonResidentialRequest)
        .futureValue mustBe mkCreateLandReturn()
      service.createLand(mixedRequest).futureValue mustBe mkCreateLandReturn()

      verify(connector).createLand(eqTo(residentialRequest))(any[HeaderCarrier])
      verify(connector).createLand(eqTo(nonResidentialRequest))(
        any[HeaderCarrier]
      )
      verify(connector).createLand(eqTo(mixedRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different interest types" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val freeholdRequest: CreateLandRequest =
        mkCreateLandRequest().copy(interestTransferredCreated = "FREEHOLD")
      val leaseholdRequest: CreateLandRequest =
        mkCreateLandRequest().copy(interestTransferredCreated = "LEASEHOLD")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(freeholdRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))
      when(connector.createLand(eqTo(leaseholdRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))

      service
        .createLand(freeholdRequest)
        .futureValue mustBe mkCreateLandReturn()
      service
        .createLand(leaseholdRequest)
        .futureValue mustBe mkCreateLandReturn()

      verify(connector).createLand(eqTo(freeholdRequest))(any[HeaderCarrier])
      verify(connector).createLand(eqTo(leaseholdRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: CreateLandRequest = mkCreateLandRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn()))

      service.createLand(request).futureValue

      verify(connector, times(1)).createLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request1: CreateLandRequest =
        mkCreateLandRequest("STORN11111", "100001")
      val request2: CreateLandRequest =
        mkCreateLandRequest("STORN22222", "100002")
      val request3: CreateLandRequest =
        mkCreateLandRequest("STORN33333", "100003")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn("100001", "1")))
      when(connector.createLand(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn("100002", "2")))
      when(connector.createLand(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateLandReturn("100003", "3")))

      service.createLand(request1).futureValue mustBe mkCreateLandReturn(
        "100001",
        "1"
      )
      service.createLand(request2).futureValue mustBe mkCreateLandReturn(
        "100002",
        "2"
      )
      service.createLand(request3).futureValue mustBe mkCreateLandReturn(
        "100003",
        "3"
      )

      verify(connector, times(3)).createLand(any[CreateLandRequest])(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: CreateLandRequest = mkCreateLandRequest()
      val boom = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createLand(request).failed.futureValue
      ex mustBe boom

      verify(connector).createLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "FilingLandService updateLand" - {

    "must delegate to connector (happy path)" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: UpdateLandRequest = mkUpdateLandRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn()))

      val result: UpdateLandReturn = service.updateLand(request).futureValue
      result mustBe mkUpdateLandReturn()

      verify(connector).updateLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request1: UpdateLandRequest =
        mkUpdateLandRequest("STORN11111", "100001", "100001")
      val request2: UpdateLandRequest =
        mkUpdateLandRequest("STORN22222", "100002", "100002")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn(true)))
      when(connector.updateLand(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn(true)))

      service.updateLand(request1).futureValue mustBe mkUpdateLandReturn(true)
      service.updateLand(request2).futureValue mustBe mkUpdateLandReturn(true)

      verify(connector).updateLand(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateLand(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: UpdateLandRequest = mkUpdateLandRequest()
      val boom = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateLand(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update with all optional fields" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: UpdateLandRequest = mkUpdateLandRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn()))

      val result: UpdateLandReturn = service.updateLand(request).futureValue
      result mustBe mkUpdateLandReturn()

      verify(connector).updateLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update with minimal fields" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: UpdateLandRequest = mkUpdateLandRequest().copy(
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        landArea = None,
        areaUnit = None,
        localAuthorityNumber = None,
        mineralRights = None,
        nlpgUprn = None,
        willSendPlansByPost = None,
        titleNumber = None,
        nextLandId = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn()))

      val result: UpdateLandReturn = service.updateLand(request).futureValue
      result mustBe mkUpdateLandReturn()

      verify(connector).updateLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different property types" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val residentialRequest: UpdateLandRequest =
        mkUpdateLandRequest().copy(propertyType = "RESIDENTIAL")
      val nonResidentialRequest: UpdateLandRequest =
        mkUpdateLandRequest().copy(propertyType = "NON_RESIDENTIAL")
      val mixedRequest: UpdateLandRequest =
        mkUpdateLandRequest().copy(propertyType = "MIXED")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(any[UpdateLandRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn()))

      service
        .updateLand(residentialRequest)
        .futureValue mustBe mkUpdateLandReturn()
      service
        .updateLand(nonResidentialRequest)
        .futureValue mustBe mkUpdateLandReturn()
      service.updateLand(mixedRequest).futureValue mustBe mkUpdateLandReturn()

      verify(connector).updateLand(eqTo(residentialRequest))(any[HeaderCarrier])
      verify(connector).updateLand(eqTo(nonResidentialRequest))(
        any[HeaderCarrier]
      )
      verify(connector).updateLand(eqTo(mixedRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: UpdateLandRequest = mkUpdateLandRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn()))

      service.updateLand(request).futureValue

      verify(connector, times(1)).updateLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request1: UpdateLandRequest =
        mkUpdateLandRequest("STORN11111", "100001", "100001")
      val request2: UpdateLandRequest =
        mkUpdateLandRequest("STORN22222", "100002", "100002")
      val request3: UpdateLandRequest =
        mkUpdateLandRequest("STORN33333", "100003", "100003")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn()))
      when(connector.updateLand(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn()))
      when(connector.updateLand(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateLandReturn()))

      service.updateLand(request1).futureValue mustBe mkUpdateLandReturn()
      service.updateLand(request2).futureValue mustBe mkUpdateLandReturn()
      service.updateLand(request3).futureValue mustBe mkUpdateLandReturn()

      verify(connector, times(3)).updateLand(any[UpdateLandRequest])(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: UpdateLandRequest = mkUpdateLandRequest()
      val boom = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateLand(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "FilingLandService deleteLand" - {

    "must delegate to connector (happy path)" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: DeleteLandRequest = mkDeleteLandRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLandReturn()))

      val result: DeleteLandReturn = service.deleteLand(request).futureValue
      result mustBe mkDeleteLandReturn()

      verify(connector).deleteLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request1: DeleteLandRequest =
        mkDeleteLandRequest("STORN11111", "100001", "100001")
      val request2: DeleteLandRequest =
        mkDeleteLandRequest("STORN22222", "100002", "100002")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteLand(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLandReturn(true)))
      when(connector.deleteLand(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLandReturn(true)))

      service.deleteLand(request1).futureValue mustBe mkDeleteLandReturn(true)
      service.deleteLand(request2).futureValue mustBe mkDeleteLandReturn(true)

      verify(connector).deleteLand(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteLand(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: DeleteLandRequest = mkDeleteLandRequest()
      val boom = UpstreamErrorResponse("Not found", 404)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteLand(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different resource reference formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request1: DeleteLandRequest =
        mkDeleteLandRequest("STORN12345", "100001", "100001")
      val request2: DeleteLandRequest =
        mkDeleteLandRequest("STORN12345", "100001", "999999")
      val request3: DeleteLandRequest =
        mkDeleteLandRequest("STORN12345", "100001", "LRF-2024-001")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteLand(any[DeleteLandRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLandReturn()))

      service.deleteLand(request1).futureValue mustBe mkDeleteLandReturn()
      service.deleteLand(request2).futureValue mustBe mkDeleteLandReturn()
      service.deleteLand(request3).futureValue mustBe mkDeleteLandReturn()

      verify(connector).deleteLand(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteLand(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteLand(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: DeleteLandRequest = mkDeleteLandRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLandReturn()))

      service.deleteLand(request).futureValue

      verify(connector, times(1)).deleteLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request1: DeleteLandRequest =
        mkDeleteLandRequest("STORN11111", "100001", "100001")
      val request2: DeleteLandRequest =
        mkDeleteLandRequest("STORN22222", "100002", "100002")
      val request3: DeleteLandRequest =
        mkDeleteLandRequest("STORN33333", "100003", "100003")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteLand(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLandReturn()))
      when(connector.deleteLand(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLandReturn()))
      when(connector.deleteLand(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteLandReturn()))

      service.deleteLand(request1).futureValue mustBe mkDeleteLandReturn()
      service.deleteLand(request2).futureValue mustBe mkDeleteLandReturn()
      service.deleteLand(request3).futureValue mustBe mkDeleteLandReturn()

      verify(connector, times(3)).deleteLand(any[DeleteLandRequest])(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingLandService(connector)
      val request: DeleteLandRequest = mkDeleteLandRequest()
      val boom = new RuntimeException("Connection timeout")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.deleteLand(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteLand(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteLand(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}
