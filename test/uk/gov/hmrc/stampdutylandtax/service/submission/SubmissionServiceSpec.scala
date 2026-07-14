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

package uk.gov.hmrc.stampdutylandtax.service.submission

import base.SpecBase
import connectors.ChrisConnector
import models.filing.*
import models.submission.*
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import service.filing.ChrisService
import service.submission.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.xml.Elem

final class SubmissionServiceSpec extends SpecBase {

  private given ExecutionContext = ExecutionContext.global
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val storn     = "STORN12345"
  private val returnId  = "100001"
  private val cred      = "CRED-1"
  private val sender    = SenderType.Agent
  private val periodEnd = LocalDate.parse("2026-01-31")
  private val sentMark  = "SENT-MARK"
  private val utrn      = "123456789MA"

  private def aReturn(
                       stornId: Option[String] = Some(storn),
                       ref: Option[String] = Some(returnId),
                       version: Option[String] = Some("1"),
                       submission: Option[Submission] = None
                     ): FullReturn =
    FullReturn(
      stornId           = stornId,
      returnResourceRef = ref,
      returnInfo        = Some(ReturnInfo(returnID = Some(returnId), version = version)),
      submission        = submission
    )

  private def withStatus(status: String): FullReturn =
    aReturn(submission = Some(Submission(submissionStatus = Some(status))))

  private def completed(utrn: Option[String], receivedIrMark: Option[String]): ChrisResponse.Completed =
    ChrisResponse.Completed(utrn, receivedIrMark, Some("corr"), Some("url"), "<response/>")

  private def acknowledged: ChrisResponse.Acknowledged =
    ChrisResponse.Acknowledged(Some("corr"), Some(10), Some("url"), "<ack/>")

  private def errored(errs: GovTalkError*): ChrisResponse.Errored =
    ChrisResponse.Errored(errs.toSeq, Some("corr"), Some("url"), "<error/>")

  private def transportError(message: String): ChrisResponse.TransportError =
    ChrisResponse.TransportError(message, "<transport-error/>")

  private val departmentalError: GovTalkError =
    GovTalkError(raisedBy = "Department", number = Some("3001"), errorType = "business", text = Some("Invalid STORN"), location = Some("/SDLT/x"))

  private val fatalError: GovTalkError =
    GovTalkError(raisedBy = "Gateway", number = Some("9999"), errorType = "fatal", text = Some("Boom"), location = None)

  private val fatalError2: GovTalkError =
    GovTalkError(raisedBy = "Gateway", number = Some("8888"), errorType = "fatal", text = Some("Boom two"), location = None)

  private val recoverableError: GovTalkError =
    GovTalkError(raisedBy = "Gateway", number = Some("2005"), errorType = "fatal", text = Some("Transient"), location = None)

  private final class Fixtures:
    val envelopeBuilder: GovTalkEnvelopeBuilder = mock[GovTalkEnvelopeBuilder]
    val validator: SchemaValidator              = mock[SchemaValidator]
    val connector: ChrisConnector               = mock[ChrisConnector]
    val audit: SubmissionAuditService           = mock[SubmissionAuditService]
    val chrisService: ChrisService              = mock[ChrisService]
    val appConfig: ServicesConfig               = mock[ServicesConfig]

    val service = new SubmissionService(envelopeBuilder, validator, connector, audit, chrisService, appConfig)

    when(appConfig.baseUrl("chris")).thenReturn("http://chris")
    when(validator.validateSdlt(any[Elem])).thenReturn(Right(()))
    when(envelopeBuilder.submissionRequest(any[Elem], any[String], any[LocalDate], any[SenderType], any[String]))
      .thenReturn(IrMarkResult(<Envelope/>, sentMark, "SENT-MARK-B32"))

