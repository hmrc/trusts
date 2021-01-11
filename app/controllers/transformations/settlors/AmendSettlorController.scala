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

package controllers.transformations.settlors

import controllers.actions.IdentifierAction
import controllers.transformations.AmendTransformationController
import models.variation._
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.{LocalDateService, TransformationService}
import transformers.DeltaTransform
import transformers.settlors.AmendSettlorTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AmendSettlorController @Inject()(identify: IdentifierAction,
                                       transformationService: TransformationService,
                                       localDateService: LocalDateService)
                                      (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AmendTransformationController(identify, transformationService) with SettlorController {

  def amendIndividual(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[Settlor](identifier, index, `type` = INDIVIDUAL_SETTLOR)

  def amendBusiness(identifier: String, index: Int): Action[JsValue] =
    addNewTransform[SettlorCompany](identifier, index, `type` = BUSINESS_SETTLOR)

  def amendDeceased(identifier: String): Action[JsValue] =
    addNewTransform[AmendDeceasedSettlor](identifier, `type` = DECEASED_SETTLOR)

  override def transform[T](original: JsValue, amended: T, index: Int, `type`: String)(implicit wts: Writes[T]): DeltaTransform = {
    AmendSettlorTransform(index, Json.toJson(amended), original, localDateService.now, `type`)
  }
}
