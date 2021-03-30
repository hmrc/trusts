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

package connectors

import connector.TaxEnrolmentConnector
import exceptions.{BadRequestException, InternalServerErrorException, ServiceNotAvailableException}
import models.tax_enrolments.{TaxEnrolmentSubscription, TaxEnrolmentSuccess, TaxEnrolmentsSubscriptionsResponse}
import play.api.http.Status._
import play.api.libs.json.Json

class TaxEnrolmentConnectorSpec extends ConnectorSpecHelper {

  private lazy val connector: TaxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]

  ".getTaxEnrolmentSubscription" when {

    "taxable" should {

      "return correct config" in {
        val result = connector.getTaxEnrolmentSubscription("123456789", taxable = true, "trn")
        result mustBe TaxEnrolmentSubscription(
          "serviceNameTaxable",
          "http://localhost:9782/trusts/tax-enrolment/registration/taxable/hmrc-ters-org/trn/subscriptions",
          "123456789"
        )
      }

    }

    "non-taxable" should {

      "return correct config" in {
        val result = connector.getTaxEnrolmentSubscription("123456789", taxable = false, "trn")
        result mustBe TaxEnrolmentSubscription(
          "serviceNameNonTaxable",
          "http://localhost:9782/trusts/tax-enrolment/registration/non-taxable/hmrc-tersnt-org/trn/subscriptions",
          "123456789"
        )
      }

    }

  }

  ".enrolSubscriber" should {

    val taxable: Boolean = true
    val trn = "XTRN1234567"

    "return Success" when {
      "tax enrolments successfully subscribed to provided subscription id" in {

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", 204)

        val futureResult = connector.enrolSubscriber("123456789", taxable, trn)

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    "return BadRequestException " when {
      "tax enrolments returns bad request " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 400)

        val futureResult = connector.enrolSubscriber("987654321", taxable, trn)

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

    "return InternalServerErrorException " when {
      "tax enrolments returns internal server error " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 500)

        val futureResult = connector.enrolSubscriber("987654321", taxable, trn)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".migrateSubscriberToTaxable" should {

    val urn = "NTTRUST00000001"

    "return Success" when {
      "tax enrolments successfully subscribed to provided subscription id" in {

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", 204)

        val futureResult = connector.migrateSubscriberToTaxable("123456789", urn)

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    "return BadRequestException " when {
      "tax enrolments returns bad request " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 400)

        val futureResult = connector.migrateSubscriberToTaxable("987654321", urn)

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

    "return InternalServerErrorException " when {
      "tax enrolments returns internal server error " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 500)

        val futureResult = connector.migrateSubscriberToTaxable("987654321", urn)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".subscriptions" should {

    val urn = "NTTRUST00000001"
    val utr = "123456789"
    val subscriptionId = "987654321"

    "return Success" when {

      val response = Json.parse(
        s"""
           |{
           |   "created": 1482329348256,
           |    "lastModified": 1482329348256,
           |    "credId": "d8474a25-71b6-45ed-859e-77dd5f087be6",
           |    "serviceName": "HMRC-TERS-ORG",
           |    "identifiers": [{
           |        "key": "SAUTR",
           |        "value": "$utr"
           |    }],
           |    "callback": "http://trusts.protected.mdtp/tax-enrolments/migration-to-taxable/urn/$urn/subscriptionId/$subscriptionId",
           |    "state": "PROCESSED",
           |    "etmpId": "da4053bf-2ea3-4cb8-bb9c-65b70252b656",
           |    "groupIdentifier": "c808798d-0d81-4a34-82c2-bbf13b3ac2fa"
           |}
           |""".stripMargin)

      "tax enrolments successfully returned for provided subscription id" in {

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", OK, response.toString())

        val futureResult = connector.subscriptions(subscriptionId)

        whenReady(futureResult) {
          result => {
            result mustBe TaxEnrolmentsSubscriptionsResponse(subscriptionId, utr, "PROCESSED")
          }
        }
      }

      "return ServiceNotAvailableException " when {
        "tax enrolments returns service not available error " in {

          stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", SERVICE_UNAVAILABLE, response.toString())

          val futureResult = connector.subscriptions(subscriptionId)

          whenReady(futureResult.failed) {
            result => result mustBe an[ServiceNotAvailableException]
          }
        }
      }
    }
  }
}
