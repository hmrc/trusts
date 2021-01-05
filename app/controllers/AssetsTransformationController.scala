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
import models.variation._
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.AssetsTransformationService
import transformers.remove.RemoveAsset
import utils.ValidationUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssetsTransformationController @Inject()(identify: IdentifierAction,
                                               assetsTransformationService: AssetsTransformationService)
                                              (implicit val executionContext: ExecutionContext, cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def addMoney(identifier: String): Action[JsValue] = addAsset[AssetMonetaryAmount](identifier)

  def amendMoney(identifier: String, index: Int): Action[JsValue] = amendAsset[AssetMonetaryAmount](identifier, index)

  def addPropertyOrLand(identifier: String): Action[JsValue] = addAsset[PropertyLandType](identifier)

  def amendPropertyOrLand(identifier: String, index: Int): Action[JsValue] = amendAsset[PropertyLandType](identifier, index)

  def addShares(identifier: String): Action[JsValue] = addAsset[SharesType](identifier)

  def amendShares(identifier: String, index: Int): Action[JsValue] = amendAsset[SharesType](identifier, index)

  def addBusiness(identifier: String): Action[JsValue] = addAsset[BusinessAssetType](identifier)

  def amendBusiness(identifier: String, index: Int): Action[JsValue] = amendAsset[BusinessAssetType](identifier, index)

  def addPartnership(identifier: String): Action[JsValue] = addAsset[PartnershipType](identifier)

  def amendPartnership(identifier: String, index: Int): Action[JsValue] = amendAsset[PartnershipType](identifier, index)

  def addOther(identifier: String): Action[JsValue] = addAsset[OtherAssetType](identifier)

  def amendOther(identifier: String, index: Int): Action[JsValue] = amendAsset[OtherAssetType](identifier, index)

  def addNonEeaBusiness(identifier: String): Action[JsValue] = addAsset[NonEEABusinessType](identifier)

  def amendNonEeaBusiness(identifier: String, index: Int): Action[JsValue] = amendAsset[NonEEABusinessType](identifier, index)

  private def addAsset[T <: AssetType](identifier: String)
                                      (implicit rds: Reads[T], wts: Writes[T]): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[T] match {
        case JsSuccess(asset, _) =>
          assetsTransformationService.addAsset(
            identifier = identifier,
            internalId = request.internalId,
            asset = asset
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addAsset][Session ID: ${request.sessionId}] Supplied json could not be read as asset type - $errors")
          Future.successful(BadRequest)
      }
  }

  private def amendAsset[T <: AssetType](identifier: String, index: Int)
                                        (implicit rds: Reads[T], wts: Writes[T]): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[T] match {
        case JsSuccess(asset, _) =>
          assetsTransformationService.amendAsset(
            identifier = identifier,
            index = index,
            internalId = request.internalId,
            asset = asset
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendAsset][Session ID: ${request.sessionId}] Supplied json could not be read as asset type - $errors")
          Future.successful(BadRequest)
      }
  }

  def removeAsset(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[RemoveAsset] match {
        case JsSuccess(asset, _) =>
          assetsTransformationService.removeAsset(
            identifier = identifier,
            internalId = request.internalId,
            asset = asset
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendAsset][Session ID: ${request.sessionId}] Supplied json could not be read as RemoveAsset - $errors")
          Future.successful(BadRequest)
      }
  }
}
