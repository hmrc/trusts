/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.actions.IdentifierAction
import models.taxable_migration.TaxableMigrationFlag
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.TaxableMigrationService
import javax.inject.Inject
import utils.Session

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxableMigrationController @Inject()(
                                            identify: IdentifierAction,
                                            taxableMigrationService: TaxableMigrationService,
                                            cc: ControllerComponents
                                          ) extends TrustsBaseController(cc) with Logging {

  def getTaxableMigrationFlag(identifier: String): Action[AnyContent] = identify.async { implicit request =>
    taxableMigrationService.getTaxableMigrationFlag(identifier, request.internalId, Session.id(hc)) map { x =>
      Ok(Json.toJson(TaxableMigrationFlag(x)))
    } recoverWith {
      case _ => Future.successful(InternalServerError)
    }
  }

  def setTaxableMigrationFlag(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[Boolean] match {
        case JsSuccess(migratingToTaxable, _) =>
          taxableMigrationService.setTaxableMigrationFlag(identifier, request.internalId, Session.id(hc), migratingToTaxable) map { _ =>
            Ok
          } recoverWith {
            case _ => Future.successful(InternalServerError)
          }
        case JsError(errors) =>
          logger.warn(s"[TaxableMigrationController][setTaxableMigrationFlag] failed to validate request body: $errors")
          Future.successful(BadRequest)
      }
  }

}
