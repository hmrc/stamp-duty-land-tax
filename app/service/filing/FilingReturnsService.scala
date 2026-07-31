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

package service.filing

import connectors.FilingFormpProxyConnector
import models.filing._
import uk.gov.hmrc.http.HeaderCarrier
import models.purge._
import connectors.FormpProxyConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class FilingReturnsService @Inject()(formp: FilingFormpProxyConnector,
                                     formpProx: FormpProxyConnector) {

  def createReturn(createReturnRequest: CreateReturnRequest)
                  (implicit hc: HeaderCarrier): Future[CreateReturnResult] =
    formp.createReturn(createReturnRequest)

  def deleteReturn(deleteReturnRequest: DeleteReturnRequest)
                  (implicit hc: HeaderCarrier): Future[DeleteReturnResponse] =
    formpProx.deleteReturn(deleteReturnRequest)

  def getFullReturn(getReturnByRefRequest: GetReturnByRefRequest)
                   (implicit hc: HeaderCarrier): Future[FullReturn] =
    formp.getFullReturn(getReturnByRefRequest)

  def updateReturnInfo(updateReturnRequest: UpdateReturnRequest)
                      (implicit hc: HeaderCarrier): Future[UpdateReturnReturn] =
    formp.updateReturnInfo(updateReturnRequest)
}