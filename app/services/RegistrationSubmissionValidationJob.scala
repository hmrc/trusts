/*
 * Copyright 2024 HM Revenue & Customs
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

@Singleton
class RegistrationValidationJobStarter @Inject() (
  system: ActorSystem,
  config: AppConfig,
  submissionRepository: RegistrationSubmissionRepository
)(implicit ec: ExecutionContext) {

  system.actorOf(
    Props(RegistrationSubmissionValidationJob(config, submissionRepository)),
    "registration-submission-validation"
  )

}

case class RegistrationSubmissionValidationJob(
  config: AppConfig,
  submissionRepository: RegistrationSubmissionRepository
)(implicit ec: ExecutionContext)
    extends Actor with ActorLogging with Logging {

  override def preStart(): Unit =
    context.system.scheduler.scheduleOnce(
      delay = config.registrationValidationInterval,
      receiver = self,
      message = "runAggregation"
    )

  override def receive: Receive = { case "runAggregation" =>
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
        case Left(e: TrustErrors)                                            =>
          logger.info(s"[RegistrationSubmissionValidationJob][receive] $e")
      }
  }

}
