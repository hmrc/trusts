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

package models.variation

import errors.{BadRequestErrorResponse, InternalServerErrorResponse, ServiceNotAvailableErrorResponse, VariationErrors}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

sealed trait VariationResponse
case class VariationSuccessResponse(tvn: String) extends VariationResponse
case class VariationFailureResponse(status: Int, errorType: VariationErrors, message: String) extends VariationResponse

object VariationResponse extends Logging {

  implicit val formats: Format[VariationSuccessResponse] = Json.format[VariationSuccessResponse]

  implicit lazy val httpReads: HttpReads[VariationResponse] =
    new HttpReads[VariationResponse] {
      override def read(method: String, url: String, response: HttpResponse): VariationResponse =
        response.status match {
          case OK                                                            =>
            response.json.as[VariationSuccessResponse]
          case BAD_REQUEST if response.body contains "INVALID_CORRELATIONID" =>
            logger.error(s"[VariationResponse][httpReads] Bad Request for invalid correlation id response from des ")
            VariationFailureResponse(
              BAD_REQUEST,
              InternalServerErrorResponse,
              "Invalid correlation id response from des"
            )

          case BAD_REQUEST =>
            logger.error(s"[VariationResponse][httpReads] Bad Request response from des")
            VariationFailureResponse(BAD_REQUEST, BadRequestErrorResponse, "Bad request")

          case CONFLICT =>
            logger.error(s"[VariationResponse][httpReads] Conflict response from des")
            VariationFailureResponse(CONFLICT, InternalServerErrorResponse, "Conflict response from des")

          case INTERNAL_SERVER_ERROR =>
            logger.error(s"[VariationResponse][httpReads] Internal server error response from des")
            VariationFailureResponse(
              INTERNAL_SERVER_ERROR,
              InternalServerErrorResponse,
              "des is currently experiencing problems that require live service intervention"
            )

          case SERVICE_UNAVAILABLE =>
            logger.error("[VariationResponse][httpReads] Service unavailable response from des.")
            VariationFailureResponse(
              SERVICE_UNAVAILABLE,
              ServiceNotAvailableErrorResponse,
              "des dependent service is down."
            )

          case status =>
            logger.error(s"[VariationResponse][httpReads] Unexpected error response from des. Status: $status")
            VariationFailureResponse(
              status,
              InternalServerErrorResponse,
              s"Unexpected error response from des. Status: $status"
            )
        }
    }

}
