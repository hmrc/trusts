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

package controllers.transformations.assets

import controllers.actions.IdentifierAction
import controllers.transformations.AddTransformationController
import models.variation.{AssetMonetaryAmount, BusinessAssetType, NonEEABusinessType, OtherAssetType, PartnershipType, PropertyLandType, SharesType}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform
import transformers.assets.AddAssetTransform

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddAssetController @Inject()(identify: IdentifierAction,
                                   transformationService: TransformationService)
                                  (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AddTransformationController(identify, transformationService) {

  def addMoney(identifier: String): Action[JsValue] = addNewTransform[AssetMonetaryAmount](identifier)

  def addPropertyOrLand(identifier: String): Action[JsValue] = addNewTransform[PropertyLandType](identifier)

  def addShares(identifier: String): Action[JsValue] = addNewTransform[SharesType](identifier)

  def addBusiness(identifier: String): Action[JsValue] = addNewTransform[BusinessAssetType](identifier)

  def addPartnership(identifier: String): Action[JsValue] = addNewTransform[PartnershipType](identifier)

  def addOther(identifier: String): Action[JsValue] = addNewTransform[OtherAssetType](identifier)

  def addNonEeaBusiness(identifier: String): Action[JsValue] = addNewTransform[NonEEABusinessType](identifier)

  override def transform[T](value: T, key: String)(implicit wts: Writes[T]): DeltaTransform = {
    AddAssetTransform(Json.toJson(value), value.toString)
  }
}
