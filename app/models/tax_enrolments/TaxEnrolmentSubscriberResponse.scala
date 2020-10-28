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

package models.tax_enrolments

import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import exceptions._


sealed trait TaxEnrolmentSuscriberResponse

case object TaxEnrolmentSuccess extends TaxEnrolmentSuscriberResponse
case object TaxEnrolmentFailure extends TaxEnrolmentSuscriberResponse
case object TaxEnrolmentNotProcessed extends TaxEnrolmentSuscriberResponse

object TaxEnrolmentSuscriberResponse extends Logging {

  implicit lazy val httpReads: HttpReads[TaxEnrolmentSuscriberResponse] =
    new HttpReads[TaxEnrolmentSuscriberResponse] {
      override def read(method: String, url: String, response: HttpResponse): TaxEnrolmentSuscriberResponse = {

        logger.info(s"Response status received from tax enrolment: ${response.status}")
        response.status match {
          case NO_CONTENT =>
            TaxEnrolmentSuccess
          case BAD_REQUEST =>
            logger.error("Bad request response received from tax enrolment")
            throw  BadRequestException
          case status =>
            logger.error(s"Error response from tax enrolment:  $status")
            throw  InternalServerErrorException(s"Error response from tax enrolment:  $status")
        }
      }
    }
}

