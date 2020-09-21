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

import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.exceptions.{AlreadyRegisteredException, _}
import uk.gov.hmrc.trusts.models.existing_trust.ExistingCheckRequest._
import uk.gov.hmrc.trusts.models.existing_trust.ExistingCheckResponse._
import uk.gov.hmrc.trusts.models.existing_trust.ExistingCheckRequest
import uk.gov.hmrc.trusts.models.get_trust._
import uk.gov.hmrc.trusts.models.get_trust.get_trust.{TrustProcessedResponse, _}
import uk.gov.hmrc.trusts.models.registration.RegistrationTrnResponse
import uk.gov.hmrc.trusts.models.tax_enrolments.SubscriptionIdResponse
import uk.gov.hmrc.trusts.models.variation.{TrustVariation, VariationResponse}

import scala.concurrent.Future

class DesConnectorSpec extends ConnectorSpecHelper {

  lazy val connector: DesConnector = injector.instanceOf[DesConnector]

  lazy val request = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")

  def get4MLDTrustEndpoint(utr: String) = s"/trusts/registration/$utr"

  def get5MLDTrustUTREndpoint(utr: String) = s"/trusts/registration/UTR/$utr"
  def get5MLDTrustURNEndpoint(utr: String) = s"/trusts/registration/URN/$utr"

  ".checkExistingTrust" should {

    "return Matched " when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, OK, """{"match": true}""")

