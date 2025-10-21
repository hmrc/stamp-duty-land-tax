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

package service

import connectors.FormpProxyConnector
import models.AgentDetails
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ManageAgentsService @Inject()(formp: FormpProxyConnector) {

  def getAgentDetails(storn: String)
                     (implicit hc: HeaderCarrier): Future[Option[AgentDetails]] =
    formp.getAgentDetails(storn)

  def getAllAgents(storn: String)
                     (implicit hc: HeaderCarrier): Future[List[AgentDetails]] =
    formp.getAllAgents(storn)
}
