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

import controllers.actions.IdentifierAction
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import services.TrustDetailsTransformationService
import transformers.trustDetails.SetTrustDetailTransform
import utils.ValidationUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TrustDetailsTransformationController @Inject()(identify: IdentifierAction,
                                                     transformService: TrustDetailsTransformationService)
                                                    (implicit val executionContext: ExecutionContext, cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def setExpress(identifier: String): Action[JsValue] = set(identifier, "expressTrust")
  def setResident(identifier: String): Action[JsValue] = set(identifier, "trustUKResident")
  def setTaxable(identifier: String): Action[JsValue] = set(identifier, "trustTaxable")
  def setProperty(identifier: String): Action[JsValue] = set(identifier, "trustUKProperty")
  def setRecorded(identifier: String): Action[JsValue] = set(identifier, "trustRecorded")
  def setUKRelation(identifier: String): Action[JsValue] = set(identifier, "trustUKRelation")

  private def set(identifier: String, trustDetail: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[Boolean] match {
        case JsSuccess(value, _) =>
          transformService.set(
            identifier,
            request.internalId,
            SetTrustDetailTransform(value, trustDetail)
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[set][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
            s"Supplied json could not be read as a Boolean - $errors")
          Future.successful(BadRequest)
      }
    }
  }
}
