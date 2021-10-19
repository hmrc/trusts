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
import utils.RetryPolicy

sealed trait NrsResponse extends RetryPolicy

object NrsResponse extends Logging {
  final val CHECKSUM_FAILED = 419
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
        case CHECKSUM_FAILED =>
          ChecksumFailedResponse
        case status =>
          logger.error(s"Error response from NRS with status $status and body: ${response.body}")
          InternalServerErrorResponse
      }
    }
}

case class SuccessfulNrsResponse(nrsSubscriptionId: String) extends NrsResponse with RetryPolicy { override val retry = false}

object SuccessfulNrsResponse {
  implicit val formats: Format[SuccessfulNrsResponse] = Json.format[SuccessfulNrsResponse]
}

case object BadRequestResponse extends NrsResponse with RetryPolicy { override val retry = false}
case object BadGatewayResponse extends NrsResponse with RetryPolicy { override val retry = true}
case object UnauthorisedResponse extends NrsResponse with RetryPolicy { override val retry = false}
case object ServiceUnavailableResponse extends NrsResponse with RetryPolicy { override val retry = true}
case object GatewayTimeoutResponse extends NrsResponse with RetryPolicy { override val retry = true}
case object InternalServerErrorResponse extends NrsResponse with RetryPolicy { override val retry = true}
case object ChecksumFailedResponse extends NrsResponse with RetryPolicy { override val retry = false}
case object NoActiveSessionResponse extends NrsResponse with RetryPolicy { override val retry = false}

