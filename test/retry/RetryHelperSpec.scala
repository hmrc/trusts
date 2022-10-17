/*
 * Copyright 2022 HM Revenue & Customs
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
import models.nonRepudiation.NRSResponse
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetryHelperSpec extends BaseSpec with MockitoSugar with ScalaFutures with Matchers with OptionValues {

  private val retryHelper: NrsRetryHelper = injector.instanceOf[NrsRetryHelper]

  private val TIMEOUT: Int = 20

  "RetryHelper" must {

    "return a successful Future" in {
      val successfulFunction = () => Future.successful(NRSResponse.Success("1234567890"))

      whenReady(retryHelper.retryOnFailure(
        successfulFunction
      )) {
        e =>
          e.totalTime mustBe 0
          e.ticks mustBe Seq(0)
          e.result.value mustBe NRSResponse.Success("1234567890")
      }
    }

    "retry when retry policy is true" in {
      val failedFunction = () => Future.successful(NRSResponse.BadGateway)
      whenReady(retryHelper.retryOnFailure(
        failedFunction
      ), timeout(Span(TIMEOUT, Seconds))) {
        e =>
          e.totalTime mustBe 90
          e.ticks.size mustBe 10
          e.result.value mustBe NRSResponse.BadGateway
      }
    }

    "back off exponentially" in {

      val failedFunction = () => Future.successful(NRSResponse.InternalServerError)

      whenReady(retryHelper.retryOnFailure(
        failedFunction
      ), timeout(Span(TIMEOUT, Seconds))) {
        e =>
          e.totalTime mustBe 90
          e.ticks.size mustBe 10
          e.result.value mustBe NRSResponse.InternalServerError
      }
    }

    "recover from a throwable" in {

      val failedFunction = () => Future.failed(new RuntimeException("ran out of memory"))

      whenReady(retryHelper.retryOnFailure(
        failedFunction
      ), timeout(Span(TIMEOUT, Seconds))) {
        e =>
          e.totalTime mustBe 0
          e.ticks.size mustBe 1
          e.result must not be defined
      }
    }

    "show that it can pass after it fails" in {

      val NUMBER_OF_RETRIES = 4
      var counter = 0

      val failThenSuccessFunc = () => {
        if (counter < NUMBER_OF_RETRIES) {
          counter = counter + 1
          Future.successful(NRSResponse.InternalServerError)
        } else {
          Future.successful(NRSResponse.Success("1234567890"))
        }
      }

      whenReady(retryHelper.retryOnFailure(
        failThenSuccessFunc
      ), timeout(Span(TIMEOUT, Seconds))) {
        e => {
          e.totalTime mustBe 40
          e.ticks.size mustBe 5
          e.result.value mustBe NRSResponse.Success("1234567890")
        }
      }
    }

    "when using real config values take less than 20 seconds" in {
      val app = new GuiceApplicationBuilder().build()
      val helper = app.injector.instanceOf[NrsRetryHelper]

      val failedFunction = () => Future.successful(NRSResponse.BadGateway)
      whenReady(helper.retryOnFailure(
        failedFunction
      ), timeout(Span(TIMEOUT, Seconds))) {
        e =>
          e.totalTime mustBe 15600
          e.timeOfEachTick mustBe Seq(0, 1200, 4800, 15600)
          e.result.value mustBe NRSResponse.BadGateway
      }
    }
  }
}
