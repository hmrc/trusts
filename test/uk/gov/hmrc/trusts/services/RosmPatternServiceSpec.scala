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
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.models.{SubscriptionIdResponse, TaxEnrolmentFailure, TaxEnrolmentSuccess}
import uk.gov.hmrc.trusts.exceptions._

import scala.concurrent.Future


class RosmPatternServiceSpec extends BaseSpec {

  val mockDesService = mock[DesService]
  val mockTaxEnrolmentsService = mock[TaxEnrolmentsService]


  val SUT = new RosmPatternServiceImpl(mockDesService, mockTaxEnrolmentsService)

  ".completeRosmTransaction" should {

    "return success taxEnrolmentSuscriberResponse " when {
      "successfully sets subscriptionId id in tax enrolments for provided trn." in {
        when(mockDesService.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789")).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.completeRosmTransaction("trn123456789")

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    "return InternalServerErrorException " when {
      "des is down and not able to return subscription id." in {
        when(mockDesService.getSubscriptionId("trn123456789")).
          thenReturn(Future.failed(new InternalServerErrorException("")))
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789")).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.completeRosmTransaction("trn123456789")

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InternalServerErrorException " when {
      "tax enrolment service is down " in {
        when(mockDesService.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789")).
          thenReturn(Future.successful(TaxEnrolmentFailure))

        val futureResult = SUT.completeRosmTransaction("trn123456789")

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentFailure
        }
      }
    }
    "return BadRequestException " when {
      "tax enrolment service does not found provided subscription id." in {
        when(mockDesService.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789")).
          thenReturn(Future.failed(BadRequestException))

        val futureResult = SUT.completeRosmTransaction("trn123456789")

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

  }//completeRosmTransaction

}


