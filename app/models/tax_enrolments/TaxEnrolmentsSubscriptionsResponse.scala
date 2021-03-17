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

import play.api.libs.json.{Format, Json}

case class TaxEnrolmentsSubscriptionsResponse(identifiers: List[SubscriptionIdentifier], callback: String, state: String) {

  val urn: Option[String] = {
    val urnStart = callback.indexOf("/urn/")
    if (urnStart > 0) {
      val strippedCallback = callback.substring(urnStart + 5)
      val urnEnd = strippedCallback.indexOf("/")
      val urn = if (urnEnd > 0) strippedCallback.substring(0, urnEnd) else strippedCallback
      if (urn.trim.isEmpty) None else Some(urn)
    } else {
      None
    }
  }

  val utr: Option[String] = identifiers.find(_.key == "SAUTR").map(_.value)
}

object TaxEnrolmentsSubscriptionsResponse {
  implicit val formats: Format[TaxEnrolmentsSubscriptionsResponse] = Json.format[TaxEnrolmentsSubscriptionsResponse]
}

case class SubscriptionIdentifier(key: String, value: String)

object SubscriptionIdentifier {
  implicit val formats: Format[SubscriptionIdentifier] = Json.format[SubscriptionIdentifier]
}


