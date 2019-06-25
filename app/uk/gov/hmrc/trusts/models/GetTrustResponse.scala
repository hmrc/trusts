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
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.exceptions.{BadRequestException, InternalServerErrorException, NotFoundException, ServiceNotAvailableException}

trait GetTrustResponse

sealed abstract class FailureResponse extends GetTrustResponse {
  val code: String
  val reason: String
}

case class TrustFoundResponse(getTrustDesResponse: GetTrustDesResponse) extends GetTrustResponse

case object InvalidUTRResponse extends FailureResponse {
  override val code = "INVALID_UTR"
  override val reason = "Bad Request"
}

case object InvalidRegimeResponse extends FailureResponse {
  override val code = "INVALID_REGIME"
  override val reason = "Bad Request"
}

case object BadRequestResponse extends FailureResponse {
  override val code = "BAD_REQUEST"
  override val reason = "Bad Request"
}

case object ResourceNotFoundResponse extends FailureResponse {
  override val code = "NOT_FOUND"
  override val reason = "Resource not found"
}

case object InternalServerErrorResponse extends FailureResponse {
  override val code = "INTERNAL_SERVER_ERROR"
  override val reason = "Internal Server Error"
}

case object ServiceUnavailableResponse extends FailureResponse {
  override val code = "SERVICE_UNAVAILABLE"
  override val reason = "Service Unavailable"
}


object TrustFoundResponse {
  implicit val format = Json.format[TrustFoundResponse]
}

object GetTrustResponse {

  implicit lazy val httpReads: HttpReads[GetTrustResponse] =
    new HttpReads[GetTrustResponse] {
      override def read(method: String, url: String, response: HttpResponse): GetTrustResponse = {
        Logger.info(s"[SubscriptionIdResponse]  response status received from des: ${response.status}")
        response.status match {
          case OK =>
            val trust = response.json.asOpt[GetTrust]
            TrustFoundResponse(GetTrustDesResponse(None, ResponseHeader("TODO", 1)))
          case BAD_REQUEST => {
            response.json.asOpt[DesErrorResponse] match {
              case Some(desErrorResponse) =>
                desErrorResponse.code match {
                  case "INVALID_UTR" =>
                    InvalidUTRResponse
                  case "INVALID_REGIME" =>
                    InvalidRegimeResponse
                  case _ =>
                    BadRequestResponse
                }
              case None =>
                InternalServerErrorResponse
            }
          }
          case NOT_FOUND => ResourceNotFoundResponse
          case SERVICE_UNAVAILABLE => ServiceUnavailableResponse
          case _ => InternalServerErrorResponse
        }
      }
    }


}