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
import controllers.transformations.AmendTransformationController
import models.variation._
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, ControllerComponents}
import services.{LocalDateService, TransformationService}
import transformers.DeltaTransform
import transformers.assets.AmendAssetTransform
import utils.Constants.ASSETS

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AmendAssetController @Inject()(identify: IdentifierAction,
                                     transformationService: TransformationService,
                                     localDateService: LocalDateService)
                                    (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AmendTransformationController(identify, transformationService) {

  override val section: String = ASSETS

  def amendMoney(identifier: String, index: Int): Action[JsValue] = addNewTransform[AssetMonetaryAmount](identifier, index)

  def amendPropertyOrLand(identifier: String, index: Int): Action[JsValue] = addNewTransform[PropertyLandType](identifier, index)

  def amendShares(identifier: String, index: Int): Action[JsValue] = addNewTransform[SharesType](identifier, index)

  def amendBusiness(identifier: String, index: Int): Action[JsValue] = addNewTransform[BusinessAssetType](identifier, index)

  def amendPartnership(identifier: String, index: Int): Action[JsValue] = addNewTransform[PartnershipType](identifier, index)

  def amendOther(identifier: String, index: Int): Action[JsValue] = addNewTransform[OtherAssetType](identifier, index)

  def amendNonEeaBusiness(identifier: String, index: Int): Action[JsValue] = addNewTransform[NonEEABusinessType](identifier, index)

  override def transform[T](original: JsValue, amended: T, index: Int)(implicit wts: Writes[T]): DeltaTransform = {
    AmendAssetTransform(index, Json.toJson(amended), original, localDateService.now, amended.toString)
  }
}
