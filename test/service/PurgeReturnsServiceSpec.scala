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

package service

import connectors.FormpProxyConnector
import models.purge.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class PurgeReturnsServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockConnector: FormpProxyConnector     = mock[FormpProxyConnector]
  private val mockServicesConfig: ServicesConfig     = mock[ServicesConfig]
  private val mockLockRepository: MongoLockRepository = mock[MongoLockRepository]
  private val fixedClock: Clock                       = Clock.fixed(Instant.parse("2026-07-05T00:00:00Z"), ZoneOffset.UTC)

  private def newService: PurgeReturnsService = {
    when(mockServicesConfig.getString(any())).thenReturn("20 minutes")
    when(mockLockRepository.takeLock(any(), any(), any()))
      .thenReturn(Future.successful(Some(Lock("lockId", "owner", Instant.now(), Instant.now().plusSeconds(1200)))))
    when(mockLockRepository.releaseLock(any(), any())).thenReturn(Future.unit)
    new PurgeReturnsService(mockConnector, mockServicesConfig, mockLockRepository, fixedClock)
  }

  private val submitted = ReturnForPurge("STN700", "9000001", "SUBMITTED")
  private val started   = ReturnForPurge("STN700", "9000002", "STARTED")

  "PurgeReturnsService" - {
    "invoke" - {

      "returns the resource refs of all successfully purged returns" in {
        when(mockConnector.getReturnsForPurge(any())(any()))
          .thenReturn(Future.successful(ReturnsForPurgeResponse(List(submitted, started))))
        when(mockConnector.deleteReturn(any())(any()))
          .thenReturn(Future.successful(DeleteReturnResponse(deleted = true)))

        newService.invoke.futureValue mustBe Right(List("9000001", "9000002"))
      }

      "records a return as not purged and continues when its delete throws, returning only the successful refs" in {
        when(mockConnector.getReturnsForPurge(any())(any()))
          .thenReturn(Future.successful(ReturnsForPurgeResponse(List(submitted, started))))
        when(mockConnector.deleteReturn(eqTo(DeleteReturnRequest("STN700", "9000001")))(any()))
          .thenReturn(Future.successful(DeleteReturnResponse(deleted = true)))
        when(mockConnector.deleteReturn(eqTo(DeleteReturnRequest("STN700", "9000002")))(any()))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        newService.invoke.futureValue mustBe Right(List("9000001"))
      }

      "returns an empty list when nothing is due for purge" in {
        when(mockConnector.getReturnsForPurge(any())(any()))
          .thenReturn(Future.successful(ReturnsForPurgeResponse(Nil)))

        newService.invoke.futureValue mustBe Right(Nil)
      }
    }
  }
}
