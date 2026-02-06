/*
 * Copyright 2026 HM Revenue & Customs
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

import connector.SubscriptionConnector
import errors.ServerError
import models.tax_enrolments.SubscriptionIdSuccessResponse
import org.scalatest.matchers.must.Matchers._
import play.api.http.Status._
import play.api.libs.json.Json

class SubscriptionConnectorSpec extends ConnectorSpecHelper {

  private lazy val connector: SubscriptionConnector = injector.instanceOf[SubscriptionConnector]

  ".getSubscriptionId" should {

    "return subscription Id  " when {
      "valid trn has been submitted" in {
        val trn                       = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, OK, """{"subscriptionId": "987654321"}""")

        val futureResult = connector.getSubscriptionId(trn).value

        whenReady(futureResult) { result =>
          result mustBe Right(SubscriptionIdSuccessResponse("987654321"))
        }
      }
    }

    "return ServerError(message)" when {
      "invalid trn has been submitted (Bad Request)" in {
        val trn                       = "invalidtrn"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, BAD_REQUEST, Json.stringify(jsonResponse400GetSubscriptionId))

        val futureResult = connector.getSubscriptionId(trn).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Bad request"))
        }
      }

      "trn submitted has no data in des (Not Found)" in {
        val trn                       = "notfoundtrn"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, NOT_FOUND, Json.stringify(jsonResponse404GetSubscriptionId))

        val futureResult = connector.getSubscriptionId(trn).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Not found"))
        }
      }

      "des dependent service is not responding (Service Unavailable)" in {
        val trn                       = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.getSubscriptionId(trn).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Des dependent service is down."))
        }
      }

      "des is experiencing some problem (Internal ServerError)" in {
        val trn                       = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.getSubscriptionId(trn).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError(s"Error response from des $INTERNAL_SERVER_ERROR"))
        }
      }

      "unexpected error response from des" in {
        val trn                       = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, FORBIDDEN, Json.stringify(jsonResponse500))

        val futureResult = connector.getSubscriptionId(trn).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError(s"Error response from des $FORBIDDEN"))
        }
      }
    }
  }

}
