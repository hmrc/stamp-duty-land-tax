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
import service.filing.ReturnAgentService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class ReturnAgentServiceSpec extends SpecBase {

  private def mkCreateReturnAgentRequest(
                                          stornId: String = "STORN12345",
                                          returnResourceRef: String = "RRF-2024-001",
                                          agentType: String = "SOLICITOR"
                                        ): CreateReturnAgentRequest =
    CreateReturnAgentRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      agentType = agentType,
      name = "Smith & Partners",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Suite 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = "TE23 5TT",
      phoneNumber = Some("01234567890"),
      email = Some("agent@example.com"),
      agentReference = Some("AGT-001"),
      isAuthorised = Some("YES")
    )

  private def mkCreateReturnAgentReturn(returnAgentID: String = "AGID-001"): CreateReturnAgentReturn =
    CreateReturnAgentReturn(returnAgentID = returnAgentID)

  private def mkUpdateReturnAgentRequest(
                                          stornId: String = "STORN12345",
                                          returnResourceRef: String = "RRF-2024-001",
                                          agentType: String = "SOLICITOR"
                                        ): UpdateReturnAgentRequest =
    UpdateReturnAgentRequest(
      stornId = stornId,
      returnResourceRef = returnResourceRef,
      agentType = agentType,
      name = "Smith & Partners Updated",
      houseNumber = Some("10"),
      addressLine1 = "Main Street",
      addressLine2 = Some("Suite 5"),
      addressLine3 = Some("Building A"),
      addressLine4 = Some("District B"),
      postcode = "TE23 5TT",
      phoneNumber = Some("01234567890"),
      email = Some("agent@example.com"),
      agentReference = Some("AGT-001"),
      isAuthorised = Some("YES")
    )

  private def mkUpdateReturnAgentReturn(updated: Boolean = true): UpdateReturnAgentReturn =
    UpdateReturnAgentReturn(updated = updated)

  private def mkDeleteReturnAgentRequest(
                                          storn: String = "STORN12345",
                                          returnResourceRef: String = "RRF-2024-001",
                                          agentType: String = "SOLICITOR"
                                        ): DeleteReturnAgentRequest =
    DeleteReturnAgentRequest(
      storn = storn,
      returnResourceRef = returnResourceRef,
      agentType = agentType
    )

  private def mkDeleteReturnAgentReturn(deleted: Boolean = true): DeleteReturnAgentReturn =
    DeleteReturnAgentReturn(deleted = deleted)

  "ReturnAgentService createReturnAgent" - {

    "must delegate to connector (happy path)" in {
      val connector                               = mock[FilingFormpProxyConnector]
      val service                                 = new ReturnAgentService(connector)
      val request: CreateReturnAgentRequest       = mkCreateReturnAgentRequest()
      implicit val hc: HeaderCarrier              = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn()))

      val result: CreateReturnAgentReturn = service.createReturnAgent(request).futureValue
      result mustBe mkCreateReturnAgentReturn()

      verify(connector).createReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                                = mock[FilingFormpProxyConnector]
      val service                                  = new ReturnAgentService(connector)
      val request1: CreateReturnAgentRequest       = mkCreateReturnAgentRequest("STORN11111", "RRF-001")
      val request2: CreateReturnAgentRequest       = mkCreateReturnAgentRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier               = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn("AGID-001")))
      when(connector.createReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn("AGID-002")))

      service.createReturnAgent(request1).futureValue mustBe mkCreateReturnAgentReturn("AGID-001")
      service.createReturnAgent(request2).futureValue mustBe mkCreateReturnAgentReturn("AGID-002")

      verify(connector).createReturnAgent(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createReturnAgent(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: CreateReturnAgentRequest  = mkCreateReturnAgentRequest()
      val boom                               = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(connector).createReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different agent types" in {
      val connector                                     = mock[FilingFormpProxyConnector]
      val service                                       = new ReturnAgentService(connector)
      val solicitorRequest: CreateReturnAgentRequest    = mkCreateReturnAgentRequest(agentType = "SOLICITOR")
      val conveyancerRequest: CreateReturnAgentRequest  = mkCreateReturnAgentRequest(agentType = "CONVEYANCER")
      val otherRequest: CreateReturnAgentRequest        = mkCreateReturnAgentRequest(agentType = "OTHER")
      implicit val hc: HeaderCarrier                    = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(solicitorRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn()))
      when(connector.createReturnAgent(eqTo(conveyancerRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn()))
      when(connector.createReturnAgent(eqTo(otherRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn()))

      service.createReturnAgent(solicitorRequest).futureValue mustBe mkCreateReturnAgentReturn()
      service.createReturnAgent(conveyancerRequest).futureValue mustBe mkCreateReturnAgentReturn()
      service.createReturnAgent(otherRequest).futureValue mustBe mkCreateReturnAgentReturn()

      verify(connector).createReturnAgent(eqTo(solicitorRequest))(any[HeaderCarrier])
      verify(connector).createReturnAgent(eqTo(conveyancerRequest))(any[HeaderCarrier])
      verify(connector).createReturnAgent(eqTo(otherRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: CreateReturnAgentRequest  = mkCreateReturnAgentRequest().copy(
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn()))

      val result: CreateReturnAgentReturn = service.createReturnAgent(request).futureValue
      result mustBe mkCreateReturnAgentReturn()

      verify(connector).createReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle request with all optional fields populated" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: CreateReturnAgentRequest  = mkCreateReturnAgentRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn()))

      val result: CreateReturnAgentReturn = service.createReturnAgent(request).futureValue
      result mustBe mkCreateReturnAgentReturn()

      verify(connector).createReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: CreateReturnAgentRequest  = mkCreateReturnAgentRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn()))

      service.createReturnAgent(request).futureValue

      verify(connector, times(1)).createReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new ReturnAgentService(connector)
      val request1: CreateReturnAgentRequest  = mkCreateReturnAgentRequest("STORN11111", "RRF-001")
      val request2: CreateReturnAgentRequest  = mkCreateReturnAgentRequest("STORN22222", "RRF-002")
      val request3: CreateReturnAgentRequest  = mkCreateReturnAgentRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn("AGID-001")))
      when(connector.createReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn("AGID-002")))
      when(connector.createReturnAgent(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn("AGID-003")))

      service.createReturnAgent(request1).futureValue mustBe mkCreateReturnAgentReturn("AGID-001")
      service.createReturnAgent(request2).futureValue mustBe mkCreateReturnAgentReturn("AGID-002")
      service.createReturnAgent(request3).futureValue mustBe mkCreateReturnAgentReturn("AGID-003")

      verify(connector, times(3)).createReturnAgent(any[CreateReturnAgentRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: CreateReturnAgentRequest  = mkCreateReturnAgentRequest()
      val boom                               = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(connector).createReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different stornId formats" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new ReturnAgentService(connector)
      val request1: CreateReturnAgentRequest  = mkCreateReturnAgentRequest("STORN12345")
      val request2: CreateReturnAgentRequest  = mkCreateReturnAgentRequest("STORN-ABC-123")
      val request3: CreateReturnAgentRequest  = mkCreateReturnAgentRequest("12345678")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.createReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn("AGID-001")))
      when(connector.createReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn("AGID-002")))
      when(connector.createReturnAgent(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkCreateReturnAgentReturn("AGID-003")))

      service.createReturnAgent(request1).futureValue mustBe mkCreateReturnAgentReturn("AGID-001")
      service.createReturnAgent(request2).futureValue mustBe mkCreateReturnAgentReturn("AGID-002")
      service.createReturnAgent(request3).futureValue mustBe mkCreateReturnAgentReturn("AGID-003")

      verify(connector).createReturnAgent(eqTo(request1))(any[HeaderCarrier])
      verify(connector).createReturnAgent(eqTo(request2))(any[HeaderCarrier])
      verify(connector).createReturnAgent(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "ReturnAgentService updateReturnAgent" - {

    "must delegate to connector (happy path)" in {
      val connector                               = mock[FilingFormpProxyConnector]
      val service                                 = new ReturnAgentService(connector)
      val request: UpdateReturnAgentRequest       = mkUpdateReturnAgentRequest()
      implicit val hc: HeaderCarrier              = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn()))

      val result: UpdateReturnAgentReturn = service.updateReturnAgent(request).futureValue
      result mustBe mkUpdateReturnAgentReturn()

      verify(connector).updateReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                                = mock[FilingFormpProxyConnector]
      val service                                  = new ReturnAgentService(connector)
      val request1: UpdateReturnAgentRequest       = mkUpdateReturnAgentRequest("STORN11111", "RRF-001")
      val request2: UpdateReturnAgentRequest       = mkUpdateReturnAgentRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier               = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn(true)))
      when(connector.updateReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn(true)))

      service.updateReturnAgent(request1).futureValue mustBe mkUpdateReturnAgentReturn(true)
      service.updateReturnAgent(request2).futureValue mustBe mkUpdateReturnAgentReturn(true)

      verify(connector).updateReturnAgent(eqTo(request1))(any[HeaderCarrier])
      verify(connector).updateReturnAgent(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest()
      val boom                               = UpstreamErrorResponse("Not found", 404)
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different agent types" in {
      val connector                                     = mock[FilingFormpProxyConnector]
      val service                                       = new ReturnAgentService(connector)
      val solicitorRequest: UpdateReturnAgentRequest    = mkUpdateReturnAgentRequest(agentType = "SOLICITOR")
      val conveyancerRequest: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest(agentType = "CONVEYANCER")
      val otherRequest: UpdateReturnAgentRequest        = mkUpdateReturnAgentRequest(agentType = "OTHER")
      implicit val hc: HeaderCarrier                    = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(solicitorRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn()))
      when(connector.updateReturnAgent(eqTo(conveyancerRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn()))
      when(connector.updateReturnAgent(eqTo(otherRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn()))

      service.updateReturnAgent(solicitorRequest).futureValue mustBe mkUpdateReturnAgentReturn()
      service.updateReturnAgent(conveyancerRequest).futureValue mustBe mkUpdateReturnAgentReturn()
      service.updateReturnAgent(otherRequest).futureValue mustBe mkUpdateReturnAgentReturn()

      verify(connector).updateReturnAgent(eqTo(solicitorRequest))(any[HeaderCarrier])
      verify(connector).updateReturnAgent(eqTo(conveyancerRequest))(any[HeaderCarrier])
      verify(connector).updateReturnAgent(eqTo(otherRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle minimal request with no optional fields" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest().copy(
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn()))

      val result: UpdateReturnAgentReturn = service.updateReturnAgent(request).futureValue
      result mustBe mkUpdateReturnAgentReturn()

      verify(connector).updateReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn()))

      service.updateReturnAgent(request).futureValue

      verify(connector, times(1)).updateReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new ReturnAgentService(connector)
      val request1: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest("STORN11111", "RRF-001")
      val request2: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest("STORN22222", "RRF-002")
      val request3: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn(true)))
      when(connector.updateReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn(true)))
      when(connector.updateReturnAgent(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn(true)))

      service.updateReturnAgent(request1).futureValue mustBe mkUpdateReturnAgentReturn(true)
      service.updateReturnAgent(request2).futureValue mustBe mkUpdateReturnAgentReturn(true)
      service.updateReturnAgent(request3).futureValue mustBe mkUpdateReturnAgentReturn(true)

      verify(connector, times(3)).updateReturnAgent(any[UpdateReturnAgentRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest()
      val boom                               = new RuntimeException("Connection timeout")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle update result with false status" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: UpdateReturnAgentRequest  = mkUpdateReturnAgentRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.updateReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkUpdateReturnAgentReturn(false)))

      val result: UpdateReturnAgentReturn = service.updateReturnAgent(request).futureValue
      result mustBe mkUpdateReturnAgentReturn(false)
      result.updated mustBe false

      verify(connector).updateReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "ReturnAgentService deleteReturnAgent" - {

    "must delegate to connector (happy path)" in {
      val connector                               = mock[FilingFormpProxyConnector]
      val service                                 = new ReturnAgentService(connector)
      val request: DeleteReturnAgentRequest       = mkDeleteReturnAgentRequest()
      implicit val hc: HeaderCarrier              = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn()))

      val result: DeleteReturnAgentReturn = service.deleteReturnAgent(request).futureValue
      result mustBe mkDeleteReturnAgentReturn()

      verify(connector).deleteReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must return different results for different requests" in {
      val connector                                = mock[FilingFormpProxyConnector]
      val service                                  = new ReturnAgentService(connector)
      val request1: DeleteReturnAgentRequest       = mkDeleteReturnAgentRequest("STORN11111", "RRF-001")
      val request2: DeleteReturnAgentRequest       = mkDeleteReturnAgentRequest("STORN22222", "RRF-002")
      implicit val hc: HeaderCarrier               = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))
      when(connector.deleteReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))

      service.deleteReturnAgent(request1).futureValue mustBe mkDeleteReturnAgentReturn(true)
      service.deleteReturnAgent(request2).futureValue mustBe mkDeleteReturnAgentReturn(true)

      verify(connector).deleteReturnAgent(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteReturnAgent(eqTo(request2))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest()
      val boom                               = UpstreamErrorResponse("Internal Server Error", 500)
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different agent types" in {
      val connector                                     = mock[FilingFormpProxyConnector]
      val service                                       = new ReturnAgentService(connector)
      val solicitorRequest: DeleteReturnAgentRequest    = mkDeleteReturnAgentRequest(agentType = "SOLICITOR")
      val conveyancerRequest: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest(agentType = "CONVEYANCER")
      val otherRequest: DeleteReturnAgentRequest        = mkDeleteReturnAgentRequest(agentType = "OTHER")
      implicit val hc: HeaderCarrier                    = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(solicitorRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn()))
      when(connector.deleteReturnAgent(eqTo(conveyancerRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn()))
      when(connector.deleteReturnAgent(eqTo(otherRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn()))

      service.deleteReturnAgent(solicitorRequest).futureValue mustBe mkDeleteReturnAgentReturn()
      service.deleteReturnAgent(conveyancerRequest).futureValue mustBe mkDeleteReturnAgentReturn()
      service.deleteReturnAgent(otherRequest).futureValue mustBe mkDeleteReturnAgentReturn()

      verify(connector).deleteReturnAgent(eqTo(solicitorRequest))(any[HeaderCarrier])
      verify(connector).deleteReturnAgent(eqTo(conveyancerRequest))(any[HeaderCarrier])
      verify(connector).deleteReturnAgent(eqTo(otherRequest))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn()))

      service.deleteReturnAgent(request).futureValue

      verify(connector, times(1)).deleteReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new ReturnAgentService(connector)
      val request1: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest("STORN11111", "RRF-001")
      val request2: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest("STORN22222", "RRF-002")
      val request3: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest("STORN33333", "RRF-003")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))
      when(connector.deleteReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))
      when(connector.deleteReturnAgent(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))

      service.deleteReturnAgent(request1).futureValue mustBe mkDeleteReturnAgentReturn(true)
      service.deleteReturnAgent(request2).futureValue mustBe mkDeleteReturnAgentReturn(true)
      service.deleteReturnAgent(request3).futureValue mustBe mkDeleteReturnAgentReturn(true)

      verify(connector, times(3)).deleteReturnAgent(any[DeleteReturnAgentRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest()
      val boom                               = new RuntimeException("Network error")
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(connector).deleteReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle delete result with false status" in {
      val connector                          = mock[FilingFormpProxyConnector]
      val service                            = new ReturnAgentService(connector)
      val request: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest()
      implicit val hc: HeaderCarrier         = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(false)))

      val result: DeleteReturnAgentReturn = service.deleteReturnAgent(request).futureValue
      result mustBe mkDeleteReturnAgentReturn(false)
      result.deleted mustBe false

      verify(connector).deleteReturnAgent(eqTo(request))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new ReturnAgentService(connector)
      val request1: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest("STORN12345")
      val request2: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest("STORN-ABC-123")
      val request3: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest("12345678")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))
      when(connector.deleteReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))
      when(connector.deleteReturnAgent(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))

      service.deleteReturnAgent(request1).futureValue mustBe mkDeleteReturnAgentReturn(true)
      service.deleteReturnAgent(request2).futureValue mustBe mkDeleteReturnAgentReturn(true)
      service.deleteReturnAgent(request3).futureValue mustBe mkDeleteReturnAgentReturn(true)

      verify(connector).deleteReturnAgent(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteReturnAgent(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteReturnAgent(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle different returnResourceRef formats" in {
      val connector                           = mock[FilingFormpProxyConnector]
      val service                             = new ReturnAgentService(connector)
      val request1: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest(returnResourceRef = "RRF-2024-001")
      val request2: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest(returnResourceRef = "123456")
      val request3: DeleteReturnAgentRequest  = mkDeleteReturnAgentRequest(returnResourceRef = "ABC-123-XYZ")
      implicit val hc: HeaderCarrier          = HeaderCarrier()

      when(connector.deleteReturnAgent(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))
      when(connector.deleteReturnAgent(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))
      when(connector.deleteReturnAgent(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkDeleteReturnAgentReturn(true)))

      service.deleteReturnAgent(request1).futureValue mustBe mkDeleteReturnAgentReturn(true)
      service.deleteReturnAgent(request2).futureValue mustBe mkDeleteReturnAgentReturn(true)
      service.deleteReturnAgent(request3).futureValue mustBe mkDeleteReturnAgentReturn(true)

      verify(connector).deleteReturnAgent(eqTo(request1))(any[HeaderCarrier])
      verify(connector).deleteReturnAgent(eqTo(request2))(any[HeaderCarrier])
      verify(connector).deleteReturnAgent(eqTo(request3))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }
}