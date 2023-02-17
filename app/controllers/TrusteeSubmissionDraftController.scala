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

package controllers

import controllers.actions.IdentifierAction
import models.LeadTrusteeType
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.RegistrationSubmissionRepository
import services.dates.LocalDateTimeService

import javax.inject.Inject

class TrusteeSubmissionDraftController @Inject()(
                                                submissionRepository: RegistrationSubmissionRepository,
                                                identify: IdentifierAction,
                                                localDateTimeService: LocalDateTimeService,
                                                cc: ControllerComponents
                                              ) extends SubmissionDraftController(submissionRepository, identify, localDateTimeService, cc) {

  def getLeadTrustee(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>
      val path = JsPath \ "registration" \ "trust/entities/leadTrustees"
      implicit val writes: Writes[LeadTrusteeType] = LeadTrusteeType.writes

      getResult[LeadTrusteeType](draftId, path)
  }

}
