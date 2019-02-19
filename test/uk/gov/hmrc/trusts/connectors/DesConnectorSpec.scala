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
import uk.gov.hmrc.trusts.models.ExistingTrustResponse._
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.utils.WireMockHelper

class DesConnectorSpec extends BaseConnectorSpec
  with GuiceOneAppPerSuite with WireMockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Seq("microservice.services.des.port" -> server.port(),
        "auditing.enabled" -> false): _*).build()


  lazy val connector: DesConnector = app.injector.instanceOf[DesConnector]

  lazy val request = ExistingTrustCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")


  ".checkExistingTrust" should {

    "return Matched " when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor(server, "/trusts/match", requestBody, 200, """{"match": true}""")

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe Matched
        }

      }
    }
    "return NotMatched " when {
      "trusts data does not with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor(server, "/trusts/match", requestBody, 200, """{"match": false}""")

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

        stubFor(server, "/trusts/match", requestBody, 400, Json.stringify(jsonResponse400))

        val futureResult = connector.checkExistingTrust(wrongPayloadRequest)

        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor(server, "/trusts/match", requestBody, 409, Json.stringify(jsonResponseAlreadyRegistered))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor(server, "/trusts/match", requestBody, 503, Json.stringify(jsonResponse503))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }
      }
    }

    "return ServerError " when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor(server, "/trusts/match", requestBody, 500, Json.stringify(jsonResponse500))

        val futureResult = connector.checkExistingTrust(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }

    "return ServerError " when {
      "des is returning forbidden response" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor(server, "/trusts/match", requestBody, 409, "{}")

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

        stubFor(server, "/trusts/registration", requestBody, 200, """{"trn": "XTRN1234567"}""")

        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationTrustResponse("XTRN1234567")
        }

      }
    }

    "return BadRequestException  " when {
      "payload sent to des is invalid" in {
        val requestBody = Json.stringify(Json.toJson(invalidRegistrationRequest))
        stubFor(server, "/trusts/registration", requestBody, 400, Json.stringify(jsonResponse400))

        val futureResult = connector.registerTrust(invalidRegistrationRequest)


        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }

      }
    }

    "return AlreadyRegisteredException  " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubFor(server, "/trusts/registration", requestBody, 403, Json.stringify(jsonResponseAlreadyRegistered))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return NoMatchException  " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubFor(server, "/trusts/registration", requestBody, 403, Json.stringify(jsonResponse403NoMatch))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe NoMatchException
        }
      }
    }

    "return ServiceUnavailableException  " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))
        stubFor(server, "/trusts/registration", requestBody, 503, Json.stringify(jsonResponse503))
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubFor(server, "/trusts/registration", requestBody, 500, Json.stringify(jsonResponse500))
        val futureResult = connector.registerTrust(registrationRequest)
        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "des is returning 403 without ALREADY REGISTERED code." in {
        val requestBody = Json.stringify(Json.toJson(registrationRequest))

        stubFor(server, "/trusts/registration", requestBody, 403, "{}")
        val futureResult = connector.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }
}

