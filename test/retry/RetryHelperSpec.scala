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

import base.BaseSpec
import models.nonRepudiation.{BadGatewayResponse, InternalServerErrorResponse, SuccessfulNrsResponse}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetryHelperSpec extends BaseSpec with MockitoSugar with ScalaFutures with Matchers with OptionValues {

  val retryHelper = new RetryHelperClass()

  val TIMEOUT = 20

  "RetryHelper" must {

    "return a successful Future" in {
      val successfulFunction = () => Future.successful(SuccessfulNrsResponse("1234567890"))

      whenReady(retryHelper.retryOnFailure(successfulFunction, appConfig)) {
        e =>
          e.totalTime mustBe 0
          e.ticks mustBe Seq(0)
          e.result.value mustBe SuccessfulNrsResponse("1234567890")
      }
    }

    "retry when retry policy is true" in {
      val failedFunction = () => Future.successful(BadGatewayResponse)
      whenReady(retryHelper.retryOnFailure(failedFunction, appConfig), timeout(Span(TIMEOUT, Seconds))) {
        e =>
          e.totalTime mustBe 90
          e.ticks.size mustBe 10
          e.result.value mustBe BadGatewayResponse
      }
    }

    "back off exponentially" in {

      val failedFunction = () => Future.successful(InternalServerErrorResponse)

      whenReady(retryHelper.retryOnFailure(failedFunction, appConfig), timeout(Span(TIMEOUT, Seconds))) {
        e =>
          e.totalTime mustBe 90
          e.ticks.size mustBe 10
          e.result.value mustBe InternalServerErrorResponse
      }
    }

    "show that it can pass after it fails" in {

      val NUMBER_OF_RETRIES = 4
      var counter = 0

      val failThenSuccessFunc = () => {
        if (counter < NUMBER_OF_RETRIES) {
          counter = counter + 1
          Future.successful(InternalServerErrorResponse)
        } else {
          Future.successful(SuccessfulNrsResponse("1234567890"))
        }
      }

      whenReady(retryHelper.retryOnFailure(failThenSuccessFunc, appConfig), timeout(Span(TIMEOUT, Seconds))) {
        e => {
          e.totalTime mustBe 40
          e.ticks.size mustBe 5
          e.result.value mustBe SuccessfulNrsResponse("1234567890")
        }
      }
    }

//    "when using real config values take less than 20 seconds" ignore {
//      val app = new GuiceApplicationBuilder().build()
//      val config = app.injector.instanceOf[AppConfig]
//
//      val attempts = config.nrsRetryAttempts
//      val retryMs = config.nrsRetryWaitMs
//      val retryWaitFactor = config.nrsRetryWaitFactor
//
//      val expectedTime = calculateDuration(1, attempts, retryWaitFactor, retryMs)
//
//      expectedTime must be < 20.seconds.toMillis
//    }
  }
}

class RetryHelperClass extends RetryHelper
