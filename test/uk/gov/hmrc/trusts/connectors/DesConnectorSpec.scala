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
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.exceptions.{AlreadyRegisteredException, _}
import uk.gov.hmrc.trusts.models.ExistingCheckRequest._
import uk.gov.hmrc.trusts.models.ExistingCheckResponse._
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.utils.WireMockHelper
import play.api.http.Status._
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.EstateFoundResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._

class DesConnectorSpec extends BaseConnectorSpec
  with GuiceOneAppPerSuite with WireMockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Seq("microservice.services.des-trusts.port" -> server.port(),
        "microservice.services.des-estates.port" -> server.port(),
        "auditing.enabled" -> false): _*).build()


  lazy val connector: DesConnector = app.injector.instanceOf[DesConnector]

  lazy val request = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")

  def createTrustOrEstateEndpoint(utr: String) = s"/trusts/registration/$utr"

  ".checkExistingTrust" should {

    "return Matched " when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, OK, """{"match": true}""")

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe Matched
        }

      }
    }
    "return NotMatched " when {
      "trusts data does not with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, OK, """{"match": false}""")

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

        stubForPost(server, "/trusts/match", requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

        val futureResult = connector.checkExistingTrust(wrongPayloadRequest)

        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, CONFLICT, Json.stringify(jsonResponseAlreadyRegistered))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }
      }
    }

    "return ServerError " when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }

    "return ServerError " when {
      "des is returning forbidden response" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, CONFLICT, "{}")

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }//trustsmatch


  ".checkExistingEstate" should {

    "return Matched " when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, OK, """{"match": true}""")

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe Matched
        }

      }
    }
    "return NotMatched " when {
      "trusts data does not with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, OK, """{"match": false}""")

        val futureResult = connector.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "payload sent is not valid" in {
        val wrongPayloadRequest = request.copy(utr = "NUMBER1234")
        val requestBody = Json.stringify(Json.toJson(wrongPayloadRequest))

        stubForPost(server, "/estates/match", requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

        val futureResult = connector.checkExistingEstate(wrongPayloadRequest)

        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, CONFLICT, Json.stringify(jsonResponseAlreadyRegistered))

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }
      }
    }

    "return ServerError " when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }

    "return ServerError " when {
      "des is returning forbidden response" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, CONFLICT, "{}")

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }//estatematch


  ".registerTrust" should {

    "return TRN  " when {
      "valid request to des register trust." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, OK, """{"trn": "XTRN1234567"}""")

        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("XTRN1234567")
        }

      }
    }

    "return BadRequestException  " when {
      "payload sent to des is invalid" in {
        val requestBody = Json.stringify(Json.toJson(invalidRegistrationRequest))
        stubForPost(server, "/trusts/registration", requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

        val futureResult = connector.registerTrust(invalidRegistrationRequest)


        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }

      }
    }

    "return AlreadyRegisteredException  " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, FORBIDDEN, Json.stringify(jsonResponseAlreadyRegistered))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return NoMatchException  " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, FORBIDDEN, Json.stringify(jsonResponse403NoMatch))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe NoMatchException
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))
        stubForPost(server, "/trusts/registration", requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.registerTrust(registrationRequest)
        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is returning 403 without ALREADY REGISTERED code." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, FORBIDDEN, "{}")
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

        stubForPost(server, "/estates/registration", requestBody, OK, """{"trn": "XTRN1234567"}""")

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("XTRN1234567")
        }

      }
    }

    "return BadRequestException  " when {
      "payload sent to des is invalid" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))
        stubForPost(server, "/estates/registration", requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

        val futureResult = connector.registerEstate(estateRegRequest)


        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }

      }
    }

    "return AlreadyRegisteredException  " when {
      "estates is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, FORBIDDEN, Json.stringify(jsonResponseAlreadyRegistered))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return NoMatchException  " when {
      "estates is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, FORBIDDEN, Json.stringify(jsonResponse403NoMatch))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe NoMatchException
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))
        stubForPost(server, "/estates/registration", requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.registerEstate(estateRegRequest)
        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is returning 403 without ALREADY REGISTERED code." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, FORBIDDEN, "{}")
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
        stubForGet(server, subscriptionIdEndpointUrl,  OK, """{"subscriptionId": "987654321"}""")

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
        stubForGet(server, subscriptionIdEndpointUrl,  BAD_REQUEST,Json.stringify(jsonResponse400GetSubscriptionId))

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
        stubForGet(server, subscriptionIdEndpointUrl,  NOT_FOUND ,Json.stringify(jsonResponse404GetSubscriptionId))

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
        stubForGet(server, subscriptionIdEndpointUrl,  SERVICE_UNAVAILABLE ,Json.stringify(jsonResponse503))

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
        stubForGet(server, subscriptionIdEndpointUrl,  INTERNAL_SERVER_ERROR ,Json.stringify(jsonResponse500))

        val futureResult = connector.getSubscriptionId(trn)
        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }//getSubscriptionId

  ".getTrustInfo" should {

    "return TrustFoundResponse" when {
      "des has returned a 200 with trust details" in {
        val utr = "1234567890"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustResponseJson)

        val futureResult = connector.getTrustInfo(utr)
        whenReady(futureResult) { result =>
          val expectedResult = Json.fromJson[TrustFoundResponse](getTrustResponse) match {
            case JsSuccess(data, _) => data
            case _ => fail("Json could not be parsed to TrustFoundResponse model")
          }
          result mustBe expectedResult
        }
      }

      "des has returned a 200 and indicated that the submission is still being processed" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateNoDetailsResponseJson)

        val futureResult = connector.getTrustInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe TrustFoundResponse(None, ResponseHeader("In Processing", 1))
        }
      }
    }

    "return InvalidUTRResponse" when {
      "des has returned a 400 with the code INVALID_UTR" in {
        val invalidUTR = "123456789"
        stubForGet(server, createTrustOrEstateEndpoint(invalidUTR), BAD_REQUEST,
                   Json.stringify(jsonResponse400InvalidUTR))

        val futureResult = connector.getTrustInfo(invalidUTR)
        whenReady(futureResult) { result =>
          result mustBe InvalidUTRResponse
        }
      }
    }

    "return InvalidRegimeResponse" when {
      "des has returned a 400 with the code INVALID_REGIME" in {
        val utr = "1234567891"
        stubForGet(server, createTrustOrEstateEndpoint(utr), BAD_REQUEST,
                   Json.stringify(jsonResponse400InvalidRegime))

        val futureResult = connector.getTrustInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe InvalidRegimeResponse
        }
      }
    }

    "return BadRequestResponse" when {
      "des has returned a 400 with a code which is not INVALID_UTR OR INVALID_REGIME" in {
        val utr = "1234567891"
        stubForGet(server, createTrustOrEstateEndpoint(utr), BAD_REQUEST,
          Json.stringify(jsonResponse400))

        val futureResult = connector.getTrustInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {
      "des has returned a 404" in {
        val utr = "1234567892"
        stubForGet(server, createTrustOrEstateEndpoint(utr), NOT_FOUND, "")

        val futureResult = connector.getTrustInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return InternalServerErrorResposne" when {
      "des has returned a 500 with the code SERVER_ERROR" in {
        val utr = "1234567893"
        stubForGet(server, createTrustOrEstateEndpoint(utr), INTERNAL_SERVER_ERROR, "")

        val futureResult = connector.getTrustInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {
      "des has returned a 503 with the code SERVICE_UNAVAILABLE" in {
        val utr = "1234567894"
        stubForGet(server, createTrustOrEstateEndpoint(utr), SERVICE_UNAVAILABLE, "")

        val futureResult = connector.getTrustInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  }//getTrustInfo

  ".getEstateInfo" should {

    "return EstateFoundResponse" when {
      "des has returned a 200 with trust details" in {
        val utr = "1234567890"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getEstateResponseJson)

        val futureResult = connector.getEstateInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe Json.fromJson[EstateFoundResponse](getEstateResponse)
        }
      }

      "des has returned a 200 and indicated that the submission is still being processed" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateNoDetailsResponseJson)

        val futureResult = connector.getEstateInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("In Processing", 1))
        }
      }
    }

    "return InvalidUTRResponse" when {
      "des has returned a 400 with the code INVALID_UTR" in {
        val invalidUTR = "123456789"
        stubForGet(server, createTrustOrEstateEndpoint(invalidUTR), BAD_REQUEST,
          Json.stringify(jsonResponse400InvalidUTR))

        val futureResult = connector.getEstateInfo(invalidUTR)
        whenReady(futureResult) { result =>
          result mustBe InvalidUTRResponse
        }
      }
    }

    "return InvalidRegimeResponse" when {
      "des has returned a 400 with the code INVALID_REGIME" in {
        val utr = "1234567891"
        stubForGet(server, createTrustOrEstateEndpoint(utr), BAD_REQUEST,
          Json.stringify(jsonResponse400InvalidRegime))

        val futureResult = connector.getEstateInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe InvalidRegimeResponse
        }
      }
    }

    "return BadRequestResponse" when {
      "des has returned a 400 with a code which is not INVALID_UTR OR INVALID_REGIME" in {
        val utr = "1234567891"
        stubForGet(server, createTrustOrEstateEndpoint(utr), BAD_REQUEST,
          Json.stringify(jsonResponse400))

        val futureResult = connector.getEstateInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {
      "des has returned a 404" in {
        val utr = "1234567892"
        stubForGet(server, createTrustOrEstateEndpoint(utr), NOT_FOUND, "")

        val futureResult = connector.getEstateInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return InternalServerErrorResposne" when {
      "des has returned a 500 with the code SERVER_ERROR" in {
        val utr = "1234567893"
        stubForGet(server, createTrustOrEstateEndpoint(utr), INTERNAL_SERVER_ERROR, "")

        val futureResult = connector.getEstateInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {
      "des has returned a 503 with the code SERVICE_UNAVAILABLE" in {
        val utr = "1234567894"
        stubForGet(server, createTrustOrEstateEndpoint(utr), SERVICE_UNAVAILABLE, "")

        val futureResult = connector.getEstateInfo(utr)
        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  }//getEstateInfo
}
