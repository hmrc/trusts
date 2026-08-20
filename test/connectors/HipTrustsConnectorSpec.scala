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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import connector.HipTrustsConnector
import errors.{BadRequestErrorResponse, ServiceNotAvailableErrorResponse, TrustErrors, VariationFailureForAudit}
import models.existing_trust.ExistingCheckRequest
import models.existing_trust.ExistingCheckResponse.{
  AlreadyRegistered, BadRequest, Matched, NotMatched, ServerError, ServiceUnavailable
}
import models.get_trust._
import models.registration.RegistrationResponse
import models.variation.{TrustVariation, VariationSuccessResponse}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsLookupResult, JsObject, JsValue, Json, Reads}
import play.api.test.Helpers.CONTENT_TYPE
import utils.{NonTaxable5MLDFixtures, TrustsJsonBridge}

import scala.concurrent.Future

class HipTrustsConnectorSpec extends ConnectorSpecHelper with EitherValues {

  val converter = new TrustsJsonBridge {}
  import converter._

  override def applicationBuilder(): GuiceApplicationBuilder =
    super
      .applicationBuilder()
      .configure(
        Seq(
          "microservice.services.hip.registration.port" -> server.port(),
          "microservice.services.hip.variation.port"    -> server.port(),
          "microservice.services.hip.playback.port"     -> server.port()
        ): _*
      )

  override def stubForPost(
    server: WireMockServer,
    url: String,
    requestBody: String,
    returnStatus: Int,
    responseBody: String,
    delayResponse: Int = 0
  ): StubMapping =
    server.stubFor(
      post(urlEqualTo(url))
        .withHeader(CONTENT_TYPE, containing("application/json"))
        .withRequestBody(equalTo(requestBody))
        .willReturn(
          aResponse()
            .withStatus(returnStatus)
            .withBody(responseBody)
            .withFixedDelay(delayResponse)
        )
    )

