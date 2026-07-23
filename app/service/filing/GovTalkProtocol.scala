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

package service.filing

import connectors.ChrisConnector
import models.filing.*
import models.submission.*
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

trait GovTalkProtocol { self: Logging =>

  protected def chrisService: ChrisService
  protected def chrisConnector: ChrisConnector
  protected def logPrefix: String
  protected def logRef(storn: String, returnId: String, correlationId: String): String

  protected def setGovTalkProtocol(storn: String, formResultId: String, protocolStatus: String, correlationId: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    chrisService.updateGovTalkStatus(UpdateGovTalkStatusRequest(
      userIdentifier    = storn,
      formResultId      = formResultId,
      endStateTimestamp = nowSqlTimestamp,
      protocolStatus    = protocolStatus
    )).map { _ =>
      logger.info(s"[$logPrefix] GovTalk protocolStatus set to '$protocolStatus' " +
        s"${logRef(storn, formResultId, correlationId)}")
      ()
    }

  protected def setGovTalkCorrelationId(storn: String, formResultId: String, correlationId: String)
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    chrisService.updateGovTalkStatusCorrelationId(UpdateGovTalkStatusCorrelationIdRequest(
      userIdentifier = storn,
      formResultId   = formResultId,
      correlationId  = correlationId,
      pollInterval   = 0,
      gatewayUrl     = chrisConnector.defaultPath
    )).map { _ =>
      logger.info(s"[$logPrefix] GovTalk correlation id stored ${logRef(storn, formResultId, correlationId)}")
      ()
    }

  protected def warnOnCorrelationIdMismatch(storn: String,
                                            returnId: String,
                                            sent: String,
                                            received: Option[String]): Unit =
    received.map(_.trim).filter(id => id.nonEmpty && id != sent).foreach { answered =>
      logger.warn(s"[$logPrefix] ChRIS answered with correlation id '$answered' but the submission " +
        s"used a different one, the stored id is kept ${logRef(storn, returnId, sent)}")
    }

  protected def updateGovTalkStatistics(storn: String,
                                        formResultId: String,
                                        gatewayUrl: Option[String],
                                        correlationId: String,
                                        pollInterval: String = "0",
                                        numberOfPolls: Int = 0)
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    val target = gatewayUrl.filter(_.nonEmpty).getOrElse(chrisConnector.defaultPath)
    chrisService.updateGovTalkStatistics(UpdateGovTalkStatisticsRequest(
      userIdentifier = storn,
      formResultId   = formResultId,
      govTalkStatus  = GovTalkStatusStatistics(
        lastMessageTimestamp = nowSqlTimestamp,
        numberOfPolls        = numberOfPolls.toString,
        pollInterval         = pollInterval,
        gatewayUrl           = target
      )
    )).map { _ =>
      logger.info(s"[$logPrefix] GovTalk statistics updated gatewayUrl=$target " +
        s"pollInterval=$pollInterval ${logRef(storn, formResultId, correlationId)}")
      ()
    }

  // spec F52 step 14.1.3: after a successful delete, mark endState and then RESET the GovTalk status row.
  // Error-suppressed: this is post-success housekeeping and must never turn an accepted filing into a failure.
  protected def finaliseGovTalkStatus(storn: String,
                                      formResultId: String,
                                      correlationId: String,
                                      oldProtocol: String = "endState")
                                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    (for {
      _ <- setGovTalkProtocol(storn, formResultId, "endState", correlationId)
      _ <- chrisService.resetGovTalkStatus(buildResetRequest(storn, formResultId, oldProtocol)).map(_ => ())
    } yield {
      logger.info(s"[$logPrefix] GovTalk Status finalised (endState + reset) " +
        s"${logRef(storn, formResultId, correlationId)}")
      ()
    }).recover { case e =>
      logger.warn(s"[$logPrefix] GovTalk finalise (endState/reset) FAILED (suppressed) " +
        s"${logRef(storn, formResultId, correlationId)}: ${e.getMessage}")
      ()
    }

  protected def buildLockRequest(storn: String,
                                 formResultId: String,
                                 formLockOld: String,
                                 formLockNew: String,
                                 pollInterval: String = "0",
                                 gatewayUrl: Option[String] = None): UpdateGovTalkStatusLockRequest =
    UpdateGovTalkStatusLockRequest(
      userIdentifier = storn,
      formResultId   = formResultId,
      govTalkStatus  = GovTalkStatusLock(
        formLockOld  = formLockOld,
        formLockNew  = formLockNew,
        pollInterval = pollInterval,
        gatewayUrl   = gatewayUrl.filter(_.nonEmpty).getOrElse(chrisConnector.defaultPath)
      )
    )

  protected def nowIso: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  protected def buildResetRequest(storn: String,
                                  formResultId: String,
                                  oldProtocol: String = "endState"): ResetGovTalkStatusRequest =
    val now = nowSqlTimestamp
    ResetGovTalkStatusRequest(
      userIdentifier = storn,
      formResultId   = formResultId,
      correlationId  = "empty",
      govTalkStatus  = GovTalkStatusReset(
        formLock             = "N",
        createTimestamp      = now,
        endStateTimestamp    = None,          // spec F53 step 7: null for the end state date on reset
        lastMessageTimestamp = now,
        numberOfPolls        = "0",
        pollInterval         = "0",
        protocolStatusOld    = oldProtocol,
        protocolStatusNew    = "initial",
        gatewayUrl           = chrisConnector.defaultPath
      )
    )

  protected def sendChrisPoll(storn: String, returnId: String, endpoint: Option[String], correlationId: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ChrisResponse] =
    logger.info(s"[$logPrefix] sending ChRIS POLL ${logRef(storn, returnId, correlationId)} " +
      s"endpoint=${endpoint.getOrElse("<default>")}")
    chrisConnector.poll(endpoint, correlationId)

  // Returns true when the ChRIS resource was deleted (or was already gone), false on any error.
  protected def sendChrisDelete(storn: String, returnId: String, endpoint: Option[String], correlationId: String)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    logger.info(s"[$logPrefix] sending ChRIS DELETE ${logRef(storn, returnId, correlationId)} " +
      s"endpoint=${endpoint.getOrElse("<default>")}")
    chrisConnector.delete(endpoint, correlationId).map {
      case ChrisDeleteResponse.Deleted(_, _) =>
        logger.info(s"[$logPrefix] ChRIS resource DELETED ${logRef(storn, returnId, correlationId)}")
        true
      case ChrisDeleteResponse.NotFound(_, _) =>
        logger.info(s"[$logPrefix] ChRIS resource already gone (2000) ${logRef(storn, returnId, correlationId)}")
        true
      case ChrisDeleteResponse.Errored(errors, _, _) =>
        logger.warn(s"[$logPrefix] ChRIS DELETE returned errors (suppressed) " +
          s"${logRef(storn, returnId, correlationId)}: ${errors.mkString("; ")}")
        false
      case ChrisDeleteResponse.TransportError(msg, _) =>
        logger.warn(s"[$logPrefix] ChRIS DELETE transport error (suppressed) " +
          s"${logRef(storn, returnId, correlationId)}: $msg")
        false
    }.recover { case e =>
      logger.warn(s"[$logPrefix] ChRIS DELETE failed (suppressed) " +
        s"${logRef(storn, returnId, correlationId)}: ${e.getMessage}")
      false
    }

  protected def createSubmissionErrorDetails(storn: String,
                                             returnId: String,
                                             errors: Seq[GovTalkError],
                                             correlationId: String)
                                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    if (errors.isEmpty) {
      logger.info(s"[$logPrefix] no GovTalk errors to persist ${logRef(storn, returnId, correlationId)}")
      Future.unit
    } else {
      logger.info(s"[$logPrefix] persisting ${errors.size} GovTalk error detail(s) " +
        s"${logRef(storn, returnId, correlationId)}")
      errors.zipWithIndex.foldLeft(Future.unit) { case (acc, (err, index)) =>
        acc.flatMap { _ =>
          chrisService.createSubmissionErrorDetail(CreateSubmissionErrorDetailRequest(
            storn                  = storn,
            returnResourceRef      = returnId,
            submissionErrorDetails = SubmissionErrorDetail(
              position     = index.toString,
              errorMessage = err.number.fold(err.text.getOrElse(""))(code => s"$code: ${err.text.getOrElse("")}")
            )
          )).map { _ =>
            logger.info(s"[$logPrefix] error detail persisted number=${err.number.getOrElse("-")} " +
              s"location=${err.location.getOrElse("-")} ${logRef(storn, returnId, correlationId)}")
            ()
          }
        }
      }
    }

  protected def persistUpdate(storn: String, returnId: String, acc: SubmissionUpdate, correlationId: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubmissionUpdate] =
    chrisService.updateSubmission(UpdateSubmissionRequest(storn, returnId, acc)).map { _ =>
      logger.info(s"[$logPrefix] submission updated status=${acc.submittableStatus.getOrElse("-")} " +
        s"utrn=${acc.utrn.getOrElse("-")} ${logRef(storn, returnId, correlationId)}")
      acc
    }

  protected def completeSuccessfulSubmission(storn: String,
                                             returnId: String,
                                             formResultId: String,
                                             correlationId: String,
                                             update: SubmissionUpdate,
                                             endpoint: Option[String],
                                             numberOfPolls: Int = 0)
                                            (implicit hc: HeaderCarrier,
                                             ec: ExecutionContext): Future[SubmissionUpdate] =
    for {
      acc     <- persistUpdate(storn, returnId, update, correlationId)
      _       <- updateGovTalkStatistics(storn, formResultId, endpoint, correlationId, "0", numberOfPolls)
      _       <- setGovTalkProtocol(storn, formResultId, "deleteRequest", correlationId)
      deleted <- sendChrisDelete(storn, returnId, endpoint, correlationId)
      _       <- if (deleted) finaliseGovTalkStatus(storn, formResultId, correlationId)
                 else {
                   logger.warn(s"[$logPrefix] ChRIS delete unsuccessful; leaving GovTalk at " +
                     s"deleteRequest (no endState/reset) ${logRef(storn, returnId, correlationId)}")
                   Future.unit
                 }
    } yield acc

  protected def closeDepartmentalGovTalk(storn: String,
                                         returnId: String,
                                         formResultId: String,
                                         correlationId: String,
                                         endpoint: Option[String],
                                         numberOfPolls: Int = 0)
                                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    for {
      _ <- updateGovTalkStatistics(storn, formResultId, endpoint, correlationId, "0", numberOfPolls)
      _ <- setGovTalkProtocol(storn, formResultId, "deleteRequest", correlationId)
      _ <- sendChrisDelete(storn, returnId, endpoint, correlationId)
      _ <- setGovTalkProtocol(storn, formResultId, "endState", correlationId)
    } yield ()

  protected def isRecoverable(errors: Seq[GovTalkError]): Boolean =
    UniversalStatus.resetsToStarted(errors)

  protected def nowSqlTimestamp: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(GovTalkProtocol.SqlTimestampFormatter)
}

object GovTalkProtocol {
  private val SqlTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
}
