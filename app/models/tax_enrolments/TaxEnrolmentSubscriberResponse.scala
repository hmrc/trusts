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
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

sealed trait TaxEnrolmentSubscriberResponse

case object TaxEnrolmentSuccess extends TaxEnrolmentSubscriberResponse
case object TaxEnrolmentFailure extends TaxEnrolmentSubscriberResponse
case object TaxEnrolmentNotProcessed extends TaxEnrolmentSubscriberResponse

case class TaxEnrolmentFailureResponse(message: String) extends TaxEnrolmentSubscriberResponse

object TaxEnrolmentSubscriberResponse extends Logging {

  implicit lazy val httpReads: HttpReads[TaxEnrolmentSubscriberResponse] = (_: String, _: String, response: HttpResponse) => {
    response.status match {
      case NO_CONTENT =>
        TaxEnrolmentSuccess
      case BAD_REQUEST =>
        logger.error("[TaxEnrolmentSubscriberResponse][httpReads] Bad request response received from tax enrolment")
        TaxEnrolmentFailureResponse("Bad request")
      case status =>
        logger.error(s"[TaxEnrolmentSubscriberResponse][httpReads] Unexpected error response from tax enrolment: $status")
        TaxEnrolmentFailureResponse(s"Unexpected error response from tax enrolment. Status: $status")
    }
  }
}
