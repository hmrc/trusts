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

package uk.gov.hmrc.trusts.models

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.utils.Constants._
import uk.gov.hmrc.trusts.models.DesErrorResponse.formats

sealed trait RegistrationResponse

final case class RegistrationTrustResponse(trn: String) extends RegistrationResponse

object RegistrationResponse {

  implicit val formats = Json.format[RegistrationTrustResponse]


  implicit lazy val httpReads: HttpReads[RegistrationResponse] =
    new HttpReads[RegistrationResponse] {
      override def read(method: String, url: String, response: HttpResponse): RegistrationResponse = {

        Logger.info(s"[RegistrationTrustResponse]  response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[RegistrationTrustResponse]

          case FORBIDDEN =>
            response.json.asOpt[DesErrorResponse] match {
              case Some(desReponse) if desReponse.code == ALREADY_REGISTERED_CODE =>
                Logger.info(s"[RegistrationTrustResponse] already registered response from des.")
                throw AlreadyRegisteredException
              case Some(desReponse) if desReponse.code == NO_MATCH_CODE =>
                Logger.info(s"[RegistrationTrustResponse] No match response from des.")
                throw NoMatchException
              case _ =>
                Logger.error("[RegistrationTrustResponse] Forbidden response from des.")
                throw InternalServerErrorException("Forbidden response from des.")
            }

          case BAD_REQUEST =>
            throw BadRequestException
          case SERVICE_UNAVAILABLE =>
            Logger.error("[RegistrationTrustResponse] Service unavailable response from des.")
            throw ServiceNotAvailableException("Des dependent service is down.")
          case status =>
            throw InternalServerErrorException(s"Error response from des $status")
        }
      }
    }


}



