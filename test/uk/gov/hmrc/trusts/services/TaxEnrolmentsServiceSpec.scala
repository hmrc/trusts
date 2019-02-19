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
import uk.gov.hmrc.trusts.connector.{DesConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.exceptions.{AlreadyRegisteredException, BadRequestException, InternalServerErrorException}
import uk.gov.hmrc.trusts.models.ExistingTrustResponse.Matched
import uk.gov.hmrc.trusts.models.TaxEnrolmentSuscriberResponse
import uk.gov.hmrc.trusts.models.TaxEnrolmentSuscriberResponse.Success

import scala.concurrent.Future


class TaxEnrolmentsServiceSpec extends BaseSpec {

  val mockConnector = mock[TaxEnrolmentConnector]

  val SUT = new TaxEnrolmentsServiceImpl(mockConnector)


  ".setSubscriptionId" should {

    "return success taxEnrolmentSuscriberResponse " when {
      "connector returns success taxEnrolmentSuscriberResponse." in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.successful(Success))

        val futureResult = SUT.setSubscriptionId("123456789")

        whenReady(futureResult) {
          result => result mustBe Success
        }

      }
    }


    "return BadRequestException " when {
      "connector BadRequestException." in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.failed(new BadRequestException))

        val futureResult = SUT.setSubscriptionId("123456789")

        whenReady(futureResult.failed) {
          result => result mustBe an[BadRequestException]
        }

      }
    }

    "return InternalServerErrorException " when {
      "connector InternalServerErrorException." in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.failed(new InternalServerErrorException("")))

        val futureResult = SUT.setSubscriptionId("123456789")

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }

      }
    }
  }//setSubscriptionId

}
