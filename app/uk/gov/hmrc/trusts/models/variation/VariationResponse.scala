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

package uk.gov.hmrc.trusts.models.variation

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.exceptions._

final case class VariationResponse(tvn: String)

object VariationResponse {

  implicit val formats: Format[VariationResponse] = Json.format[VariationResponse]

  implicit lazy val httpReads: HttpReads[VariationResponse] =
    new HttpReads[VariationResponse] {
      override def read(method: String, url: String, response: HttpResponse): VariationResponse = {
        Logger.info(s"[VariationTvnResponse]  response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[VariationResponse]
          case BAD_REQUEST if response.body contains "INVALID_CORRELATIONID" =>
            Logger.error(s"[VariationTvnResponse] Bad Request for invalid correlation id response from des ")
            throw InvalidCorrelationIdException
          case BAD_REQUEST =>
            Logger.error(s"[VariationTvnResponse] Bad Request response from des ")
            throw BadRequestException
          case CONFLICT =>
            Logger.error(s"[VariationTvnResponse] Conflict response from des")
            throw DuplicateSubmissionException
          case INTERNAL_SERVER_ERROR =>
            Logger.error(s"[VariationTvnResponse] Internal server error response from des")
            throw InternalServerErrorException("des is currently experiencing problems that require live service intervention")
          case SERVICE_UNAVAILABLE =>
            Logger.error("[VariationTvnResponse] Service unavailable response from des.")
            throw ServiceNotAvailableException("des dependent service is down.")
          case status =>
            Logger.error(s"[VariationTvnResponse]  Error response from des : $status")
            throw InternalServerErrorException(s"Error response from des $status")
        }
      }
    }

}
