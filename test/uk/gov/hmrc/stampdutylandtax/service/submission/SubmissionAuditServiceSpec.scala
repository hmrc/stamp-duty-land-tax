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
import models.filing.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.libs.json.{JsObject, Json}
import service.submission.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.stampdutylandtax.service.submission.SdltReturnFixtures.*

import scala.concurrent.{ExecutionContext, Future}

final class SubmissionAuditServiceSpec extends SpecBase {

  private given ExecutionContext = ExecutionContext.global

  private val storn         = "STORN12345"
  private val returnId      = "R1"
  private val correlationId = "CORR-1"
  private val utrn          = "UTRN12345"

  private val fullReturn: FullReturn = freeholdReturn(1, 1, 1)

  private val mappedDetail: JsObject = Json.obj("auditDetailKey" -> "auditDetailValue")
  
  private def completed(utrn: Option[String]): ChrisResponse =
    ChrisResponse.Completed(utrn, Some(correlationId), None,Some("url"), "2026-01-02T09:00:00Z")

  private def transportError(message: String): ChrisResponse =
    ChrisResponse.TransportError(message, "")

  private def acknowledged: ChrisResponse =
    ChrisResponse.Acknowledged(Some(correlationId), Some(10),Some("url"), "<GovTalkMessage/>")

  private def errored(errors: Seq[GovTalkError]): ChrisResponse =
    ChrisResponse.Errored(errors, Some(correlationId),Some("url"), "<GovTalkMessage/>")

  private def businessError: GovTalkError =
    GovTalkError(raisedBy = "HMRC", number = Some("1001"), errorType = "business", text = Some("Invalid STORN"), location = None)

  private def fatalError: GovTalkError =
    GovTalkError(raisedBy = "HMRC", number = Some("5000"), errorType = "fatal", text = Some("System failure"), location = None)
  
  private def fixture(auditResult: AuditResult = AuditResult.Success)
                     (setup: Future[AuditResult] => Future[AuditResult] = identity)
  : (AuditConnector, SdltAuditDetailMapper, SubmissionAuditService) =
    val connector = mock[AuditConnector]
    val mapper    = mock[SdltAuditDetailMapper]
    when(mapper.submissionDetail(any[FullReturn])).thenReturn(mappedDetail)
    when(connector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(setup(Future.successful(auditResult)))
    (connector, mapper, new SubmissionAuditService(connector, mapper))

  private def captureEvent(connector: AuditConnector): ExtendedDataEvent =
    val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
    verify(connector).sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
    captor.getValue

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  
  "SubmissionAuditService auditSubmission" - {

    "on a Completed response carrying a UTRN" - {

      "must emit an SDLTSubmissionSuccess event with the STORN, UTRN and mapped detail" in {
        val (connector, _, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(Some(utrn))).futureValue

        val event = captureEvent(connector)
        event.auditType mustBe "SDLTSubmissionSuccess"
        (event.detail \ "stampTaxesOnlineReferenceNumber").as[String]  mustBe storn
        (event.detail \ "uniqueTransactionReferenceNumber").as[String] mustBe utrn
        (event.detail \ "auditDetailKey").as[String]                   mustBe "auditDetailValue"
      }

      "must call the detail mapper with the full return" in {
        val (_, mapper, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(Some(utrn))).futureValue

        verify(mapper).submissionDetail(fullReturn)
      }
    }
    
    "on a Completed response with no UTRN" - {

      "must emit an SDLTSubmissionFailure event with failureType no_receipt" in {
        val (connector, _, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(None)).futureValue

        val event = captureEvent(connector)
        event.auditType mustBe "SDLTSubmissionFailure"
        (event.detail \ "failureType").as[String]   mustBe "no_receipt"
        (event.detail \ "correlationId").as[String] mustBe correlationId
      }

      "must not call the detail mapper" in {
        val (_, mapper, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(None)).futureValue

        verify(mapper, never()).submissionDetail(any[FullReturn])
      }

      "must not include an errors block or a failureReason when neither is present" in {
        val (connector, _, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(None)).futureValue

        val detail = captureEvent(connector).detail
        (detail \ "errors").toOption        mustBe None
        (detail \ "failureReason").toOption  mustBe None
      }
    }
    
    "on an Errored business-reject response" - {

      "must emit an SDLTSubmissionFailure with failureType departmental" in {
        val (connector, _, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, errored(Seq(businessError))).futureValue

        (captureEvent(connector).detail \ "failureType").as[String] mustBe "departmental"
      }

      "must include the serialised errors when present" in {
        val (connector, _, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, errored(Seq(businessError))).futureValue

        (captureEvent(connector).detail \ "errors").toOption must not be empty
      }
    }

    "on an Errored non-business response" - {

      "must emit an SDLTSubmissionFailure with failureType fatal" in {
        val (connector, _, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, errored(Seq(fatalError))).futureValue

        (captureEvent(connector).detail \ "failureType").as[String] mustBe "fatal"
      }
    }
    
    "on a TransportError response" - {

      "must emit an SDLTSubmissionFailure with failureType system and the message as failureReason" in {
        val (connector, _, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, transportError("connection reset")).futureValue

        val event = captureEvent(connector)
        (event.detail \ "failureType").as[String]   mustBe "system"
        (event.detail \ "failureReason").as[String] mustBe "connection reset"
      }
    }
    
    "on an Acknowledged response" - {

      "must not send any audit event" in {
        val (connector, _, service) = fixture()()

        service.auditSubmission(storn, returnId, correlationId, fullReturn, acknowledged).futureValue

        verify(connector, never()).sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext])
      }
    }
    
