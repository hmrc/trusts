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

package controllers.transformations.protectors

import controllers.actions.IdentifierAction
import controllers.transformations.RemoveTransformationController
import play.api.libs.json.{JsPath, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform
import transformers.protectors.RemoveProtectorTransform
import transformers.remove.{Remove, RemoveProtector}
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RemoveProtectorController @Inject()(identify: IdentifierAction,
                                          transformationService: TransformationService)
                                         (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends RemoveTransformationController(identify, transformationService) {

  override def path(`type`: String, index: Int): JsPath = ENTITIES \ PROTECTORS \ `type` \ index

  def remove(identifier: String): Action[JsValue] = addNewTransform[RemoveProtector](identifier)

  override def transform[T <: Remove](remove: T, entity: JsValue): DeltaTransform = {
    RemoveProtectorTransform(remove.index, entity, remove.endDate, remove.`type`)
  }
}
