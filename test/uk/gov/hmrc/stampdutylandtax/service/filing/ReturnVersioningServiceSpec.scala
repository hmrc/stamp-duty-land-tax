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
import models.filing.{ReturnVersionUpdateRequest, ReturnVersionUpdateReturn}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import service.filing.ReturnVersioningService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class ReturnVersioningServiceSpec extends SpecBase {

  private def mkReturnVersionUpdateRequest(
      storn: String = "STORN12345",
      returnResourceRef: String = "100001",
      currentVersion: String = "1"
  ): ReturnVersionUpdateRequest =
    ReturnVersionUpdateRequest(
      storn = storn,
      returnResourceRef = returnResourceRef,
      currentVersion = currentVersion
    )

  private def mkReturnVersionUpdateReturn(
      newVersion: Int = 2
  ): ReturnVersionUpdateReturn =
    ReturnVersionUpdateReturn(newVersion = newVersion)

  "ReturnVersioningService updateReturnVersion" - {

    "must delegate to connector (happy path)" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest = mkReturnVersionUpdateRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))

      val result: ReturnVersionUpdateReturn =
        service.updateReturnVersion(request).futureValue
      result mustBe mkReturnVersionUpdateReturn(2)
      result.newVersion mustBe 2

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must return different version numbers for different requests" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request1: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN11111", "100001", "1")
      val request2: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN22222", "100002", "5")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))
      when(connector.updateReturnVersioning(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(6)))

      service
        .updateReturnVersion(request1)
        .futureValue mustBe mkReturnVersionUpdateReturn(2)
      service
        .updateReturnVersion(request2)
        .futureValue mustBe mkReturnVersionUpdateReturn(6)

      verify(connector).updateReturnVersioning(eqTo(request1))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request2))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest = mkReturnVersionUpdateRequest()
      val boom = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable =
        service.updateReturnVersion(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle version 0 to version 1" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "0")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(1)))

      val result: ReturnVersionUpdateReturn =
        service.updateReturnVersion(request).futureValue
      result.newVersion mustBe 1

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle higher version numbers" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request1: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "5")
      val request2: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "10")
      val request3: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "99")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(6)))
      when(connector.updateReturnVersioning(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(11)))
      when(connector.updateReturnVersioning(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(100)))

      service.updateReturnVersion(request1).futureValue.newVersion mustBe 6
      service.updateReturnVersion(request2).futureValue.newVersion mustBe 11
      service.updateReturnVersion(request3).futureValue.newVersion mustBe 100

      verify(connector).updateReturnVersioning(eqTo(request1))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request2))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request3))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle different storn formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request1: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN12345")
      val request2: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN-ABC-123")
      val request3: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("12345678")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))
      when(connector.updateReturnVersioning(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))
      when(connector.updateReturnVersioning(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))

      service.updateReturnVersion(request1).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request2).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request3).futureValue.newVersion mustBe 2

      verify(connector).updateReturnVersioning(eqTo(request1))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request2))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request3))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle different returnResourceRef formats" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request1: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(returnResourceRef = "100001")
      val request2: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(returnResourceRef = "999999")
      val request3: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(returnResourceRef = "123")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))
      when(connector.updateReturnVersioning(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))
      when(connector.updateReturnVersioning(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))

      service.updateReturnVersion(request1).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request2).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request3).futureValue.newVersion mustBe 2

      verify(connector).updateReturnVersioning(eqTo(request1))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request2))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request3))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest = mkReturnVersionUpdateRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))

      service.updateReturnVersion(request).futureValue

      verify(connector, times(1)).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request1: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN11111", "100001", "1")
      val request2: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN22222", "100002", "2")
      val request3: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN33333", "100003", "3")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))
      when(connector.updateReturnVersioning(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(3)))
      when(connector.updateReturnVersioning(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(4)))

      service.updateReturnVersion(request1).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request2).futureValue.newVersion mustBe 3
      service.updateReturnVersion(request3).futureValue.newVersion mustBe 4

      verify(connector, times(3)).updateReturnVersioning(
        any[ReturnVersionUpdateRequest]
      )(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest = mkReturnVersionUpdateRequest()
      val boom = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable =
        service.updateReturnVersion(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle different new version values" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest = mkReturnVersionUpdateRequest()
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(0)))

      val result: ReturnVersionUpdateReturn =
        service.updateReturnVersion(request).futureValue
      result.newVersion mustBe 0

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle 404 error from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest = mkReturnVersionUpdateRequest()
      val boom = UpstreamErrorResponse("Return not found", 404)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable =
        service.updateReturnVersion(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle 400 error from connector" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest = mkReturnVersionUpdateRequest()
      val boom = UpstreamErrorResponse("Bad Request", 400)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable =
        service.updateReturnVersion(request).failed.futureValue
      ex mustBe boom

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle version conflict scenarios" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "1")
      val conflictError = UpstreamErrorResponse("Version conflict", 409)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(conflictError))

      val ex: Throwable =
        service.updateReturnVersion(request).failed.futureValue
      ex mustBe conflictError

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle version strings as integers" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request1: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "1")
      val request2: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "25")
      val request3: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "314")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))
      when(connector.updateReturnVersioning(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(26)))
      when(connector.updateReturnVersioning(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(315)))

      service.updateReturnVersion(request1).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request2).futureValue.newVersion mustBe 26
      service.updateReturnVersion(request3).futureValue.newVersion mustBe 315

      verify(connector).updateReturnVersioning(eqTo(request1))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request2))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request3))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle version increments from same return" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request1: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN12345", "100001", "1")
      val request2: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN12345", "100001", "2")
      val request3: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest("STORN12345", "100001", "3")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(2)))
      when(connector.updateReturnVersioning(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(3)))
      when(connector.updateReturnVersioning(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(4)))

      service.updateReturnVersion(request1).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request2).futureValue.newVersion mustBe 3
      service.updateReturnVersion(request3).futureValue.newVersion mustBe 4

      verify(connector).updateReturnVersioning(eqTo(request1))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request2))(
        any[HeaderCarrier]
      )
      verify(connector).updateReturnVersioning(eqTo(request3))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle network timeout errors" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest = mkReturnVersionUpdateRequest()
      val boom = new java.util.concurrent.TimeoutException("Request timeout")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable =
        service.updateReturnVersion(request).failed.futureValue
      ex mustBe a[java.util.concurrent.TimeoutException]
      ex.getMessage mustBe "Request timeout"

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "must handle large version numbers" in {
      val connector = mock[FilingFormpProxyConnector]
      val service = new ReturnVersioningService(connector)
      val request: ReturnVersionUpdateRequest =
        mkReturnVersionUpdateRequest(currentVersion = "999")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.updateReturnVersioning(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkReturnVersionUpdateReturn(1000)))

      val result: ReturnVersionUpdateReturn =
        service.updateReturnVersion(request).futureValue
      result.newVersion mustBe 1000

      verify(connector).updateReturnVersioning(eqTo(request))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }
  }
}
