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

package utils

import akka.actor.ActorSystem
import akka.pattern.Patterns.after
import config.AppConfig
import play.api.Logging
import uk.gov.hmrc.http.HttpErrorFunctions.is5xx
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.util.concurrent.Callable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait RetryHelper extends Logging {

  val as: ActorSystem = ActorSystem()

  def retryOnFailure[T](f: () => Future[T], config: AppConfig)(implicit ec: ExecutionContext): Future[T] = {
    retryWithBackOff(1, config.nrsRetryWaitMs, f, config)
  }

  private def retryWithBackOff[T](currentAttempt: Int,
                                  currentWait: Int,
                                  f: () => Future[T], config: AppConfig)(implicit ec: ExecutionContext): Future[T] = {
    f.apply().recoverWith {
      case e: UpstreamErrorResponse if is5xx(e.statusCode) =>
        if (currentAttempt < config.nrsRetryAttempts) {
          val wait = Math.ceil(currentWait * config.nrsRetryWaitFactor).toInt
          logger.warn(s"Failure, retrying after $wait ms, attempt $currentAttempt")
          after(
            duration = wait.milliseconds,
            scheduler = as.scheduler,
            context = ec,
            value = new Callable[Future[Int]] {
              override def call(): Future[Int] = Future.successful(1)
            }
          ).flatMap { _ =>
            retryWithBackOff(currentAttempt + 1, wait.toInt, f, config)
          }
        } else {
          Future.failed(e)
        }
      case e =>
        Future.failed(e)
    }
  }
}