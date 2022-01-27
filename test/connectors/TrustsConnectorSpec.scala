/*
 * Copyright 2022 HM Revenue & Customs
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

import connector.TrustsConnector
import exceptions._
import models.existing_trust.ExistingCheckRequest
import models.existing_trust.ExistingCheckRequest._
import models.existing_trust.ExistingCheckResponse._
import models.get_trust._
import models.variation.{TrustVariation, VariationResponse}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, Reads}
import utils.NonTaxable5MLDFixtures
import org.scalatest.matchers.must.Matchers._

import scala.concurrent.Future

class TrustsConnectorSpec extends ConnectorSpecHelper {

  lazy val connector: TrustsConnector = injector.instanceOf[TrustsConnector]

  lazy val request: ExistingCheckRequest = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")

  def get5MLDTrustUTREndpoint(utr: String) = s"/trusts/registration/UTR/$utr"
  def get5MLDTrustURNEndpoint(urn: String) = s"/trusts/registration/URN/$urn"

  ".checkExistingTrust" should {

    "return Matched" when {
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

    "return NotMatched" when {
      "trusts data does not with existing trusts" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/trusts/match", requestBody, OK, """{"match": false}""")

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "5MLD" when {
      "return BadRequest" when {
        "payload sent is not valid" in {
          val wrongPayloadRequest = request.copy(utr = "NUMBER1234")
          val requestBody = Json.stringify(Json.toJson(wrongPayloadRequest))

          stubForPost(server, "/trusts/match", requestBody, BAD_REQUEST, Json.stringify(Json.parse(
            """
              |{
              |  "failures": [
              |    {
              |      "code": "INVALID_PAYLOAD",
              |      "reason": "Submission has not passed validation. Invalid payload."
              |    }
              |  ]
              |}
              |""".stripMargin)))

          val futureResult = connector.checkExistingTrust(wrongPayloadRequest)

          whenReady(futureResult) {
            result => result mustBe BadRequest
          }
        }
      }

      "return AlreadyRegistered " when {
        "trusts is already registered with provided details" in {
          val requestBody = Json.stringify(Json.toJson(request))

          stubForPost(server, "/trusts/match", requestBody, CONFLICT, Json.stringify(Json.parse(
            """
              |{
              |  "failures": [
              |    {
              |      "code": "ALREADY_REGISTERED",
              |      "reason": "The Trust/ Estate is already registered."
              |    }
              |  ]
              |}
              |""".stripMargin)))

          val futureResult = connector.checkExistingTrust(request)

          whenReady(futureResult) {
            result => result mustBe AlreadyRegistered
          }
        }
      }

      "return ServiceUnavailable" when {
        "des dependent service is not responding" in {
          val requestBody = Json.stringify(Json.toJson(request))

          stubForPost(server, "/trusts/match", requestBody, SERVICE_UNAVAILABLE, Json.stringify(Json.parse(
            """
              |{
              |  "failures": [
              |    {
              |      "code": "SERVICE_UNAVAILABLE",
              |      "reason": "Dependent systems are currently not responding."
              |    }
              |  ]
              |}
              |""".stripMargin)))

          val futureResult = connector.checkExistingTrust(request)

          whenReady(futureResult) {
            result => result mustBe ServiceUnavailable
          }
        }
      }

      "return ServerError" when {
        "des is experiencing some problem" in {
          val requestBody = Json.stringify(Json.toJson(request))

          stubForPost(server, "/trusts/match", requestBody, INTERNAL_SERVER_ERROR, Json.stringify(Json.parse(
            """
              |{
              |  "failures": [
              |    {
              |      "code": "SERVER_ERROR",
              |      "reason": "IF is currently experiencing problems that require live service intervention."
              |    }
              |  ]
              |}
              |""".stripMargin)))

          val futureResult = connector.checkExistingTrust(request)

          whenReady(futureResult) {
            result => result mustBe ServerError
          }
        }
      }

      "return ServerError" when {
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

  }

  ".registerTrust" should {

    "return TRN" when {
      "valid request to register a trust" in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubForPost(server, "/trusts/registration", requestBody, OK,
        """{"trn": "XTRN1234567"}"""
        )

        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult) {
          result => result mustBe models.registration.RegistrationTrnResponse("XTRN1234567")
        }
      }
    }

    "5MLD" when {

      "return BadRequestException" when {

        "payload sent downstream is invalid" in {
          val requestBody = Json.stringify(Json.toJson(invalidRegistrationRequest))
          stubForPost(server, "/trusts/registration", requestBody, BAD_REQUEST,
            s"""
               |{
               |  "failures": [
               |    {
               |      "code": "INVALID_PAYLOAD",
               |      "reason": "Submission has not passed validation. Invalid payload."
               |    }
               |  ]
               |}
               |""".stripMargin
          )

          val futureResult = connector.registerTrust(invalidRegistrationRequest)

          whenReady(futureResult) {
            result => result mustBe models.registration.BadRequestResponse
          }

        }
      }

      "return AlreadyRegisteredException" when {

        "trusts is already registered with provided details" in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubForPost(server, "/trusts/registration", requestBody, FORBIDDEN,
            s"""
               |{
               |  "failures": [
               |    {
               |      "code": "ALREADY_REGISTERED",
               |      "reason": "Trust/ Estate is already registered."
               |    }
               |  ]
               |}
               |""".stripMargin
          )

          val futureResult = connector.registerTrust(registrationRequest)

          whenReady(futureResult) {
            result => result mustBe models.registration.AlreadyRegisteredResponse
          }
        }
      }

      "return NoMatchException" when {

        "payload has UTR that does not match" in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubForPost(server, "/trusts/registration", requestBody, FORBIDDEN,
            s"""
               |{
               |  "failures": [
               |    {
               |      "code": "NO_MATCH",
               |      "reason": "There is no match in HMRC records."
               |    }
               |  ]
               |}
               |""".stripMargin
          )

          val futureResult = connector.registerTrust(registrationRequest)

          whenReady(futureResult) {
            result => result mustBe models.registration.NoMatchResponse
          }
        }
      }

      "return ServiceUnavailableException" when {

        "downstream dependent service is not responding" in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubForPost(server, "/trusts/registration", requestBody, SERVICE_UNAVAILABLE,
            s"""
               |{
               |  "failures": [
               |    {
               |      "code": "SERVICE_UNAVAILABLE",
               |      "reason": "Dependent systems are currently not responding."
               |    }
               |  ]
               |}
               |""".stripMargin
          )

          val futureResult = connector.registerTrust(registrationRequest)

          whenReady(futureResult) {
            result => result mustBe models.registration.ServiceUnavailableResponse
          }
        }
      }

      "return InternalServerErrorException" when {

        "downstream is experiencing some problem" in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubForPost(server, "/trusts/registration", requestBody, INTERNAL_SERVER_ERROR,
            s"""
               |{
               |  "failures": [
               |    {
               |      "code": "SERVER_ERROR",
               |      "reason": "IF is currently experiencing problems that require live service intervention."
               |    }
               |  ]
               |}
               |""".stripMargin
          )

          val futureResult = connector.registerTrust(registrationRequest)


          whenReady(futureResult) {
            result => result mustBe models.registration.InternalServerErrorResponse
          }
        }
      }

      "return InternalServerErrorException" when {

        "downstream is returning 403 without ALREADY REGISTERED code" in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubForPost(server, "/trusts/registration", requestBody, FORBIDDEN, "{}")
          val futureResult = connector.registerTrust(registrationRequest)


          whenReady(futureResult) {
            result => result mustBe models.registration.InternalServerErrorResponse
          }
        }
      }
    }

  }

  ".getTrustInfoJson" when {

    "5MLD" when {

      "identifier is UTR" must {

        "return TrustFoundResponse" when {

          "des has returned a 200 with trust details" in {

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

            val utr = "1234567890"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, get5MLDTrustResponseJson)

            val futureResult: Future[GetTrustResponse] = connector.getTrustInfo(utr)

            whenReady(futureResult) { result =>

              val expectedHeader: ResponseHeader = (get5MLDTrustResponse \ "responseHeader").as[ResponseHeader]
              val expectedJson = (get5MLDTrustResponse \ "trustOrEstateDisplay").as[JsValue]

              result match {
                case r: TrustProcessedResponse =>
                  r.responseHeader mustBe expectedHeader
                  r.getTrust mustBe expectedJson
                case _ => fail
              }
            }
          }

          "des has returned a 200 with property or land asset with no previous value" in {

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

        "return BadRequestResponse" when {

          "des has returned a 400" in {

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponse)

            val futureResult: Future[GetTrustResponse] = connector.getTrustInfo(urn)

            val expectedResponse = Json.parse(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponse)

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

        "return BadRequestResponse" when {

          "des has returned a 400" in {

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), BAD_REQUEST,
              Json.stringify(jsonResponse4005mld))

            val futureResult = connector.getTrustInfo(urn)

            whenReady(futureResult) { result =>
              result mustBe BadRequestResponse
            }
          }
        }

        "return NotEnoughDataResponse" when {

          "des has returned a 204" in {

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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

            stubForGet(server, "/trusts-store/features/5mld", OK,
              """
                |{
                | "name": "5mld",
                | "isEnabled": true
                |}""".stripMargin
            )

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
        stubForPost(server, url, requestBody, OK,
          s"""{"tvn": "XXTVN1234567890"}"""
        )

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
        stubForPost(server, url, requestBody, BAD_REQUEST,
          s"""
             |{
             | "code": "INVALID_PAYLOAD",
             | "reason": "Submission has not passed validation. Invalid Payload."
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(variation))

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }

      }
    }

    "return DuplicateSubmissionException" when {
      "trusts two requests are submitted with the same Correlation ID." in {

        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, CONFLICT,
          s"""
             |{
             | "code": "DUPLICATE_SUBMISSION",
             | "reason": "Duplicate Correlation Id was submitted."
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InvalidCorrelationIdException" when {
      "trusts provides an invalid Correlation ID." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, BAD_REQUEST,
          s"""
             |{
             | "code": "INVALID_CORRELATIONID",
             | "reason": "Submission has not passed validation. Invalid CorrelationId."
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, SERVICE_UNAVAILABLE,
          s"""
             |{
             | "code": "SERVICE_UNAVAILABLE",
             | "reason": "Dependent systems are currently not responding"
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(server, url, requestBody, INTERNAL_SERVER_ERROR,
          s"""
             |{
             | "code": "SERVER_ERROR",
             | "reason": "DES is currently experiencing problems that require live service intervention"
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest))

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  }
}
