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

import scala.concurrent.ExecutionContext
import org.apache.pekko.pattern.pipe
import play.api.Logging

import javax.inject.{Inject, Singleton}
import config.AppConfig
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.time.LocalDateTime
import scala.concurrent.duration.{Duration, MINUTES}

@Singleton
class RegistrationValidationJobStarter @Inject() (
  system: ActorSystem,
  config: AppConfig,
  submissionRepository: RegistrationSubmissionRepository,
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext) {

  private val lockService: LockService = LockService(
    new MongoLockRepository(
      mongoComponent = mongoComponent,
      timestampSupport = new CurrentTimestampSupport
    ),
    lockId = s"registration-submission-validation-job-${LocalDateTime.now()}",
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

      logger.info(s"[RegistrationSubmissionValidationJob] Running job with lock id: ${lockService.lockId}")

      lockService.withLock(
        submissionRepository
          .countRecordsWithMissingOrIncorrectCreatedAt()
          .value
          .pipeTo(self)
          .map {
            case Right(validationResults: RegistrationSubmissionValidationStats) =>
              logger.info(
                s"[RegistrationSubmissionValidationJob][receive] createdAt beyond TTL record count:" +
                  s" ${validationResults.createdAtBeyondTTLCount}, createdAt not a Date record count: " +
                  s"${validationResults.createdAtNotDateTimeCount}, no createdAt record count: ${validationResults.noCreatedAtCount}"
              )
            case Left(e: TrustErrors) =>
              logger.info(s"[RegistrationSubmissionValidationJob][receive] $e")
          }
      )
  }

}
