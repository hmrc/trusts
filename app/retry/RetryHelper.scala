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
import com.google.inject.ImplementedBy
import play.api.Logging
import retry.RetryHelper.RetryExecution

import java.util.concurrent.Callable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import utils.Session
import uk.gov.hmrc.http.HeaderCarrier

@ImplementedBy(classOf[NrsRetryHelper])
trait RetryHelper extends Logging {

  val as: ActorSystem = ActorSystem()

  val maxAttempts: Int
  val factor: Int
  val initialWait: Int

  def retryOnFailure(f: () => Future[RetryPolicy])(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[RetryExecution] =
    retryWithBackOff(
      currentAttempt = 1,
      currentWait = initialWait,
      f = f,
      lastExecution = RetryExecution(Seq(0), None)
    )

  private def retryWithBackOff(currentAttempt: Int,
                               currentWait: Int,
                               f: () => Future[RetryPolicy],
                              lastExecution: RetryExecution
                              )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[RetryExecution] = {
    f.apply().flatMap {
      case result: RetryPolicy if result.retry =>

          RetryHelper.calculateWaitTime(
            maxAttempts,
            factor,
            currentWait,
            currentAttempt
          )(
            next = { nextWait =>
              logger.warn(s"[Session ID: ${Session.id(hc)}] failed, retrying after $nextWait ms, attempt $currentAttempt")
              after(
                duration = nextWait.milliseconds,
                scheduler = as.scheduler,
                context = ec,
                value = new Callable[Future[Int]] {
                  override def call(): Future[Int] = Future.successful(1)
                }
              ).flatMap { _ =>
                val nextExecution = lastExecution.copy(ticks = lastExecution.ticks :+ nextWait, Some(result))
                retryWithBackOff(currentAttempt + 1, nextWait, f, nextExecution)
              }
          },
            last = { () =>
              logger.info(s"[Session ID: ${Session.id(hc)}] last retry completed, attempt $currentAttempt, result: $lastExecution, time of each attempt: ${lastExecution.timeOfEachTick}")
              Future.successful(RetryExecution(lastExecution.ticks, Some(result)))
            }
          )
      case success =>
        logger.info(s"[Session ID: ${Session.id(hc)}] attempt completed, result did not require retry. $success, time of each attempt: ${lastExecution.timeOfEachTick}")
        Future.successful(RetryExecution(lastExecution.ticks, Some(success)))
    } recoverWith {
      case _ =>
        logger.error(s"[Session ID: ${Session.id(hc)}] attempt failed due to Future throwing a throwable")
        Future.successful(RetryExecution(lastExecution.ticks, None))
    }
  }
}

object RetryHelper extends Logging {

  case class RetryExecution(ticks: Seq[Int], result: Option[RetryPolicy]) {
    val totalTime: Int = ticks.sum

    def timeOfEachTick: Seq[Int] =
      if (ticks.isEmpty) Nil
      else ticks.drop(1).scanLeft(ticks.head)((acc, x) => acc + x)
  }

  def calculateWaitTime[T](maxAttempts: Int, waitFactor: Int, currentWait: Int, currentAttempt: Int)
                          (next: Int => Future[T], last: () => Future[T]): Future[T] = {
    if (currentAttempt < maxAttempts) {
      val wait = Math.ceil(currentWait * waitFactor).toInt
      logger.info(s"[RetryHelper] waiting for $wait milliseconds")
      next(wait)
    } else {
      logger.info(s"[RetryHelper] no more attempts left")
      last()
    }
  }
}

trait RetryPolicy {
  val retry : Boolean
}