        val futureResult = connector.checkExistingTrust(request)

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
  }

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
  }

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

  ".getTrustInfoJson" when {

    "4MLD" when {

      "return TrustFoundResponse" when {

        "des has returned a 200 with trust details" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "1234567890"
          stubForGet(server, get4MLDTrustEndpoint(utr), OK, get4MLDTrustResponseJson)

          val futureResult: Future[GetTrustResponse] = connector.getTrustInfo(utr)

          whenReady(futureResult) { result =>

            val expectedHeader: ResponseHeader = (get4MLDTrustResponse \ "responseHeader").as[ResponseHeader]
            val expectedJson = (get4MLDTrustResponse \ "trustOrEstateDisplay").as[JsValue]

            result match {
              case r: TrustProcessedResponse =>
                r.responseHeader mustBe expectedHeader
                r.getTrust mustBe expectedJson
              case _ => fail
            }
          }
        }

        "des has returned a 200 with property or land asset with no previous value" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "1234567890"
          stubForGet(server, get4MLDTrustEndpoint(utr), OK, getTrustPropertyLandNoPreviousValue)

          val futureResult: Future[GetTrustResponse] = connector.getTrustInfo(utr)


          whenReady(futureResult) { result =>

            val expectedHeader: ResponseHeader = (getTrustPropertyLandNoPreviousValueJson \ "responseHeader").as[ResponseHeader]
            val expectedJson = (getTrustPropertyLandNoPreviousValueJson \ "trustOrEstateDisplay").as[JsValue]

            result match {
              case r: TrustProcessedResponse =>
                r.responseHeader mustBe expectedHeader
                r.getTrust mustBe expectedJson
              case _ => fail
            }
          }
        }

        "des has returned a 200 and indicated that the submission is still being processed" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "1234567800"
          stubForGet(server, get4MLDTrustEndpoint(utr), OK, getTrustOrEstateProcessingResponseJson)

          val futureResult = connector.getTrustInfo(utr)


          whenReady(futureResult) { result =>
            result mustBe TrustFoundResponse(ResponseHeader("In Processing", "1"))
          }
        }
      }

      "return NotEnoughData" when {

        "json does not validate as GetData model" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "123456789"
          stubForGet(server, get4MLDTrustEndpoint(utr), OK, getTrustMalformedJsonResponse)

          val futureResult = connector.getTrustInfo(utr)


          whenReady(futureResult) { result =>
            result mustBe NotEnoughDataResponse(Json.parse(getTrustMalformedJsonResponse), Json.parse(
              """
                |{"obj.details.trust.entities.leadTrustees.phoneNumber":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.identification":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.name":[{"msg":["error.path.missing"],"args":[]}]}
                |""".stripMargin))
          }
        }

      }

      "return InvalidUTRResponse" when {

        "des has returned a 400 with the code INVALID_UTR" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val invalidUTR = "123456789"
          stubForGet(server, get4MLDTrustEndpoint(invalidUTR), BAD_REQUEST,
            Json.stringify(jsonResponse400InvalidUTR))

          val futureResult = connector.getTrustInfo(invalidUTR)


          whenReady(futureResult) { result =>
            result mustBe InvalidUTRResponse
          }
        }
      }

      "return InvalidRegimeResponse" when {

        "des has returned a 400 with the code INVALID_REGIME" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "1234567891"
          stubForGet(server, get4MLDTrustEndpoint(utr), BAD_REQUEST,
            Json.stringify(jsonResponse400InvalidRegime))

          val futureResult = connector.getTrustInfo(utr)


          whenReady(futureResult) { result =>
            result mustBe InvalidRegimeResponse
          }
        }
      }

      "return BadRequestResponse" when {

        "des has returned a 400 with a code which is not INVALID_UTR OR INVALID_REGIME" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "1234567891"
          stubForGet(server, get4MLDTrustEndpoint(utr), BAD_REQUEST,
            Json.stringify(jsonResponse400))

          val futureResult = connector.getTrustInfo(utr)


          whenReady(futureResult) { result =>
            result mustBe BadRequestResponse
          }
        }
      }

      "return NotEnoughDataResponse" when {

        "des has returned a 204" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "6666666666"
          stubForGet(server, get4MLDTrustEndpoint(utr), OK, Json.stringify(jsonResponse204))

          val futureResult = connector.getTrustInfo(utr)


          whenReady(futureResult) { result =>
            result mustBe NotEnoughDataResponse(jsonResponse204, Json.parse(
              """
                |{"obj":[{"msg":["'responseHeader' is undefined on object: {\"code\":\"NO_CONTENT\",\"reason\":\"No Content.\"}"],"args":[]}]}
                |""".stripMargin))
          }
        }
      }

      "return ResourceNotFoundResponse" when {

        "des has returned a 404" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "1234567892"
          stubForGet(server, get4MLDTrustEndpoint(utr), NOT_FOUND, "")

          val futureResult = connector.getTrustInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe ResourceNotFoundResponse
          }
        }
      }

      "return InternalServerErrorResponse" when {

        "des has returned a 500 with the code SERVER_ERROR" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "1234567893"
          stubForGet(server, get4MLDTrustEndpoint(utr), INTERNAL_SERVER_ERROR, "")

          val futureResult = connector.getTrustInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe InternalServerErrorResponse
          }
        }
      }

      "return ServiceUnavailableResponse" when {

        "des has returned a 503 with the code SERVICE_UNAVAILABLE" in {

          stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
            """
              |{
              | "name": "5mld",
              | "isEnabled": false
              |}""".stripMargin
          )))

          val utr = "1234567894"
          stubForGet(server, get4MLDTrustEndpoint(utr), SERVICE_UNAVAILABLE, "")

          val futureResult = connector.getTrustInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe ServiceUnavailableResponse
          }
        }
      }

    }

    "5MLD" when {

      "identifier is UTR" must {

        "return TrustFoundResponse" when {

          "des has returned a 200 with trust details" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567890"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, get4MLDTrustResponseJson)

            val futureResult: Future[GetTrustResponse] = connector.getTrustInfo(utr)

            whenReady(futureResult) { result =>

              val expectedHeader: ResponseHeader = (get4MLDTrustResponse \ "responseHeader").as[ResponseHeader]
              val expectedJson = (get4MLDTrustResponse \ "trustOrEstateDisplay").as[JsValue]

              result match {
                case r: TrustProcessedResponse =>
                  r.responseHeader mustBe expectedHeader
                  r.getTrust mustBe expectedJson
                case _ => fail
              }
            }
          }

          "des has returned a 200 with property or land asset with no previous value" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567890"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, getTrustPropertyLandNoPreviousValue)

            val futureResult: Future[GetTrustResponse] = connector.getTrustInfo(utr)


            whenReady(futureResult) { result =>

              val expectedHeader: ResponseHeader = (getTrustPropertyLandNoPreviousValueJson \ "responseHeader").as[ResponseHeader]
              val expectedJson = (getTrustPropertyLandNoPreviousValueJson \ "trustOrEstateDisplay").as[JsValue]

              result match {
                case r: TrustProcessedResponse =>
                  r.responseHeader mustBe expectedHeader
                  r.getTrust mustBe expectedJson
                case _ => fail
              }
            }
          }

          "des has returned a 200 and indicated that the submission is still being processed" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567800"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, getTrustOrEstateProcessingResponseJson)

            val futureResult = connector.getTrustInfo(utr)


            whenReady(futureResult) { result =>
              result mustBe TrustFoundResponse(ResponseHeader("In Processing", "1"))
            }
          }
        }

        "return NotEnoughData" when {

          "json does not validate as GetData model" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567890"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, getTrustMalformedJsonResponse)

            val futureResult = connector.getTrustInfo(utr)

            whenReady(futureResult) { result =>
              result mustBe NotEnoughDataResponse(Json.parse(getTrustMalformedJsonResponse), Json.parse(
                """
                  |{"obj.details.trust.entities.leadTrustees.phoneNumber":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.identification":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.name":[{"msg":["error.path.missing"],"args":[]}]}
                  |""".stripMargin))
            }
          }

        }

        "return InvalidUTRResponse" when {

          "des has returned a 400 with the code INVALID_UTR" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val invalidUTR = "1234567890"
            stubForGet(server, get5MLDTrustUTREndpoint(invalidUTR), BAD_REQUEST,
              Json.stringify(jsonResponse400InvalidUTR))

            val futureResult = connector.getTrustInfo(invalidUTR)


            whenReady(futureResult) { result =>
              result mustBe InvalidUTRResponse
            }
          }
        }

        "return InvalidRegimeResponse" when {

          "des has returned a 400 with the code INVALID_REGIME" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567891"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), BAD_REQUEST,
              Json.stringify(jsonResponse400InvalidRegime))

            val futureResult = connector.getTrustInfo(utr)


            whenReady(futureResult) { result =>
              result mustBe InvalidRegimeResponse
            }
          }
        }

        "return BadRequestResponse" when {

          "des has returned a 400 with a code which is not INVALID_UTR OR INVALID_REGIME" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567891"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), BAD_REQUEST,
              Json.stringify(jsonResponse400))

            val futureResult = connector.getTrustInfo(utr)


            whenReady(futureResult) { result =>
              result mustBe BadRequestResponse
            }
          }
        }

        "return NotEnoughDataResponse" when {

          "des has returned a 204" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "6666666666"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, Json.stringify(jsonResponse204))

            val futureResult = connector.getTrustInfo(utr)


            whenReady(futureResult) { result =>
              result mustBe NotEnoughDataResponse(jsonResponse204, Json.parse(
                """
                  |{"obj":[{"msg":["'responseHeader' is undefined on object: {\"code\":\"NO_CONTENT\",\"reason\":\"No Content.\"}"],"args":[]}]}
                  |""".stripMargin))
            }
          }
        }

        "return ResourceNotFoundResponse" when {

          "des has returned a 404" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567892"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), NOT_FOUND, "")

            val futureResult = connector.getTrustInfo(utr)

            whenReady(futureResult) { result =>
              result mustBe ResourceNotFoundResponse
            }
          }
        }

        "return InternalServerErrorResponse" when {

          "des has returned a 500 with the code SERVER_ERROR" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567893"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), INTERNAL_SERVER_ERROR, "")

            val futureResult = connector.getTrustInfo(utr)

            whenReady(futureResult) { result =>
              result mustBe InternalServerErrorResponse
            }
          }
        }

        "return ServiceUnavailableResponse" when {

          "des has returned a 503 with the code SERVICE_UNAVAILABLE" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val utr = "1234567894"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), SERVICE_UNAVAILABLE, "")

            val futureResult = connector.getTrustInfo(utr)

            whenReady(futureResult) { result =>
              result mustBe ServiceUnavailableResponse
            }
          }
        }
      }

      "identifier is URN" must {

        "return TrustFoundResponse" when {

          "des has returned a 200 with trust details" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, get5MLDTrustNonTaxableResponseJson)

            val futureResult: Future[GetTrustResponse] = connector.getTrustInfo(urn)

            val expectedResponse = Json.parse(get5MLDTrustNonTaxableResponseJson)

            whenReady(futureResult) { result =>

              val expectedHeader: ResponseHeader = (expectedResponse \ "responseHeader").as[ResponseHeader]
              val expectedJson = (expectedResponse \ "trustOrEstateDisplay").as[JsValue]

              result match {
                case r: TrustProcessedResponse =>

                  r.responseHeader mustBe expectedHeader
                  r.getTrust mustBe expectedJson
                case _ => fail
              }
            }
          }

          "des has returned a 200 with property or land asset with no previous value" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, getTrustPropertyLandNoPreviousValue)

            val futureResult: Future[GetTrustResponse] = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>

              val expectedHeader: ResponseHeader = (getTrustPropertyLandNoPreviousValueJson \ "responseHeader").as[ResponseHeader]
              val expectedJson = (getTrustPropertyLandNoPreviousValueJson \ "trustOrEstateDisplay").as[JsValue]

              result match {
                case r: TrustProcessedResponse =>
                  r.responseHeader mustBe expectedHeader
                  r.getTrust mustBe expectedJson
                case _ => fail
              }
            }
          }

          "des has returned a 200 and indicated that the submission is still being processed" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, getTrustOrEstateProcessingResponseJson)

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe TrustFoundResponse(ResponseHeader("In Processing", "1"))
            }
          }
        }

        "return NotEnoughData" when {

          "json does not validate as GetData model" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, getTrustMalformedJsonResponse)

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe NotEnoughDataResponse(Json.parse(getTrustMalformedJsonResponse), Json.parse(
                """
                  |{"obj.details.trust.entities.leadTrustees.phoneNumber":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.identification":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.name":[{"msg":["error.path.missing"],"args":[]}]}
                  |""".stripMargin))
            }
          }

        }

        "return InvalidUTRResponse" when {

          "des has returned a 400 with the code INVALID_UTR" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), BAD_REQUEST,
              Json.stringify(jsonResponse400InvalidUTR))

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe InvalidUTRResponse
            }
          }
        }

        "return InvalidRegimeResponse" when {

          "des has returned a 400 with the code INVALID_REGIME" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), BAD_REQUEST,
              Json.stringify(jsonResponse400InvalidRegime))

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe InvalidRegimeResponse
            }
          }
        }

        "return BadRequestResponse" when {

          "des has returned a 400 with a code which is not INVALID_UTR OR INVALID_REGIME" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), BAD_REQUEST,
              Json.stringify(jsonResponse400))

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe BadRequestResponse
            }
          }
        }

        "return NotEnoughDataResponse" when {

          "des has returned a 204" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, Json.stringify(jsonResponse204))

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe NotEnoughDataResponse(jsonResponse204, Json.parse(
                """
                  |{"obj":[{"msg":["'responseHeader' is undefined on object: {\"code\":\"NO_CONTENT\",\"reason\":\"No Content.\"}"],"args":[]}]}
                  |""".stripMargin))
            }
          }
        }

        "return ResourceNotFoundResponse" when {

          "des has returned a 404" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), NOT_FOUND, "")

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe ResourceNotFoundResponse
            }
          }
        }

        "return InternalServerErrorResponse" when {

          "des has returned a 500 with the code SERVER_ERROR" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), INTERNAL_SERVER_ERROR, "")

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe InternalServerErrorResponse
            }
          }
        }

        "return ServiceUnavailableResponse" when {

          "des has returned a 503 with the code SERVICE_UNAVAILABLE" in {

            stubForGet(server, "/trusts-store/features/5mld", OK, Json.stringify(Json.parse(
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )))

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), SERVICE_UNAVAILABLE, "")

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe ServiceUnavailableResponse
            }
          }
        }
      }

    }

  }

  ".TrustVariation" should {

    val url = "/trusts/variation"

    "return a VariationTrnResponse" when {
      "des has returned a 200 with a trn" in {

        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))
        stubForPost(server, url, requestBody, OK, """{"tvn": "XXTVN1234567890"}""")

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))

        whenReady(futureResult) { result =>
          result mustBe a[VariationResponse]
          inside(result) { case VariationResponse(tvn) => tvn must fullyMatch regex """^[a-zA-Z0-9]{15}$""".r }
        }
      }
    }

    "return a VariationTrnResponse" when {
      "des has returned a 200 with a trn for a submission of property or land without previousValue" in {

        val requestBody = Json.stringify(Json.toJson(trustVariationsNoPreviousPropertyValueRequest))
        stubForPost(server, url, requestBody, OK, """{"tvn": "XXTVN1234567890"}""")

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsNoPreviousPropertyValueRequest))

        whenReady(futureResult) { result =>
          result mustBe a[VariationResponse]
          inside(result) { case VariationResponse(tvn) => tvn must fullyMatch regex """^[a-zA-Z0-9]{15}$""".r }
        }
      }
    }

    "return BadRequestException" when {
      "payload sent to des is invalid" in {

        implicit val invalidVariationRead: Reads[TrustVariation] = Json.reads[TrustVariation]

        val variation = invalidTrustVariationsRequest.validate[TrustVariation].get

        val requestBody = Json.stringify(Json.toJson(variation))
        stubForPost(server, url, requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

        val futureResult = connector.trustVariation(Json.toJson(variation))

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }

      }
    }

    "return DuplicateSubmissionException" when {
      "trusts two requests are submitted with the same Correlation ID." in {

        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, CONFLICT, Json.stringify(jsonResponse409DuplicateCorrelation))
        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InvalidCorrelationIdException" when {
      "trusts provides an invalid Correlation ID." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, BAD_REQUEST, Json.stringify(jsonResponse400CorrelationId))
        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))


        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  }
}
