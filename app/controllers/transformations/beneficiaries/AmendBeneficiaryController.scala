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

package controllers.transformations.beneficiaries

import controllers.actions.IdentifierAction
import controllers.transformations.AmendTransformationController
import models.variation._
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import services.dates.LocalDateService
import transformers.DeltaTransform
import transformers.beneficiaries.AmendBeneficiaryTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AmendBeneficiaryController @Inject()(identify: IdentifierAction,
                                           transformationService: TransformationService,
                                           localDateService: LocalDateService)
                                          (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AmendTransformationController(identify, transformationService) with BeneficiaryController {

  def amendUnidentified(identifier: String, index: Int): Action[JsValue] = addNewTransform[String](identifier, Some(index), UNIDENTIFIED_BENEFICIARY)

  def amendIndividual(identifier: String, index: Int): Action[JsValue] = addNewTransform[IndividualDetailsType](identifier, Some(index), INDIVIDUAL_BENEFICIARY)

  def amendCharity(identifier: String, index: Int): Action[JsValue] = addNewTransform[BeneficiaryCharityType](identifier, Some(index), CHARITY_BENEFICIARY)

  def amendOther(identifier: String, index: Int): Action[JsValue] = addNewTransform[OtherType](identifier, Some(index), OTHER_BENEFICIARY)

  def amendCompany(identifier: String, index: Int): Action[JsValue] = addNewTransform[BeneficiaryCompanyType](identifier, Some(index), COMPANY_BENEFICIARY)

  def amendTrust(identifier: String, index: Int): Action[JsValue] = addNewTransform[BeneficiaryTrustType](identifier, Some(index), TRUST_BENEFICIARY)

  def amendLarge(identifier: String, index: Int): Action[JsValue] = addNewTransform[LargeType](identifier, Some(index), LARGE_BENEFICIARY)

  override def transform[T](original: JsValue, amended: T, index: Option[Int], `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform = {
    AmendBeneficiaryTransform(index, Json.toJson(amended), original, localDateService.now, `type`)
  }
}
