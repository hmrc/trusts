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
import controllers.transformations.AddTransformationController
import models.variation._
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, ControllerComponents}
import services.{TaxableMigrationService, TransformationService}
import transformers.DeltaTransform
import transformers.assets.AddAssetTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddAssetController @Inject()(identify: IdentifierAction,
                                   transformationService: TransformationService,
                                   taxableMigrationService: TaxableMigrationService)
                                  (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AddTransformationController(identify, transformationService, taxableMigrationService) with AssetController {

  def addMoney(identifier: String, index: Int): Action[JsValue] = addNewTransform[AssetMonetaryAmountType](identifier, MONEY_ASSET, false, Some(index))

  def addPropertyOrLand(identifier: String, index: Int): Action[JsValue] = addNewTransform[PropertyLandType](identifier, PROPERTY_OR_LAND_ASSET, false, Some(index))

  def addShares(identifier: String, index: Int): Action[JsValue] = addNewTransform[SharesType](identifier, SHARES_ASSET, false, Some(index))

  def addBusiness(identifier: String, index: Int): Action[JsValue] = addNewTransform[BusinessAssetType](identifier, BUSINESS_ASSET, false, Some(index))

  def addPartnership(identifier: String, index: Int): Action[JsValue] = addNewTransform[PartnershipType](identifier, PARTNERSHIP_ASSET, false, Some(index))

  def addOther(identifier: String, index: Int): Action[JsValue] = addNewTransform[OtherAssetType](identifier, OTHER_ASSET, false, Some(index))

  def addNonEeaBusiness(identifier: String, index: Int): Action[JsValue] = addNewTransform[NonEEABusinessType](identifier, NON_EEA_BUSINESS_ASSET, false, Some(index))

  override def transform[T](value: T, `type`: String, isTaxable: Boolean, migratingFromNonTaxableToTaxable: Boolean)
                           (implicit wts: Writes[T]): DeltaTransform = {
    AddAssetTransform(Json.toJson(value), `type`)
  }
}
