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

package scheduler

import jakarta.inject.Inject
import models.UpdatedCounterValues
import org.apache.pekko.stream.scaladsl.{Keep, Sink, SinkQueueWithCancel, Source}
import org.apache.pekko.stream.{ActorAttributes, Materializer}
import play.api.{Configuration, Logger}
import repositories.{CacheRepositoryImpl, RegistrationSubmissionRepository, TaxableMigrationRepositoryImpl, TransformationRepositoryImpl}

import javax.inject.Singleton
import scala.concurrent.duration.FiniteDuration

@Singleton
class SchedulerForLastUpdated @Inject()(
                                         registrationSubmissionRepository: RegistrationSubmissionRepository,
                                         cacheRepositoryImpl: CacheRepositoryImpl,
                                         TaxableMigrationRepositoryImpl: TaxableMigrationRepositoryImpl,
                                         TransformationRepositoryImpl: TransformationRepositoryImpl,
                                         config: Configuration
                                       )(implicit mat: Materializer) extends WorkerConfig {


  logger.info("################### SchedulerForLastUpdated started ###################")

  private val logger = Logger(this.getClass)
  private val initialDelay: FiniteDuration = durationValueFromConfig("workers.metrics-worker.initial-delay", config)
  private val interval: FiniteDuration = durationValueFromConfig("workers.metrics-worker.interval ", config)

  val tap: SinkQueueWithCancel[UpdatedCounterValues] = {
    Source
      .tick(initialDelay, interval, cacheRepositoryImpl.fixBadUpdatedAt(limit = 1000))
      .flatMapConcat(identity)
      .map { metrics =>
        metrics.report()
        metrics
      }
      .wireTapMat(Sink.queue())(Keep.right)
      .toMat(Sink.ignore)(Keep.left)
      .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
      .run()
  }
  logger.info("################### SchedulerForLastUpdated ended ###################")

}
