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
import models.FirstTaxYearAvailable
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.RegistrationSubmissionRepository
import services.TaxYearService
import services.dates.LocalDateTimeService

import java.time.LocalDate
import javax.inject.Inject

class TaxLiabilitySubmissionDraftController @Inject()(
                                                submissionRepository: RegistrationSubmissionRepository,
                                                identify: IdentifierAction,
                                                localDateTimeService: LocalDateTimeService,
                                                cc: ControllerComponents,
                                                taxYearService: TaxYearService
                                              ) extends SubmissionDraftController(submissionRepository, identify, localDateTimeService, cc) {

  private val whenTrustSetupPath: JsPath = JsPath \ "trustDetails" \ "data" \ "trustDetails" \ "whenTrustSetup"

  def getTaxLiabilityStartDate(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>
      val taxLiabilityStartDatePath = __ \ "taxLiability" \ "data" \ "trustStartDate"
      getResult[LocalDate, JsValue](draftId, taxLiabilityStartDatePath)(x => Json.obj("startDate" -> x))
  }

  def getFirstTaxYearAvailable(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>

      getResult[LocalDate, FirstTaxYearAvailable](draftId, whenTrustSetupPath)(taxYearService.firstTaxYearAvailable)
  }
}
