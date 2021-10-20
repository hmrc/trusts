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

import base.BaseSpec
import config.AppConfig
import models.nonRepudiation.{BadGatewayResponse, InternalServerErrorResponse, SuccessfulNrsResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class RetryHelperSpec extends BaseSpec with MockitoSugar with ScalaFutures with Matchers {

  private val MAX_ATTEMPTS: Int = 10
  private val INITIAL_WAIT: Int = 10
  private val WAIT_FACTOR: Int = 1

  val retryHelper = new RetryHelperClass()
  val TIMEOUT = 5

  "RetryHelper" must {

    "return a successful Future" in {
      val successfulFunction = () => Future.successful(SuccessfulNrsResponse("1234567890"))

      whenReady(retryHelper.retryOnFailure(successfulFunction, appConfig)) {
        result => result mustEqual SuccessfulNrsResponse("1234567890")
      }
    }

    "retry when retry policy is true" in {
      val failedFunction = () => Future.successful(BadGatewayResponse)
      whenReady(retryHelper.retryOnFailure(failedFunction, appConfig), timeout(Span(TIMEOUT, Seconds))) {
        e => e mustEqual BadGatewayResponse
      }
    }

    "back off exponentially" in {

      val failedFunction = () => Future.successful(InternalServerErrorResponse)
      val startTime = LocalDateTime.now

      whenReady(retryHelper.retryOnFailure(failedFunction, appConfig), timeout(Span(TIMEOUT, Seconds))) {
        e => e mustEqual InternalServerErrorResponse
      }

      val endTime = LocalDateTime.now
      val expectedTime = calculateDuration(1, MAX_ATTEMPTS, WAIT_FACTOR, INITIAL_WAIT)
      ChronoUnit.MILLIS.between(startTime, endTime) must be >= expectedTime
    }

    "show that it can pass after it fails" in {

      val NUMBER_OF_RETRIES = 5
      var counter = 0

      val failThenSuccessFunc = () => {
        if (counter < NUMBER_OF_RETRIES) {
          counter = counter + 1
          Future.successful(InternalServerErrorResponse)
        }
        else {
          Future.successful(SuccessfulNrsResponse("1234567890"))
        }
      }

      whenReady(retryHelper.retryOnFailure(failThenSuccessFunc, appConfig), timeout(Span(TIMEOUT, Seconds))) {
        result => {
          result mustEqual SuccessfulNrsResponse("1234567890")
          counter must be >= NUMBER_OF_RETRIES
        }
      }
    }

    "when using real config values take less than 20 seconds" in {
      val app = new GuiceApplicationBuilder().build()
      val config = app.injector.instanceOf[AppConfig]

      val attempts = config.nrsRetryAttempts
      val retryMs = config.nrsRetryWaitMs
      val retryWaitFactor = config.nrsRetryWaitFactor

      val expectedTime = calculateDuration(1, attempts, retryWaitFactor, retryMs)

      expectedTime must be < 20.seconds.toMillis
    }
  }
}

class RetryHelperClass extends RetryHelper
