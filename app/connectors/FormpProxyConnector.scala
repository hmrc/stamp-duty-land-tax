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

package connectors

import models.agent.{AgentDetailsBeforeCreation, CreatedAgent, SdltOrganisationResponse, SubmitAgentDetailsResponse}
import models.manage.{SdltReturnRecordRequest, SdltReturnRecordResponse}
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class FormpProxyConnector @Inject()(http: HttpClientV2,
                                    config: ServicesConfig)
                                   (implicit ec: ExecutionContext) extends Logging {

  private val stubPath = config.baseUrl("stamp-duty-land-tax-stub") + "/stamp-duty-land-tax-stub"
  private val formpPath = config.baseUrl("formp-proxy") + "/formp-proxy"
  val stubFormPBool: Boolean = config.getBoolean("features.stub-formp-enabled")

  def submitAgentDetails(agentDetailsBeforeCreation: AgentDetailsBeforeCreation)(implicit hc: HeaderCarrier): Future[SubmitAgentDetailsResponse] =
    val url: URL = if(stubFormPBool) url"$stubPath/manage-agents/agent-details/submit" else url"$formpPath/manage-agents/agent-details/submit"
    http.post(url)
      .withBody(Json.toJson(agentDetailsBeforeCreation))
      .execute[SubmitAgentDetailsResponse]
      .recover {
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][submitAgentDetails]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getSdltOrganisation(storn: String)(implicit hc: HeaderCarrier): Future[SdltOrganisationResponse] =
    val url: URL = if(stubFormPBool) url"$stubPath/organisation" else url"$formpPath/organisation"
    http.post(url)
      .withBody(Json.obj("storn" -> storn))
      .execute[SdltOrganisationResponse]
      .recover {
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][getSdltOrganisation]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def removeAgent(storn: String, agentReferenceNumber: String)
                 (implicit hc: HeaderCarrier): Future[Boolean] =
    val url: URL = if(stubFormPBool) url"$stubPath/manage-agents/agent-details/remove" else url"$formpPath/manage-agents/agent-details/remove"
    http.post(url)
      .withBody(Json.obj(
        "storn" -> storn,
        "agentReferenceNumber" -> agentReferenceNumber
      ))
      .execute[Boolean]
      .recover {
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][removeAgent]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getReturns(request: SdltReturnRecordRequest)
                (implicit hc: HeaderCarrier): Future[SdltReturnRecordResponse] =
    val url: URL = if(stubFormPBool) url"$stubPath/returns" else url"$formpPath/returns"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[Either[UpstreamErrorResponse, SdltReturnRecordResponse]]
      .flatMap {
        case Right(resp) => Future.successful(resp)
        case Left(error) => Future.failed(error)
      }
      .recoverWith {
        case NonFatal(e) =>
          logger.error(s"[FormpProxyConnector][getReturns] failed for storn ${request.storn}: ${e.getMessage}", e)
          Future.failed(e)
      }
}
