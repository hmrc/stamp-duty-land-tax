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

package uk.gov.hmrc.stampdutylandtax.controllers.actions

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.stampdutylandtax.controllers.actions.controllers.actions.TestAuthRetrievals.Ops

import java.util.UUID
import scala.concurrent.Future

class AuthActionSpec extends SpecBase {

  type RetrievalsType = Option[String] ~ Enrolments ~ Option[AffinityGroup] ~ Option[CredentialRole]

  trait Fixture {
    val id: String = UUID.randomUUID().toString
    val testStorn: String = "STN001"

    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val application: Application = applicationBuilder()
      .overrides(bind[AuthConnector].toInstance(mockAuthConnector))
      .build()

    val bodyParsers: BodyParsers.Default = application.injector.instanceOf[BodyParsers.Default]

    val emptyEnrolments = Enrolments(Set.empty)

    val orgActiveEnrollment: Enrolment = Enrolment(
      "IR-SDLT-ORG",
      Seq(
        EnrolmentIdentifier("STORN", testStorn)
      ),
      "activated",
      None
    )
  }

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

    "Authentication Action " - {

      "Incoming request contains:: enrollment | affinityGroup | credentialRole " - {
        "must process request and return 200:OK " in new Fixture  {

          val enrollments: Enrolments = Enrolments(Set(orgActiveEnrollment))
          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn( Future.successful( Some(id) ~ enrollments ~ Some(Organisation) ~ Some(User) ) )

          running(application) {
            val authAction = new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
            val controller = new Harness(authAction)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustBe OK
          }

        }
      }

      "Enrollment is missing:: ?? and any other" - new Fixture  {
        "must return 403:Forbidden" in {
          running(application) {
            val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), bodyParsers)
            val controller = new Harness(authAction)
            val result = controller.onPageLoad()(FakeRequest())
            status(result) mustBe FORBIDDEN
          }
        }
      }

  }

}