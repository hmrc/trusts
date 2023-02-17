/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.transformations.RemoveTransformationController
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform
import transformers.remove.{Remove, RemoveSettlor}
import transformers.settlors.RemoveSettlorTransform

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RemoveSettlorController @Inject()(identify: IdentifierAction,
                                        transformationService: TransformationService)
                                       (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends RemoveTransformationController(identify, transformationService) with SettlorController {

  def remove(identifier: String): Action[JsValue] = addNewTransform[RemoveSettlor](identifier)

  override def transform[T <: Remove](remove: T, entity: JsValue): DeltaTransform = {
    RemoveSettlorTransform(Some(remove.index), entity, remove.endDate, remove.`type`)
  }
}
