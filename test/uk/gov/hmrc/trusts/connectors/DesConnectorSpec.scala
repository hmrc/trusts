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

import play.api.libs.json.{Format, JsError, JsSuccess, Json, Reads, Writes}
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.exceptions.{AlreadyRegisteredException, _}
import uk.gov.hmrc.trusts.models.ExistingCheckRequest._
import uk.gov.hmrc.trusts.models.ExistingCheckResponse._
import uk.gov.hmrc.trusts.models._
import play.api.http.Status._
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.EstateFoundResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.variation.{EstateVariation, TrustVariation, VariationResponse}

class DesConnectorSpec extends BaseConnectorSpec {

  lazy val connector: DesConnector = injector.instanceOf[DesConnector]

  lazy val request = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")

  def createTrustOrEstateEndpoint(utr: String) = s"/trusts/registration/$utr"

  ".checkExistingTrust" should {

    "return Matched " when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, OK, """{"match": true}""")

        val futureResult = connector.checkExistingTrust(request)

        application.stop()

        whenReady(futureResult) {
          result =>
            result mustBe Matched
        }


      }
    }
    "return NotMatched " when {
      "trusts data does not with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, OK, """{"match": false}""")

        val futureResult = connector.checkExistingTrust(request)

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }

  ".checkExistingEstate" should {

    "return Matched " when {
      "estate data match with existing estate." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, OK, """{"match": true}""")

        val futureResult = connector.checkExistingEstate(request)

        application.stop()

        whenReady(futureResult) {
          result => result mustBe Matched
        }

      }
    }
    "return NotMatched " when {
      "estate data does not with existing estate." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, OK, """{"match": false}""")

        val futureResult = connector.checkExistingEstate(request)

        application.stop()

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

        application.stop()

        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "estate is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, CONFLICT, Json.stringify(jsonResponseAlreadyRegistered))

        val futureResult = connector.checkExistingEstate(request)

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        stubForPost(server, "/trusts/registration", requestBody, OK, """{"trn": "XTRN1234567"}""")

        val futureResult = connector.registerTrust(registrationRequest)

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".registerEstate" should {

    "return TRN  " when {
      "valid request to des register an estate." in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest))

        stubForPost(server, "/estates/registration", requestBody, OK, """{"trn": "XTRN1234567"}""")

        val futureResult = connector.registerEstate(estateRegRequest)

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".getSubscriptionId" should {

    "return subscription Id  " when {
      "valid trn has been submitted" in {
        val trn = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl,  OK, """{"subscriptionId": "987654321"}""")

        val futureResult = connector.getSubscriptionId(trn)

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".getTrustInfo" should {

    "return TrustFoundResponse" when {

      "des has returned a 200 with trust details" in {

        val utr = "1234567890"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustResponseJson)

        val futureResult = connector.getTrustInfo(utr)

        application.stop()

        whenReady(futureResult) { result =>

          val expectedResult = Json.fromJson[TrustFoundResponse](getTrustResponse).get

          result mustBe expectedResult

          result match {
            case TrustFoundResponse(Some(trust), _) =>
              trust.trust.assets.shares.get.head.utr mustBe Some("2134514321")
              trust.trust.assets.business.get.head.orgName mustBe "Lone Wolf Ltd"
              trust.trust.assets.business.get.head.utr mustBe Some("2134514322")
              trust.trust.assets.business.get.head.businessDescription mustBe "Travel Business"
              trust.trust.assets.business.get.head.address.get.line1 mustBe "Suite 10"

              trust.trust.entities.leadTrustee.leadTrusteeInd.get.name.firstName mustBe "Jimmy"
            case _ => fail
          }
        }
      }

      "des has returned a 200 and indicated that the submission is still being processed" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateProcessingResponseJson)

        val futureResult = connector.getTrustInfo(utr)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe TrustFoundResponse(None, ResponseHeader("In Processing", "1"))
        }
      }
    }

    "return InvalidUTRResponse" when {

      "des has returned a 400 with the code INVALID_UTR" in {
        val invalidUTR = "123456789"
        stubForGet(server, createTrustOrEstateEndpoint(invalidUTR), BAD_REQUEST,
                   Json.stringify(jsonResponse400InvalidUTR))

        val futureResult = connector.getTrustInfo(invalidUTR)

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  }

  ".getEstateInfo" should {

    "return EstateFoundResponse" when {
      "des has returned a 200 with estate details" in {
        val utr = "1234567890"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getEstateResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        application.stop()

        whenReady(futureResult) {
          case estateFoundResponse: EstateFoundResponse =>
            val actualResult = Json.toJson(estateFoundResponse)

            actualResult mustBe getEstateExpectedResponse
          case _ =>
            fail("Test Failed: Should have parsed the json into EstateFoundResponse model.")
        }
      }

      "des has returned a 200 and indicated that the submission is still being processed" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateProcessingResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("In Processing", "1"))
        }
      }

      "des has returned a 200 and indicated that the submission is pending closure" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstatePendingClosureResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("Pending Closure", "1"))
        }
      }

      "des has returned a 200 and indicated that the submission is closed" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateClosedResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("Closed", "1"))
        }
      }

      "des has returned a 200 and indicated that the submission is suspended" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateSuspendedResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("Suspended", "1"))
        }
      }

      "des has returned a 200 and indicated that the submission is parked" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateParkedResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("Parked", "1"))
        }
      }

      "des has returned a 200 and indicated that the submission is obsoleted" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateObsoletedResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("Obsoleted", "1"))
        }
      }
    }

    "return InvalidUTRResponse" when {
      "des has returned a 400 with the code INVALID_UTR" in {
        val invalidUTR = "123456789"
        stubForGet(server, createTrustOrEstateEndpoint(invalidUTR), BAD_REQUEST,
          Json.stringify(jsonResponse400InvalidUTR))

        val futureResult = connector.getEstateInfo(invalidUTR)

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

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

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  }//getEstateInfo

  ".TrustVariation" should {

    val url = "/trusts/variation"

    "return a VariationTrnResponse" when {
      "des has returned a 200 with a trn" in {

        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))
        stubForPost(server, url, requestBody, OK, """{"tvn": "XXTVN1234567890"}""")

        val futureResult = connector.trustVariation(trustVariationsRequest)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe a[VariationResponse]
          inside(result){ case VariationResponse(tvn)  => tvn must fullyMatch regex """^[a-zA-Z0-9]{15}$""".r }
        }
      }
    }

    "return BadRequestException" when {
      "payload sent to des is invalid" in {

        implicit val invalidVariationRead: Reads[TrustVariation] = Json.reads[TrustVariation]

        val variation = invalidTrustVariationsRequest.validate[TrustVariation].get

        val requestBody = Json.stringify(Json.toJson(variation))
        stubForPost(server, url, requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

        val futureResult = connector.trustVariation(variation)

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }

      }
    }

    "return DuplicateSubmissionException" when {
      "trusts two requests are submitted with the same Correlation ID." in {

        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, CONFLICT, Json.stringify(jsonResponse409DuplicateCorrelation))
        val futureResult = connector.trustVariation(trustVariationsRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InvalidCorrelationIdException" when {
      "trusts provides an invalid Correlation ID." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, BAD_REQUEST, Json.stringify(jsonResponse400CorrelationId))
        val futureResult = connector.trustVariation(trustVariationsRequest)

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.trustVariation(trustVariationsRequest)

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.trustVariation(trustVariationsRequest)

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  }
  ".EstateVariation" should {

    val url = "/estates/variation"

    "return a VariationTrnResponse" when {

      "des has returned a 200 with a trn" in {

        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))
        stubForPost(server, url, requestBody, OK, """{"tvn": "XXTVN1234567890"}""")

        val futureResult = connector.estateVariation(estateVariationsRequest)

        application.stop()

        whenReady(futureResult) { result =>
          result mustBe a[VariationResponse]
          inside(result){ case VariationResponse(tvn)  => tvn must fullyMatch regex """^[a-zA-Z0-9]{15}$""".r }
        }
      }
    }

    "payload sent to des is invalid" in {

      implicit val invalidVariationRead: Reads[EstateVariation] = Json.reads[EstateVariation]

      val variation = estateVariationsRequest

      val requestBody = Json.stringify(Json.toJson(variation))
      stubForPost(server, url, requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

      val futureResult = connector.estateVariation(variation)

      application.stop()

      whenReady(futureResult.failed) {
        result => result mustBe BadRequestException
      }

    }

    "return DuplicateSubmissionException" when {
      "trusts two requests are submitted with the same Correlation ID." in {

        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPost(server, url, requestBody, CONFLICT, Json.stringify(jsonResponse409DuplicateCorrelation))
        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InvalidCorrelationIdException" when {
      "trusts provides an invalid Correlation ID." in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPost(server, url, requestBody, BAD_REQUEST, Json.stringify(jsonResponse400CorrelationId))
        val futureResult = connector.estateVariation(estateVariationsRequest)

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPost(server, url, requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.estateVariation(estateVariationsRequest)

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPost(server, url, requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.estateVariation(estateVariationsRequest)

        application.stop()

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }
}
