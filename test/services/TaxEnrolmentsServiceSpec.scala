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

package services

import base.BaseSpec
import connector.TaxEnrolmentConnector
import exceptions.{BadRequestException, InternalServerErrorException}
import models.tax_enrolments.{TaxEnrolmentFailure, TaxEnrolmentSuccess}
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when, _}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TaxEnrolmentsServiceSpec extends BaseSpec {

  lazy val mockConnector = mock[TaxEnrolmentConnector]

  lazy val SUT = new TaxEnrolmentsServiceImpl(mockConnector,appConfig)

  before {
    reset(mockConnector)
  }

  ".setSubscriptionId" should {

    val taxable: Boolean = false
    val trn = "XTRN1234567"

    "return TaxEnrolmentSuccess  " when {
      "connector returns success taxEnrolmentSubscriberResponse." in {
        when(mockConnector.enrolSubscriber("123456789", taxable, trn)).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.setSubscriptionId("123456789", taxable, trn)

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
        verify(mockConnector, times(1)).enrolSubscriber(any(), any(), any())(any[HeaderCarrier])
      }
    }

    "return TaxEnrolmentFailure " when {
      "tax enrolment returns internal server error." in {
        when(mockConnector.enrolSubscriber("123456789", taxable, trn)).
          thenReturn(Future.failed(new InternalServerErrorException("")))
        val result = Await.result(SUT.setSubscriptionId("123456789", taxable, trn), Duration.Inf)
        result mustBe TaxEnrolmentFailure
        verify(mockConnector, times(10)).enrolSubscriber(any(), any(), any())(any[HeaderCarrier])
      }
    }

    "return TaxEnrolmentFailure " when {
      "tax enrolment returns error" in {
        when(mockConnector.enrolSubscriber("123456789", taxable, trn)).
          thenReturn(Future.failed(BadRequestException))

        val result = Await.result(SUT.setSubscriptionId("123456789", taxable, trn), Duration.Inf)
        result mustBe TaxEnrolmentFailure
        verify(mockConnector, times(10)).enrolSubscriber(any(), any(), any())(any[HeaderCarrier])
      }

    }
  }

}
