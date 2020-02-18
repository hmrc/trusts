/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.connectors

import org.scalatest.time.{Millis, Seconds, Span}
import uk.gov.hmrc.trusts.connector.TaxEnrolmentConnector
import uk.gov.hmrc.trusts.exceptions.{BadRequestException, InternalServerErrorException}
import uk.gov.hmrc.trusts.models.TaxEnrolmentSuccess

class TaxEnrolmentConnectorSpec extends BaseConnectorSpec {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(15, Millis))

  lazy val connector: TaxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]

  ".enrolSubscriber" should {

    "return Success " when {
      "tax enrolments succesfully subscribed to provided subscription id" in {

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", 204)

        val futureResult = connector.enrolSubscriber("123456789")

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    "return BadRequestException " when {
      "tax enrolments returns bad request " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 400)

        val futureResult = connector.enrolSubscriber("987654321")

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

    "return InternalServerErrorException " when {
      "tax enrolments returns internal server error " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 500)

        val futureResult = connector.enrolSubscriber("987654321")

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

}
