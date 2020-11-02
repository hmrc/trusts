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

package models.registration

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{Format, JsResult, JsValue, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import exceptions._
import models.existing_trust.DesErrorResponse
import utils.Constants._

sealed trait RegistrationResponse

case class RegistrationTrnResponse(trn: String) extends RegistrationResponse

object RegistrationTrnResponse {
  implicit val formats = Json.format[RegistrationTrnResponse]
}

case class RegistrationFailureResponse(status: Int, code: String, message: String) extends RegistrationResponse

object RegistrationFailureResponse {
  implicit val formats = Json.format[RegistrationFailureResponse]
}

object RegistrationResponse extends Logging {

  implicit object RegistrationResponseFormats extends Format[RegistrationResponse] with Logging {

    override def reads(json: JsValue): JsResult[RegistrationResponse] = json.validate[RegistrationTrnResponse]

    override def writes(o: RegistrationResponse): JsValue = o match {
      case x : RegistrationTrnResponse => Json.toJson(x)(RegistrationTrnResponse.formats)
      case x : RegistrationFailureResponse => Json.toJson(x)(RegistrationFailureResponse.formats)

    }

  }

  implicit lazy val httpReads: HttpReads[RegistrationResponse] =
    new HttpReads[RegistrationResponse] {
      override def read(method: String, url: String, response: HttpResponse): RegistrationResponse = {
        logger.info(s"response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[RegistrationTrnResponse]
          case FORBIDDEN =>
            response.json.asOpt[DesErrorResponse] match {
              case Some(desReponse) if desReponse.code == ALREADY_REGISTERED_CODE =>
                logger.info(s"already registered response from des.")
                throw AlreadyRegisteredException
              case Some(desReponse) if desReponse.code == NO_MATCH_CODE =>
                logger.info(s"No match response from des.")
                throw NoMatchException
              case _ =>
                logger.error("Forbidden response from des.")
                throw InternalServerErrorException("Forbidden response from des.")
            }
          case BAD_REQUEST =>
            throw BadRequestException
          case SERVICE_UNAVAILABLE =>
            logger.error("Service unavailable response from des.")
            throw ServiceNotAvailableException("Des dependent service is down.")
          case status =>
            throw InternalServerErrorException(s"Error response from des $status body: ${response.body}")
        }
      }
    }

}


