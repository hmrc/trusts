/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.transformations.otherindividuals

import controllers.actions.IdentifierAction
import controllers.transformations.AddTransformationController
import models.variation.NaturalPersonType
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, ControllerComponents}
import services.{TaxableMigrationService, TransformationService}
import transformers.DeltaTransform
import transformers.otherindividuals.AddOtherIndividualTransform

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddOtherIndividualController @Inject() (
  identify: IdentifierAction,
  transformationService: TransformationService,
  taxableMigrationService: TaxableMigrationService
)(implicit ec: ExecutionContext, cc: ControllerComponents)
    extends AddTransformationController(identify, transformationService, taxableMigrationService) {

  def add(identifier: String): Action[JsValue] = addNewTransform[NaturalPersonType](identifier)

  override def transform[T](value: T, `type`: String, isTaxable: Boolean, migratingFromNonTaxableToTaxable: Boolean)(
    implicit wts: Writes[T]
  ): DeltaTransform =
    AddOtherIndividualTransform(Json.toJson(value))

}
