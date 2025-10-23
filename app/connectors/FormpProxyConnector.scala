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

import models.AgentDetails
import models.response.SubmitAgentDetailsResponse
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.libs.ws.JsonBodyWritables.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FormpProxyConnector @Inject()(http: HttpClientV2,
                                    config: ServicesConfig)
                                   (implicit ec: ExecutionContext) extends Logging {

  private val base = config.baseUrl("formp-proxy")

  private val servicePath = config.getString("microservice.services.formp-proxy.url")

  def getAgentDetails(storn: String)(implicit hc: HeaderCarrier): Future[Option[AgentDetails]] =
    http.post(url"$base/$servicePath/manage-agents/agent-details")
      .withBody(Json.obj("storn" -> storn))
      .execute[Option[AgentDetails]]
      .recover {
        case e: Throwable =>
          logger.error(s"[getAgentDetails]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def submitAgentDetails(agentDetails: AgentDetails)(implicit hc: HeaderCarrier): Future[SubmitAgentDetailsResponse] =
    http.post(url"$base/$servicePath/manage-agents/agent-details/submit")
      .withBody(Json.toJson(agentDetails))
      .execute[SubmitAgentDetailsResponse]
      .recover {
        case e: Throwable =>
          logger.error(s"[submitAgentDetails]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getAllAgents(storn: String)(implicit hc: HeaderCarrier): Future[List[AgentDetails]] =
    http.post(url"$base/$servicePath/manage-agents/agent-details/get-all-agents")
      .withBody(Json.obj("storn" -> storn))
      .execute[List[AgentDetails]]
      .recover {
        case e: Throwable =>
          logger.error(s"[getAllAgents]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def removeAgent(storn: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    http.post(url"$base/$servicePath/manage-agents/agent-details/remove")
      .withBody(Json.obj("storn" -> storn))
      .execute[Boolean]
      .recover {
        case e: Throwable =>
          logger.error(s"[removeAgent]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
}
