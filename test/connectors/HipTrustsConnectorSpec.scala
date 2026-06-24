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
import models.existing_trust.ExistingCheckRequest
import models.existing_trust.ExistingCheckResponse.{
  AlreadyRegistered, BadRequest, Matched, NotMatched, ServerError, ServiceUnavailable
}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.CONTENT_TYPE

class HipTrustsConnectorSpec extends ConnectorSpecHelper with EitherValues {

  override def applicationBuilder(): GuiceApplicationBuilder =
    super
      .applicationBuilder()
      .configure(
        Seq(
          "microservice.services.hip.registration.port" -> server.port()
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

  ".registerTrust" should {
    "return HipSuccessRegistrationTrnResponse" when {
      "registration is successful " in {
        ()
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

}
