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

package models.nonRepudiation

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

sealed trait NrsResponse

object NrsResponse extends Logging {
  implicit lazy val httpReads: HttpReads[NrsResponse] =
    (_: String, _: String, response: HttpResponse) => {
      logger.info(s"response status received from NRS: ${response.status}")
      response.status match {
        case ACCEPTED =>
          response.json.as[SuccessfulNrsResponse]
        case BAD_REQUEST =>
          BadRequestResponse
        case UNAUTHORIZED =>
          logger.error("No X-API-Key provided or it is invalid.")
          UnauthorisedResponse
        case BAD_GATEWAY =>
          BadGatewayResponse
        case SERVICE_UNAVAILABLE =>
          logger.error("Service unavailable response from NRS.")
          ServiceUnavailableResponse
        case GATEWAY_TIMEOUT =>
          GatewayTimeoutResponse
        case status =>
          logger.error(s"Error response from NRS with status $status and body: ${response.body}")
          InternalServerErrorResponse
      }
    }
}

case class SuccessfulNrsResponse(nrsSubscriptionId: String) extends NrsResponse

object SuccessfulNrsResponse {
  implicit val formats: Format[SuccessfulNrsResponse] = Json.format[SuccessfulNrsResponse]
}

case object BadRequestResponse extends NrsResponse
case object BadGatewayResponse extends NrsResponse
case object UnauthorisedResponse extends NrsResponse
case object ServiceUnavailableResponse extends NrsResponse
case object GatewayTimeoutResponse extends NrsResponse
case object InternalServerErrorResponse extends NrsResponse

