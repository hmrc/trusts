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

package controllers

import controllers.actions.IdentifierAction
import javax.inject.Inject
import models.variation._
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import services.AssetsTransformationService
import transformers.remove.RemoveAsset
import utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class AssetsTransformationController @Inject()(
                                                identify: IdentifierAction,
                                                assetsTransformationService: AssetsTransformationService
                                              )(implicit val executionContext: ExecutionContext, cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def amendNonEeaBusiness(identifier: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[NonEEABusinessType] match {
        case JsSuccess(nonEEABusiness, _) =>
          assetsTransformationService.amendNonEeaBusinessAssetTransformer(
            identifier,
            index,
            request.identifier,
            nonEEABusiness
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendNonEeaBusinessAsset][Session ID: ${request.sessionId}]" +
            s" Supplied json could not be read as a NonEEABusinessType - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addNonEeaBusiness(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[NonEEABusinessType] match {
        case JsSuccess(newEeaBusinessAsset, _) =>
          assetsTransformationService.addNonEeaBusinessAssetTransformer(
            identifier,
            request.identifier,
            newEeaBusinessAsset
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addNonEeaBusinessAsset][Session ID: ${request.sessionId}]" +
            s" Supplied json could not be read as a NonEEABusinessType - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def removeAsset(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveAsset] match {
        case JsSuccess(asset, _) =>
          assetsTransformationService.removeAsset(identifier, request.identifier, asset) map { _ =>
            Ok
          }
        case JsError(_) => Future.successful(BadRequest)
      }
    }
  }
}
