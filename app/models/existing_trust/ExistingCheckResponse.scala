/*
 * Copyright 2024 HM Revenue & Customs
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

package models.existing_trust

import play.api.http.Status._
import play.api.libs.json.Format
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Constants


sealed trait ExistingCheckResponse

object ExistingCheckResponse {

  final case object Matched extends ExistingCheckResponse
  final case object NotMatched extends ExistingCheckResponse
  final case object BadRequest extends ExistingCheckResponse
  final case object AlreadyRegistered extends ExistingCheckResponse
  final case object ServerError extends ExistingCheckResponse
  final case object ServiceUnavailable extends ExistingCheckResponse

  implicit val desResponseReads : Format[ExistingTrustResponse] = ExistingTrustResponse.formats
  implicit val desErrorResponseReads : Format[DesErrorResponse] = DesErrorResponse.formats

  implicit lazy val httpReads: HttpReads[ExistingCheckResponse] =
    new HttpReads[ExistingCheckResponse] {
      override def read(method: String, url: String, response: HttpResponse): ExistingCheckResponse = {
        response.status match {
          case OK =>
            if (response.json.as[ExistingTrustResponse].`match`) Matched else NotMatched
          case CONFLICT =>

            if (response.json.toString().contains(Constants.ALREADY_REGISTERED_CODE)) {
              AlreadyRegistered
            } else {
              ServerError
            }
          case BAD_REQUEST =>
            BadRequest
          case SERVICE_UNAVAILABLE =>
            ServiceUnavailable
          case _ =>
            ServerError
        }
      }
    }

}



