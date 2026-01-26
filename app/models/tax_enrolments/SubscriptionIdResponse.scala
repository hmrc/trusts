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

package models.tax_enrolments

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

trait SubscriptionIdResponse

final case class SubscriptionIdSuccessResponse(subscriptionId: String) extends SubscriptionIdResponse
final case class SubscriptionIdFailureResponse(message: String) extends SubscriptionIdResponse

object SubscriptionIdResponse extends Logging {

  implicit val formats: OFormat[SubscriptionIdSuccessResponse] = Json.format[SubscriptionIdSuccessResponse]

  implicit lazy val httpReads: HttpReads[SubscriptionIdResponse] =
    new HttpReads[SubscriptionIdResponse] {
      override def read(method: String, url: String, response: HttpResponse): SubscriptionIdResponse =
        response.status match {
          case OK                  =>
            response.json.as[SubscriptionIdSuccessResponse]
          case BAD_REQUEST         =>
            logger.error(s"[SubscriptionIdResponse][httpReads] Bad Request response from des")
            SubscriptionIdFailureResponse("Bad request")
          case NOT_FOUND           =>
            logger.error(s"[SubscriptionIdResponse][httpReads] Not found response from des")
            SubscriptionIdFailureResponse("Not found")
          case SERVICE_UNAVAILABLE =>
            logger.error("[SubscriptionIdResponse][httpReads] Service unavailable response from des.")
            SubscriptionIdFailureResponse("Des dependent service is down.")
          case status              =>
            logger.error(s"[SubscriptionIdResponse][httpReads] Error response from des : $status")
            SubscriptionIdFailureResponse(s"Error response from des $status")
        }
    }

}
