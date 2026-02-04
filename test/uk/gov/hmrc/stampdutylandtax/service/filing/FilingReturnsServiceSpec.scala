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
import service.filing.FilingReturnsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class FilingReturnsServiceSpec extends SpecBase {

  private def mkCreateRequest(stornId: String = "STORN12345"): CreateReturnRequest =
    CreateReturnRequest(
      stornId = stornId,
      purchaserIsCompany = "N",
      surNameOrCompanyName = "Smith",
      houseNumber = Some(42),
      addressLine1 = "High Street",
      addressLine2 = Some("Kensington"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      postcode = Some("SW1A 1AA"),
      transactionType = "RESIDENTIAL"
    )

  private def mkCreateResult(
                              stornId: String = "STORN12345",
                              returnResourceRef: String = "RRF-2024-001"
                            ): CreateReturnResult =
    CreateReturnResult(
      returnResourceRef = returnResourceRef
    )

  private def mkGetReturnRequest(
                                  returnResourceRef: String = "RRF-2024-001",
                                  storn: String = "STORN12345"
                                ): GetReturnByRefRequest =
    GetReturnByRefRequest(
      returnResourceRef = returnResourceRef,
      storn = storn
    )

  private def mkGetReturnResponse(
                                   stornId: String = "STORN12345",
                                   returnResourceRef: String = "RRF-2024-001"
                                 ): GetReturnRequest =
    GetReturnRequest(
      stornId = Some(stornId),
      returnResourceRef = Some(returnResourceRef),
      sdltOrganisation = None,
      returnInfo = None,
      purchaser = None,
      companyDetails = None,
      vendor = None,
      land = None,
      transaction = None,
      returnAgent = None,
      agent = None,
      lease = None,
      taxCalculation = None,
      submission = None,
      submissionErrorDetails = None,
      residency = None
    )

  private def mkUpdateReturnRequest(
                                       storn: String = "STORN12345",
                                       returnResourceRef: String = "100001",
                                       mainPurchaserId: String = "1",
                                       mainVendorId: String = "1",
                                       mainLandId: String = "1",
                                       irmarkGenerated: String = "IRMark123456",
                                       landCertForEachProp: String = "Y",
                                       declaration: String = "Y"
                                     ): UpdateReturnRequest =
      UpdateReturnRequest(
        storn = storn,
        returnResourceRef = returnResourceRef,
        mainPurchaserId = mainPurchaserId,
        mainVendorId = mainVendorId,
        mainLandId = mainLandId,
        irmarkGenerated = irmarkGenerated,
        landCertForEachProp = landCertForEachProp,
        declaration = declaration
      )

  private def mkUpdateReturnReturn(updated: Boolean = true): UpdateReturnReturn =
    UpdateReturnReturn(updated = updated)

  "FilingReturnsService createReturn" - {

    "must delegate to connector (happy path)" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new FilingReturnsService(connector)
      val request: CreateReturnRequest = mkCreateRequest()
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.createReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult()))

      val result: CreateReturnResult = service.createReturn(request).futureValue
      result mustBe mkCreateResult()

      verify(connector).createReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new FilingReturnsService(connector)
      val request1: CreateReturnRequest = mkCreateRequest("STORN11111")
      val request2: CreateReturnRequest = mkCreateRequest("STORN22222")
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.createReturn(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult("STORN11111", "RRF-2024-001")))
      when(connector.createReturn(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult("STORN22222", "RRF-2024-002")))

      service.createReturn(request1).futureValue mustBe mkCreateResult("STORN11111", "RRF-2024-001")
      service.createReturn(request2).futureValue mustBe mkCreateResult("STORN22222", "RRF-2024-002")

      verify(connector).createReturn(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createReturn(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new FilingReturnsService(connector)
      val request: CreateReturnRequest = mkCreateRequest()
      val boom                         = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.createReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createReturn(request).failed.futureValue
      ex mustBe boom

      verify(connector).createReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle company purchaser requests" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new FilingReturnsService(connector)
      val request: CreateReturnRequest = mkCreateRequest().copy(
        purchaserIsCompany = "Y",
        surNameOrCompanyName = "ABC Property Ltd",
        transactionType = "NON_RESIDENTIAL"
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult()))

      val result: CreateReturnResult = service.createReturn(request).futureValue
      result mustBe mkCreateResult()

      verify(connector).createReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new FilingReturnsService(connector)
      val request: CreateReturnRequest = mkCreateRequest().copy(
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult()))

      val result: CreateReturnResult = service.createReturn(request).futureValue
      result mustBe mkCreateResult()

      verify(connector).createReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle request with all optional fields populated" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new FilingReturnsService(connector)
      val request: CreateReturnRequest = mkCreateRequest().copy(
        houseNumber = Some(42),
        addressLine2 = Some("Kensington"),
        addressLine3 = Some("London"),
        addressLine4 = Some("Greater London"),
        postcode = Some("SW1A 1AA")
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult()))

      val result: CreateReturnResult = service.createReturn(request).futureValue
      result mustBe mkCreateResult()

      verify(connector).createReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different transaction types" in {
      val connector                                  = mock[FilingFormpProxyConnector]
      val service                                    = new FilingReturnsService(connector)
      val residentialRequest: CreateReturnRequest    = mkCreateRequest().copy(transactionType = "RESIDENTIAL")
      val nonResidentialRequest: CreateReturnRequest = mkCreateRequest().copy(transactionType = "NON_RESIDENTIAL")
      val mixedRequest: CreateReturnRequest          = mkCreateRequest().copy(transactionType = "MIXED")
      implicit val hc: HeaderCarrier                 = HeaderCarrier()

      when(connector.createReturn(eqTo(residentialRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult()))
      when(connector.createReturn(eqTo(nonResidentialRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult()))
      when(connector.createReturn(eqTo(mixedRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult()))

      service.createReturn(residentialRequest).futureValue mustBe mkCreateResult()
      service.createReturn(nonResidentialRequest).futureValue mustBe mkCreateResult()
      service.createReturn(mixedRequest).futureValue mustBe mkCreateResult()

      verify(connector).createReturn(eqTo(residentialRequest))(any[HeaderCarrier])
      verify(connector).createReturn(eqTo(nonResidentialRequest))(any[HeaderCarrier])
      verify(connector).createReturn(eqTo(mixedRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn ID formats" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new FilingReturnsService(connector)
      val request1: CreateReturnRequest = mkCreateRequest("STORN12345")
      val request2: CreateReturnRequest = mkCreateRequest("STORN-ABC-123")
      val request3: CreateReturnRequest = mkCreateRequest("12345678")
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.createReturn(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult("STORN12345", "RRF-001")))
      when(connector.createReturn(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult("STORN-ABC-123", "RRF-002")))
      when(connector.createReturn(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult("12345678", "RRF-003")))

      service.createReturn(request1).futureValue mustBe mkCreateResult("STORN12345", "RRF-001")
      service.createReturn(request2).futureValue mustBe mkCreateResult("STORN-ABC-123", "RRF-002")
      service.createReturn(request3).futureValue mustBe mkCreateResult("12345678", "RRF-003")

      verify(connector).createReturn(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createReturn(eqTo(request2))(any[HeaderCarrier])
      verify(connector).createReturn(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new FilingReturnsService(connector)
      val request: CreateReturnRequest = mkCreateRequest()
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.createReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult()))

      service.createReturn(request).futureValue

      verify(connector, times(1)).createReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                     = mock[FilingFormpProxyConnector]
      val service                       = new FilingReturnsService(connector)
      val request1: CreateReturnRequest = mkCreateRequest("STORN11111")
      val request2: CreateReturnRequest = mkCreateRequest("STORN22222")
      val request3: CreateReturnRequest = mkCreateRequest("STORN33333")
      implicit val hc: HeaderCarrier    = HeaderCarrier()

      when(connector.createReturn(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult("STORN11111", "RRF-001")))
      when(connector.createReturn(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult("STORN22222", "RRF-002")))
      when(connector.createReturn(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateResult("STORN33333", "RRF-003")))

      service.createReturn(request1).futureValue mustBe mkCreateResult("STORN11111", "RRF-001")
      service.createReturn(request2).futureValue mustBe mkCreateResult("STORN22222", "RRF-002")
      service.createReturn(request3).futureValue mustBe mkCreateResult("STORN33333", "RRF-003")

      verify(connector, times(3)).createReturn(any[CreateReturnRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                    = mock[FilingFormpProxyConnector]
      val service                      = new FilingReturnsService(connector)
      val request: CreateReturnRequest = mkCreateRequest()
      val boom                         = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier   = HeaderCarrier()

      when(connector.createReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createReturn(request).failed.futureValue
      ex mustBe boom

      verify(connector).createReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "FilingReturnsService getFullReturn" - {

    "must delegate to connector" in {
      val connector                             = mock[FilingFormpProxyConnector]
      val service                               = new FilingReturnsService(connector)
      val request: GetReturnByRefRequest        = mkGetReturnRequest()
      val response: GetReturnRequest            = mkGetReturnResponse()
      implicit val hc: HeaderCarrier            = HeaderCarrier()

      when(connector.getFullReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val result: GetReturnRequest = service.getFullReturn(request).futureValue
      result mustBe response

      verify(connector).getFullReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return correct data for different return IDs" in {
      val connector                              = mock[FilingFormpProxyConnector]
      val service                                = new FilingReturnsService(connector)
      val request1: GetReturnByRefRequest        = mkGetReturnRequest("RRF-001", "STORN11111")
      val request2: GetReturnByRefRequest        = mkGetReturnRequest("RRF-002", "STORN22222")
      val response1: GetReturnRequest            = mkGetReturnResponse("STORN11111", "RRF-001")
      val response2: GetReturnRequest            = mkGetReturnResponse("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier             = HeaderCarrier()

      when(connector.getFullReturn(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response1))
      when(connector.getFullReturn(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response2))

      service.getFullReturn(request1).futureValue mustBe response1
      service.getFullReturn(request2).futureValue mustBe response2

      verify(connector).getFullReturn(eqTo(request1))(any[HeaderCarrier])
      verify(connector).getFullReturn(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new FilingReturnsService(connector)
      val request: GetReturnByRefRequest = mkGetReturnRequest()
      val boom                           = UpstreamErrorResponse("Not found", 404)
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.getFullReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.getFullReturn(request).failed.futureValue
      ex mustBe boom

      verify(connector).getFullReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different returnResourceRef formats" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = FilingReturnsService(connector)
      val request1: GetReturnByRefRequest = mkGetReturnRequest("123456", "STORN12345")
      val request2: GetReturnByRefRequest = mkGetReturnRequest("RRF-2024-001", "STORN12345")
      val request3: GetReturnByRefRequest = mkGetReturnRequest("ABC-123-XYZ", "STORN12345")
      val response: GetReturnRequest      = mkGetReturnResponse()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.getFullReturn(any[GetReturnByRefRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      service.getFullReturn(request1).futureValue mustBe response
      service.getFullReturn(request2).futureValue mustBe response
      service.getFullReturn(request3).futureValue mustBe response

      verify(connector).getFullReturn(eqTo(request1))(any[HeaderCarrier])
      verify(connector).getFullReturn(eqTo(request2))(any[HeaderCarrier])
      verify(connector).getFullReturn(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector                       = mock[FilingFormpProxyConnector]
      val service                         = new FilingReturnsService(connector)
      val request1: GetReturnByRefRequest = mkGetReturnRequest("RRF-2024-001", "STORN123456")
      val request2: GetReturnByRefRequest = mkGetReturnRequest("RRF-2024-001", "STORN-ABC-123")
      val request3: GetReturnByRefRequest = mkGetReturnRequest("RRF-2024-001", "12345678")
      val response: GetReturnRequest      = mkGetReturnResponse()
      implicit val hc: HeaderCarrier      = HeaderCarrier()

      when(connector.getFullReturn(any[GetReturnByRefRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      service.getFullReturn(request1).futureValue mustBe response
      service.getFullReturn(request2).futureValue mustBe response
      service.getFullReturn(request3).futureValue mustBe response

      verify(connector).getFullReturn(eqTo(request1))(any[HeaderCarrier])
      verify(connector).getFullReturn(eqTo(request2))(any[HeaderCarrier])
      verify(connector).getFullReturn(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new FilingReturnsService(connector)
      val request: GetReturnByRefRequest = mkGetReturnRequest()
      val response: GetReturnRequest     = mkGetReturnResponse()
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.getFullReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      service.getFullReturn(request).futureValue

      verify(connector, times(1)).getFullReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                        = mock[FilingFormpProxyConnector]
      val service                          = new FilingReturnsService(connector)
      val request1: GetReturnByRefRequest  = mkGetReturnRequest("RRF-001", "STORN11111")
      val request2: GetReturnByRefRequest  = mkGetReturnRequest("RRF-002", "STORN22222")
      val request3: GetReturnByRefRequest  = mkGetReturnRequest("RRF-003", "STORN33333")
      val response1: GetReturnRequest      = mkGetReturnResponse("STORN11111", "RRF-001")
      val response2: GetReturnRequest      = mkGetReturnResponse("STORN22222", "RRF-002")
      val response3: GetReturnRequest      = mkGetReturnResponse("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier       = HeaderCarrier()

      when(connector.getFullReturn(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response1))
      when(connector.getFullReturn(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response2))
      when(connector.getFullReturn(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response3))

      service.getFullReturn(request1).futureValue mustBe response1
      service.getFullReturn(request2).futureValue mustBe response2
      service.getFullReturn(request3).futureValue mustBe response3

      verify(connector, times(3)).getFullReturn(any[GetReturnByRefRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                      = mock[FilingFormpProxyConnector]
      val service                        = new FilingReturnsService(connector)
      val request: GetReturnByRefRequest = mkGetReturnRequest()
      val boom                           = new RuntimeException("Connection timeout")
      implicit val hc: HeaderCarrier     = HeaderCarrier()

      when(connector.getFullReturn(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.getFullReturn(request).failed.futureValue
      ex mustBe boom

      verify(connector).getFullReturn(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "FilingReturnsService updateReturnInfo" - {

    "must delegate to connector (happy path)" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request: UpdateReturnRequest = mkUpdateReturnRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))

      val result: UpdateReturnReturn = service.updateReturnInfo(request).futureValue
      result mustBe mkUpdateReturnReturn()

      verify(connector).updateReturnInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request1: UpdateReturnRequest = mkUpdateReturnRequest("STORN11111", "100001")
      val request2: UpdateReturnRequest = mkUpdateReturnRequest("STORN22222", "100002")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn(true)))
      when(connector.updateReturnInfo(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn(true)))

      service.updateReturnInfo(request1).futureValue mustBe mkUpdateReturnReturn(true)
      service.updateReturnInfo(request2).futureValue mustBe mkUpdateReturnReturn(true)

      verify(connector).updateReturnInfo(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateReturnInfo(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request: UpdateReturnRequest = mkUpdateReturnRequest()
      val boom = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateReturnInfo(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateReturnInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update with Y values for boolean fields" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request: UpdateReturnRequest = mkUpdateReturnRequest(
        landCertForEachProp = "Y",
        declaration = "Y"
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))

      val result: UpdateReturnReturn = service.updateReturnInfo(request).futureValue
      result mustBe mkUpdateReturnReturn()

      verify(connector).updateReturnInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update with N values for boolean fields" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request: UpdateReturnRequest = mkUpdateReturnRequest(
        landCertForEachProp = "N",
        declaration = "N"
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))

      val result: UpdateReturnReturn = service.updateReturnInfo(request).futureValue
      result mustBe mkUpdateReturnReturn()

      verify(connector).updateReturnInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different IRMark formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request1: UpdateReturnRequest = mkUpdateReturnRequest(irmarkGenerated = "IRMark123456")
      val request2: UpdateReturnRequest = mkUpdateReturnRequest(irmarkGenerated = "IRMark-ABC-123")
      val request3: UpdateReturnRequest = mkUpdateReturnRequest(irmarkGenerated = "12345678")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))

      service.updateReturnInfo(request1).futureValue mustBe mkUpdateReturnReturn()
      service.updateReturnInfo(request2).futureValue mustBe mkUpdateReturnReturn()
      service.updateReturnInfo(request3).futureValue mustBe mkUpdateReturnReturn()

      verify(connector).updateReturnInfo(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateReturnInfo(eqTo(request2))(any[HeaderCarrier])
      verify(connector).updateReturnInfo(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different entity IDs" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request1: UpdateReturnRequest = mkUpdateReturnRequest(
        mainPurchaserId = "1",
        mainVendorId = "1",
        mainLandId = "1"
      )
      val request2: UpdateReturnRequest = mkUpdateReturnRequest(
        mainPurchaserId = "100",
        mainVendorId = "200",
        mainLandId = "300"
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))

      service.updateReturnInfo(request1).futureValue mustBe mkUpdateReturnReturn()
      service.updateReturnInfo(request2).futureValue mustBe mkUpdateReturnReturn()

      verify(connector).updateReturnInfo(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateReturnInfo(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request: UpdateReturnRequest = mkUpdateReturnRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))

      service.updateReturnInfo(request).futureValue

      verify(connector, times(1)).updateReturnInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request1: UpdateReturnRequest = mkUpdateReturnRequest("STORN11111", "100001")
      val request2: UpdateReturnRequest = mkUpdateReturnRequest("STORN22222", "100002")
      val request3: UpdateReturnRequest = mkUpdateReturnRequest("STORN33333", "100003")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))
      when(connector.updateReturnInfo(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))
      when(connector.updateReturnInfo(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))

      service.updateReturnInfo(request1).futureValue mustBe mkUpdateReturnReturn()
      service.updateReturnInfo(request2).futureValue mustBe mkUpdateReturnReturn()
      service.updateReturnInfo(request3).futureValue mustBe mkUpdateReturnReturn()

      verify(connector, times(3)).updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request: UpdateReturnRequest = mkUpdateReturnRequest()
      val boom = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateReturnInfo(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateReturnInfo(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different return resource reference formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new FilingReturnsService(connector)
      val request1: UpdateReturnRequest = mkUpdateReturnRequest(returnResourceRef = "100001")
      val request2: UpdateReturnRequest = mkUpdateReturnRequest(returnResourceRef = "RRF-2024-001")
      val request3: UpdateReturnRequest = mkUpdateReturnRequest(returnResourceRef = "999999")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnInfo(any[UpdateReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnReturn()))

      service.updateReturnInfo(request1).futureValue mustBe mkUpdateReturnReturn()
      service.updateReturnInfo(request2).futureValue mustBe mkUpdateReturnReturn()
      service.updateReturnInfo(request3).futureValue mustBe mkUpdateReturnReturn()

      verify(connector).updateReturnInfo(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateReturnInfo(eqTo(request2))(any[HeaderCarrier])
      verify(connector).updateReturnInfo(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}