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

package controllers.transformations.otherindividuals

import controllers.actions.IdentifierAction
import controllers.transformations.AmendTransformationController
import models.variation.NaturalPersonType
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.{LocalDateService, TransformationService}
import transformers.DeltaTransform
import transformers.otherindividuals.AmendOtherIndividualTransform

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AmendOtherIndividualController @Inject()(identify: IdentifierAction,
                                               transformationService: TransformationService,
                                               localDateService: LocalDateService)
                                              (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AmendTransformationController(identify, transformationService) with OtherIndividualController {

  def amend(identifier: String, index: Int): Action[JsValue] = addNewTransform[NaturalPersonType](identifier, Some(index))

  override def transform[T](original: JsValue, amended: T, index: Option[Int], `type`: String)(implicit wts: Writes[T]): DeltaTransform = {
    AmendOtherIndividualTransform(index, Json.toJson(amended), original, localDateService.now)
  }
}
