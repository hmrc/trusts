/*
 * Copyright 2024 HM Revenue & Customs
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
import services.TransformationService
import services.dates.LocalDateService
import transformers.DeltaTransform
import transformers.assets.AmendAssetTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AmendAssetController @Inject()(identify: IdentifierAction,
                                     transformationService: TransformationService,
                                     localDateService: LocalDateService)
                                    (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AmendTransformationController(identify, transformationService) with AssetController {

  def amendMoney(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[AssetMonetaryAmountType](identifier, Some(index), MONEY_ASSET)

  def amendPropertyOrLand(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[PropertyLandType](identifier, Some(index), PROPERTY_OR_LAND_ASSET)

  def amendShares(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[SharesType](identifier, Some(index), SHARES_ASSET)

  def amendBusiness(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[BusinessAssetType](identifier, Some(index), BUSINESS_ASSET)

  def amendPartnership(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[PartnershipType](identifier, Some(index), PARTNERSHIP_ASSET)

  def amendOther(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[OtherAssetType](identifier, Some(index), OTHER_ASSET)

  def amendNonEeaBusiness(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[NonEEABusinessType](identifier, Some(index), NON_EEA_BUSINESS_ASSET)

  override def transform[T](original: JsValue, amended: T, index: Option[Int], `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform = {
    AmendAssetTransform(index, Json.toJson(amended), original, localDateService.now, `type`)
  }
}
