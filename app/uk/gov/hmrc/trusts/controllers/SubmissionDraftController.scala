/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.controllers

import java.time.LocalDateTime

import akka.serialization.JSerializer
import javax.inject.Inject
import play.api.libs.json.{JsError, JsPath, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.RegistrationSubmissionDraft
import uk.gov.hmrc.trusts.models.requests.IdentifierRequest
import uk.gov.hmrc.trusts.repositories.RegistrationSubmissionRepository
import uk.gov.hmrc.trusts.services.{AuditService, LocalDateTimeService}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionDraftController @Inject()(submissionRepository: RegistrationSubmissionRepository,
                                          identify: IdentifierAction,
                                          auditService: AuditService,
                                          localDateTimeService: LocalDateTimeService
                                       ) extends TrustsBaseController {

  def setSection(draftId: String, sectionKey: String): Action[JsValue] = identify.async(parse.json) {
    implicit request: IdentifierRequest[JsValue] => {
      submissionRepository.getDraft(draftId, request.identifier).flatMap(
        result => {
          val draft: RegistrationSubmissionDraft = result match {
            case Some(draft) => draft
            case None => RegistrationSubmissionDraft(draftId, request.identifier, localDateTimeService.now, Json.obj())
          }

          val body: JsValue = request.body

          val path = JsPath() \ sectionKey

          draft.draftData.transform(
          path.json.prune andThen
            JsPath.json.update {
              path.json.put(Json.toJson(body))
            }
          ) match {
            case JsSuccess(newDraftData, _) =>
              val newDraft = draft.copy(draftData = newDraftData)
              submissionRepository.setDraft(newDraft).map(
                result => if (result) {
                  Ok
                } else {
                  InternalServerError
                }
              )
            case e: JsError => Future.successful(InternalServerError(e.errors.toString()))
          }

        }
      )
    }
  }

  def getSection(draftId: String, sectionKey: String): Action[AnyContent] = identify.async {
    implicit request: IdentifierRequest[AnyContent] => Future.successful(InternalServerError)
  }

}
