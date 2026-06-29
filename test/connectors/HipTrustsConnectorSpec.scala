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
import connector.HipTrustsConnector
import errors.{BadRequestErrorResponse, ServiceNotAvailableErrorResponse, TrustErrors, VariationFailureForAudit}
import models.existing_trust.ExistingCheckRequest
import models.existing_trust.ExistingCheckResponse.{
  AlreadyRegistered, BadRequest, Matched, NotMatched, ServerError, ServiceUnavailable
}
import models.registration.RegistrationResponse
import models.variation.{TrustVariation, VariationSuccessResponse}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Reads}
import play.api.test.Helpers.CONTENT_TYPE

import scala.concurrent.Future

class HipTrustsConnectorSpec extends ConnectorSpecHelper with EitherValues {

  override def applicationBuilder(): GuiceApplicationBuilder =
    super
      .applicationBuilder()
      .configure(
        Seq(
          "microservice.services.hip.registration.port" -> server.port(),
          "microservice.services.hip.variation.port"    -> server.port()
        ): _*
      )

  override def stubForPost(
    server: WireMockServer,
    url: String,
    requestBody: String,
    returnStatus: Int,
    responseBody: String,
    delayResponse: Int = 0
  ) =

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

  private lazy val connector: HipTrustsConnector = injector.instanceOf[HipTrustsConnector]

  private lazy val request: ExistingCheckRequest =
    ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")

  ".TrustVariation" should {
    val url = "/etmp/RESTAdapter/trustsandestates/variation"

    "return a VariationTrnResponse" when {
      "hip has returned a 200 with a trn" in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))
        stubForPost(server, url, requestBody, OK, """{ "success": {"tvn": "XXTVN1234567890"}}""")

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
        stubForPost(server, url, requestBody, OK, """{ "success": {"tvn": "XXTVN1234567890"}}""")

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
        stubForPost(
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

        stubForPost(
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

        stubForPost(
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

        stubForPost(
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
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(trustVariationsRequest))

        stubForPost(
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

        stubForPost(
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
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

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
        val requestBody = Json.stringify(Json.toJson(invalidRegistrationRequest))
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
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

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
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

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
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

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
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

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
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

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
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

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
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

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

  ".getTrustInfo" should {

    "return trust info when utr|urn is valid" in {
      // TODO correct test when method has been implemented
      val result = intercept[NotImplementedError] {
        connector.getTrustInfo("XXTRN1234567890")
      }

      result.getMessage must be("an implementation is missing")
    }
  }

}
