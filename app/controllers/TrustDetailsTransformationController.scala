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

package controllers

import controllers.actions.IdentifierAction
import models.ResidentialStatusType
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.TrustDetailsTransformationService
import transformers.trustDetails.SetTrustDetailTransform
import utils.Constants._
import utils.ValidationUtil

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TrustDetailsTransformationController @Inject()(identify: IdentifierAction,
                                                     transformService: TrustDetailsTransformationService)
                                                    (implicit val executionContext: ExecutionContext, cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def setExpress(identifier: String): Action[JsValue] = set[Boolean](identifier, EXPRESS)
  def setResident(identifier: String): Action[JsValue] = set[Boolean](identifier, UK_RESIDENT)
  def setTaxable(identifier: String): Action[JsValue] = set[Boolean](identifier, TAXABLE)
  def setProperty(identifier: String): Action[JsValue] = set[Boolean](identifier, UK_PROPERTY)
  def setRecorded(identifier: String): Action[JsValue] = set[Boolean](identifier, RECORDED)
  def setUKRelation(identifier: String): Action[JsValue] = set[Boolean](identifier, UK_RELATION)

  def setLawCountry(identifier: String): Action[JsValue] = set[String](identifier, LAW_COUNTRY)
  def setAdministrationCountry(identifier: String): Action[JsValue] = set[String](identifier, ADMINISTRATION_COUNTRY)
  def setTypeOfTrust(identifier: String): Action[JsValue] = set[String](identifier, TYPE_OF_TRUST)
  def setDeedOfVariation(identifier: String): Action[JsValue] = set[String](identifier, DEED_OF_VARIATION)
  def setInterVivos(identifier: String): Action[JsValue] = set[Boolean](identifier, INTER_VIVOS)
  def setEfrbsStartDate(identifier: String): Action[JsValue] = set[LocalDate](identifier, EFRBS_START_DATE)
  def setResidentialStatus(identifier: String): Action[JsValue] = set[ResidentialStatusType](identifier, RESIDENTIAL_STATUS)

  private def set[T](identifier: String, key: String)
                    (implicit rds: Reads[T], wts: Writes[T]): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[T] match {
        case JsSuccess(value, _) =>
          transformService.set(
            identifier,
            request.internalId,
            SetTrustDetailTransform(Json.toJson(value), key)
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[set][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
            s"Supplied json did not pass validation - $errors")
          Future.successful(BadRequest)
      }
    }
  }
}
