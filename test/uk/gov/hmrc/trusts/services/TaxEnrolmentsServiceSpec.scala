/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.services

import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.connector.{DesConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.exceptions.{AlreadyRegisteredException, BadRequestException, InternalServerErrorException}
import uk.gov.hmrc.trusts.models.ExistingTrustResponse.Matched
import uk.gov.hmrc.trusts.models.{TaxEnrolmentFailure, TaxEnrolmentSuccess, TaxEnrolmentSuscriberResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class TaxEnrolmentsServiceSpec extends BaseSpec with GuiceOneServerPerSuite {

  val mockConnector = mock[TaxEnrolmentConnector]
  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val SUT = new TaxEnrolmentsServiceImpl(mockConnector,appConfig)


  ".setSubscriptionId" should {

    "return TaxEnrolmentSuccess  " when {
      "connector returns success taxEnrolmentSuscriberResponse." in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.setSubscriptionId("123456789")

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }

      }
    }


    "return TaxEnrolmentFailure " when {
      "tax enrolment returns internal server error." in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.failed(new InternalServerErrorException("")))
        val result = Await.result(SUT.setSubscriptionId("123456789"), Duration.Inf)
        result mustBe TaxEnrolmentFailure
      }
    }

    "return TaxEnrolmentFailure " when {
      "tax enrolment returns error" in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.failed(BadRequestException))

        val result = Await.result(SUT.setSubscriptionId("123456789"), Duration.Inf)
        result mustBe TaxEnrolmentFailure
      }

    }
  }//setSubscriptionId

}
