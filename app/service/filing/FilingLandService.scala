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

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class FilingLandService @Inject()(formp: FilingFormpProxyConnector) {

  def createLand(createLandRequest: CreateLandRequest)
                (implicit hc: HeaderCarrier): Future[CreateLandReturn] =
    formp.createLand(createLandRequest)

  def updateLand(updateLandRequest: UpdateLandRequest)
                (implicit hc: HeaderCarrier): Future[UpdateLandReturn] =
    formp.updateLand(updateLandRequest)

  def deleteLand(deleteLandRequest: DeleteLandRequest)
                (implicit hc: HeaderCarrier): Future[DeleteLandReturn] =
    formp.deleteLand(deleteLandRequest)
}