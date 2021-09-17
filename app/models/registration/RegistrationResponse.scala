/*
 * Copyright 2021 HM Revenue & Customs
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

package models.registration

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Constants._

sealed trait RegistrationResponse

case class RegistrationTrnResponse(trn: String) extends RegistrationResponse

object RegistrationTrnResponse {
  implicit val formats: Format[RegistrationTrnResponse] = Json.format[RegistrationTrnResponse]
}

case object AlreadyRegisteredResponse extends RegistrationResponse
case object NoMatchResponse extends RegistrationResponse
case object BadRequestResponse extends RegistrationResponse
case object ServiceUnavailableResponse extends RegistrationResponse
case object InternalServerErrorResponse extends RegistrationResponse

object RegistrationResponse extends Logging {

  implicit lazy val httpReads: HttpReads[RegistrationResponse] =
    (_: String, _: String, response: HttpResponse) => {
      logger.info(s"response status received from des: ${response.status}")
      response.status match {
        case OK =>
          response.json.as[RegistrationTrnResponse]
        case FORBIDDEN =>
          parseForbiddenResponse(response.json)
        case BAD_REQUEST =>
          BadRequestResponse
        case SERVICE_UNAVAILABLE =>
          logger.error("Service unavailable response from des.")
          ServiceUnavailableResponse
        case status =>
          logger.error(s"Error response from des with status $status and body: ${response.body}")
          InternalServerErrorResponse
      }
  }

  private def parseForbiddenResponse(json: JsValue): RegistrationResponse = {
    json.toString() match {
      case response if response.contains(ALREADY_REGISTERED_CODE) =>
        logger.info("already registered response from des.")
        AlreadyRegisteredResponse
      case response if response.contains(NO_MATCH_CODE) =>
        logger.info("No match response from des.")
        NoMatchResponse
      case _ =>
        logger.error("Forbidden response from des.")
        InternalServerErrorResponse
    }
  }

}

case class RegistrationFailureResponse(status: Int, code: String, message: String)

object RegistrationFailureResponse {

  implicit val formats: OFormat[RegistrationFailureResponse] =
    Json.format[RegistrationFailureResponse]

}
