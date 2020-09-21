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

package uk.gov.hmrc.trusts.models.tax_enrolments

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.exceptions._

final case class SubscriptionIdResponse(subscriptionId: String)

object SubscriptionIdResponse {

  implicit val formats = Json.format[SubscriptionIdResponse]


  implicit lazy val httpReads: HttpReads[SubscriptionIdResponse] =
    new HttpReads[SubscriptionIdResponse] {
      override def read(method: String, url: String, response: HttpResponse): SubscriptionIdResponse = {
        Logger.info(s"[SubscriptionIdResponse]  response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[SubscriptionIdResponse]
          case BAD_REQUEST =>
            Logger.error(s"[SubscriptionIdResponse] Bad Request response from des ")
            throw  BadRequestException
          case NOT_FOUND =>
            Logger.error(s"[SubscriptionIdResponse] Not found response from des")
            throw  NotFoundException
          case SERVICE_UNAVAILABLE =>
            Logger.error("[SubscriptionIdResponse] Service unavailable response from des.")
            throw new ServiceNotAvailableException("Des depdedent service is down.")
          case status =>
            Logger.error(s"[SubscriptionIdResponse]  Error response from des : ${status}")
            throw new InternalServerErrorException(s"Error response from des $status")
        }
      }
    }


}
