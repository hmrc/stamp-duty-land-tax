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

package connectors

import models.email.EmailServiceRequest
import models.email.EmailServiceRequest.format
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailServiceConnector @Inject()(http: HttpClientV2,
                                      config: ServicesConfig)
                                     (implicit ec: ExecutionContext) extends Logging {

  private val baseUrl = config.baseUrl("email")

  private val domain = "hmrc"

  def submitEmailConfirmation(emailServiceRequest: EmailServiceRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    val url: URL = url"$baseUrl/$domain/email"
    http.post(url)
      .withBody(Json.toJson(emailServiceRequest))
      .execute[HttpResponse]
      .map {
        case response if response.status == ACCEPTED => ()
        case response =>
          logger.error(s"[EmailServiceConnector][submitEmailConfirmation]: unexpected status ${response.status}")
      }
      .recover {
        case e: Throwable =>
          logger.error(s"[EmailServiceConnector][submitEmailConfirmation]: ${e.getMessage}")
      }
}