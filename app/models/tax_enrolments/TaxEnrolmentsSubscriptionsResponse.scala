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

package models.tax_enrolments

import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import scala.language.implicitConversions

sealed trait TaxEnrolmentsSubscriptionsResponse

case class TaxEnrolmentsSubscriptionsSuccessResponse(subscriptionId: String, utr: String, state: String)
    extends TaxEnrolmentsSubscriptionsResponse

case class TaxEnrolmentsSubscriptionsFailureResponse(message: String) extends TaxEnrolmentsSubscriptionsResponse

object TaxEnrolmentsSubscriptionsResponse extends Logging {

  implicit def httpReads(subscriptionId: String): HttpReads[TaxEnrolmentsSubscriptionsResponse] =
    (_: String, _: String, response: HttpResponse) =>
      response.status match {
        case OK                  =>
          parseOkResponse(response, subscriptionId)
        case BAD_REQUEST         =>
          logger.error(
            s"[TaxEnrolmentsSubscriptionsResponse][httpReads][SubscriptionId: $subscriptionId] Bad Request response from des "
          )
          TaxEnrolmentsSubscriptionsFailureResponse("Bad request")
        case NOT_FOUND           =>
          logger.error(
            s"[TaxEnrolmentsSubscriptionsResponse][httpReads][SubscriptionId: $subscriptionId] Not found response from des"
          )
          TaxEnrolmentsSubscriptionsFailureResponse("Not found")
        case SERVICE_UNAVAILABLE =>
          logger.error(
            s"[TaxEnrolmentsSubscriptionsResponse][httpReads][SubscriptionId: $subscriptionId] Service unavailable response from des."
          )
          TaxEnrolmentsSubscriptionsFailureResponse("Des service is down.")
        case status              =>
          logger.error(
            s"[TaxEnrolmentsSubscriptionsResponse][httpReads][SubscriptionId: $subscriptionId] Error response from des : $status"
          )
          TaxEnrolmentsSubscriptionsFailureResponse(s"Error response from des $status")
      }

  case class SubscriptionIdentifier(key: String, value: String)

  private def parseOkResponse(response: HttpResponse, subscriptionId: String): TaxEnrolmentsSubscriptionsResponse = {
    val optState    = (response.json \ "state").asOpt[String]
    val identifiers = (response.json \ "identifiers").as[List[SubscriptionIdentifier]]
    val optUtr      = identifiers.find(_.key == "SAUTR").map(_.value)
    (optState, optUtr) match {
      case (Some(state), Some(utr)) => TaxEnrolmentsSubscriptionsSuccessResponse(subscriptionId, utr, state)
      case (None, Some(utr))        =>
        val message = s"[SubscriptionId: $subscriptionId, UTR: $utr] InvalidDataErrorResponse - No State supplied"
        logger.error(s"[TaxEnrolmentsSubscriptionsResponse][parseOkResponse]$message")
        TaxEnrolmentsSubscriptionsFailureResponse(message)
      case (Some(_), None)          =>
        val message = s"[SubscriptionId: $subscriptionId] InvalidDataErrorResponse - No UTR supplied"
        logger.error(s"[TaxEnrolmentsSubscriptionsResponse][parseOkResponse]$message")
        TaxEnrolmentsSubscriptionsFailureResponse(message)
      case _                        =>
        val message = s"[SubscriptionId: $subscriptionId] InvalidDataErrorResponse - No State or UTR supplied"
        logger.error(s"[TaxEnrolmentsSubscriptionsResponse][parseOkResponse]$message")
        TaxEnrolmentsSubscriptionsFailureResponse(message)
    }
  }

  object SubscriptionIdentifier {
    implicit val formats: Format[SubscriptionIdentifier] = Json.format[SubscriptionIdentifier]
  }

}
