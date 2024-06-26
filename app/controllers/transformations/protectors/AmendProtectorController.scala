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

package controllers.transformations.protectors

import controllers.actions.IdentifierAction
import controllers.transformations.AmendTransformationController
import models.variation._
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import services.dates.LocalDateService
import transformers.DeltaTransform
import transformers.protectors.AmendProtectorTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AmendProtectorController @Inject()(identify: IdentifierAction,
                                         transformationService: TransformationService,
                                         localDateService: LocalDateService)
                                        (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AmendTransformationController(identify, transformationService) with ProtectorController {

  def amendIndividual(identifier: String, index: Int): Action[JsValue] = addNewTransform[ProtectorIndividual](identifier, Some(index), INDIVIDUAL_PROTECTOR)

  def amendBusiness(identifier: String, index: Int): Action[JsValue] = addNewTransform[ProtectorCompany](identifier, Some(index), BUSINESS_PROTECTOR)

  override def transform[T](original: JsValue, amended: T, index: Option[Int], `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform = {
    AmendProtectorTransform(index, Json.toJson(amended), original, localDateService.now, `type`)
  }
}
