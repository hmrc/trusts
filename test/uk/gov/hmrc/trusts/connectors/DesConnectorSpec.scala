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
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.exceptions.{AlreadyRegisteredException, _}
import uk.gov.hmrc.trusts.models.ExistingCheckRequest._
import uk.gov.hmrc.trusts.models.ExistingCheckResponse._
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.utils.WireMockHelper

class DesConnectorSpec extends BaseConnectorSpec
  with GuiceOneAppPerSuite with WireMockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Seq("microservice.services.des-trusts.port" -> server.port(),
        "microservice.services.des-estates.port" -> server.port(),
        "auditing.enabled" -> false): _*).build()


  lazy val connector: DesConnector = app.injector.instanceOf[DesConnector]

  lazy val request = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")


  ".checkExistingTrust" should {

    "return Matched " when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, 200, """{"match": true}""")

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe Matched
        }

      }
    }
    "return NotMatched " when {
      "trusts data does not with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, 200, """{"match": false}""")

        val futureResult = connector.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "payload sent is not valid" in {
        val wrongPayloadRequest = request.copy(utr = "NUMBER1234")
        val requestBody = Json.stringify(Json.toJson(wrongPayloadRequest))

        stubForPost(server, "/trusts/match", requestBody, 400, Json.stringify(jsonResponse400))

        val futureResult = connector.checkExistingTrust(wrongPayloadRequest)

        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, 409, Json.stringify(jsonResponseAlreadyRegistered))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, 503, Json.stringify(jsonResponse503))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }
      }
    }

    "return ServerError " when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, 500, Json.stringify(jsonResponse500))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }

    "return ServerError " when {
      "des is returning forbidden response" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, 409, "{}")

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }


  ".registerTrust" should {

    "return TRN  " when {
      "valid request to des register trust." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, 200, """{"trn": "XTRN1234567"}""")

        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("XTRN1234567")
        }

      }
    }

    "return BadRequestException  " when {
      "payload sent to des is invalid" in {
        val requestBody = Json.stringify(Json.toJson(invalidRegistrationRequest))
        stubForPost(server, "/trusts/registration", requestBody, 400, Json.stringify(jsonResponse400))

        val futureResult = connector.registerTrust(invalidRegistrationRequest)


        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }

      }
    }

    "return AlreadyRegisteredException  " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, 403, Json.stringify(jsonResponseAlreadyRegistered))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return NoMatchException  " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, 403, Json.stringify(jsonResponse403NoMatch))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe NoMatchException
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))
        stubForPost(server, "/trusts/registration", requestBody, 503, Json.stringify(jsonResponse503))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, 500, Json.stringify(jsonResponse500))

        val futureResult = connector.registerTrust(registrationRequest)
        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is returning 403 without ALREADY REGISTERED code." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, 403, "{}")
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  } //registerTrust


  ".registerEstate" should {

    "return TRN  " when {
      "valid request to des register an estate." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, 200, """{"trn": "XTRN1234567"}""")

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("XTRN1234567")
        }

      }
    }

    "return BadRequestException  " when {
      "payload sent to des is invalid" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))
        stubForPost(server, "/estates/registration", requestBody, 400, Json.stringify(jsonResponse400))

        val futureResult = connector.registerEstate(estateRegRequest)


        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }

      }
    }

    "return AlreadyRegisteredException  " when {
      "estates is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, 403, Json.stringify(jsonResponseAlreadyRegistered))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return NoMatchException  " when {
      "estates is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, 403, Json.stringify(jsonResponse403NoMatch))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe NoMatchException
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))
        stubForPost(server, "/estates/registration", requestBody, 503, Json.stringify(jsonResponse503))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, 500, Json.stringify(jsonResponse500))

        val futureResult = connector.registerEstate(estateRegRequest)
        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is returning 403 without ALREADY REGISTERED code." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, 403, "{}")
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  } //registerEstate

  ".getSubscriptionId" should {

    "return subscription Id  " when {
      "valid trn has been submitted" in {
        val trn = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl,  200, """{"subscriptionId": "987654321"}""")

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
        stubForGet(server, subscriptionIdEndpointUrl,  400,Json.stringify(jsonResponse400GetSubscriptionId))

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
        stubForGet(server, subscriptionIdEndpointUrl,  404 ,Json.stringify(jsonResponse404GetSubscriptionId))

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
        stubForGet(server, subscriptionIdEndpointUrl,  503 ,Json.stringify(jsonResponse503))

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
        stubForGet(server, subscriptionIdEndpointUrl,  500 ,Json.stringify(jsonResponse500))

        val futureResult = connector.getSubscriptionId(trn)
        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }//getSubscriptionId

}

