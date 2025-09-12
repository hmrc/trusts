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
import org.bson.types.ObjectId
import play.api.{Configuration, Logger}
import repositories.RepositoryHelper

import javax.inject.Singleton
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class SchedulerForLastUpdated @Inject()(repositoriesJava: java.util.Set[RepositoryHelper[_]],
                                        config: Configuration
                                       )(implicit mat: Materializer) extends WorkerConfig {


  private val logger = Logger(this.getClass)
  private val initialDelay: FiniteDuration = durationValueFromConfig("workers.metrics-worker.initial-delay", config)
  private val interval: FiniteDuration = durationValueFromConfig("workers.metrics-worker.interval ", config)
  private val repositories = repositoriesJava.asScala.toSeq

  val tap: SinkQueueWithCancel[Unit] = {
    logger.info("[SchedulerForLastUpdated] init")
    Source
      .tick(initialDelay, interval, fixBadUpdatedAt(limit = 1000))
      .flatMapConcat(identity)
      .wireTapMat(Sink.queue())(Keep.right)
      .toMat(Sink.ignore)(Keep.left)
      .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
      .run()
  }


  def fixBadUpdatedAt(limit: Int = 1000): Source[Unit, _] = {
    val repositoryHelper = repositories.map {
      ele =>
        logger.info(s"started [$ele][fixBadUpdatedAt] method with limit = $limit")
        Source
          .fromPublisher(ele.getAllInvalidDateDocuments(limit = limit))
          .fold(List.empty[ObjectId])((acc, id) => id :: acc)
          .mapAsync(parallelism = 1) { ids =>
            if (ids.isEmpty) {
              scala.concurrent.Future.successful(UpdatedCounterValues(0, 0, 0)).map(_.report(ele.className))(mat.executionContext)
            } else {
              ele.updateAllInvalidDateDocuments(ids)
                .map(_.report(ele.className))(mat.executionContext)
            }
          }
    }.map {
      ele =>
        logger.info(s"[SchedulerForLastUpdated][fixBadUpdatedAt] ended $ele")
        ele
    }
    repositoryHelper.reduce(_ concat _)
  }

}