  override def stubForGet(
    server: WireMockServer,
    url: String,
    returnStatus: Int,
    responseBody: String,
    delayResponse: Int = 0
  ): StubMapping =
    server.stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(returnStatus)
            .withBody(responseBody)
            .withFixedDelay(delayResponse)
        )
    )

  private def wrapInSuccessNode(in: String) =
    Json.obj("success" -> Json.parse(in))

  private def expectedHeaderAndJson(response: JsObject): (ResponseHeader, JsValue) =
    (
      (response \ "success" \ "responseHeader").as[ResponseHeader],
      (response \ "success" \ "trustOrEstateDisplay").as[JsValue]
    )

  private lazy val connector: HipTrustsConnector = injector.instanceOf[HipTrustsConnector]

  private lazy val request: ExistingCheckRequest =
    ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")

  private def get5MLDTrustUTREndpoint(utr: String) = s"/etmp/RESTAdapter/trustsandestates/registration/UTR/$utr"
  private def get5MLDTrustURNEndpoint(urn: String) = s"/etmp/RESTAdapter/trustsandestates/registration/URN/$urn"

  private val registrationReq = Json.toJson(registrationRequest).convertToHipJson

  ".TrustVariation" should {
    val url = "/etmp/RESTAdapter/trustsandestates/registration"

    "return a VariationTrnResponse" when {
      "hip has returned a 200 with a trn" in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))
        stubForPutWithBody(server, url, requestBody, OK, """{ "success": {"tvn": "XXTVN1234567890"}}""")

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest)).value

        whenReady(futureResult) { result =>
          result mustBe Right(VariationSuccessResponse("XXTVN1234567890"))
          inside(result.value) { case VariationSuccessResponse(tvn) =>
            tvn must fullyMatch regex """^[a-zA-Z0-9]{15}$""".r
          }
        }
      }
    }

    "return a VariationTrnResponse" when {
      "hip has returned a 200 with a trn for a submission of property or land without previousValue" in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsNoPreviousPropertyValueRequest))
        stubForPutWithBody(server, url, requestBody, OK, """{ "success": {"tvn": "XXTVN1234567890"}}""")

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsNoPreviousPropertyValueRequest)).value

        whenReady(futureResult) { result =>
          result mustBe Right(VariationSuccessResponse("XXTVN1234567890"))
          inside(result.value) { case VariationSuccessResponse(tvn) =>
            tvn must fullyMatch regex """^[a-zA-Z0-9]{15}$""".r
          }
        }
      }
    }

    "return BadRequestErrorResponse" when {
      "payload sent to hip is invalid" in {
        implicit val invalidVariationRead: Reads[TrustVariation] = Json.reads[TrustVariation]

        val variation = invalidTrustVariationsRequest.validate[TrustVariation].get

        val requestBody = Json.stringify(Json.toJson(variation))
        stubForPutWithBody(
          server,
          url,
          requestBody,
          BAD_REQUEST,
          s"""
             |{
             | "code": "400",
             | "message": "String",
             | "logID": "00000000000000000000000000000000"
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(variation)).value

        whenReady(futureResult) { result =>
          result mustBe Left(VariationFailureForAudit(BadRequestErrorResponse, "Bad request"))
        }
      }
    }

    "return errors.InternalServerErrorResponse" when {
      "trusts two requests are submitted with the same Correlation ID." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPutWithBody(
          server,
          url,
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "004",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "Duplicate submission acknowledgment reference"
             |    }
             |}
             |""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest)).value

        whenReady(futureResult) { result =>
          result mustBe Left(VariationFailureForAudit(errors.InternalServerErrorResponse, "Conflict response from hip"))
        }
      }
    }

    "return errors.InternalServerErrorResponse" when {
      "trusts provides an invalid Correlation ID." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPutWithBody(
          server,
          url,
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "003",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "Request could not be processed"
             |    }
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest)).value
        whenReady(futureResult) { result =>
          result mustBe Left(
            VariationFailureForAudit(errors.InternalServerErrorResponse, "Invalid correlation id response from hip")
          )
        }
      }
    }

    "return ServiceNotAvailableErrorResponse  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPutWithBody(
          server,
          url,
          requestBody,
          IM_A_TEAPOT,
          "foo"
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest)).value

        whenReady(futureResult) { result =>
          result mustBe Left(
            VariationFailureForAudit(ServiceNotAvailableErrorResponse, "hip dependent service is down.")
          )
        }
      }
    }

    "return errors.InternalServerErrorResponse" when {
      "hip is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPutWithBody(
          server,
          url,
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "999",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "Technical System Error"
             |    }
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest)).value

        whenReady(futureResult) { result =>
          result mustBe Left(
            VariationFailureForAudit(
              errors.InternalServerErrorResponse,
              "hip is currently experiencing problems that require live service intervention"
            )
          )
        }
      }
    }

    "return errors.InternalServerErrorResponse" when {
      "hip returns 500" in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPutWithBody(
          server,
          url,
          requestBody,
          INTERNAL_SERVER_ERROR,
          s"""
             |{
             |  "error": {
             |    "code": "500",
             |    "message": "String",
             |    "logID": "00000000000000000000000000000000"
             |  }
             |}""".stripMargin
        )

        val futureResult = connector.trustVariation(Json.toJson(trustVariationsRequest)).value

        whenReady(futureResult) { result =>
          result mustBe Left(
            VariationFailureForAudit(
              errors.InternalServerErrorResponse,
              "hip is currently experiencing problems that require live service intervention"
            )
          )
        }
      }
    }
  }

  ".get5MLDTrustOrEstateEndpoint" should {
    "return UTR URL" when {
      "identifierLength is 10" in {
        val url = connector.get5MLDTrustOrEstateEndpoint("1234567890")
        url.contains("UTR") mustBe true
      }
    }
  }

  ".registerTrust" should {
    "return HipSuccessRegistrationTrnResponse" when {
      "registration is successful " in {
        val requestBody = Json.stringify(registrationReq)

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          CREATED,
          """{"success": {"trn": "XTRN1234567"}}"""
        )

        val futureResult: Future[Either[TrustErrors, RegistrationResponse]] =
          connector.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(
            models.registration.RegistrationTrnResponse("XTRN1234567")
          )
        }
      }
    }

    "return BadRequestResponse" when {
      "payload sent downstream is invalid" in {
        val requestBody = Json.stringify(Json.toJson(invalidRegistrationRequest).convertToHipJson)
        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          BAD_REQUEST,
          s"""
             |{
             |  "error": {
             |    "code": "400",
             |    "message": "String",
             |    "logID": "00000000000000000000000000000000"
             |  }
             |}             |""".stripMargin
        )

        val futureResult = connector.registerTrust(invalidRegistrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.registration.BadRequestResponse)
        }

      }
    }

    "return AlreadyRegisteredResponse" when {
      "trusts is already registered with provided details" in {
        val requestBody = Json.stringify(registrationReq)

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "002",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "FAIL – ALREADY REGISTERED"
             |    }
             |}
             |""".stripMargin
        )

        val futureResult = connector.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.registration.AlreadyRegisteredResponse)
        }
      }
    }

    "return NoMatchResponse" when {
      "payload has UTR that does not match" in {
        val requestBody = Json.stringify(registrationReq)

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "001",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "FAIL – NO MATCH"
             |    }
             |}
             |""".stripMargin
        )

        val futureResult = connector.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.registration.NoMatchResponse)
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "we get a 422 999" in {
        val requestBody = Json.stringify(registrationReq)

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "999",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "Technical System Error"
             |    }
             |}
             |""".stripMargin
        )

        val futureResult = connector.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.registration.InternalServerErrorResponse)
        }
      }
    }

    "return BadRequestResponse" when {
      "we get a 422 004" in {
        val requestBody = Json.stringify(registrationReq)

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "004",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "Technical System Error"
             |    }
             |}
             |""".stripMargin
        )

        val futureResult = connector.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.registration.BadRequestResponse)
        }
      }

    }

    "return ServiceUnavailableResponse" when {
      "downstream dependent service is not responding" in {
        val requestBody = Json.stringify(registrationReq)

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          IM_A_TEAPOT,
          s"""
             |{
             |  "error": {
             |    "code": "418",
             |    "message": "String",
             |    "logID": "00000000000000000000000000000000"
             |  }
             |}             |""".stripMargin
        )

        val futureResult = connector.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.registration.ServiceUnavailableResponse)
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "downstream is experiencing some problem" in {
        val requestBody = Json.stringify(registrationReq)

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          INTERNAL_SERVER_ERROR,
          s"""
             |{
             |  "error": {
             |    "code": "500",
             |    "message": "String",
             |    "logID": "00000000000000000000000000000000"
             |  }
             |}             |}
             |""".stripMargin
        )

        val futureResult = connector.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.registration.InternalServerErrorResponse)
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "downstream is returning 403 " in {
        val requestBody = Json.stringify(registrationReq)

        stubForPost(server, "/etmp/RESTAdapter/trustsandestates/registration", requestBody, FORBIDDEN, "{}")
        val futureResult = connector.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.registration.InternalServerErrorResponse)
        }
      }
    }
  }

  ".checkExistingTrust" should {
    "return Matched" when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          CREATED,
          """{ "success": {"match": true}}"""
        )
        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(Matched)
        }
      }
    }

    "return NotMatched" when {
      "trusts data does not match with existing trusts" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          UNPROCESSABLE_ENTITY,
          """{
          |  "error": {
          |    "processingDate": "2001-12-17T09:30:47.0",
          |    "errorId": "001",
          |    "text": "FAIL – NO MATCH"
          |  }
          |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(NotMatched)
        }
      }
    }

    "return AlreadyRegistered" when {
      "trusts data matches with existing trust that is registered" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          UNPROCESSABLE_ENTITY,
          """{
            |  "error": {
            |    "processingDate": "2001-12-17T09:30:47.0",
            |    "errorId": "002",
            |    "text": "FAIL – ALREADY REGISTERED"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(AlreadyRegistered)
        }
      }
    }

    "return ServerError" when {
      "trusts data matches causes Hip to return 422 with errorId 999" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          UNPROCESSABLE_ENTITY,
          """{
            |  "error": {
            |    "processingDate": "2001-12-17T09:30:47.0",
            |    "errorId": "999",
            |    "text": "Technical System Error"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(ServerError)
        }
      }
    }

    "return BadRequest" when {
      "for all other 422 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          UNPROCESSABLE_ENTITY,
          """{
            |  "error": {
            |    "processingDate": "2001-12-17T09:30:47.0",
            |    "errorId": "004",
            |    "text": "Duplicate submission acknowledgment reference"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(BadRequest)
        }
      }
    }

    "return BadRequest" when {
      "for 400 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          BAD_REQUEST,
          """{
            |  "error": {
            |    "code": "400",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(BadRequest)
        }
      }
    }

    "return BadRequest" when {
      "for 401 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          BAD_REQUEST,
          """{
            |  "error": {
            |    "code": "401",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(BadRequest)
        }
      }
    }

    "return BadRequest" when {
      "for 403 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          BAD_REQUEST,
          """{
            |  "error": {
            |    "code": "403",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(BadRequest)
        }
      }
    }

    "return BadRequest" when {
      "for 404 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          BAD_REQUEST,
          """{
            |  "error": {
            |    "code": "404",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(BadRequest)
        }
      }
    }

    "return ServerError" when {
      "for 500 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          INTERNAL_SERVER_ERROR,
          """{
            |  "error": {
            |    "code": "500",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(ServerError)
        }
      }
    }

    "return ServiceUnavailable" when {
      "for 503 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          SERVICE_UNAVAILABLE,
          """{
            |  "error": {
            |    "code": "503",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingTrust(request).value

        whenReady(futureResult) { result =>
          result mustBe Right(ServiceUnavailable)
        }
      }
    }
  }

  ".getTrustInfoJson" when {
    "5MLD" when {
      "identifier is UTR" must {
        "return TrustFoundResponse" when {
          "hip has returned a 200 with trust details" in {
            val utr                                 = "1234567890"
            val getProcessedTrustResponse: JsObject = wrapInSuccessNode(get5MLDTrustResponseJson)
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, getProcessedTrustResponse.toString)

            val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = connector.getTrustInfo(utr).value
            val (expectedHeader, expectedJson)                              = expectedHeaderAndJson(getProcessedTrustResponse)

            whenReady(futureResult) {
              case Right(r: TrustProcessedResponse) =>
                r.responseHeader mustBe expectedHeader
                r.getTrust       mustBe expectedJson
              case _                                => fail()
            }
          }

          "hip has returned a 200 with property or land asset with no previous value" in {
            val utr                                                   = "1234567890"
            val getTrustPropertyLandNoPreviousValueResponse: JsObject =
              wrapInSuccessNode(getTrustPropertyLandNoPreviousValue)
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, getTrustPropertyLandNoPreviousValueResponse.toString)

            val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = connector.getTrustInfo(utr).value
            val (expectedHeader, expectedJson)                              = expectedHeaderAndJson(getTrustPropertyLandNoPreviousValueResponse)

            whenReady(futureResult) {
              case Right(r: TrustProcessedResponse) =>
                r.responseHeader mustBe expectedHeader
                r.getTrust       mustBe expectedJson
              case _                                => fail()
            }
          }

          "hip has returned a 200 and indicated that the submission is still being processed" in {
            val utr = "1234567800"
            stubForGet(
              server,
              get5MLDTrustUTREndpoint(utr),
              OK,
              wrapInSuccessNode(getTrustOrEstateProcessingResponseJson).toString
            )

            val futureResult = connector.getTrustInfo(utr).value

            whenReady(futureResult) { result =>
              result mustBe Right(TrustFoundResponse(ResponseHeader("In Processing", "1")))
            }
          }
        }

        "return NotEnoughData" when {
          "json does not validate as GetData model" in {
            val utr      = "1234567890"
            val expected = wrapInSuccessNode(getTrustMalformedJsonResponse).toString
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, expected)

            val futureResult = connector.getTrustInfo(utr).value

            whenReady(futureResult) { result =>
              result mustBe Right(
                NotEnoughDataResponse(
                  Json.parse(expected),
                  Json.parse("""
                               |{"obj.details.trust.entities.leadTrustees.phoneNumber":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.identification":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.name":[{"msg":["error.path.missing"],"args":[]}]}
                               |""".stripMargin)
                )
              )
            }
          }
        }

        "return BadRequestResponse" when {
          "hip has returned a 400" in {
            val utr = "1234567891"
            stubForGet(
              server,
              get5MLDTrustUTREndpoint(utr),
              BAD_REQUEST,
              """
                |{
                | "code": "400",
                | "message": "String",
                | "logID": "00000000000000000000000000000000"
                |}""".stripMargin
            )

            val futureResult = connector.getTrustInfo(utr).value

            whenReady(futureResult) { result =>
              result mustBe Right(BadRequestResponse)
            }
          }
        }

        "return NotEnoughDataResponse" when {
          "hip has returned a 204" in {
            val utr = "6666666666"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, Json.stringify(jsonResponse204))

            val futureResult = connector.getTrustInfo(utr).value

            whenReady(futureResult) { result =>
              result mustBe Right(
                NotEnoughDataResponse(
                  jsonResponse204,
                  Json.parse("""
                               |{"obj":[{"msg":["'success' is undefined on object. Available keys are 'code', 'reason'"],"args":[]}]}
                               |""".stripMargin)
                )
              )
            }
          }
        }

        "return ResourceNotFoundResponse" when {
          "hip has returned a 422 000" in {
            val utr = "1234567892"
            stubForGet(
              server,
              get5MLDTrustUTREndpoint(utr),
              UNPROCESSABLE_ENTITY,
              s"""
                 |{
                 |  "error":
                 |    {
                 |      "errorId": "000",
                 |      "processingDate": "2001-12-17T09:30:47.0",
                 |      "text": "Duplicate submission acknowledgment reference"
                 |    }
                 |}
                 |""".stripMargin
            )

            val futureResult = connector.getTrustInfo(utr).value

            whenReady(futureResult) { result =>
              result mustBe Right(ResourceNotFoundResponse)
            }
          }
        }

        "return InternalServerErrorResponse" when {
          "hip has returned a 500 with the code SERVER_ERROR" in {
            val utr = "1234567893"
            stubForGet(
              server,
              get5MLDTrustUTREndpoint(utr),
              INTERNAL_SERVER_ERROR,
              """{
                |  "error": {
                |    "code": "500",
                |    "message": "String",
                |    "logID": "00000000000000000000000000000000"
                |  }
                |}
                |""".stripMargin
            )

            val futureResult = connector.getTrustInfo(utr).value

            whenReady(futureResult) { result =>
              result mustBe Right(InternalServerErrorResponse)
            }
          }
        }

        "return ServiceUnavailableResponse" when {
          "hip has returned a 503 with the code SERVICE_UNAVAILABLE" in {
            val utr = "1234567894"
            stubForGet(server, get5MLDTrustUTREndpoint(utr), SERVICE_UNAVAILABLE, "")

            val futureResult = connector.getTrustInfo(utr).value

            whenReady(futureResult) { result =>
              result mustBe Right(ServiceUnavailableResponse)
            }
          }
        }
      }

      "identifier is URN" must {
        "return TrustFoundResponse" when {
          "hip has returned a 200 with trust details" in {
            val urn                            = "1234567890ADCEF"
            val (expectedHeader, expectedJson) =
              expectedHeaderAndJson(wrapInSuccessNode(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponse))
            stubForGet(
              server,
              get5MLDTrustURNEndpoint(urn),
              OK,
              wrapInSuccessNode(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponse).toString()
            )

            val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = connector.getTrustInfo(urn).value

            whenReady(futureResult) {
              case Right(r: TrustProcessedResponse) =>
                r.responseHeader mustBe expectedHeader
                r.getTrust       mustBe expectedJson
              case _                                => fail()
            }
          }

          "hip has returned a 200 with property or land asset with no previous value" in {
            val urn                                                         = "1234567890ADCEF"
            val expectedPayload                                             = wrapInSuccessNode(getTrustPropertyLandNoPreviousValue)
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, expectedPayload.toString)
            val (expectedHeader, expectedJson)                              = expectedHeaderAndJson(expectedPayload)
            val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = connector.getTrustInfo(urn).value

            whenReady(futureResult) {
              case Right(r: TrustProcessedResponse) =>
                r.responseHeader mustBe expectedHeader
                r.getTrust       mustBe expectedJson
              case _                                => fail()
            }
          }

          "hip has returned a 200 and indicated that the submission is still being processed" in {
            val urn = "1234567890ADCEF"
            stubForGet(
              server,
              get5MLDTrustURNEndpoint(urn),
              OK,
              wrapInSuccessNode(getTrustOrEstateProcessingResponseJson).toString
            )

            val futureResult = connector.getTrustInfo(urn).value

            whenReady(futureResult) { result =>
              result mustBe Right(TrustFoundResponse(ResponseHeader("In Processing", "1")))
            }
          }
        }

        "return NotEnoughData" when {
          "json does not validate as GetData model" in {
            val urn        = "1234567890ADCEF"
            val hipPayload = wrapInSuccessNode(getTrustMalformedJsonResponse).toString
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, hipPayload)

            val futureResult = connector.getTrustInfo(urn).value

            whenReady(futureResult) { result =>
              result mustBe Right(
                NotEnoughDataResponse(
                  Json.parse(hipPayload),
                  Json.parse("""
                               |{"obj.details.trust.entities.leadTrustees.phoneNumber":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.identification":[{"msg":["error.path.missing"],"args":[]}],"obj.details.trust.entities.leadTrustees.name":[{"msg":["error.path.missing"],"args":[]}]}
                               |""".stripMargin)
                )
              )
            }
          }
        }

        "return BadRequestResponse" when {
          "hip has returned a 400" in {
            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), BAD_REQUEST, Json.stringify(jsonResponse4005mld))

            val futureResult = connector.getTrustInfo(urn).value

            whenReady(futureResult) { result =>
              result mustBe Right(BadRequestResponse)
            }
          }
        }

        "return NotEnoughDataResponse" when {
          "des has returned a 204" in {
            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), OK, Json.stringify(jsonResponse204))

            val futureResult = connector.getTrustInfo(urn).value

            whenReady(futureResult) { result =>
              result mustBe Right(
                NotEnoughDataResponse(
                  jsonResponse204,
                  Json.parse("""
                               |{"obj":[{"msg":["'success' is undefined on object. Available keys are 'code', 'reason'"],"args":[]}]}
                               |""".stripMargin)
                )
              )
            }
          }
        }

        "return ResourceNotFoundResponse" when {
          "hip has returned a 404" in {
            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), NOT_FOUND, "")

            val futureResult = connector.getTrustInfo(urn).value

            whenReady(futureResult) { result =>
              result mustBe Right(ResourceNotFoundResponse)
            }
          }
        }

        "return InternalServerErrorResponse" when {
          "hip has returned a 500 with the code INTERNAL_SERVER_ERROR" in {
            val urn = "1234567890ADCEF"
            stubForGet(
              server,
              get5MLDTrustURNEndpoint(urn),
              INTERNAL_SERVER_ERROR,
              """{
                |  "error": {
                |    "code": "500",
                |    "message": "String",
                |    "logID": "00000000000000000000000000000000"
                |  }
                |}
                |""".stripMargin
            )

            val futureResult = connector.getTrustInfo(urn).value

            whenReady(futureResult) { result =>
              result mustBe Right(InternalServerErrorResponse)
            }
          }
        }

        "return ServiceUnavailableResponse" when {
          "hip has returned a 503 with the code SERVICE_UNAVAILABLE" in {
            val urn = "1234567890ADCEF"
            stubForGet(server, get5MLDTrustURNEndpoint(urn), SERVICE_UNAVAILABLE, "")

            val futureResult = connector.getTrustInfo(urn).value

            whenReady(futureResult) { result =>
              result mustBe Right(ServiceUnavailableResponse)
            }
          }
        }
      }
    }
  }

  "TrustsJsonBridge" should {
    "change node names correctly " when {
      "converting trust json from hip to mdtp and back" in {

        val hipTrust: JsValue = Json.toJson(trustWithBeneficiaryTrustsFromHip)

        val mdtpTrust: JsValue =
          (Json.toJson(trustWithBeneficiaryTrustForHip) \ "success" \ "trustOrEstateDisplay").as[JsValue]

        val convertedToMdtp = hipTrust.convertToMdtpJson

        val unwrappedMdtpTrust = (convertedToMdtp \ "success" \ "trustOrEstateDisplay").as[JsObject]
        val unwrappedHipTrust  = (hipTrust \ "success" \ "trustOrEstateDisplay").as[JsObject]

        val convertedToHip = mdtpTrust.convertToHipJson

        assert(!(hipTrust === mdtpTrust))
        assert(unwrappedMdtpTrust === mdtpTrust)
        assert(convertedToHip === unwrappedHipTrust)
      }
    }
  }

}