    "when the audit connector reports Success" - {
      "must complete successfully" in {
        val (_, _, service) = fixture(AuditResult.Success)()
        service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(Some(utrn))).futureValue
        succeed
      }
    }

    "when the audit connector reports Disabled" - {
      "must complete successfully without failing" in {
        val (_, _, service) = fixture(AuditResult.Disabled)()
        service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(Some(utrn))).futureValue
        succeed
      }
    }

    "when the audit connector reports Failure" - {
      "must fail with a SubmissionAuditException naming the failure" in {
        val (_, _, service) = fixture(AuditResult.Failure("downstream boom", None))()

        val ex = service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(Some(utrn))).failed.futureValue
        ex mustBe a[SubmissionAuditException]
        ex.getMessage must include("Audit failed")
      }

      "must carry the underlying throwable as the exception cause when the connector supplies one" in {
        val boom = new RuntimeException("downstream detail")
        val (_, _, service) = fixture(AuditResult.Failure("downstream boom", Some(boom)))()

        val ex = service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(Some(utrn))).failed.futureValue
        ex mustBe a[SubmissionAuditException]
        ex.getCause mustBe boom
      }
    }

    "when the audit connector's future fails" - {
      "must recover into a failed SubmissionAuditException" in {
        val (_, _, service) = fixture()(_ => Future.failed(new RuntimeException("kaboom")))

        val ex = service.auditSubmission(storn, returnId, correlationId, fullReturn, completed(Some(utrn))).failed.futureValue
        ex mustBe a[SubmissionAuditException]
        ex.getMessage must include("Audit threw an exception")
      }
    }
  }
  
  "SubmissionAuditException" - {

    "must default its cause to None (and expose a null Throwable cause)" in {
      val ex = SubmissionAuditException("just a message")
      ex.getMessage mustBe "just a message"
      ex.cause      mustBe None
      ex.getCause   mustBe null
    }

    "must expose a provided cause" in {
      val boom = new RuntimeException("boom")
      val ex   = SubmissionAuditException("wrapped", Some(boom))
      ex.cause    mustBe Some(boom)
      ex.getCause mustBe boom
    }
  }
}