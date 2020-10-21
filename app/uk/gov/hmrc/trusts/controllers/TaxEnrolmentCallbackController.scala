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

package uk.gov.hmrc.trusts.controllers

import javax.inject.Inject
import play.api.Logging
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.trusts.utils.Session

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class TaxEnrolmentCallbackController @Inject()(
                                                cc: ControllerComponents
                                               ) extends BackendController(cc) with Logging {

  def subscriptionCallback() = Action.async(parse.json) {
    implicit request =>
      val hc : HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

      logger.info(s"[subscriptionCallback][Session ID: ${Session.id(hc)}]" +
        s" Tax-Enrolment: subscription callback message was  : ${request.body}")
      Future(Ok(""))
  }

}
