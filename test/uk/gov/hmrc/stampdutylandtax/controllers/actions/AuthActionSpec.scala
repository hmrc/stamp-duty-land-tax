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
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.stampdutylandtax.controllers.actions.controllers.actions.TestAuthRetrievals.Ops

import java.util.UUID
import scala.concurrent.Future

class AuthActionSpec extends SpecBase {

  type RetrievalsType = Option[String] ~ Enrolments ~ Option[AffinityGroup] ~
    Option[CredentialRole]

  trait Fixture {
    val id: String = UUID.randomUUID().toString
    val testStorn: String = "STN001"

    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val application: Application = applicationBuilder()
      .overrides(bind[AuthConnector].toInstance(mockAuthConnector))
      .build()

    val bodyParsers: BodyParsers.Default =
      application.injector.instanceOf[BodyParsers.Default]

    val emptyEnrolments = Enrolments(Set.empty)

    val orgActiveEnrollment: Enrolment = Enrolment(
      "IR-SDLT-ORG",
      Seq(
        EnrolmentIdentifier("STORN", testStorn)
      ),
      "activated",
      None
    )

    val agentActiveEnrollment: Enrolment = Enrolment(
      "IR-SDLT-AGENT",
      Seq(
        EnrolmentIdentifier("STORN", testStorn)
      ),
      "activated",
      None
    )

    val agentNotActiveEnrollment: Enrolment = Enrolment(
      "IR-SDLT-AGENT",
      Seq(
        EnrolmentIdentifier("STORN", testStorn)
      ),
      "inactivated",
      None
    )

    val agentNotYetActiveEnrollment: Enrolment = Enrolment(
      "IR-SDLT-AGENT",
      Seq(
        EnrolmentIdentifier("STORN", testStorn)
      ),
      "notyetactivated",
      None
    )

    val orgActiveNotActiveEnrollment: Enrolment = Enrolment(
      "IR-SDLT-ORG",
      Seq(
        EnrolmentIdentifier("STORN", testStorn)
      ),
      "inactivated",
      None
    )

    val orgActiveNotYetActiveEnrollment: Enrolment = Enrolment(
      "IR-SDLT-ORG",
      Seq(
        EnrolmentIdentifier("STORN", testStorn)
      ),
      "notyetactivated",
      None
    )

    val orgEnrollments: Enrolments = Enrolments(Set(orgActiveEnrollment))
    val orgEnrollmentsNotActive: Enrolments = Enrolments(
      Set(orgActiveNotActiveEnrollment)
    )
    val orgEnrollmentsNotYetActive: Enrolments = Enrolments(
      Set(orgActiveNotYetActiveEnrollment)
    )

    val agentEnrollments: Enrolments = Enrolments(Set(agentActiveEnrollment))
    val agentEnrollmentsNotActive: Enrolments = Enrolments(
      Set(agentNotActiveEnrollment)
    )
    val agentEnrollmentsNotYetActive: Enrolments = Enrolments(
      Set(agentNotYetActiveEnrollment)
    )
  }

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  "Authentication Action " - {

    "Request contains expected:: enrollments | affinityGroup | credentialRole " - {
      "must process request with OrgEnrollments :: return 200:OK " in new Fixture {
        when(
          mockAuthConnector
            .authorise[RetrievalsType](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              Some(id) ~ orgEnrollments ~ Some(Organisation) ~ Some(User)
            )
          )

        running(application) {
          val authAction =
            new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
      "must process request with AgentEnrollments :: return 200:OK " in new Fixture {
        when(
          mockAuthConnector
            .authorise[RetrievalsType](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              Some(id) ~ agentEnrollments ~ Some(Agent) ~ Some(User)
            )
          )

        running(application) {
          val authAction =
            new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }

      "must NOT process request with inactive AgentEnrollments :: return 403:Forbidden " in new Fixture {
        when(
          mockAuthConnector
            .authorise[RetrievalsType](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              Some(id) ~ agentEnrollmentsNotActive ~ Some(Agent) ~ Some(User)
            )
          )

        running(application) {
          val authAction =
            new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe FORBIDDEN
        }
      }

      "must process request with NOT YET ACTIVE AgentEnrollments :: return 200:OK " in new Fixture {
        when(
          mockAuthConnector
            .authorise[RetrievalsType](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              Some(id) ~ agentEnrollmentsNotYetActive ~ Some(Agent) ~ Some(User)
            )
          )

        running(application) {
          val authAction =
            new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }

      "must NOT process request with inactive OrgEnrollments :: return 403:Forbidden " in new Fixture {
        when(
          mockAuthConnector
            .authorise[RetrievalsType](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              Some(id) ~ orgEnrollmentsNotActive ~ Some(Organisation) ~ Some(
                User
              )
            )
          )

        running(application) {
          val authAction =
            new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe FORBIDDEN
        }
      }

      "must NOT process request with OrgEnrollments and role Agent:: return 403:Forbidden " in new Fixture {
        when(
          mockAuthConnector
            .authorise[RetrievalsType](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              Some(id) ~ orgEnrollments ~ Some(Agent) ~ Some(User)
            )
          )

        running(application) {
          val authAction =
            new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe FORBIDDEN
        }
      }

      "must process request with NOT YET ACTIVE OrgEnrollments:: return 200:OK " in new Fixture {
        when(
          mockAuthConnector
            .authorise[RetrievalsType](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              Some(id) ~ orgEnrollmentsNotYetActive ~ Some(Organisation) ~ Some(
                User
              )
            )
          )

        running(application) {
          val authAction =
            new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }

      "must NOT process request with AgentEnrollments and role Agent:: return 403:Forbidden " in new Fixture {
        when(
          mockAuthConnector
            .authorise[RetrievalsType](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              Some(id) ~ agentEnrollments ~ Some(Organisation) ~ Some(User)
            )
          )

        running(application) {
          val authAction =
            new AuthenticatedIdentifierAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe FORBIDDEN
        }
      }

    }

    "General case:: missing bearer token" - new Fixture {
      "must return 403:Forbidden" in {
        running(application) {
          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new MissingBearerToken),
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())
          status(result) mustBe FORBIDDEN
        }
      }
    }

  }

}
