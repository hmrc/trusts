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

package uk.gov.hmrc.trusts.connectors

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.trusts.connector.TaxEnrolmentConnector
import uk.gov.hmrc.trusts.exceptions.{BadRequestException, InternalServerErrorException}
import uk.gov.hmrc.trusts.models.TaxEnrolmentSuscriberResponse.Success
import uk.gov.hmrc.trusts.utils.WireMockHelper


class TaxEnrolmentConnectorSpec extends BaseConnectorSpec
  with GuiceOneAppPerSuite with WireMockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Seq("microservice.services.tax-enrolments.port" -> server.port(),
        "auditing.enabled" -> false): _*).build()

  lazy val connector: TaxEnrolmentConnector = app.injector.instanceOf[TaxEnrolmentConnector]

  ".enrolSubscriber" should {

    "return Success " when {
      "tax enrolments succesfully subscribed to provided subscription id" in {

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", 204)

        val futureResult = connector.enrolSubscriber("123456789")

        whenReady(futureResult) {
          result => result mustBe Success
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
