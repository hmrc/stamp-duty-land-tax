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

package service.submission

import base.SpecBase
import connectors.EmailServiceConnector
import models.email.EmailServiceRequest
import models.filing.{FullReturn, Purchaser, Submission}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EmailServiceSpec extends SpecBase with MockitoSugar {

  private val mockEmailServiceConnector = mock[EmailServiceConnector]
  private val service = new EmailService(mockEmailServiceConnector)

  "EmailService" - {

    "submitEmailConfirmation" - {

      "must successfully send email when email parameter is provided" in {
        val hc: HeaderCarrier = HeaderCarrier()

        val fullReturn = FullReturn(
          purchaser = Some(Seq(Purchaser(
            forename1 = Some("John"),
            forename2 = None,
            surname = Some("Smith"),
            companyName = None
          ))),
          submission = Some(Submission(
            email = Some("submission@example.com")
          ))
        )

        when(mockEmailServiceConnector.submitEmailConfirmation(any())(any())).thenReturn(Future.successful(()))

        val result = service.submitEmailConfirmation(fullReturn, "UTRN001", Some("test@example.com"))(hc).futureValue

        result mustEqual ()
      }

      "must successfully send email when email is from fullReturn.submission" in {
        val hc: HeaderCarrier = HeaderCarrier()

        val fullReturn = FullReturn(
          purchaser = Some(Seq(Purchaser(
            forename1 = Some("Jane"),
            forename2 = None,
            surname = Some("Doe"),
            companyName = None
          ))),
          submission = Some(Submission(
            email = Some("submission@example.com")
          ))
        )

        when(mockEmailServiceConnector.submitEmailConfirmation(any())(any())).thenReturn(Future.successful(()))

        val result = service.submitEmailConfirmation(fullReturn, "UTRN002", None)(hc).futureValue

        result mustEqual ()
      }

      "must complete when no email is provided" in {
        val hc: HeaderCarrier = HeaderCarrier()

        val fullReturn = FullReturn(
          purchaser = Some(Seq(Purchaser(
            forename1 = Some("John"),
            forename2 = None,
            surname = Some("Smith"),
            companyName = None
          ))),
          submission = Some(Submission(
            email = None
          ))
        )

        val result = service.submitEmailConfirmation(fullReturn, "UTRN003", None)(hc).futureValue

        result mustEqual ()
      }

      "must use company name when available" in {
        val hc: HeaderCarrier = HeaderCarrier()

        val fullReturn = FullReturn(
          purchaser = Some(Seq(Purchaser(
            forename1 = None,
            forename2 = None,
            surname = Some("Smith"),
            companyName = Some("Acme Ltd")
          ))),
          submission = Some(Submission(
            email = Some("company@example.com")
          ))
        )

        when(mockEmailServiceConnector.submitEmailConfirmation(any())(any())).thenReturn(Future.successful(()))

        val result = service.submitEmailConfirmation(fullReturn, "UTRN004", None)(hc).futureValue

        result mustEqual ()
      }

      "must use empty string when no purchaser name available" in {
        val hc: HeaderCarrier = HeaderCarrier()

        val fullReturn = FullReturn(
          purchaser = Some(Seq(Purchaser(
            forename1 = None,
            forename2 = None,
            surname = None,
            companyName = None
          ))),
          submission = Some(Submission(
            email = Some("test@example.com")
          ))
        )

        when(mockEmailServiceConnector.submitEmailConfirmation(any())(any())).thenReturn(Future.successful(()))

        val result = service.submitEmailConfirmation(fullReturn, "UTRN005", None)(hc).futureValue

        result mustEqual ()
      }

      "must use empty string when purchaser list is empty" in {
        val hc: HeaderCarrier = HeaderCarrier()

        val fullReturn = FullReturn(
          purchaser = Some(Seq()),
          submission = Some(Submission(
            email = Some("test@example.com")
          ))
        )

        when(mockEmailServiceConnector.submitEmailConfirmation(any())(any())).thenReturn(Future.successful(()))

        val result = service.submitEmailConfirmation(fullReturn, "UTRN006", None)(hc).futureValue

        result mustEqual ()
      }

      "must use empty string when purchaser is None" in {
        val hc: HeaderCarrier = HeaderCarrier()

        val fullReturn = FullReturn(
          purchaser = None,
          submission = Some(Submission(
            email = Some("test@example.com")
          ))
        )

        when(mockEmailServiceConnector.submitEmailConfirmation(any())(any())).thenReturn(Future.successful(()))

        val result = service.submitEmailConfirmation(fullReturn, "UTRN007", None)(hc).futureValue

        result mustEqual ()
      }

      "must prefer parameter email over submission email" in {
        val hc: HeaderCarrier = HeaderCarrier()

        val fullReturn = FullReturn(
          purchaser = Some(Seq(Purchaser(
            forename1 = Some("John"),
            forename2 = None,
            surname = Some("Smith"),
            companyName = None
          ))),
          submission = Some(Submission(
            email = Some("submission@example.com")
          ))
        )

        when(mockEmailServiceConnector.submitEmailConfirmation(any())(any())).thenReturn(Future.successful(()))

        val result = service.submitEmailConfirmation(fullReturn, "UTRN008", Some("param@example.com"))(hc).futureValue

        result mustEqual ()
      }
    }
  }
}