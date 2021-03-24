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
import play.api.mvc.{Action, ControllerComponents}
import reactivemongo.play.json.readOpt.optionReads
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Session

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OrchestratorCallbackController @Inject()(
                                                cc: ControllerComponents
                                               ) extends BackendController(cc) with Logging {

  def migrationToTaxableCallback(urn: String, utr: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val hc : HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

      logger.info(s"[migrationToTaxableCallback][Session ID: ${Session.id(hc)}][URN: $urn, UTR: $utr]" +
        s" Orchestrator: migrate subscription callback message was: ${request.body}")

      val success = (request.body \ "success").asOpt[Boolean]
      success match {
        case Some(false) => {
          val errorMessage = (request.body \ "errorMessage").as[Option[String]]
          logger.error(s"[migrationToTaxableCallback][Session ID: ${Session.id(hc)}][URN: $urn, UTR: $utr]" +
            s" Orchestrator: migrate subscription failed, error message was: ${errorMessage.getOrElse(request.body)}")
          Future(NoContent)
        }
        case _ => {
          Future(NoContent)
        }
      }

  }
}
