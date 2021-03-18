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

package controllers.transformations.taxliability

import controllers.actions.IdentifierAction
import controllers.transformations.AddTransformationController
import models.YearsReturns
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform
import transformers.taxliability.SetTaxLiabilityTransform

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TaxLiabilityTransformationController @Inject()(identify: IdentifierAction,
                                                     transformationService: TransformationService)
                                                    (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AddTransformationController(identify, transformationService) {

  def setYearsReturns(identifier: String): Action[JsValue] = addNewTransform[YearsReturns](identifier)

  override def transform[T](value: T, `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform = {
    SetTaxLiabilityTransform(Json.toJson(value))
  }
}
