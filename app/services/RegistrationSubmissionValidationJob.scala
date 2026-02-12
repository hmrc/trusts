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

package services

import errors.TrustErrors
import models.RegistrationSubmissionValidationStats
import org.apache.pekko.actor._
import repositories.RegistrationSubmissionRepository

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging

import javax.inject.{Inject, Singleton}
import config.AppConfig
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import scala.concurrent.duration.{Duration, MINUTES}

@Singleton
class RegistrationValidationJobStarter @Inject() (
  system: ActorSystem,
  config: AppConfig,
  submissionRepository: RegistrationSubmissionRepository,
  mongoComponent: MongoComponent)(implicit ec: ExecutionContext) {

  private val lockService: LockService = LockService(
    lockRepository = new MongoLockRepository(
      mongoComponent = mongoComponent,
      timestampSupport = new CurrentTimestampSupport
    ),
    lockId = "registration-submission-validation-job",
    ttl = Duration.create(2, MINUTES)
  )

  system.actorOf(
    Props(RegistrationSubmissionValidationJob(config, submissionRepository, lockService)),
    "registration-submission-validation"
  )

}

case class RegistrationSubmissionValidationJob(
  config: AppConfig,
  submissionRepository: RegistrationSubmissionRepository,
  lockService: LockService
)(implicit ec: ExecutionContext)
    extends Actor with ActorLogging with Logging {

  override def preStart(): Unit =
    context.system.scheduler.scheduleOnce(
      delay = config.registrationValidationInterval,
      receiver = self,
      message = "runAggregation"
    )

  override def receive: Receive = {
    case "runAggregation" =>
      val ownerId: String = "registration-submission-validation-owner"
      val lockId: String = lockService.lockId
      if (config.registrationValidationJobEnabled) {
        lockService.lockRepository.isLocked(lockId, ownerId)
          .flatMap { (alreadyRunningAggregation: Boolean) =>
            if (alreadyRunningAggregation) {
              logger.info("[RegistrationSubmissionValidationJob][receive] Lock in place not running aggregation")
              Future.successful(None)
            } else {
              acquireLockAndRunJob(ownerId, lockId).flatMap {
                case Some(eitherErrorOrValidationResults: Either[TrustErrors, RegistrationSubmissionValidationStats]) =>
                  logResults(eitherErrorOrValidationResults)
                case None =>
                  logger.info("[RegistrationSubmissionValidationJob][receive] Could not acquire lock")
                  Future.successful(None)
              }
            }
          }
      }
      else {
        logger.info("[RegistrationSubmissionValidationJob][receive] RegistrationSubmissionValidationJob not enabled")
      }
  }

  private def acquireLockAndRunJob(ownerId: String, lockId: String): Future[Option[Either[TrustErrors, RegistrationSubmissionValidationStats]]] = {
    val lockRepository = lockService.lockRepository
    (for {
      acquired: Boolean <- lockRepository.takeLock(lockId, ownerId, lockService.ttl).map(_.isDefined)
      result: Option[Either[TrustErrors, RegistrationSubmissionValidationStats]] <- if (acquired) {
        logger.info(s"[RegistrationSubmissionValidationJob][acquireLockAndRunJob] Running job with lock id: $lockId, owner " +
          s"id: $ownerId")
        runRegistrationSubmissionValidationJob
          .flatMap(value => lockRepository.releaseLock(lockId, ownerId).map(_ => Some(value)))
      }
      else {
        Future.successful(None)
      }
    } yield result
      ).recoverWith {
      case ex => lockRepository.releaseLock(lockId, ownerId).flatMap(_ => Future.failed(ex))
    }
  }

  private def runRegistrationSubmissionValidationJob: Future[Either[TrustErrors, RegistrationSubmissionValidationStats]] =
    submissionRepository
      .countRecordsWithMissingOrIncorrectCreatedAt()
      .value


  def logResults(eitherErrorOrValidationResults: Either[TrustErrors, RegistrationSubmissionValidationStats]) = {
    eitherErrorOrValidationResults match {
      case Right(validationResults: RegistrationSubmissionValidationStats) =>
        logger.info(
          s"[RegistrationSubmissionValidationJob][runRegistrationSubmissionValidationJob] createdAt beyond TTL record count:" +
            s" ${validationResults.createdAtBeyondTTLCount}, createdAt not a Date record count: " +
            s"${validationResults.createdAtNotDateTimeCount}, no createdAt record count: ${validationResults.noCreatedAtCount}"
        )
        Future.successful(None)
      case Left(e: TrustErrors) =>
        logger.info(s"[RegistrationSubmissionValidationJob][runRegistrationSubmissionValidationJob] $e")
        Future.successful(None)
    }
  }

}
