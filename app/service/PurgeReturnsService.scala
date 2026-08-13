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

package service

import connectors.FormpProxyConnector
import models.purge.{DeleteReturnRequest, GetReturnsForPurgeRequest, ReturnForPurge}
import play.api.Logging
import scheduler.ScheduleStatus.{FailedToPurgeReturns, MongoUnlockException}
import scheduler.{MongoLockKeys, ScheduleStatus, ScheduledService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future, duration}
import scala.util.Try

class PurgeReturnsService @Inject() (
  connector: FormpProxyConnector,
  servicesConfig: ServicesConfig,
  lockRepositoryProvider: MongoLockRepository,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends ScheduledService[Either[ScheduleStatus.JobFailed, List[String]]]
    with Logging {

  val jobName: String = "PurgeReturnsJob"

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val mongoLockTimeoutDuration: duration.Duration =
    duration.Duration(servicesConfig.getString(s"schedules.${MongoLockKeys.purgeReturnsLock}.mongoLockTimeout"))

  lazy val lockKeeper: LockService = LockService(
    lockRepository = lockRepositoryProvider,
    lockId = s"schedules.${MongoLockKeys.purgeReturnsLock}",
    ttl = mongoLockTimeoutDuration
  )

  def tryLock(
    f: => Future[Either[ScheduleStatus.JobFailed, List[String]]]
  ): Future[Either[ScheduleStatus.JobFailed, List[String]]] =
    lockKeeper
      .withLock(f)
      .map {
        case Some(result) => result
        case None =>
          logger.info(s"[$jobName] locked because it might be running on another instance")
          Right(Nil)
      }
      .recover { case e: Exception =>
        logger.warn(s"[$jobName] failed with exception: ${Try(e.getMessage).toOption}")
        Left(MongoUnlockException(e))
      }

  override def invoke: Future[Either[ScheduleStatus.JobFailed, List[String]]] =
    tryLock {
      purge().map(Right(_)).recover { case e: Exception =>
        logger.error(s"[$jobName] failed while purging returns", e)
        Left(FailedToPurgeReturns(e))
      }
    }

  private def purge(): Future[List[String]] = {
    val executionDate = LocalDate.now(clock)
    logger.info(s"[$jobName] starting purge, executionDate=$executionDate")
    for {
      selected <- connector.getReturnsForPurge(GetReturnsForPurgeRequest(executionDate)).map(_.returnsForPurge)
      _         = logger.info(s"[$jobName] selected for purge: ${refList(selected)}")
      results  <- purgeSequentially(selected)
    } yield {
      val purged = results.collect { case (returnForPurge, true) => returnForPurge }
      logger.info(s"[$jobName] purged: ${refList(purged)}")
      logReport(executionDate, selected, results)
      purged.map(_.returnResourceRef)
    }
  }

  private def refList(returns: List[ReturnForPurge]): String =
    if (returns.isEmpty) "none" else returns.map(r => s"${r.storn}/${r.returnResourceRef}").mkString(", ")

  private def purgeSequentially(selected: List[ReturnForPurge]): Future[List[(ReturnForPurge, Boolean)]] =
    selected.foldLeft(Future.successful(List.empty[(ReturnForPurge, Boolean)])) { (acc, returnForPurge) =>
      acc.flatMap(results => purgeOne(returnForPurge).map(results :+ _))
    }

  private def purgeOne(returnForPurge: ReturnForPurge): Future[(ReturnForPurge, Boolean)] =
    connector
      .deleteReturn(DeleteReturnRequest(returnForPurge.storn, returnForPurge.returnResourceRef))
      .map { response =>
        if (!response.deleted)
          logger.warn(s"[$jobName] return ${returnForPurge.returnResourceRef} was not purged, formp-proxy returned deleted=false")
        (returnForPurge, response.deleted)
      }
      .recover { case e: Throwable =>
        logger.warn(s"[$jobName] failed to purge return ${returnForPurge.returnResourceRef}: ${e.getMessage}")
        (returnForPurge, false)
      }

  private def logReport(
    executionDate: LocalDate,
    selected: List[ReturnForPurge],
    results: List[(ReturnForPurge, Boolean)]
  ): Unit = {
    val purged    = results.collect { case (returnForPurge, true) => returnForPurge }
    val notPurged = results.collect { case (returnForPurge, false) => returnForPurge }

    def perState(returns: List[ReturnForPurge]): String =
      returns
        .groupBy(_.status)
        .toList
        .sortBy(_._1)
        .map { case (state, rs) => f"   $state%-32s ${rs.size}" }
        .mkString("\n")

    val report =
      s"""
         |=========================================================================
         |THIS SDLT PURGE REPORT IS FOR THE PURGE THAT WAS RUN ON: $executionDate
         |
         |THE TOTAL NUMBER OF RETURNS SELECTED FOR PURGING IS: ${selected.size}
         |
         |THE TOTAL NUMBER OF PURGED RETURNS IS: ${purged.size}
         |
         |THE TOTAL NUMBER OF PURGED RETURNS PER STATE IS:
         |
         |   STATE                            NUMBER OF RETURNS
         |---------------------------------------------------------
         |${perState(purged)}
         |THE TOTAL NUMBER OF RETURNS NOT PURGED PER STATE IS:
         |
         |   STATUS                           NUMBER OF RETURNS
         |---------------------------------------------------------
         |${perState(notPurged)}
         |=========================================================================
         |""".stripMargin

    logger.info(report)
  }
}
