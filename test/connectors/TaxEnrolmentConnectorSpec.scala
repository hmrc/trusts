/*
 * Copyright 2023 HM Revenue & Customs
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
import errors.ServerError
import models.tax_enrolments.{TaxEnrolmentSubscription, TaxEnrolmentSuccess, TaxEnrolmentsSubscriptionsSuccessResponse}
import org.scalatest.matchers.must.Matchers._
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

    "return success response" when {
      "tax enrolments successfully subscribed to provided subscription id" in {

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", 204)

        val futureResult = connector.enrolSubscriber("123456789", taxable, trn).value

        whenReady(futureResult) { result =>
          result mustBe Right(TaxEnrolmentSuccess)
        }
      }
    }

    "return ServerError(message) " when {
      "tax enrolments returns bad request " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", BAD_REQUEST)

        val futureResult = connector.enrolSubscriber("987654321", taxable, trn).value

        whenReady(futureResult) {
          result => result mustBe Left(ServerError("Bad request"))
        }
      }

      "tax enrolments returns internal server error " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", INTERNAL_SERVER_ERROR)

        val futureResult = connector.enrolSubscriber("987654321", taxable, trn).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Unexpected error response from tax enrolment. Status: 500"))
        }
      }
    }
  }

  ".migrateSubscriberToTaxable" should {

    val urn = "NTTRUST00000001"

    "return success response" when {
      "tax enrolments successfully subscribed to provided subscription id" in {

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", NO_CONTENT)

        val futureResult = connector.migrateSubscriberToTaxable("123456789", urn).value

        whenReady(futureResult) { result =>
          result mustBe Right(TaxEnrolmentSuccess)
        }
      }
    }

    "return ServerError(message) " when {

      "tax enrolments returns bad request" in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", BAD_REQUEST)

        val futureResult = connector.migrateSubscriberToTaxable("987654321", urn).value

        whenReady(futureResult) {
          result => result mustBe Left(ServerError("Bad request"))
        }
      }

      "tax enrolments returns internal server error" in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", INTERNAL_SERVER_ERROR)

        val futureResult = connector.migrateSubscriberToTaxable("987654321", urn).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Unexpected error response from tax enrolment. Status: 500"))
        }
      }
    }
  }

  ".subscriptions" should {

    val urn = "NTTRUST00000001"
    val utr = "123456789"
    val subscriptionId = "987654321"

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

    "return success response" when {

      "tax enrolments successfully returned for provided subscription id" in {

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", OK, response.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        whenReady(futureResult) { result =>
          result mustBe Right(TaxEnrolmentsSubscriptionsSuccessResponse(subscriptionId, utr, "PROCESSED"))
        }
      }
    }

    "return ServerError(message)" when {

      "an exception is recovered" in {
        val invalidJsonResponse = Json.parse(
          s"""
             |{
             |   "created": 1482329348256,
             |    "lastModified": 1482329348256,
             |    "credId": "d8474a25-71b6-45ed-859e-77dd5f087be6",
             |    "serviceName": "HMRC-TERS-ORG",
             |    "identifiers": [{
             |        "key": "SAUTR"
             |    }],
             |    "etmpId": "da4053bf-2ea3-4cb8-bb9c-65b70252b656",
             |    "groupIdentifier": "c808798d-0d81-4a34-82c2-bbf13b3ac2fa"
             |}
             |""".stripMargin)

        val endpoint = s"/tax-enrolments/subscriptions/$subscriptionId"

        stubForHeaderlessGet(server, endpoint, OK, invalidJsonResponse.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        val url = server.url(endpoint)
        val exceptionMessage = "JsResultException(errors:List(((0)/value,List(JsonValidationError(List(error.path.missing),ArraySeq())))))"

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError(s"Error occurred when calling $url with exception $exceptionMessage"))
        }
      }

      "OK response but no state supplied" in {
        val responseWithNoState = Json.parse(
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
             |    "etmpId": "da4053bf-2ea3-4cb8-bb9c-65b70252b656",
             |    "groupIdentifier": "c808798d-0d81-4a34-82c2-bbf13b3ac2fa"
             |}
             |""".stripMargin)

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", OK, responseWithNoState.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError(s"[SubscriptionId: $subscriptionId, UTR: $utr] InvalidDataErrorResponse - No State supplied"))
        }
      }

      "OK response but no UTR supplied" in {
        val responseWithNoUTR = Json.parse(
          s"""
             |{
             |   "created": 1482329348256,
             |    "lastModified": 1482329348256,
             |    "credId": "d8474a25-71b6-45ed-859e-77dd5f087be6",
             |    "serviceName": "HMRC-TERS-ORG",
             |    "identifiers": [],
             |    "callback": "http://trusts.protected.mdtp/tax-enrolments/migration-to-taxable/urn/$urn/subscriptionId/$subscriptionId",
             |    "state": "PROCESSED",
             |    "etmpId": "da4053bf-2ea3-4cb8-bb9c-65b70252b656",
             |    "groupIdentifier": "c808798d-0d81-4a34-82c2-bbf13b3ac2fa"
             |}
             |""".stripMargin)

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", OK, responseWithNoUTR.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError(s"[SubscriptionId: $subscriptionId] InvalidDataErrorResponse - No UTR supplied"))
        }
      }

      "OK response but no state or UTR supplied" in {
        val responseWithNoStateOrUtr = Json.parse(
          s"""
             |{
             |   "created": 1482329348256,
             |    "lastModified": 1482329348256,
             |    "credId": "d8474a25-71b6-45ed-859e-77dd5f087be6",
             |    "serviceName": "HMRC-TERS-ORG",
             |    "identifiers": [],
             |    "callback": "http://trusts.protected.mdtp/tax-enrolments/migration-to-taxable/urn/$urn/subscriptionId/$subscriptionId",
             |    "etmpId": "da4053bf-2ea3-4cb8-bb9c-65b70252b656",
             |    "groupIdentifier": "c808798d-0d81-4a34-82c2-bbf13b3ac2fa"
             |}
             |""".stripMargin)

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", OK, responseWithNoStateOrUtr.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError(s"[SubscriptionId: $subscriptionId] InvalidDataErrorResponse - No State or UTR supplied"))
        }
      }

      "des returns a bad request response" in {

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", BAD_REQUEST, response.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Bad request"))
        }
      }

      "des returns a not found response" in {

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", NOT_FOUND, response.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Not found"))
        }
      }

      "des returns a service not available error response" in {

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", SERVICE_UNAVAILABLE, response.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Des service is down."))
        }
      }

      "des returns an unexpected error response" in {

        stubForHeaderlessGet(server, s"/tax-enrolments/subscriptions/$subscriptionId", TOO_MANY_REQUESTS, response.toString())

        val futureResult = connector.subscriptions(subscriptionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError(s"Error response from des $TOO_MANY_REQUESTS"))
        }
      }
    }
  }

}
