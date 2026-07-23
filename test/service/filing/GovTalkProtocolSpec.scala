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

package service.filing

import connectors.ChrisConnector
import models.filing.*
import models.submission.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

class GovTalkProtocolSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private implicit val ec: ExecutionContext = ExecutionContext.global
  private implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val storn         = "STN800"
  private val returnId      = "9200001"
  private val correlationId = "CORR123456789012345678901234"

  private class TestGovTalkProtocol(override protected val chrisService: ChrisService,
                                    override protected val chrisConnector: ChrisConnector)
                                   (implicit ec: ExecutionContext, hc: HeaderCarrier)
    extends GovTalkProtocol with Logging {

    override protected val logPrefix: String = "TestService"

    override protected def logRef(storn: String, returnId: String, correlationId: String): String =
      s"storn=$storn ref=$returnId"

    override protected def chrisHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier = hc

    def protocolStatus(status: String): Future[Unit]          = setGovTalkProtocol(storn, returnId, status, correlationId)
    def statistics(gateway: Option[String],
                   interval: String = "0",
                   polls: Int = 0): Future[Unit]              = updateGovTalkStatistics(storn, returnId, gateway, correlationId, interval, polls)
    def finalise(): Future[Unit]                              = finaliseGovTalkStatus(storn, returnId, correlationId)
    def delete(endpoint: Option[String]): Future[Boolean]     = sendChrisDelete(storn, returnId, endpoint, correlationId)
    def errorDetails(errors: Seq[GovTalkError]): Future[Unit] = createSubmissionErrorDetails(storn, returnId, errors, correlationId)
    def resetRequest(): ResetGovTalkStatusRequest             = buildResetRequest(storn, returnId)
    def recoverable(errors: Seq[GovTalkError]): Boolean       = isRecoverable(errors)
  }

  private trait Setup {
    val mockChrisService: ChrisService     = mock[ChrisService]
    val mockChrisConnector: ChrisConnector = mock[ChrisConnector]

    when(mockChrisConnector.defaultPath).thenReturn("http://chris.example/ChRIS/SDLT/Filing/sync/SDLT")
    when(mockChrisService.updateGovTalkStatus(any())(any())).thenReturn(Future.successful(GovTalkStatusReturn(success = true)))
    when(mockChrisService.updateGovTalkStatistics(any())(any())).thenReturn(Future.successful(GovTalkStatusReturn(success = true)))
    when(mockChrisService.resetGovTalkStatus(any())(any())).thenReturn(Future.successful(GovTalkStatusReturn(success = true)))
    when(mockChrisService.createSubmissionErrorDetail(any())(any())).thenReturn(Future.successful(CreateSubmissionErrorDetailReturn(success = true)))
    when(mockChrisConnector.delete(any(), any())(any())).thenReturn(Future.successful(ChrisDeleteResponse.Deleted(Some(correlationId), "<deleted/>")))

    val protocol: TestGovTalkProtocol = new TestGovTalkProtocol(mockChrisService, mockChrisConnector)

    def statisticsSent: GovTalkStatusStatistics = {
      val captor: ArgumentCaptor[UpdateGovTalkStatisticsRequest] = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatisticsRequest])
      verify(mockChrisService).updateGovTalkStatistics(captor.capture())(any())
      captor.getValue.govTalkStatus
    }

    def errorDetailsSent: List[SubmissionErrorDetail] = {
      val captor: ArgumentCaptor[CreateSubmissionErrorDetailRequest] = ArgumentCaptor.forClass(classOf[CreateSubmissionErrorDetailRequest])
      verify(mockChrisService, org.mockito.Mockito.atLeastOnce()).createSubmissionErrorDetail(captor.capture())(any())
      captor.getAllValues.asScala.toList.map(_.submissionErrorDetails)
    }
  }

  private def govTalkError(number: Option[String], text: Option[String] = Some("something went wrong"), location: Option[String] = Some("line 1")) =
    GovTalkError(raisedBy = "Gateway", number = number, errorType = "fatal", text = text, location = location)

  "GovTalkProtocol" - {

    "setGovTalkProtocol" - {
      "writes the requested protocol status against the return" in new Setup {
        protocol.protocolStatus("deleteRequest").futureValue

        val captor: ArgumentCaptor[UpdateGovTalkStatusRequest] = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatusRequest])
        verify(mockChrisService).updateGovTalkStatus(captor.capture())(any())
        captor.getValue.protocolStatus mustBe "deleteRequest"
        captor.getValue.userIdentifier mustBe storn
        captor.getValue.formResultId mustBe returnId
      }
    }

    "updateGovTalkStatistics" - {
      "defaults the poll count and interval to zero for the submit path" in new Setup {
        protocol.statistics(Some("http://chris.example/poll")).futureValue

        statisticsSent.numberOfPolls mustBe "0"
        statisticsSent.pollInterval mustBe "0"
      }

      "carries the poll count and interval supplied by the poller" in new Setup {
        protocol.statistics(Some("http://chris.example/poll"), interval = "120", polls = 3).futureValue

        statisticsSent.numberOfPolls mustBe "3"
        statisticsSent.pollInterval mustBe "120"
      }

      "falls back to the full ChRIS submission url when no gateway is supplied" in new Setup {
        protocol.statistics(None).futureValue

        statisticsSent.gatewayUrl mustBe "http://chris.example/ChRIS/SDLT/Filing/sync/SDLT"
      }
    }

    "finaliseGovTalkStatus" - {
      "moves the row to endState and then resets it" in new Setup {
        protocol.finalise().futureValue

        val captor: ArgumentCaptor[UpdateGovTalkStatusRequest] = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatusRequest])
        verify(mockChrisService).updateGovTalkStatus(captor.capture())(any())
        captor.getValue.protocolStatus mustBe "endState"
        verify(mockChrisService).resetGovTalkStatus(any())(any())
      }

      "does not fail the caller when the reset fails" in new Setup {
        when(mockChrisService.resetGovTalkStatus(any())(any())).thenReturn(Future.failed(new RuntimeException("formp down")))

        protocol.finalise().futureValue mustBe ()
      }
    }

    "buildResetRequest" - {
      "clears the correlation id and counters and returns the row to initial" in new Setup {
        val request: ResetGovTalkStatusRequest = protocol.resetRequest()

        request.correlationId mustBe "empty"
        request.govTalkStatus.protocolStatusOld mustBe "endState"
        request.govTalkStatus.protocolStatusNew mustBe "initial"
        request.govTalkStatus.numberOfPolls mustBe "0"
        request.govTalkStatus.formLock mustBe "N"
        request.govTalkStatus.endStateTimestamp mustBe None
      }
    }

    "sendChrisDelete" - {
      "reports success when ChRIS deletes the resource" in new Setup {
        protocol.delete(Some("http://chris.example/poll")).futureValue mustBe true
      }

      "treats an already gone resource as a successful delete" in new Setup {
        when(mockChrisConnector.delete(any(), any())(any()))
          .thenReturn(Future.successful(ChrisDeleteResponse.NotFound(Some(correlationId), "<notfound/>")))

        protocol.delete(None).futureValue mustBe true
      }

      "reports failure when ChRIS returns errors" in new Setup {
        when(mockChrisConnector.delete(any(), any())(any()))
          .thenReturn(Future.successful(ChrisDeleteResponse.Errored(Seq(govTalkError(Some("2001"), Some("nope"))), Some(correlationId), "<error/>")))

        protocol.delete(None).futureValue mustBe false
      }

      "reports failure and does not throw when the delete call itself fails" in new Setup {
        when(mockChrisConnector.delete(any(), any())(any())).thenReturn(Future.failed(new RuntimeException("chris down")))

        protocol.delete(None).futureValue mustBe false
      }
    }

    "createSubmissionErrorDetails" - {
      "numbers each error from zero and prefixes the message with the error code" in new Setup {
        protocol.errorDetails(Seq(govTalkError(Some("3001"), Some("BVR failure")), govTalkError(Some("1000"), Some("try again")))).futureValue

        errorDetailsSent.map(_.position) mustBe List("0", "1")
        errorDetailsSent.map(_.errorMessage) mustBe List("3001: BVR failure", "1000: try again")
      }

      "falls back to the bare message when the error carries no code" in new Setup {
        protocol.errorDetails(Seq(govTalkError(None, Some("no code here")))).futureValue

        errorDetailsSent.map(_.errorMessage) mustBe List("no code here")
      }

      "writes nothing when there are no errors" in new Setup {
        protocol.errorDetails(Nil).futureValue

        verify(mockChrisService, org.mockito.Mockito.never()).createSubmissionErrorDetail(any())(any())
      }
    }

    "isRecoverable" - {
      "treats 1000, 2005 and 3000 as recoverable" in new Setup {
        protocol.recoverable(Seq(govTalkError(Some("1000")))) mustBe true
        protocol.recoverable(Seq(govTalkError(Some("2005")))) mustBe true
        protocol.recoverable(Seq(govTalkError(Some("3000")))) mustBe true
      }

      "treats any other code as unrecoverable" in new Setup {
        protocol.recoverable(Seq(govTalkError(Some("3001")))) mustBe false
        protocol.recoverable(Seq(govTalkError(None))) mustBe false
        protocol.recoverable(Nil) mustBe false
      }
    }
  }
}
