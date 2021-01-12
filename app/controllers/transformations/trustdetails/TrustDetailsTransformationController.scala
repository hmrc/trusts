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

package controllers.transformations.trustdetails

import controllers.actions.IdentifierAction
import controllers.transformations.AddTransformationController
import models.ResidentialStatusType
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform
import transformers.trustdetails.SetTrustDetailTransform
import utils.Constants._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TrustDetailsTransformationController @Inject()(identify: IdentifierAction,
                                                     transformationService: TransformationService)
                                                    (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AddTransformationController(identify, transformationService) {

  def setExpress(identifier: String): Action[JsValue] = addNewTransform[Boolean](identifier, EXPRESS)
  def setResident(identifier: String): Action[JsValue] = addNewTransform[Boolean](identifier, UK_RESIDENT)
  def setTaxable(identifier: String): Action[JsValue] = addNewTransform[Boolean](identifier, TAXABLE)
  def setProperty(identifier: String): Action[JsValue] = addNewTransform[Boolean](identifier, UK_PROPERTY)
  def setRecorded(identifier: String): Action[JsValue] = addNewTransform[Boolean](identifier, RECORDED)
  def setUKRelation(identifier: String): Action[JsValue] = addNewTransform[Boolean](identifier, UK_RELATION)

  def setLawCountry(identifier: String): Action[JsValue] = addNewTransform[String](identifier, LAW_COUNTRY)
  def setAdministrationCountry(identifier: String): Action[JsValue] = addNewTransform[String](identifier, ADMINISTRATION_COUNTRY)
  def setTypeOfTrust(identifier: String): Action[JsValue] = addNewTransform[String](identifier, TYPE_OF_TRUST)
  def setDeedOfVariation(identifier: String): Action[JsValue] = addNewTransform[String](identifier, DEED_OF_VARIATION)
  def setInterVivos(identifier: String): Action[JsValue] = addNewTransform[Boolean](identifier, INTER_VIVOS)
  def setEfrbsStartDate(identifier: String): Action[JsValue] = addNewTransform[LocalDate](identifier, EFRBS_START_DATE)
  def setResidentialStatus(identifier: String): Action[JsValue] = addNewTransform[ResidentialStatusType](identifier, RESIDENTIAL_STATUS)

  override def transform[T](value: T, `type`: String)(implicit wts: Writes[T]): DeltaTransform = {
    SetTrustDetailTransform(Json.toJson(value), `type`)
  }
}
