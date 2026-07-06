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
import models.submission.*
import models.filing.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ChrisService @Inject()(formp: FilingFormpProxyConnector) {
  
  def lockReturn(lockReturnRequest: LockReturnRequest)
                (implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, LockReturnResponse]] =
    formp.lockReturn(lockReturnRequest)
  
  def createSubmission(createSubmissionRequest: CreateSubmissionRequest)
                      (implicit hc: HeaderCarrier): Future[CreateSubmissionReturn] =
    formp.createSubmission(createSubmissionRequest)

  def updateSubmission(updateSubmissionRequest: UpdateSubmissionRequest)
                      (implicit hc: HeaderCarrier): Future[UpdateSubmissionReturn] =
    formp.updateSubmission(updateSubmissionRequest)

  def createSubmissionErrorDetail(createSubmissionErrorDetailRequest: CreateSubmissionErrorDetailRequest)
                                 (implicit hc: HeaderCarrier): Future[CreateSubmissionErrorDetailReturn] =
    formp.createSubmissionErrorDetail(createSubmissionErrorDetailRequest)

  def deleteSubmissionErrorDetail(deleteSubmissionErrorDetailRequest: DeleteSubmissionErrorDetailRequest)
                                 (implicit hc: HeaderCarrier): Future[DeleteSubmissionErrorDetailReturn] =
    formp.deleteSubmissionErrorDetail(deleteSubmissionErrorDetailRequest)
  
  def insertInitialGovTalkStatus(insertInitialGovTalkStatusRequest: InsertInitialGovTalkStatusRequest)
                                (implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
    formp.insertInitialGovTalkStatus(insertInitialGovTalkStatusRequest)

  def resetGovTalkStatus(resetGovTalkStatusRequest: ResetGovTalkStatusRequest)
                        (implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
    formp.resetGovTalkStatus(resetGovTalkStatusRequest)

  def updateGovTalkStatus(updateGovTalkStatusRequest: UpdateGovTalkStatusRequest)
                         (implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
    formp.updateGovTalkStatus(updateGovTalkStatusRequest)

  def updateGovTalkStatusCorrelationId(updateGovTalkStatusCorrelationIdRequest: UpdateGovTalkStatusCorrelationIdRequest)
                                      (implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
    formp.updateGovTalkStatusCorrelationId(updateGovTalkStatusCorrelationIdRequest)

  def updateGovTalkStatusLock(updateGovTalkStatusLockRequest: UpdateGovTalkStatusLockRequest)
                             (implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
    formp.updateGovTalkStatusLock(updateGovTalkStatusLockRequest)

  def updateGovTalkStatistics(updateGovTalkStatisticsRequest: UpdateGovTalkStatisticsRequest)
                             (implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
    formp.updateGovTalkStatistics(updateGovTalkStatisticsRequest)

  def deleteGovTalkStatus(deleteGovTalkStatusRequest: DeleteGovTalkStatusRequest)
                         (implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
    formp.deleteGovTalkStatus(deleteGovTalkStatusRequest)

  def selectGovTalkStatus(selectGovTalkStatusRequest: SelectGovTalkStatusRequest)
                         (implicit hc: HeaderCarrier): Future[SelectGovTalkStatusResponse] =
    formp.selectGovTalkStatus(selectGovTalkStatusRequest)

  def selectGovTalkFormResultId(selectGovTalkFormResultIdRequest: SelectGovTalkFormResultIdRequest)
                               (implicit hc: HeaderCarrier): Future[SelectGovTalkFormResultIdResponse] =
    formp.selectGovTalkFormResultId(selectGovTalkFormResultIdRequest)
}