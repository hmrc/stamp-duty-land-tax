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
import org.mockito.Mockito
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
                         formLock: String = "N",
                         gateway: Option[String] = Some("http://chris.example/poll")): SelectGovTalkStatusResponse =
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
      gatewayUrl           = gateway
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
    when(mockChrisConnector.defaultPath).thenReturn("http://chris.example/ChRIS/SDLT/Filing/sync/SDLT")
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

    def submissionUpdates: List[SubmissionUpdate] = {
      val captor: ArgumentCaptor[UpdateSubmissionRequest] = ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(mockChrisService, atLeastOnce()).updateSubmission(captor.capture())(any())
      captor.getAllValues.asScala.toList.map(_.submission)
    }

    def submissionStatuses: List[String] = submissionUpdates.flatMap(_.submittableStatus)

    def govTalkStatistics: List[GovTalkStatusStatistics] = {
      val captor: ArgumentCaptor[UpdateGovTalkStatisticsRequest] = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatisticsRequest])
      verify(mockChrisService, atLeastOnce()).updateGovTalkStatistics(captor.capture())(any())
      captor.getAllValues.asScala.toList.map(_.govTalkStatus)
    }

    def lockReleases: List[GovTalkStatusLock] = {
      val captor: ArgumentCaptor[UpdateGovTalkStatusLockRequest] = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatusLockRequest])
      verify(mockChrisService, atLeastOnce()).updateGovTalkStatusLock(captor.capture())(any())
      captor.getAllValues.asScala.toList.map(_.govTalkStatus).filter(_.formLockNew == "N")
    }

    def protocolStatuses: List[String] = {
      val captor: ArgumentCaptor[UpdateGovTalkStatusRequest] = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatusRequest])
      verify(mockChrisService, atLeastOnce()).updateGovTalkStatus(captor.capture())(any())
      captor.getAllValues.asScala.toList.map(_.protocolStatus)
    }

    def submissionErrors: List[SubmissionErrorDetail] = {
      val captor: ArgumentCaptor[CreateSubmissionErrorDetailRequest] = ArgumentCaptor.forClass(classOf[CreateSubmissionErrorDetailRequest])
      verify(mockChrisService, atLeastOnce()).createSubmissionErrorDetail(captor.capture())(any())
      captor.getAllValues.asScala.toList.map(_.submissionErrorDetails)
    }

    def polledUrls: List[Option[String]] = {
      val captor: ArgumentCaptor[Option[String]] = ArgumentCaptor.forClass(classOf[Option[String]])
      verify(mockChrisConnector, atLeastOnce()).poll(captor.capture(), any())(any())
      captor.getAllValues.asScala.toList
    }
  }

  "PollSubmissionsService" - {
    "invoke" - {

      "marks the submission SUBMITTED and completes the delete, reset, audit and email steps when the poll completes with a matching IR mark" in new Setup {
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

      "leaves the submission ACCEPTED and stores the new poll interval and gateway when ChRIS acknowledges again" in new Setup {
        worklist(submission)
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Acknowledged(Some(correlationId), Some(120), Some("http://chris.example/next"), "<ack/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("ACCEPTED")
        govTalkStatistics.map(_.pollInterval) must contain("120")
        govTalkStatistics.map(_.gatewayUrl) must contain("http://chris.example/next")
      }

      "leaves the submission untouched when ChRIS gives no response" in new Setup {
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
        submissionErrors.map(_.position)     mustBe List("0")
        submissionErrors.map(_.errorMessage) mustBe List("3001: BVR failure")
        verify(mockChrisConnector).delete(any(), any())(any())
      }

      "marks the submission FATAL_ERROR and sends no delete on a fatal gateway error" in new Setup {
        worklist(submission)
        private val fatal = GovTalkError(raisedBy = "Gateway", number = Some("2000"), errorType = "fatal", text = Some("schema failure"), location = None)
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Errored(Seq(fatal), Some(correlationId), None, "<error/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("FATAL_ERROR")
        verify(mockChrisConnector, never()).delete(any(), any())(any())
        verify(mockChrisService, never()).updateGovTalkStatus(any())(any())
        submissionErrors.map(_.errorMessage) mustBe List("2000: schema failure")
      }

      "leaves GovTalk at deleteRequest when the ChRIS delete fails after a successful poll" in new Setup {
        worklist(submission)
        when(mockChrisConnector.poll(any(), any())(any())).thenReturn(Future.successful(completed(sentIrMark)))
        when(mockChrisConnector.delete(any(), any())(any()))
          .thenReturn(Future.successful(ChrisDeleteResponse.TransportError("client timeout")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("SUBMITTED")
        protocolStatuses mustBe List("deleteRequest")
        verify(mockChrisService, never()).resetGovTalkStatus(any())(any())
        verify(mockEmailService).submitEmailConfirmation(any(), any(), any())(any())
      }

      "persists the UTRN and IR mark even when the CIP audit fails" in new Setup {
        worklist(submission)
        when(mockChrisConnector.poll(any(), any())(any())).thenReturn(Future.successful(completed(sentIrMark)))
        when(mockAudit.auditSubmission(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("datastream down")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionUpdates.flatMap(_.utrn)           must contain("123456789MA")
        submissionUpdates.flatMap(_.IRMarkRecieved) must contain(sentIrMark)
        verify(mockEmailService).submitEmailConfirmation(any(), any(), any())(any())
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

        lockReleases.map(_.pollInterval) must contain("120")
        lockReleases.map(_.gatewayUrl)   must contain("http://chris.example/next")
      }

      "increments the number of polls before the poll is sent" in new Setup {
        worklist(submission)
        when(mockChrisConnector.poll(any(), any())(any())).thenReturn(Future.successful(completed(sentIrMark)))

        service.invoke.futureValue mustBe Right(List(ref))

        private val ordered = Mockito.inOrder(mockChrisService, mockChrisConnector)
        ordered.verify(mockChrisService).updateGovTalkStatistics(any())(any())
        ordered.verify(mockChrisConnector).poll(any(), any())(any())
        govTalkStatistics.head.numberOfPolls mustBe "3"
      }

      "ignores a stored gateway url that is only the ChRIS host" in new Setup {
        worklist(submission)
        when(mockChrisService.selectGovTalkStatus(any())(any()))
          .thenReturn(Future.successful(govTalkRow(gateway = Some("http://chris.example"))))
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Acknowledged(Some(correlationId), Some(60), None, "<ack/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        polledUrls mustBe List(None)
      }

      "falls back to the full ChRIS submission url when no gateway url is stored or returned" in new Setup {
        worklist(submission)
        when(mockChrisService.selectGovTalkStatus(any())(any()))
          .thenReturn(Future.successful(govTalkRow(gateway = None)))
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Acknowledged(Some(correlationId), Some(60), None, "<ack/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        govTalkStatistics.map(_.gatewayUrl) must contain("http://chris.example/ChRIS/SDLT/Filing/sync/SDLT")
      }

      "resets the submission to STARTED when the error is recoverable" in new Setup {
        worklist(submission)
        private val recoverable = GovTalkError(raisedBy = "Gateway", number = Some("1000"), errorType = "fatal", text = Some("try again"), location = None)
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Errored(Seq(recoverable), Some(correlationId), None, "<error/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("STARTED")
      }

      "resets the submission to STARTED when a departmental error is also recoverable" in new Setup {
        worklist(submission)
        private val departmental = GovTalkError(raisedBy = "Department", number = Some("3001"), errorType = "business", text = Some("BVR failure"), location = None)
        private val recoverable  = GovTalkError(raisedBy = "Gateway", number = Some("1000"), errorType = "fatal", text = Some("try again"), location = None)
        when(mockChrisConnector.poll(any(), any())(any()))
          .thenReturn(Future.successful(ChrisResponse.Errored(Seq(departmental, recoverable), Some(correlationId), None, "<error/>")))

        service.invoke.futureValue mustBe Right(List(ref))

        submissionStatuses must contain("DEPARTMENTAL_ERROR")
        submissionStatuses.last mustBe "STARTED"
        submissionUpdates.last.submissionRequestDate mustBe None
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

        lockReleases must not be empty
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
