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

import connectors.EmailServiceConnector
import models.email.EmailServiceRequest
import models.filing.{FullReturn, NameOfPurchaser, Purchaser}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject()(emailServiceConnector: EmailServiceConnector)(implicit ec: ExecutionContext) {

  def submitEmailConfirmation(fullReturn: FullReturn,
                              utrn: String,
                              email: Option[String])(implicit hc: HeaderCarrier): Future[Unit] = {
    val email2 = fullReturn.submission.flatMap(_.email)
    val mainPurchaserId = fullReturn.returnInfo.flatMap(_.mainPurchaserID)
    val purchaserName: String = mainPurchaserId
      .flatMap(id => fullReturn.purchaser.flatMap(_.find(_.purchaserID.contains(id))))
      .map(createPurchaserName(_).fullName)
      .getOrElse("")

    (email, email2) match {
      case (Some(email1), _) =>
        val emailRequest = EmailServiceRequest(
          to = email1,
          templateId = "sdlt_submission_confirmation",
          templateParameters = Map(
            "purchaserName" -> purchaserName,
            "utrn" -> utrn
          )
        )
        emailServiceConnector.submitEmailConfirmation(emailRequest)

      case (None, Some(fullReturnEmail)) =>
        val emailRequest = EmailServiceRequest(
          to = fullReturnEmail,
          templateId = "sdlt_submission_confirmation",
          templateParameters = Map(
            "purchaserName" -> purchaserName,
            "utrn" -> utrn
          )
        )
        emailServiceConnector.submitEmailConfirmation(emailRequest)

      case _ => Future.unit
    }
  }

  private def createPurchaserName(purchaser: Purchaser): NameOfPurchaser = {
    (purchaser.companyName, purchaser.surname) match {
      case (Some(companyName), _) =>
        NameOfPurchaser(
          forename1 = None,
          forename2 = None,
          name = companyName
        )

      case (_, Some(surname)) =>
        NameOfPurchaser(
          forename1 = purchaser.forename1,
          forename2 = purchaser.forename2,
          name = surname
        )

      case _ =>
        NameOfPurchaser(
          forename1 = purchaser.forename1,
          forename2 = purchaser.forename2,
          name = ""
        )
    }
  }
}