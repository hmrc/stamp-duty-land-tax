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

package uk.gov.hmrc.stampdutylandtax.controllers.actions

import com.google.inject.Inject
import models.auth.IdentifierRequest
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import scala.concurrent.{ExecutionContext, Future}


class AuthenticatedIdentifierAction @Inject()(
                                               override val authConnector: AuthConnector,
                                               val parser: BodyParsers.Default
                                             )
                                             (implicit val executionContext: ExecutionContext) extends IdentifierAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A],
                              block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val defaultPredicate: Predicate = AuthProviders(GovernmentGateway)

    // We expect one to one mapping between AffinityGroup and corresponding Enrollment
    authorised(defaultPredicate)
      .retrieve(
        Retrievals.internalId and
          Retrievals.allEnrolments and
          Retrievals.affinityGroup and
          Retrievals.credentialRole
      ) {
        // TODO: apply what is required from FE auth
        case _ =>
          logger.info("[AuthenticatedIdentifierAction][authorised] - user authenticated")
          block(IdentifierRequest(request, "internalId", "storn"))
      }
    // TODO: resurrect recover if needed
  }

}
