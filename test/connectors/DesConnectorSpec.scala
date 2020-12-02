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

package connectors

import connector.DesConnector
import exceptions.{BadRequestException, InternalServerErrorException, NotFoundException, ServiceNotAvailableException}
import models.tax_enrolments.SubscriptionIdResponse
import play.api.http.Status._
import play.api.libs.json.Json

class DesConnectorSpec extends ConnectorSpecHelper {

  lazy val connector: DesConnector = injector.instanceOf[DesConnector]

  ".getSubscriptionId" should {

    "return subscription Id  " when {
      "valid trn has been submitted" in {
        val trn = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, OK, """{"subscriptionId": "987654321"}""")

        val futureResult = connector.getSubscriptionId(trn)


        whenReady(futureResult) {
          result => result mustBe SubscriptionIdResponse("987654321")
        }
      }
    }

    "return BadRequestException   " when {
      "invalid trn has been submitted" in {
        val trn = "invalidtrn"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, BAD_REQUEST, Json.stringify(jsonResponse400GetSubscriptionId))

        val futureResult = connector.getSubscriptionId(trn)


        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

    "return NotFoundException   " when {
      "trn submitted has no data in des " in {
        val trn = "notfoundtrn"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, NOT_FOUND, Json.stringify(jsonResponse404GetSubscriptionId))

        val futureResult = connector.getSubscriptionId(trn)


        whenReady(futureResult.failed) {
          result => result mustBe NotFoundException
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val trn = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.getSubscriptionId(trn)


        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val trn = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.getSubscriptionId(trn)


        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }
}
