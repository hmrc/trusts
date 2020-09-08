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

import play.api.Logger
import play.api.mvc.Action
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.trusts.config.AppConfig
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class TaxEnrolmentCallbackController @Inject()(
                                               auditConnector :AuditConnector
                                               ) extends BaseController {

  def subscriptionCallback() = Action.async(parse.json) {
    implicit request =>
      Logger.info(s"[subscriptionCallback] Tax-Enrolment: subscription callback message was  : ${request.body}")
      Future(Ok(""))
  }

}
