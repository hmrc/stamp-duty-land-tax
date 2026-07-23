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

import connectors.{ChrisConnector, FilingFormpProxyConnector, FormpProxyConnector}
import models.filing.*
import models.polling.{SubmissionForPolling, SubmissionsForPollingResponse}
import models.submission.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import scheduler.ScheduleStatus.{FailedToPollSubmissions, MongoUnlockException}
import service.filing.ChrisService
import service.submission.{EmailService, SubmissionAuditService}
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

class PollSubmissionsServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC)

  private val storn         = "STN800"
  private val ref           = "9200001"
  private val correlationId = "CORR123456789012345678901234"
  private val sentIrMark    = "SENTIRMARK=="

  private val submission = SubmissionForPolling(submissionId = "9600001", storn = storn, returnResourceRef = ref, submissionStatus = "ACCEPTED")

  private def govTalkRow(protocolStatus: String = "dataPoll",
                         lastMessage: String = "2026-07-23 11:00:00.000",
                         pollInterval: String = "60",
                         corrId: String = correlationId,
                         formLock: String = "N"): SelectGovTalkStatusResponse =
    SelectGovTalkStatusResponse(
      userIdentifier       = Some(storn),
      formResultId         = Some(ref),
      correlationId        = Some(corrId),
      formLock             = Some(formLock),
      createTimestamp      = Some("2026-07-23 10:00:00.000"),
      endStateTimestamp    = None,
      lastMessageTimestamp = Some(lastMessage),
      numberOfPolls        = Some("2"),
      pollInterval         = Some(pollInterval),
      protocolStatus       = Some(protocolStatus),
      gatewayUrl           = Some("http://chris.example/poll")
    )

  private val fullReturn = FullReturn(
    stornId           = Some(storn),
    returnResourceRef = Some(ref),
    submission        = Some(Submission(
      submissionID          = Some("9600001"),
      storn                 = Some(storn),
      submissionStatus      = Some("ACCEPTED"),
      irmarkSent            = Some(sentIrMark),
      submissionRequestDate = Some("2026-07-23 10:00:00.000")
    ))
  )

  private def completed(irMark: String): ChrisResponse.Completed =
    ChrisResponse.Completed(
      utrn             = Some("123456789MA"),
      receivedIrMark   = Some(irMark),
      correlationId    = Some(correlationId),
      responseEndPoint = Some("http://chris.example/poll"),
      rawXml           = "<response/>"
    )

  private trait Setup {
    val mockFormpConnector: FormpProxyConnector        = mock[FormpProxyConnector]
    val mockChrisConnector: ChrisConnector             = mock[ChrisConnector]
    val mockChrisService: ChrisService                 = mock[ChrisService]
    val mockFilingConnector: FilingFormpProxyConnector = mock[FilingFormpProxyConnector]
    val mockAudit: SubmissionAuditService              = mock[SubmissionAuditService]
    val mockEmailService: EmailService                 = mock[EmailService]
    val mockServicesConfig: ServicesConfig             = mock[ServicesConfig]
    val mockLockRepository: MongoLockRepository        = mock[MongoLockRepository]

    when(mockServicesConfig.getString(any())).thenReturn("20 minutes")
    when(mockServicesConfig.baseUrl(any())).thenReturn("http://chris.example")
    when(mockLockRepository.takeLock(any(), any(), any()))
      .thenReturn(Future.successful(Some(Lock("lockId", "owner", Instant.now(), Instant.now().plusSeconds(1200)))))
    when(mockLockRepository.releaseLock(any(), any())).thenReturn(Future.unit)
    when(mockChrisService.selectGovTalkStatus(any())(any())).thenReturn(Future.successful(govTalkRow()))
    when(mockChrisService.updateGovTalkStatusLock(any())(any())).thenReturn(Future.successful(GovTalkStatusReturn(success = true)))
    when(mockChrisService.updateGovTalkStatistics(any())(any())).thenReturn(Future.successful(GovTalkStatusReturn(success = true)))
    when(mockChrisService.updateGovTalkStatus(any())(any())).thenReturn(Future.successful(GovTalkStatusReturn(success = true)))
    when(mockChrisService.resetGovTalkStatus(any())(any())).thenReturn(Future.successful(GovTalkStatusReturn(success = true)))
    when(mockChrisService.updateSubmission(any())(any())).thenReturn(Future.successful(UpdateSubmissionReturn(success = true)))
    when(mockChrisService.createSubmissionErrorDetail(any())(any())).thenReturn(Future.successful(CreateSubmissionErrorDetailReturn(success = true)))
    when(mockFilingConnector.getFullReturn(any())(any())).thenReturn(Future.successful(fullReturn))
    when(mockChrisConnector.delete(any(), any())(any())).thenReturn(Future.successful(ChrisDeleteResponse.Deleted(Some(correlationId), "<deleted/>")))
    when(mockAudit.auditSubmission(any(), any(), any(), any(), any())(any())).thenReturn(Future.unit)
    when(mockEmailService.submitEmailConfirmation(any(), any(), any())(any())).thenReturn(Future.unit)

    val service: PollSubmissionsService = new PollSubmissionsService(
      mockFormpConnector,
      mockChrisConnector,
      mockChrisService,
      mockFilingConnector,
      mockAudit,
      mockEmailService,
      mockServicesConfig,
      mockLockRepository,
      fixedClock
    )

    def worklist(subs: SubmissionForPolling*): Unit =
      when(mockFormpConnector.getSubmissionsForPolling()(any()))
        .thenReturn(Future.successful(SubmissionsForPollingResponse(subs.toList)))

    def submissionStatuses: List[String] = {
      val captor: ArgumentCaptor[UpdateSubmissionRequest] = ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(mockChrisService, atLeastOnce()).updateSubmission(captor.capture())(any())
      captor.getAllValues.asScala.toList.flatMap(_.submission.submittableStatus)
    }
  }

  "PollSubmissionsService" - {
    "invoke" - {

      "marks the submission SUBMITTED when the poll completes with a matching IR mark" in new Setup {
        worklist(submission)
        when(mockChrisConnector.poll(any(), any())(any())).thenReturn(Future.successful(completed(sentIrMark)))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("SUBMITTED")
        verify(mockChrisConnector).delete(any(), any())(any())
        verify(mockChrisService).resetGovTalkStatus(any())(any())
        verify(mockAudit).auditSubmission(any(), any(), any(), any(), any())(any())
        verify(mockEmailService).submitEmailConfirmation(any(), any(), any())(any())
      }

      "marks the submission SUBMITTED_NO_RECEIPT when the received IR mark does not match" in new Setup {
        worklist(submission)
        when(mockChrisConnector.poll(any(), any())(any())).thenReturn(Future.successful(completed("DIFFERENTMARK==")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("SUBMITTED_NO_RECEIPT")
      }

      "leaves the submission ACCEPTED when ChRIS acknowledges again" in new Setup {
        worklist(submission)
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Acknowledged(Some(correlationId), Some(120), Some("http://chris.example/poll"), "<ack/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        verify(mockChrisService, never()).updateSubmission(any())(any())
        verify(mockChrisService, atLeastOnce()).updateGovTalkStatistics(any())(any())
      }

      "leaves the submission untouched and polls again next cycle on a timeout" in new Setup {
        worklist(submission)
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.TransportError("client timeout")))

        service.invoke.futureValue mustBe Right(List(ref))

        verify(mockChrisService, never()).updateSubmission(any())(any())
        verify(mockAudit, never()).auditSubmission(any(), any(), any(), any(), any())(any())
      }

      "marks the submission DEPARTMENTAL_ERROR and records error details on a departmental error" in new Setup {
        worklist(submission)
        private val departmental = GovTalkError(raisedBy = "Department", number = Some("3001"), errorType = "business", text = Some("BVR failure"), location = Some("line 1"))
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Errored(Seq(departmental), Some(correlationId), None, "<error/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("DEPARTMENTAL_ERROR")
        verify(mockChrisService).createSubmissionErrorDetail(any())(any())
        verify(mockChrisConnector).delete(any(), any())(any())
      }

      "releases the GovTalk row lock with the freshest interval and gateway after an acknowledgement" in new Setup {
        worklist(submission)
        when(mockChrisService.selectGovTalkStatus(any())(any()))
          .thenReturn(
            Future.successful(govTalkRow()),
            Future.successful(govTalkRow(formLock = "Y", pollInterval = "120").copy(gatewayUrl = Some("http://chris.example/next")))
          )
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Acknowledged(Some(correlationId), Some(120), Some("http://chris.example/next"), "<ack/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        private val captor: ArgumentCaptor[UpdateGovTalkStatusLockRequest] = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatusLockRequest])
        verify(mockChrisService, atLeastOnce()).updateGovTalkStatusLock(captor.capture())(any())
        private val releases = captor.getAllValues.asScala.toList.filter(_.govTalkStatus.formLockNew == "N")
        releases.map(_.govTalkStatus.pollInterval) must contain("120")
        releases.map(_.govTalkStatus.gatewayUrl) must contain("http://chris.example/next")
      }

      "resets the submission to STARTED when the error is recoverable" in new Setup {
        worklist(submission)
        private val recoverable = GovTalkError(raisedBy = "Gateway", number = Some("1000"), errorType = "fatal", text = Some("try again"), location = None)
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Errored(Seq(recoverable), Some(correlationId), None, "<error/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("STARTED")
      }

      "skips a submission whose GovTalk status is not at dataPoll" in new Setup {
        worklist(submission)
        when(mockChrisService.selectGovTalkStatus(any())(any()))
          .thenReturn(Future.successful(govTalkRow(protocolStatus = "initial")))

        service.invoke.futureValue mustBe Right(Nil)

        verify(mockChrisConnector, never()).poll(any(), any())(any())
      }

      "skips a submission whose poll interval has not yet elapsed" in new Setup {
        worklist(submission)
        when(mockChrisService.selectGovTalkStatus(any())(any()))
          .thenReturn(Future.successful(govTalkRow(lastMessage = "2026-07-23 11:59:30.000", pollInterval = "3600")))

        service.invoke.futureValue mustBe Right(Nil)

        verify(mockChrisConnector, never()).poll(any(), any())(any())
      }

      "skips a submission with no correlation id on its GovTalk status" in new Setup {
        worklist(submission)
        when(mockChrisService.selectGovTalkStatus(any())(any()))
          .thenReturn(Future.successful(govTalkRow(corrId = "empty")))

        service.invoke.futureValue mustBe Right(Nil)

        verify(mockChrisConnector, never()).poll(any(), any())(any())
      }

      "skips a submission when the GovTalk row lock cannot be acquired" in new Setup {
        worklist(submission)
        when(mockChrisService.updateGovTalkStatusLock(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("already locked")))

        service.invoke.futureValue mustBe Right(Nil)

        verify(mockChrisConnector, never()).poll(any(), any())(any())
      }

      "releases the GovTalk row lock and continues when polling one submission fails" in new Setup {
        private val second = submission.copy(submissionId = "9600002", returnResourceRef = "9200002")
        worklist(submission, second)
        when(mockChrisService.selectGovTalkStatus(any())(any()))
          .thenReturn(
            Future.successful(govTalkRow()),
            Future.successful(govTalkRow(formLock = "Y")),
            Future.successful(govTalkRow()),
            Future.successful(govTalkRow(formLock = "Y"))
          )
        when(mockFilingConnector.getFullReturn(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("formp unavailable")))
          .thenReturn(Future.successful(fullReturn))
        when(mockChrisConnector.poll(any(), any())(any())).thenReturn(Future.successful(completed(sentIrMark)))

        service.invoke.futureValue mustBe Right(List("9200002"))

        private val captor: ArgumentCaptor[UpdateGovTalkStatusLockRequest] = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatusLockRequest])
        verify(mockChrisService, atLeastOnce()).updateGovTalkStatusLock(captor.capture())(any())
        captor.getAllValues.asScala.toList.map(_.govTalkStatus.formLockNew) must contain("N")
      }

      "returns an empty list when nothing is due for polling" in new Setup {
        worklist()

        service.invoke.futureValue mustBe Right(Nil)
      }

      "skips the run when the lock is held by another instance" in new Setup {
        when(mockLockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(None))

        service.invoke.futureValue mustBe Right(Nil)
      }

      "returns MongoUnlockException when the lock repository fails" in new Setup {
        private val lockFailure = new RuntimeException("mongo down")
        when(mockLockRepository.takeLock(any(), any(), any())).thenReturn(Future.failed(lockFailure))

        service.invoke.futureValue mustBe Left(MongoUnlockException(lockFailure))
      }

      "returns FailedToPollSubmissions when fetching the submissions due for polling fails" in new Setup {
        private val fetchFailure = new RuntimeException("formp-proxy unavailable")
        when(mockFormpConnector.getSubmissionsForPolling()(any())).thenReturn(Future.failed(fetchFailure))

        service.invoke.futureValue mustBe Left(FailedToPollSubmissions(fetchFailure))
      }
    }
  }
}
