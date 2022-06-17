/*
 * Copyright 2022 HM Revenue & Customs
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
import models.registration.RegistrationSubmission.{AnswerSection, MappedPiece}
import models.requests.IdentifierRequest
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.RegistrationSubmissionRepository
import services.dates.LocalDateTimeService
import utils.JsonOps.prunePath

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CleanupSubmissionDraftController @Inject()(
                                                submissionRepository: RegistrationSubmissionRepository,
                                                identify: IdentifierAction,
                                                localDateTimeService: LocalDateTimeService,
                                                cc: ControllerComponents
                                              ) extends SubmissionDraftController(submissionRepository, identify, localDateTimeService, cc) {

  def removeDraft(draftId: String): Action[AnyContent] = identify.async { request =>
    submissionRepository.removeDraft(draftId, request.internalId).map { _ => Ok }
  }

  def reset(draftId: String, section: String, mappedDataKey: String): Action[AnyContent] = identify.async {
    implicit request =>
      submissionRepository.getDraft(draftId, request.internalId).flatMap {
        case Some(draft) =>
          val userAnswersPath = __ \ section
          val rowsPath = AnswerSection.path \ section
          val mappedDataPath = MappedPiece.path \ mappedDataKey

          draft.draftData.transform {
            prunePath(userAnswersPath) andThen
              prunePath(rowsPath) andThen
              prunePath(mappedDataPath)
          } match {
            case JsSuccess(data, _) =>
              val draftWithSectionReset = draft.copy(draftData = data)
              logger.info(s"[CleanupSubmissionDraftController][reset][Session ID: ${request.sessionId}]" +
                s" removed mapped data and answers $section")
              submissionRepository.setDraft(draftWithSectionReset).map(x => if (x) Ok else InternalServerError)
            case _: JsError =>
              logger.error(s"[CleanupSubmissionDraftController][reset][Session ID: ${request.sessionId}]" +
                s" failed to reset for $section")
              Future.successful(InternalServerError)
          }
        case None =>
          logger.warn(s"[CleanupSubmissionDraftController][reset][Session ID: ${request.sessionId}]" +
            s" no draft, cannot reset section $section")
          Future.successful(InternalServerError)
      }
  }

  def removeRoleInCompany(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>

      import utils.JsonOps.RemoveRoleInCompanyFields

      submissionRepository.getDraft(draftId, request.internalId).flatMap {
        case Some(draft) =>

          val initialDraftData: JsValue = draft.draftData

          val updatedDraftData = initialDraftData.removeRoleInCompanyFields()

          val newDraft = draft.copy(draftData = updatedDraftData)

          submissionRepository.setDraft(newDraft).map(
            result => if (result) Ok else InternalServerError
          )

        case _ => Future.successful(InternalServerError)
      }
  }

  def removeDeceasedSettlorMappedPiece(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>

      val path: JsPath = JsPath \ "registration" \ "trust/entities/deceased"
      removeAtPath(draftId, path)
  }

  def removeLivingSettlorsMappedPiece(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>

      val path: JsPath = JsPath \ "registration" \ "trust/entities/settlors"
      removeAtPath(draftId, path)
  }

  private def removeAtPath(draftId: String, path: JsPath)
                          (implicit request: IdentifierRequest[AnyContent]): Future[Result] = {

    submissionRepository.getDraft(draftId, request.internalId).flatMap {
      case Some(draft) =>

        val initialDraftData: JsValue = draft.draftData

        val updatedDraftData: JsValue = initialDraftData.transform(path.json.prune) match {
          case JsSuccess(value, _) => value
          case _ => initialDraftData
        }

        val newDraft = draft.copy(draftData = updatedDraftData)

        submissionRepository.setDraft(newDraft).map(
          result => if (result) Ok else InternalServerError
        )

      case _ => Future.successful(NotFound)
    }
  }
}
