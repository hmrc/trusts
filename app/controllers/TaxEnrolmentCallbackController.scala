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

package controllers

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.TaxableMigrationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Session

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class TaxEnrolmentCallbackController @Inject()(migrationService: TaxableMigrationService,
                                               cc: ControllerComponents
                                               ) extends BackendController(cc) with Logging {

  def taxableSubscriptionCallback(trn: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val hc : HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

      logger.info(s"[taxableSubscriptionCallback][Session ID: ${Session.id(hc)}][TRN: $trn]" +
        s" Tax-enrolment: taxable subscription callback message was: ${request.body}")
      Future(Ok(""))
  }

  def nonTaxableSubscriptionCallback(trn: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val hc : HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

      logger.info(s"[nonTaxableSubscriptionCallback][Session ID: ${Session.id(hc)}][TRN: $trn]" +
        s" Tax-enrolment: non-taxable subscription callback message was: ${request.body}")
      Future(Ok(""))
  }

  def migrationSubscriptionCallback(subscriptionId: String, urn: String): Action[AnyContent] = Action.async {
    implicit request =>
      implicit val hc : HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

      logger.info(s"[migrationSubscriptionCallback][Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId, URN: $urn]" +
        s" Tax-enrolment: migration subscription callback triggered")
      for {
        utr <- migrationService.completeMigration(subscriptionId, urn)
      } yield {
        logger.info(s"[migrationSubscriptionCallback] callback complete utr $utr")
        Ok("")
      }
  }
}
