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

package models.tax_enrolments

import exceptions.{BadRequestException, InternalServerErrorException, InvalidDataException, NotFoundException, ServiceNotAvailableException}
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

case class TaxEnrolmentsSubscriptionsResponse(subscriptionId: String, utr: String, state: String)

object TaxEnrolmentsSubscriptionsResponse extends Logging {

  implicit def httpReads(subscriptionId: String): HttpReads[TaxEnrolmentsSubscriptionsResponse] = (_: String, _: String, response: HttpResponse) => {
    response.status match {
      case OK =>
        parseOkResponse(response, subscriptionId)
      case BAD_REQUEST =>
        logger.error(s"[TaxEnrolmentsSubscriptions] [SubscriptionId: $subscriptionId] Bad Request response from des ")
        throw BadRequestException
      case NOT_FOUND =>
        logger.error(s"[TaxEnrolmentsSubscriptions] [SubscriptionId: $subscriptionId] Not found response from des")
        throw NotFoundException
      case SERVICE_UNAVAILABLE =>
        logger.error(s"[TaxEnrolmentsSubscriptions] [SubscriptionId: $subscriptionId] Service unavailable response from des.")
        throw ServiceNotAvailableException("Des service is down.")
      case status =>
        logger.error(s"[TaxEnrolmentsSubscriptions] [SubscriptionId: $subscriptionId] Error response from des : ${status}")
        throw InternalServerErrorException(s"Error response from des $status")
    }
  }

  private def parseOkResponse(response: HttpResponse, subscriptionId: String) : TaxEnrolmentsSubscriptionsResponse = {
    val optState =  (response.json \ "state").asOpt[String]
    val identifiers = (response.json \ "identifiers").as[List[SubscriptionIdentifier]]
    val optUtr = identifiers.find(_.key == "SAUTR").map(_.value)
    (optState, optUtr) match {
      case (Some(state), Some(utr)) => TaxEnrolmentsSubscriptionsResponse(subscriptionId, utr, state)
      case (None, Some(utr)) => throw InvalidDataException(s"[TaxEnrolmentsSubscriptions] [SubscriptionId: $subscriptionId, UTR: $utr] No State supplied")
      case (Some(_), None) => throw InvalidDataException(s"[TaxEnrolmentsSubscriptions] [SubscriptionId: $subscriptionId] No UTR supplied")
      case _ =>  throw InvalidDataException(s"[TaxEnrolmentsSubscriptions] [SubscriptionId: $subscriptionId] No State or UTR supplied")
    }
  }

  case class SubscriptionIdentifier(key: String, value: String)

  object SubscriptionIdentifier {
    implicit val formats: Format[SubscriptionIdentifier] = Json.format[SubscriptionIdentifier]
  }
}

