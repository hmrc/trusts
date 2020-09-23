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
import org.slf4j.LoggerFactory
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class TaxEnrolmentCallbackController @Inject()(
                                                cc: ControllerComponents
                                               ) extends BackendController(cc) {

  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

  def subscriptionCallback() = Action.async(parse.json) {
    implicit request =>
      logger.info(s"[subscriptionCallback] Tax-Enrolment: subscription callback message was  : ${request.body}")
      Future(Ok(""))
  }

}
