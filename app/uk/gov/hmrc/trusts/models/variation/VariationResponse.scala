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

package uk.gov.hmrc.trusts.models.variation

import org.slf4j.LoggerFactory
import play.api.http.Status._
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.exceptions._

final case class VariationResponse(tvn: String)

object VariationResponse {

  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

  implicit val formats: Format[VariationResponse] = Json.format[VariationResponse]

  implicit lazy val httpReads: HttpReads[VariationResponse] =
    new HttpReads[VariationResponse] {
      override def read(method: String, url: String, response: HttpResponse): VariationResponse = {

        logger.debug(s"[VariationResponse] response body ${response.body}")

        logger.info(s"[VariationTvnResponse]  response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[VariationResponse]
          case BAD_REQUEST if response.body contains "INVALID_CORRELATIONID" =>
            logger.error(s"[VariationTvnResponse] Bad Request for invalid correlation id response from des ")
            throw InternalServerErrorException("Invalid correlation id response from des")
          case BAD_REQUEST =>
            logger.error(s"[VariationTvnResponse] Bad Request response from des ")
            throw BadRequestException
          case CONFLICT =>
            logger.error(s"[VariationTvnResponse] Conflict response from des")
            throw InternalServerErrorException("Conflict response from des")
          case INTERNAL_SERVER_ERROR =>
            logger.error(s"[VariationTvnResponse] Internal server error response from des")
            throw InternalServerErrorException("des is currently experiencing problems that require live service intervention")
          case SERVICE_UNAVAILABLE =>
            logger.error("[VariationTvnResponse] Service unavailable response from des.")
            throw ServiceNotAvailableException("des dependent service is down.")
          case status =>
            logger.error(s"[VariationTvnResponse]  Error response from des : $status")
            throw InternalServerErrorException(s"Error response from des $status")
        }
      }
    }

}
