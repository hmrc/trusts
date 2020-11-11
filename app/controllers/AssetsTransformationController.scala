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
import play.api.libs.json.{JsError, JsString, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import services.{AssetsTransformationService, BeneficiaryTransformationService}
import transformers.remove.{RemoveAsset, RemoveBeneficiary}
import utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class AssetsTransformationController @Inject()(
                                          identify: IdentifierAction,
                                          assetsTransformationService: AssetsTransformationService
                                        )(implicit val executionContext: ExecutionContext,
                                          cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def amendNonEeaBusinessAsset(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[NonEEABusinessType] match {
        case JsSuccess(nonEEABusiness, _) =>
          assetsTransformationService.amendNonEeaBusinessAssetTransformer(
            utr,
            index,
            request.identifier,
            nonEEABusiness
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendNonEeaBusinessAsset][Session ID: ${request.sessionId}]" +
            s" Supplied description could not be read as a JsString - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addNonEeaBusinessAsset(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[NonEEABusinessType] match {
        case JsSuccess(newEeaBusinessAsset, _) =>

          assetsTransformationService.addNonEeaBusinessAssetTransformer(
            utr,
            request.identifier,
            newEeaBusinessAsset
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addNonEeaBusinessAsset][Session ID: ${request.sessionId}] " +
            s"Supplied json could not be read as an addEeaBusiness Asset - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def removeNonEeaBusinessAsset(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveAsset] match {
        case JsSuccess(asset, _) =>
          assetsTransformationService.removeAsset(utr, request.identifier, asset) map { _ =>
            Ok
          }
        case JsError(_) => Future.successful(BadRequest)
      }
    }
  }
}
