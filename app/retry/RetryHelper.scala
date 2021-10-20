/*
 * Copyright 2021 HM Revenue & Customs
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

package retry

import akka.actor.ActorSystem
import akka.pattern.Patterns.after
import config.AppConfig
import play.api.Logging

import java.util.concurrent.Callable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait RetryHelper extends Logging {

  val as: ActorSystem = ActorSystem()

  case class RetryExecution(ticks: Seq[Int], result: Option[RetryPolicy]) {
    val totalTime: Int = ticks.sum

    def timeOfEachTick: Seq[Int] =
      if (ticks.isEmpty) Nil
      else ticks.drop(1).scanLeft(ticks.head)((acc, x) => acc + x)
  }

  def retryOnFailure(f: () => Future[RetryPolicy], config: AppConfig)(implicit ec: ExecutionContext): Future[RetryExecution] =
    retryWithBackOff(
      currentAttempt = 1,
      currentWait = config.nrsRetryWaitMs,
      config = config,
      f = f,
      lastExecution = RetryExecution(Seq(0), None)
    )

  private def retryWithBackOff(currentAttempt: Int,
                               currentWait: Int,
                               config: AppConfig,
                               f: () => Future[RetryPolicy],
                              lastExecution: RetryExecution
                              )(implicit ec: ExecutionContext): Future[RetryExecution] = {
    f.apply().flatMap {
      case result: RetryPolicy if result.retry =>

          RetryHelper.calculateWaitTime(
            config.nrsRetryAttempts,
            config.nrsRetryWaitFactor,
            currentWait,
            currentAttempt
          )(
            next = { wait =>
              logger.warn(s"Failure, retrying after $wait ms, attempt $currentAttempt")
              after(
                duration = wait.milliseconds,
                scheduler = as.scheduler,
                context = ec,
                value = new Callable[Future[Int]] {
                  override def call(): Future[Int] = Future.successful(1)
                }
              ).flatMap { _ =>
                val nextExecution = lastExecution.copy(ticks = lastExecution.ticks :+ wait, Some(result))
                retryWithBackOff(currentAttempt + 1, wait, config, f, nextExecution)
              }
          },
            last = { () =>
              Future.successful(RetryExecution(lastExecution.ticks, Some(result)))
            }
          )
      case success =>
        Future.successful(RetryExecution(lastExecution.ticks, Some(success)))
    }
  }
}

object RetryHelper extends Logging {

  def calculateWaitTime[T](maxAttempts: Int, waitFactor: Int, currentWait: Int, currentAttempt: Int)
                          (next: Int => Future[T], last: () => Future[T]): Future[T] = {
    if (currentAttempt < maxAttempts) {
      val wait = Math.ceil(currentWait * waitFactor).toInt
      logger.info(s"[RetryHelper] waiting for $wait milliseconds")
      next(wait)
    } else {
      logger.info(s"[RetryHelper] waited a total of $currentWait milliseconds")
      last()
    }
  }
}

trait RetryPolicy {
  val retry : Boolean
}