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

import models.agent.{AgentDetailsRequest, AgentDetailsResponse, SdltOrganisationResponse, SubmitAgentDetailsResponse}
import models.filing.{CreateReturnRequest, CreateReturnResult, GetReturnByRefRequest, GetReturnRequest}
import models.manage.SdltReturnRecordResponse
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FormpProxyConnector @Inject()(http: HttpClientV2,
                                    config: ServicesConfig)
                                   (implicit ec: ExecutionContext) extends Logging {

  private val stubPath = config.baseUrl("stamp-duty-land-tax-stub") + "/stamp-duty-land-tax-stub"
  private val formpPath = config.baseUrl("formp-proxy") + "/formp-proxy"
  val stubFormPBool: Boolean = config.getBoolean("features.stub-formp-enabled")

  def getAgentDetails(storn: String, agentReferenceNumber: String)
                     (implicit hc: HeaderCarrier): Future[Option[AgentDetailsResponse]] =
    val url: URL = if(stubFormPBool) url"$stubPath/manage-agents/agent-details" else url"$formpPath/manage-agents/agent-details"
    http.post(url)
      .withBody(Json.obj(
        "storn" -> storn,
        "agentReferenceNumber" -> agentReferenceNumber
      ))
      .execute[Option[AgentDetailsResponse]]
      .recover {
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][getAgentDetails]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def submitAgentDetails(agentDetails: AgentDetailsRequest)(implicit hc: HeaderCarrier): Future[SubmitAgentDetailsResponse] =
    val url: URL = if(stubFormPBool) url"$stubPath/manage-agents/agent-details/submit" else url"$formpPath/manage-agents/agent-details/submit"
    http.post(url)
      .withBody(Json.toJson(agentDetails))
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

  def getAllAgentsLegacy(storn: String)(implicit hc: HeaderCarrier): Future[List[AgentDetailsResponse]] =
    val url: URL = if(stubFormPBool) url"$stubPath/manage-agents/agent-details/get-all-agents-legacy" else url"$formpPath/manage-agents/agent-details/get-all-agents-legacy"
    http.post(url)
      .withBody(Json.obj("storn" -> storn))
      .execute[List[AgentDetailsResponse]]
      .recover {
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][getAllAgentsLegacy]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def removeAgent(storn: String, agentReferenceNumber: String)
                 (implicit hc: HeaderCarrier): Future[Unit] =
    val url: URL = if(stubFormPBool) url"$stubPath/manage-agents/agent-details/remove" else url"$formpPath/manage-agents/agent-details/remove"
    http.post(url)
      .withBody(Json.obj(
        "storn" -> storn,
        "agentReferenceNumber" -> agentReferenceNumber
      ))
      .execute[HttpResponse]
      .flatMap { response =>
        if(response.status / 100 == 2) Future.unit
        else Future.failed(UpstreamErrorResponse(response.body, response.status))
      }
      .recover {
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][removeAgent]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getReturns(storn: String)
                (implicit hc: HeaderCarrier): Future[Option[SdltReturnRecordResponse]] =
    val url: URL = if(stubFormPBool) url"$stubPath/manage-returns/get-all" else url"$formpPath/manage-returns/get-all"
    http.post(url)
      .withBody(Json.obj(
        "storn" -> storn
      ))
      .execute[Option[SdltReturnRecordResponse]]
      .recover {
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][getReturns]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
}
