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

import models.filing.*
import models.submission.*
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FilingFormpProxyConnector @Inject()(http: HttpClientV2,
                                          config: ServicesConfig)
                                         (implicit ec: ExecutionContext) extends Logging {

  private val formpPath = config.baseUrl("formp-proxy") + "/formp-proxy"

  def createReturn(createReturnRequest: CreateReturnRequest)(implicit hc: HeaderCarrier): Future[CreateReturnResult] =
    http.post(url"$formpPath/create/return")
      .withBody(Json.toJson(createReturnRequest))
      .execute[CreateReturnResult]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createReturn]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createReturn]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }


  def getFullReturn(getReturnByRefRequest: GetReturnByRefRequest)(implicit hc: HeaderCarrier): Future[FullReturn] =
    http.post(url"$formpPath/retrieve-return")
      .withBody(Json.toJson(getReturnByRefRequest))
      .execute[FullReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][getFullReturn]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def updateReturnInfo(updateReturnRequest: UpdateReturnRequest)(implicit hc: HeaderCarrier): Future[UpdateReturnReturn] =
    http.post(url"$formpPath/filing/update/return-info")
      .withBody(Json.toJson(updateReturnRequest))
      .execute[UpdateReturnReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateReturnInfo]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateReturnInfo]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
      
  def updateTaxCalculationInfo(updateTaxCalculationRequest: UpdateTaxCalculationRequest)(implicit hc: HeaderCarrier): Future[UpdateTaxCalculationReturn] =
      http.post(url"$formpPath/filing/update/tax-calculation")
        .withBody(Json.toJson(updateTaxCalculationRequest))
        .execute[UpdateTaxCalculationReturn]
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][updateTaxCalcInfo]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][updateTaxCalcInfo]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }

  def createVendor(createVendorRequest: CreateVendorRequest)(implicit hc: HeaderCarrier): Future[CreateVendorReturn] =
    http.post(url"$formpPath/filing/create/vendor")
      .withBody(Json.toJson(createVendorRequest))
      .execute[CreateVendorReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createVendor]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createVendor]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def updateVendor(updateVendorRequest: UpdateVendorRequest)(implicit hc: HeaderCarrier): Future[UpdateVendorReturn] =
    http.post(url"$formpPath/filing/update/vendor")
      .withBody(Json.toJson(updateVendorRequest))
      .execute[UpdateVendorReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateVendor]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateVendor]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def deleteVendor(deleteVendorRequest: DeleteVendorRequest)(implicit hc: HeaderCarrier): Future[DeleteVendorReturn] =
    http.post(url"$formpPath/filing/delete/vendor")
      .withBody(Json.toJson(deleteVendorRequest))
      .execute[DeleteVendorReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][deleteVendor]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][deleteVendor]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def createReturnAgent(createReturnAgentRequest: CreateReturnAgentRequest)(implicit hc: HeaderCarrier): Future[CreateReturnAgentReturn] =
    http.post(url"$formpPath/filing/create/return-agent")
      .withBody(Json.toJson(createReturnAgentRequest))
      .execute[CreateReturnAgentReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createReturnAgent]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createReturnAgent]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def updateReturnAgent(updateReturnAgentRequest: UpdateReturnAgentRequest)(implicit hc: HeaderCarrier): Future[UpdateReturnAgentReturn] =
    http.post(url"$formpPath/filing/update/return-agent")
      .withBody(Json.toJson(updateReturnAgentRequest))
      .execute[UpdateReturnAgentReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateReturnAgent]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateReturnAgent]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def deleteReturnAgent(deleteReturnAgentRequest: DeleteReturnAgentRequest)(implicit hc: HeaderCarrier): Future[DeleteReturnAgentReturn] =
    http.post(url"$formpPath/filing/delete/return-agent")
      .withBody(Json.toJson(deleteReturnAgentRequest))
      .execute[DeleteReturnAgentReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][deleteReturnAgent]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][deleteReturnAgent]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }


  def updateReturnVersioning(returnVersionUpdateRequest: ReturnVersionUpdateRequest)(implicit hc: HeaderCarrier): Future[ReturnVersionUpdateReturn] =
    http.post(url"$formpPath/filing/update/return-version")
      .withBody(Json.toJson(returnVersionUpdateRequest))
      .execute[ReturnVersionUpdateReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateReturnVersioning]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateReturnVersioning]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }


  def createPurchaser(createPurchaserRequest: CreatePurchaserRequest)(implicit hc: HeaderCarrier): Future[CreatePurchaserReturn] =
    http.post(url"$formpPath/filing/create/purchaser")
      .withBody(Json.toJson(createPurchaserRequest))
      .execute[CreatePurchaserReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createPurchaser]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createPurchaser]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def updatePurchaser(updatePurchaserRequest: UpdatePurchaserRequest)(implicit hc: HeaderCarrier): Future[UpdatePurchaserReturn] =
    http.post(url"$formpPath/filing/update/purchaser")
      .withBody(Json.toJson(updatePurchaserRequest))
      .execute[UpdatePurchaserReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updatePurchaser]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updatePurchaser]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def deletePurchaser(deletePurchaserRequest: DeletePurchaserRequest)(implicit hc: HeaderCarrier): Future[DeletePurchaserReturn] =
    http.post(url"$formpPath/filing/delete/purchaser")
      .withBody(Json.toJson(deletePurchaserRequest))
      .execute[DeletePurchaserReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][deletePurchaser]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][deletePurchaser]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }


  def createCompanyDetails(createCompanyDetailsRequest: CreateCompanyDetailsRequest)(implicit hc: HeaderCarrier): Future[CreateCompanyDetailsReturn] =
    http.post(url"$formpPath/filing/create/company-details")
      .withBody(Json.toJson(createCompanyDetailsRequest))
      .execute[CreateCompanyDetailsReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createCompanyDetails]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createCompanyDetails]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def updateCompanyDetails(updateCompanyDetailsRequest: UpdateCompanyDetailsRequest)(implicit hc: HeaderCarrier): Future[UpdateCompanyDetailsReturn] =
    http.post(url"$formpPath/filing/update/company-details")
      .withBody(Json.toJson(updateCompanyDetailsRequest))
      .execute[UpdateCompanyDetailsReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateCompanyDetails]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateCompanyDetails]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def deleteCompanyDetails(deleteCompanyDetailsRequest: DeleteCompanyDetailsRequest)(implicit hc: HeaderCarrier): Future[DeleteCompanyDetailsReturn] =
    http.post(url"$formpPath/filing/delete/company-details")
      .withBody(Json.toJson(deleteCompanyDetailsRequest))
      .execute[DeleteCompanyDetailsReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][deleteCompanyDetails]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][deleteCompanyDetails]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }


  def createLand(createLandRequest: CreateLandRequest)(implicit hc: HeaderCarrier): Future[CreateLandReturn] =
    http.post(url"$formpPath/filing/create/land")
      .withBody(Json.toJson(createLandRequest))
      .execute[CreateLandReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createLand]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createLand]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def updateLand(updateLandRequest: UpdateLandRequest)(implicit hc: HeaderCarrier): Future[UpdateLandReturn] =
    http.post(url"$formpPath/filing/update/land")
      .withBody(Json.toJson(updateLandRequest))
      .execute[UpdateLandReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateLand]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateLand]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def deleteLand(deleteLandRequest: DeleteLandRequest)(implicit hc: HeaderCarrier): Future[DeleteLandReturn] =
    http.post(url"$formpPath/filing/delete/land")
      .withBody(Json.toJson(deleteLandRequest))
      .execute[DeleteLandReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][deleteLand]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][deleteLand]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
    
   def createResidency(createResidencyRequest: CreateResidencyRequest)(implicit hc: HeaderCarrier): Future[CreateResidencyReturn] =
      http.post(url"$formpPath/filing/create/residency")
        .withBody(Json.toJson(createResidencyRequest))
        .execute[CreateResidencyReturn]
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][createResidency]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][createResidency]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }

  def updateResidency(updateResidencyRequest: UpdateResidencyRequest)(implicit hc: HeaderCarrier): Future[UpdateResidencyReturn] =
    http.post(url"$formpPath/filing/update/residency")
      .withBody(Json.toJson(updateResidencyRequest))
      .execute[UpdateResidencyReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateResidency]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateResidency]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def deleteResidency(deleteResidencyRequest: DeleteResidencyRequest)(implicit hc: HeaderCarrier): Future[DeleteResidencyReturn] =
    http.post(url"$formpPath/filing/delete/residency")
      .withBody(Json.toJson(deleteResidencyRequest))
      .execute[DeleteResidencyReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][deleteResidency]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][deleteResidency]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def createLease(createLeaseRequest: CreateLeaseRequest)(implicit hc: HeaderCarrier): Future[CreateLeaseReturn] =
    http.post(url"$formpPath/filing/create/lease")
      .withBody(Json.toJson(createLeaseRequest))
      .execute[CreateLeaseReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createLease]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createLease]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def updateLease(updateLeaseRequest: UpdateLeaseRequest)(implicit hc: HeaderCarrier): Future[UpdateLeaseReturn] =
    http.post(url"$formpPath/filing/update/lease")
      .withBody(Json.toJson(updateLeaseRequest))
      .execute[UpdateLeaseReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateLease]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateLease]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def deleteLease(deleteLeaseRequest: DeleteLeaseRequest)(implicit hc: HeaderCarrier): Future[DeleteLeaseReturn] =
    http.post(url"$formpPath/filing/delete/lease")
      .withBody(Json.toJson(deleteLeaseRequest))
      .execute[DeleteLeaseReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][deleteLease]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][deleteLease]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def updateTransaction(updateTransactionRequest: UpdateTransactionRequest)(implicit hc: HeaderCarrier): Future[UpdateTransactionReturn] =
    http.post(url"$formpPath/filing/update/transaction")
      .withBody(Json.toJson(updateTransactionRequest))
      .execute[UpdateTransactionReturn]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateTransaction]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateTransaction]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def lockReturn(lockReturnRequest: LockReturnRequest)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, LockReturnResponse]] =
    http.post(url"$formpPath/filing/return/lock")
      .withBody(Json.toJson(lockReturnRequest))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(response) if response.status >= 200 && response.status < 300 =>
          logger.debug(s"[FormpProxyConnector][lockReturn]: OK status=${response.status} storn=${lockReturnRequest.storn} returnRef=${lockReturnRequest.returnResourceRef}")
          Right(LockReturnResponse(success = true))
        case Right(response) =>
          logger.warn(s"[FormpProxyConnector][lockReturn]: Unexpected non-2xx in Right branch - ${response.status}")
          Left(UpstreamErrorResponse(s"Unexpected status ${response.status}", response.status))
        case Left(error) =>
          logger.warn(s"[FormpProxyConnector][lockReturn]: Upstream error (likely version conflict) - ${error.statusCode} ${error.message}")
          Left(error)
      }
      .recover {
        case e: UpstreamErrorResponse =>
          logger.warn(s"[FormpProxyConnector][lockReturn]: Upstream error (likely version conflict) - ${e.statusCode} ${e.message}")
          Left(e)
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][lockReturn]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }


  def createSubmission(createSubmissionRequest: CreateSubmissionRequest)(implicit hc: HeaderCarrier): Future[CreateSubmissionReturn] =
    http.post(url"$formpPath/filing/submission")
      .withBody(Json.toJson(createSubmissionRequest))
      .execute[HttpResponse]
      .map { response =>
        if (response.status >= 200 && response.status < 300) {
          logger.debug(s"[FormpProxyConnector][createSubmission]: OK status=${response.status} storn=${createSubmissionRequest.storn} returnRef=${createSubmissionRequest.returnResourceRef}")
          CreateSubmissionReturn(success = true)
        } else {
          throw UpstreamErrorResponse(s"createSubmission failed: ${response.status} ${response.body}", response.status)
        }
      }
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createSubmission]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createSubmission]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
  
  def updateSubmission(updateSubmissionRequest: UpdateSubmissionRequest)(implicit hc: HeaderCarrier): Future[UpdateSubmissionReturn] =
    http.post(url"$formpPath/filing/update/submission")
      .withBody(Json.toJson(updateSubmissionRequest))
      .execute[HttpResponse]
      .map { response =>
        if (response.status >= 200 && response.status < 300) {
          logger.debug(s"[FormpProxyConnector][updateSubmission]: OK status=${response.status} storn=${updateSubmissionRequest.storn} returnRef=${updateSubmissionRequest.returnResourceRef}")
          UpdateSubmissionReturn(success = true)
        } else {
          throw UpstreamErrorResponse(s"updateSubmission failed: ${response.status} ${response.body}", response.status)
        }
      }
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][updateSubmission]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][updateSubmission]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
  
  def createSubmissionErrorDetail(createSubmissionErrorDetailRequest: CreateSubmissionErrorDetailRequest)(implicit hc: HeaderCarrier): Future[CreateSubmissionErrorDetailReturn] =
    http.post(url"$formpPath/filing/submission-error-detail")
      .withBody(Json.toJson(createSubmissionErrorDetailRequest))
      .execute[HttpResponse]
      .map { response =>
        if (response.status >= 200 && response.status < 300) {
          logger.debug(s"[FormpProxyConnector][createSubmissionErrorDetail]: OK status=${response.status} storn=${createSubmissionErrorDetailRequest.storn} returnRef=${createSubmissionErrorDetailRequest.returnResourceRef}")
          CreateSubmissionErrorDetailReturn(success = true)
        } else {
          throw UpstreamErrorResponse(s"createSubmissionErrorDetail failed: ${response.status} ${response.body}", response.status)
        }
      }
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][createSubmissionErrorDetail]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][createSubmissionErrorDetail]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
  
  def deleteSubmissionErrorDetail(deleteSubmissionErrorDetailRequest: DeleteSubmissionErrorDetailRequest)(implicit hc: HeaderCarrier): Future[DeleteSubmissionErrorDetailReturn] =
    http.post(url"$formpPath/filing/delete/submission-error-detail")
      .withBody(Json.toJson(deleteSubmissionErrorDetailRequest))
      .execute[HttpResponse]
      .map { response =>
        if (response.status >= 200 && response.status < 300) {
          logger.debug(s"[FormpProxyConnector][deleteSubmissionErrorDetail]: OK status=${response.status} storn=${deleteSubmissionErrorDetailRequest.storn} returnRef=${deleteSubmissionErrorDetailRequest.returnResourceRef}")
          DeleteSubmissionErrorDetailReturn(success = true)
        } else {
          throw UpstreamErrorResponse(s"deleteSubmissionErrorDetail failed: ${response.status} ${response.body}", response.status)
        }
      }
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[FormpProxyConnector][deleteSubmissionErrorDetail]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[FormpProxyConnector][deleteSubmissionErrorDetail]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def insertInitialGovTalkStatus(insertInitialGovTalkStatusRequest: InsertInitialGovTalkStatusRequest)(implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
      http.post(url"$formpPath/filing/govtalk-status")
        .withBody(Json.toJson(insertInitialGovTalkStatusRequest))
        .execute[HttpResponse]
        .map { response =>
          if (response.status >= 200 && response.status < 300) {
            logger.debug(s"[FormpProxyConnector][insertInitialGovTalkStatus]: OK status=${response.status} formResultId=${insertInitialGovTalkStatusRequest.formResultId}")
            GovTalkStatusReturn(success = true)
          } else {
            throw UpstreamErrorResponse(s"insertInitialGovTalkStatus failed: ${response.status} ${response.body}", response.status)
          }
        }
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][insertInitialGovTalkStatus]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][insertInitialGovTalkStatus]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }
  
  def resetGovTalkStatus(resetGovTalkStatusRequest: ResetGovTalkStatusRequest)(implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
      http.post(url"$formpPath/filing/reset/govtalk-status/reset")
        .withBody(Json.toJson(resetGovTalkStatusRequest))
        .execute[HttpResponse]
        .map { response =>
          if (response.status >= 200 && response.status < 300) {
            logger.debug(s"[FormpProxyConnector][resetGovTalkStatus]: OK status=${response.status} formResultId=${resetGovTalkStatusRequest.formResultId}")
            GovTalkStatusReturn(success = true)
          } else {
            throw UpstreamErrorResponse(s"resetGovTalkStatus failed: ${response.status} ${response.body}", response.status)
          }
        }
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][resetGovTalkStatus]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][resetGovTalkStatus]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }
  
  def updateGovTalkStatus(updateGovTalkStatusRequest: UpdateGovTalkStatusRequest)(implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
      http.post(url"$formpPath/filing/update/govtalk-status")
        .withBody(Json.toJson(updateGovTalkStatusRequest))
        .execute[HttpResponse]
        .map { response =>
          if (response.status >= 200 && response.status < 300) {
            logger.debug(s"[FormpProxyConnector][updateGovTalkStatus]: OK status=${response.status} formResultId=${updateGovTalkStatusRequest.formResultId}")
            GovTalkStatusReturn(success = true)
          } else {
            throw UpstreamErrorResponse(s"updateGovTalkStatus failed: ${response.status} ${response.body}", response.status)
          }
        }
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][updateGovTalkStatus]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][updateGovTalkStatus]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }
  
  def updateGovTalkStatusCorrelationId(updateGovTalkStatusCorrelationIdRequest: UpdateGovTalkStatusCorrelationIdRequest)(implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
      http.post(url"$formpPath/filing/update/govtalk-status/correlation-Id")
        .withBody(Json.toJson(updateGovTalkStatusCorrelationIdRequest))
        .execute[HttpResponse]
        .map { response =>
          if (response.status >= 200 && response.status < 300) {
            logger.debug(s"[FormpProxyConnector][updateGovTalkStatusCorrelationId]: OK status=${response.status} formResultId=${updateGovTalkStatusCorrelationIdRequest.formResultId}")
            GovTalkStatusReturn(success = true)
          } else {
            throw UpstreamErrorResponse(s"updateGovTalkStatusCorrelationId failed: ${response.status} ${response.body}", response.status)
          }
        }
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][updateGovTalkStatusCorrelationId]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][updateGovTalkStatusCorrelationId]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }
  
  def updateGovTalkStatusLock(updateGovTalkStatusLockRequest: UpdateGovTalkStatusLockRequest)(implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
      http.post(url"$formpPath/filing/update/govtalk-status/lock")
        .withBody(Json.toJson(updateGovTalkStatusLockRequest))
        .execute[HttpResponse]
        .map { response =>
          if (response.status >= 200 && response.status < 300) {
            logger.debug(s"[FormpProxyConnector][updateGovTalkStatusLock]: OK status=${response.status} formResultId=${updateGovTalkStatusLockRequest.formResultId}")
            GovTalkStatusReturn(success = true)
          } else {
            throw UpstreamErrorResponse(s"updateGovTalkStatusLock failed: ${response.status} ${response.body}", response.status)
          }
        }
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][updateGovTalkStatusLock]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][updateGovTalkStatusLock]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }
  
  def updateGovTalkStatistics(updateGovTalkStatisticsRequest: UpdateGovTalkStatisticsRequest)(implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
      http.post(url"$formpPath/filing/update/govtalk-status/statistics")
        .withBody(Json.toJson(updateGovTalkStatisticsRequest))
        .execute[HttpResponse]
        .map { response =>
          if (response.status >= 200 && response.status < 300) {
            logger.debug(s"[FormpProxyConnector][updateGovTalkStatistics]: OK status=${response.status} formResultId=${updateGovTalkStatisticsRequest.formResultId}")
            GovTalkStatusReturn(success = true)
          } else {
            throw UpstreamErrorResponse(s"updateGovTalkStatistics failed: ${response.status} ${response.body}", response.status)
          }
        }
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][updateGovTalkStatistics]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][updateGovTalkStatistics]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }
  
  def deleteGovTalkStatus(deleteGovTalkStatusRequest: DeleteGovTalkStatusRequest)(implicit hc: HeaderCarrier): Future[GovTalkStatusReturn] =
      http.post(url"$formpPath/filing/delete/govtalk-status")
        .withBody(Json.toJson(deleteGovTalkStatusRequest))
        .execute[HttpResponse]
        .map { response =>
          if (response.status >= 200 && response.status < 300) {
            logger.debug(s"[FormpProxyConnector][deleteGovTalkStatus]: OK status=${response.status} resultId=${deleteGovTalkStatusRequest.resultId}")
            GovTalkStatusReturn(success = true)
          } else {
            throw UpstreamErrorResponse(s"deleteGovTalkStatus failed: ${response.status} ${response.body}", response.status)
          }
        }
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][deleteGovTalkStatus]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][deleteGovTalkStatus]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }

  def selectGovTalkStatus(selectGovTalkStatusRequest: SelectGovTalkStatusRequest)(implicit hc: HeaderCarrier): Future[SelectGovTalkStatusResponse] =
      http.get(url"$formpPath/filing/govtalk-status")
        .withBody(Json.toJson(selectGovTalkStatusRequest))
        .execute[SelectGovTalkStatusResponse]
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][selectGovTalkStatus]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][selectGovTalkStatus]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }

  def selectGovTalkFormResultId(selectGovTalkFormResultIdRequest: SelectGovTalkFormResultIdRequest)(implicit hc: HeaderCarrier): Future[SelectGovTalkFormResultIdResponse] =
      http.get(url"$formpPath/filing/govtalk-status/form-result-Id")
        .withBody(Json.toJson(selectGovTalkFormResultIdRequest))
        .execute[SelectGovTalkFormResultIdResponse]
        .recover {
          case e: UpstreamErrorResponse =>
            logger.error(s"[FormpProxyConnector][selectGovTalkFormResultId]: Upstream error - ${e.getMessage}")
            throw e
          case e: Throwable =>
            logger.error(s"[FormpProxyConnector][selectGovTalkFormResultId]: ${e.getMessage}")
            throw new RuntimeException(e.getMessage)
        }
}
