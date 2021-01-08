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

package controllers.transformations.beneficiaries

import controllers.actions.IdentifierAction
import controllers.transformations.AddTransformationController
import models.variation._
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform
import transformers.beneficiaries.AddBeneficiaryTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddBeneficiaryController @Inject()(identify: IdentifierAction,
                                         transformationService: TransformationService
                                        )(implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AddTransformationController(identify, transformationService) {

  def addUnidentified(identifier: String): Action[JsValue] = addNewTransform[UnidentifiedType](identifier, UNIDENTIFIED_BENEFICIARY)

  def addIndividual(identifier: String): Action[JsValue] = addNewTransform[IndividualDetailsType](identifier, INDIVIDUAL_BENEFICIARY)

  def addCharity(identifier: String): Action[JsValue] = addNewTransform[BeneficiaryCharityType](identifier, CHARITY_BENEFICIARY)

  def addOther(identifier: String): Action[JsValue] = addNewTransform[OtherType](identifier, OTHER_BENEFICIARY)

  def addCompany(identifier: String): Action[JsValue] = addNewTransform[BeneficiaryCompanyType](identifier, COMPANY_BENEFICIARY)

  def addTrust(identifier: String): Action[JsValue] = addNewTransform[BeneficiaryTrustType](identifier, TRUST_BENEFICIARY)

  def addLarge(identifier: String): Action[JsValue] = addNewTransform[LargeType](identifier, LARGE_BENEFICIARY)

  override def transform[T](value: T, `type`: String)(implicit wts: Writes[T]): DeltaTransform = {
    AddBeneficiaryTransform(Json.toJson(value), `type`)
  }
}