    when(chrisService.lockReturn(any[LockReturnRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(LockReturnResponse(true))))
    when(chrisService.deleteSubmissionErrorDetail(any[DeleteSubmissionErrorDetailRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(DeleteSubmissionErrorDetailReturn(true)))
    when(chrisService.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(CreateSubmissionReturn(true)))
    when(chrisService.selectGovTalkStatus(any[SelectGovTalkStatusRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(SelectGovTalkStatusResponse(None, None, None, None, None, None, None, None, None, None, None)))
    when(chrisService.resetGovTalkStatus(any[ResetGovTalkStatusRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(GovTalkStatusReturn(true)))
    when(chrisService.insertInitialGovTalkStatus(any[InsertInitialGovTalkStatusRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(GovTalkStatusReturn(true)))
    when(chrisService.updateSubmission(any[UpdateSubmissionRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(UpdateSubmissionReturn(true)))
    when(chrisService.updateGovTalkStatusLock(any[UpdateGovTalkStatusLockRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(GovTalkStatusReturn(true)))
    when(chrisService.updateGovTalkStatus(any[UpdateGovTalkStatusRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(GovTalkStatusReturn(true)))
    when(chrisService.updateGovTalkStatistics(any[UpdateGovTalkStatisticsRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(GovTalkStatusReturn(true)))
    when(chrisService.createSubmissionErrorDetail(any[CreateSubmissionErrorDetailRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(CreateSubmissionErrorDetailReturn(true)))
    when(audit.auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier]))
      .thenReturn(Future.unit)

    when(connector.delete(any[Option[String]], any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(ChrisDeleteResponse.Deleted(Some("corr"), "<delete-response/>")))

    def onResponse(resp: ChrisResponse): Unit =
      when(connector.submit(any[Elem], any[Option[String]], any[String])(any[HeaderCarrier])).thenReturn(Future.successful(resp))

    def submissions: Seq[SubmissionUpdate] =
      val captor = ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(chrisService, atLeastOnce()).updateSubmission(captor.capture())(any[HeaderCarrier])
      captor.getAllValues.asScala.toSeq.map(_.submission)

    def statuses: Seq[String] = submissions.flatMap(_.submittableStatus)

    def protocols: Seq[String] =
      val captor = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatusRequest])
      verify(chrisService, atLeastOnce()).updateGovTalkStatus(captor.capture())(any[HeaderCarrier])
      captor.getAllValues.asScala.toSeq.map(_.protocolStatus)

    def lockFlags: Seq[String] =
      val captor = ArgumentCaptor.forClass(classOf[UpdateGovTalkStatusLockRequest])
      verify(chrisService, atLeastOnce()).updateGovTalkStatusLock(captor.capture())(any[HeaderCarrier])
      captor.getAllValues.asScala.toSeq.map(_.govTalkStatus.formLockNew)

    def neverSubmitted(): Unit =
      verify(connector, never()).submit(any[Elem], any[Option[String]], any[String])(any[HeaderCarrier])

  "SubmissionService.submit requireContext" - {

    "must fail with MissingSubmissionContextException when the credential identifier is blank" in {
      val f = new Fixtures
      f.service.submit(aReturn(), sender, periodEnd, "   ").failed.futureValue mustBe a[MissingSubmissionContextException]
      verify(f.chrisService, never()).lockReturn(any[LockReturnRequest])(any[HeaderCarrier])
    }

    "must fail with MissingSubmissionContextException when stornId is missing" in {
      val f = new Fixtures
      f.service.submit(aReturn(stornId = None), sender, periodEnd, cred).failed.futureValue mustBe a[MissingSubmissionContextException]
    }

    "must fail with MissingSubmissionContextException when the return resource ref is missing" in {
      val f = new Fixtures
      f.service.submit(aReturn(ref = None), sender, periodEnd, cred).failed.futureValue mustBe a[MissingSubmissionContextException]
    }

    "must fail with MissingSubmissionContextException when the return version is missing" in {
      val f = new Fixtures
      f.service.submit(aReturn(version = None), sender, periodEnd, cred).failed.futureValue mustBe a[MissingSubmissionContextException]
    }
  }

  "SubmissionService.submit return lock" - {

    "must fail with ReturnLockConflictException when the lock is refused" in {
      val f = new Fixtures
      when(f.chrisService.lockReturn(any[LockReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("locked", 423))))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue mustBe a[ReturnLockConflictException]
    }

    "must make no ChRIS submit call when the lock is refused" in {
      val f = new Fixtures
      when(f.chrisService.lockReturn(any[LockReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("locked", 423))))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue
      f.neverSubmitted()
    }

    "must lock the return with the storn, resource ref and numeric version" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred)

      val captor = ArgumentCaptor.forClass(classOf[LockReturnRequest])
      verify(f.chrisService, Mockito.timeout(2000)).lockReturn(captor.capture())(any[HeaderCarrier])
      val req = captor.getValue
      req.storn mustBe storn
      req.returnResourceRef mustBe returnId
      req.version mustBe 1
    }
  }

  "SubmissionService.submit existing submission" - {

    "must clear prior error details when the existing submission is in ERROR" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(withStatus("ERROR"), sender, periodEnd, cred).futureValue
      verify(f.chrisService).deleteSubmissionErrorDetail(any[DeleteSubmissionErrorDetailRequest])(any[HeaderCarrier])
      verify(f.chrisService, never()).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
    }

    "must clear prior error details when the existing submission is in FAILED" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(withStatus("FAILED"), sender, periodEnd, cred).futureValue
      verify(f.chrisService).deleteSubmissionErrorDetail(any[DeleteSubmissionErrorDetailRequest])(any[HeaderCarrier])
    }

    "must neither clear nor create when the existing submission is not re-submittable" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(withStatus("PENDING"), sender, periodEnd, cred).futureValue
      verify(f.chrisService, never()).deleteSubmissionErrorDetail(any[DeleteSubmissionErrorDetailRequest])(any[HeaderCarrier])
      verify(f.chrisService, never()).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
    }

    "must create a submission when none exists" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      verify(f.chrisService).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
      verify(f.chrisService, never()).deleteSubmissionErrorDetail(any[DeleteSubmissionErrorDetailRequest])(any[HeaderCarrier])
    }
  }

  "SubmissionService.submit GovTalk status" - {

    "must reset the row when one already exists" in {
      val f = new Fixtures
      when(f.chrisService.selectGovTalkStatus(any[SelectGovTalkStatusRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(SelectGovTalkStatusResponse(
          userIdentifier = Some(storn),
          formResultId = Some(returnId),
          correlationId = Some("corr"),
          formLock = Some("false"),
          createTimestamp = Some("2026-01-31 09:15:30"),
          endStateTimestamp = None,
          lastMessageTimestamp = Some("2026-01-31 09:15:30"),
          numberOfPolls = Some("0"),
          pollInterval = Some("0"),
          protocolStatus = Some("initial"),
          gatewayUrl = Some("http://chris")
        )))
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      verify(f.chrisService).resetGovTalkStatus(any[ResetGovTalkStatusRequest])(any[HeaderCarrier])
      verify(f.chrisService, never()).insertInitialGovTalkStatus(any[InsertInitialGovTalkStatusRequest])(any[HeaderCarrier])
    }

    "must insert an initial row when the lookup fails" in {
      val f = new Fixtures
      when(f.chrisService.selectGovTalkStatus(any[SelectGovTalkStatusRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("not found")))
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      verify(f.chrisService).insertInitialGovTalkStatus(any[InsertInitialGovTalkStatusRequest])(any[HeaderCarrier])
      verify(f.chrisService, never()).resetGovTalkStatus(any[ResetGovTalkStatusRequest])(any[HeaderCarrier])
    }
  }

  "SubmissionService.submit build and validate" - {

    "must fail with SchemaValidationException when validation fails" in {
      val f = new Fixtures
      when(f.validator.validateSdlt(any[Elem])).thenReturn(Left(Seq("bad element", "missing field")))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue mustBe a[SchemaValidationException]
    }

    "must build no envelope and make no ChRIS call when validation fails" in {
      val f = new Fixtures
      when(f.validator.validateSdlt(any[Elem])).thenReturn(Left(Seq("bad element")))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue
      verify(f.envelopeBuilder, never()).submissionRequest(any[Elem], any[String], any[LocalDate], any[SenderType], any[String])
      f.neverSubmitted()
    }

    "must persist a PENDING seed carrying the sent IRmark" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      val seed = f.submissions.head
      seed.submittableStatus mustBe Some("PENDING")
      seed.IRMarkSent        mustBe Some(sentMark)
    }

    "must build the envelope with the storn, period end, sender and credential" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      verify(f.envelopeBuilder).submissionRequest(any[Elem], eqTo(storn), eqTo(periodEnd), eqTo(sender), eqTo(cred))
    }
  }

  "SubmissionService.submit GovTalk lock gate" - {

    "must fail with GovTalkLockNotAcquiredException when the lock cannot be acquired" in {
      val f = new Fixtures
      when(f.chrisService.updateGovTalkStatusLock(any[UpdateGovTalkStatusLockRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("lock held")))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue mustBe a[GovTalkLockNotAcquiredException]
    }

    "must make no ChRIS call when the lock cannot be acquired" in {
      val f = new Fixtures
      when(f.chrisService.updateGovTalkStatusLock(any[UpdateGovTalkStatusLockRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("lock held")))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue
      f.neverSubmitted()
    }

    "must submit to ChRIS once when the lock is acquired" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      verify(f.connector).submit(any[Elem], any[Option[String]], any[String])(any[HeaderCarrier])
    }
  }

  "SubmissionService.submit on Completed with a matching IRmark" - {

    def run(f: Fixtures) =
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred)

    "must return the Completed response" in {
      val f = new Fixtures
      val resp = completed(Some(utrn), Some(sentMark))
      f.onResponse(resp)
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue mustBe resp
    }

    "must resolve the status to SUBMITTED" in {
      val f = new Fixtures; run(f).futureValue
      f.statuses must contain("SUBMITTED")
    }

    "must preserve the sent IRmark on the UTRN write (accumulator not nulled)" in {
      val f = new Fixtures; run(f).futureValue
      f.submissions.find(_.utrn.isDefined).value.IRMarkSent mustBe Some(sentMark)
    }

    "must record the UTRN and received IRmark" in {
      val f = new Fixtures; run(f).futureValue
      val w = f.submissions.find(_.utrn.isDefined).value
      w.utrn           mustBe Some(utrn)
      w.IRMarkRecieved mustBe Some(sentMark)
    }

    "must drive the GovTalk deleteRequest and endState protocol transitions" in {
      val f = new Fixtures; run(f).futureValue
      f.protocols must contain allOf ("deleteRequest", "endState")
    }

    "must audit the submission" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.audit).auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier])
    }

    "must not create any error details" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.chrisService, never()).createSubmissionErrorDetail(any[CreateSubmissionErrorDetailRequest])(any[HeaderCarrier])
    }

    "must run the datetime footer" in {
      val f = new Fixtures; run(f).futureValue
      f.submissions.exists(_.submissionRequestDate.isDefined) mustBe true
    }
  }

  "SubmissionService.submit on Completed with a non-matching IRmark" - {

    "must resolve the status to SUBMITTED_NO_RECEIPT" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some("DIFFERENT-MARK")))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      f.statuses must contain("SUBMITTED_NO_RECEIPT")
    }

    "must still record the UTRN and audit" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some("DIFFERENT-MARK")))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      f.submissions.exists(_.utrn.contains(utrn)) mustBe true
      verify(f.audit).auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier])
    }
  }

  "SubmissionService.submit on Acknowledged" - {

    def run(f: Fixtures) =
      f.onResponse(acknowledged)
      f.service.submit(aReturn(), sender, periodEnd, cred)

    "must return the Acknowledged response" in {
      val f = new Fixtures; run(f).futureValue mustBe acknowledged
    }

    "must resolve the status to ACCEPTED" in {
      val f = new Fixtures; run(f).futureValue
      f.statuses must contain("ACCEPTED")
    }

    "must set the acceptedDate" in {
      val f = new Fixtures; run(f).futureValue
      f.submissions.exists(_.acceptedDate.isDefined) mustBe true
    }

    "must drive the dataPoll protocol transition" in {
      val f = new Fixtures; run(f).futureValue
      f.protocols must contain("dataPoll")
    }

    "must update the GovTalk statistics" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.chrisService).updateGovTalkStatistics(any[UpdateGovTalkStatisticsRequest])(any[HeaderCarrier])
    }

    "must not audit" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.audit, never()).auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier])
    }

    "must not create any error details" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.chrisService, never()).createSubmissionErrorDetail(any[CreateSubmissionErrorDetailRequest])(any[HeaderCarrier])
    }
  }

  "SubmissionService.submit on a departmental (business-reject) error" - {

    def run(f: Fixtures) =
      f.onResponse(errored(departmentalError))
      f.service.submit(aReturn(), sender, periodEnd, cred)

    "must resolve the status to DEPARTMENTAL_ERROR" in {
      val f = new Fixtures; run(f).futureValue
      f.statuses must contain("DEPARTMENTAL_ERROR")
    }

    "must record the first error's code, type and message" in {
      val f = new Fixtures; run(f).futureValue
      val w = f.submissions.find(_.govTalkErrorCode.isDefined).value
      w.govTalkErrorCode    mustBe Some("3001")
      w.govTalkErrorType    mustBe Some("business")
      w.govTalkErrorMessage mustBe Some("Invalid STORN")
    }

    "must drive the GovTalk deleteRequest and endState protocol transitions" in {
      val f = new Fixtures; run(f).futureValue
      f.protocols must contain allOf ("deleteRequest", "endState")
    }

    "must create an error detail" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.chrisService).createSubmissionErrorDetail(any[CreateSubmissionErrorDetailRequest])(any[HeaderCarrier])
    }

    "must audit the submission" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.audit).auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier])
    }
  }

  "SubmissionService.submit on a fatal error" - {

    def run(f: Fixtures) =
      f.onResponse(errored(fatalError))
      f.service.submit(aReturn(), sender, periodEnd, cred)

    "must resolve the status to FATAL_ERROR" in {
      val f = new Fixtures; run(f).futureValue
      f.statuses must contain("FATAL_ERROR")
    }

    "must not drive any GovTalk protocol transition" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.chrisService, never()).updateGovTalkStatus(any[UpdateGovTalkStatusRequest])(any[HeaderCarrier])
    }

    "must still create an error detail and audit" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.chrisService).createSubmissionErrorDetail(any[CreateSubmissionErrorDetailRequest])(any[HeaderCarrier])
      verify(f.audit).auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier])
    }

    "must run the datetime footer" in {
      val f = new Fixtures; run(f).futureValue
      f.submissions.exists(_.submissionRequestDate.isDefined) mustBe true
    }
  }

  "SubmissionService.submit on a recoverable error" - {

    def run(f: Fixtures) =
      f.onResponse(errored(recoverableError))
      f.service.submit(aReturn(), sender, periodEnd, cred)

    "must overwrite the status to STARTED" in {
      val f = new Fixtures; run(f).futureValue
      f.statuses must contain("STARTED")
    }

    "must clear the request datetime and skip the footer" in {
      val f = new Fixtures; run(f).futureValue
      f.submissions.forall(_.submissionRequestDate.isEmpty) mustBe true
    }

    "must not drive any GovTalk protocol transition" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.chrisService, never()).updateGovTalkStatus(any[UpdateGovTalkStatusRequest])(any[HeaderCarrier])
    }

    "must audit the submission" in {
      val f = new Fixtures; run(f).futureValue
      verify(f.audit).auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier])
    }
  }

  "SubmissionService.submit on a TransportError" - {

    "must resolve a non-timeout transport error to FATAL_ERROR" in {
      val f = new Fixtures
      f.onResponse(transportError("connection reset"))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      f.statuses must contain("FATAL_ERROR")
    }

    "must create no error details and set no error fields for a transport error (no GovTalk errors)" in {
      val f = new Fixtures
      f.onResponse(transportError("connection reset"))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      verify(f.chrisService, never()).createSubmissionErrorDetail(any[CreateSubmissionErrorDetailRequest])(any[HeaderCarrier])
      f.submissions.forall(_.govTalkErrorCode.isEmpty) mustBe true
    }

    "must still audit a transport error" in {
      val f = new Fixtures
      f.onResponse(transportError("connection reset"))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      verify(f.audit).auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier])
    }

    "must resolve a timeout transport error to STARTED" in {
      val f = new Fixtures
      f.onResponse(transportError("Read timed out"))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      f.statuses must contain("STARTED")
    }
  }

  "SubmissionService.submit on multiple GovTalk errors" - {

    "must create one error detail per error" in {
      val f = new Fixtures
      f.onResponse(errored(fatalError, fatalError2))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      verify(f.chrisService, times(2)).createSubmissionErrorDetail(any[CreateSubmissionErrorDetailRequest])(any[HeaderCarrier])
    }

    "must record the fields of the first error only" in {
      val f = new Fixtures
      f.onResponse(errored(fatalError, fatalError2))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      f.submissions.find(_.govTalkErrorCode.isDefined).value.govTalkErrorCode mustBe Some("9999")
    }
  }

  "SubmissionService.submit lock lifecycle" - {

    "must release the GovTalk lock after a successful response" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      f.lockFlags must contain("false")
    }

    "must release the GovTalk lock after an error response" in {
      val f = new Fixtures
      f.onResponse(errored(fatalError))
      f.service.submit(aReturn(), sender, periodEnd, cred).futureValue
      f.lockFlags must contain("false")
    }

    "must release the GovTalk lock even when the ChRIS submit fails" in {
      val f = new Fixtures
      when(f.connector.submit(any[Elem], any[Option[String]], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("chris down")))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue
      f.lockFlags must contain("false")
    }
  }

  "SubmissionService.submit cleanup on failure" - {

    "must fail and write no status beyond PENDING when the ChRIS submit fails" in {
      val f = new Fixtures
      when(f.connector.submit(any[Elem], any[Option[String]], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("chris down")))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue
      f.statuses.toSet mustBe Set("PENDING")
      f.submissions.forall(_.utrn.isEmpty) mustBe true
    }

    "must propagate the failure and still release the lock when auditing fails" in {
      val f = new Fixtures
      f.onResponse(completed(Some(utrn), Some(sentMark)))
      when(f.audit.auditSubmission(any[String], any[String], any[String], any[FullReturn], any[ChrisResponse])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("audit down")))
      f.service.submit(aReturn(), sender, periodEnd, cred).failed.futureValue
      f.lockFlags must contain("false")
    }
  }
}