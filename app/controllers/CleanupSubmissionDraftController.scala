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

package controllers

import controllers.actions.IdentifierAction
import errors.ServerError
import models.registration.RegistrationSubmission.{AnswerSection, MappedPiece}
import models.registration.RegistrationSubmissionDraft
import models.requests.IdentifierRequest
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.RegistrationSubmissionRepository
import services.dates.TimeService
import utils.JsonOps.prunePath

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CleanupSubmissionDraftController @Inject()(
                                                  submissionRepository: RegistrationSubmissionRepository,
                                                  identify: IdentifierAction,
                                                  timeService: TimeService,
                                                  cc: ControllerComponents
                                              )(implicit ec: ExecutionContext)
  extends SubmissionDraftController(submissionRepository, identify, timeService, cc) {

  private val className = this.getClass.getSimpleName

  def removeDraft(draftId: String): Action[AnyContent] = identify.async { request =>
    submissionRepository.removeDraft(draftId, request.internalId).value.map {
      case Right(_) => Ok
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][removeDraft][Session ID: ${request.sessionId}] failed to remove draft. Message: $message")
        InternalServerError
      case Left(_) =>
        logger.warn(s"[$className][removeDraft][Session ID: ${request.sessionId}] an error occurred, failed to remove draft.")
        InternalServerError
    }
  }

  def reset(draftId: String, section: String, mappedDataKey: String): Action[AnyContent] = identify.async {
    implicit request =>
      submissionRepository.getDraft(draftId, request.internalId).value.flatMap {
        case Right(Some(draft)) => transformDraftDataForReset(draft, section, mappedDataKey)
        case Right(None) =>
          logger.warn(s"[$className][reset][Session ID: ${request.sessionId}] no draft, cannot reset section $section")
          Future.successful(InternalServerError)
        case Left(ServerError(message)) if message.nonEmpty =>
          logger.warn(s"[$className][reset][Session ID: ${request.sessionId}] cannot reset section $section. Message: $message")
          Future.successful(InternalServerError)
        case Left(_) =>
          logger.warn(s"[$className][reset][Session ID: ${request.sessionId}] an error occurred, cannot reset section $section")
          Future.successful(InternalServerError)
      }
  }

  private def transformDraftDataForReset(draft: RegistrationSubmissionDraft, section: String, mappedDataKey: String)
                                        (implicit request: IdentifierRequest[AnyContent]): Future[Status] = {
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
        logger.info(s"[$className][reset][Session ID: ${request.sessionId}] removed mapped data and answers $section")
        submissionRepository.setDraft(draftWithSectionReset).value.map {
          case Right(true) => Ok
          case Right(_) => InternalServerError
          case Left(ServerError(message)) if message.nonEmpty =>
            logger.warn(s"[$className][reset][Session ID: ${request.sessionId}] failed to reset for $section. Message: $message")
            InternalServerError
          case Left(_) =>
            logger.warn(s"[$className][reset][Session ID: ${request.sessionId}] there was a problem, failed to reset for $section.")
            InternalServerError
        }
      case _: JsError =>
        logger.error(s"[$className][reset][Session ID: ${request.sessionId}] failed to reset for $section")
        Future.successful(InternalServerError)
    }
  }

  def removeRoleInCompany(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>

      import utils.JsonOps.RemoveRoleInCompanyFields

      submissionRepository.getDraft(draftId, request.internalId).value.flatMap {
        case Right(Some(draft)) =>

          val initialDraftData: JsValue = draft.draftData

          val updatedDraftData = initialDraftData.removeRoleInCompanyFields()

          val newDraft = draft.copy(draftData = updatedDraftData)

          submissionRepository.setDraft(newDraft).value.map {
            case Right(true) => Ok
            case Right(_) => InternalServerError
            case Left(ServerError(message)) if message.nonEmpty =>
              logger.warn(s"[$className][reset][Session ID: ${request.sessionId}] failed to set draft. Message: $message")
              InternalServerError
            case Left(_) =>
              logger.warn(s"[$className][removeRoleInCompany][Session ID: ${request.sessionId}] problem to set draft.")
              InternalServerError
          }
        case Right(_) => Future.successful(InternalServerError)
        case Left(ServerError(message)) if message.nonEmpty =>
          logger.warn(s"[$className][removeRoleInCompany][Session ID: ${request.sessionId}] " +
            s"failed to remove role in company fields. Message: $message")
          Future.successful(InternalServerError)
        case Left(_) =>
          logger.warn(s"[$className][removeRoleInCompany][Session ID: ${request.sessionId}] " +
            s"an error occurred while getting draft, cannot remove role in company fields")
          Future.successful(InternalServerError)
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

    submissionRepository.getDraft(draftId, request.internalId).value.flatMap {
      case Right(Some(draft)) =>

        val initialDraftData: JsValue = draft.draftData

        val updatedDraftData: JsValue = initialDraftData.transform(path.json.prune) match {
          case JsSuccess(value, _) => value
          case _ => initialDraftData
        }

        val newDraft = draft.copy(draftData = updatedDraftData)

        submissionRepository.setDraft(newDraft).value.map {
          case Right(true) => Ok
          case Right(_) => InternalServerError
          case Left(ServerError(message)) if message.nonEmpty =>
            logger.warn(s"[$className][removeAtPath][Session ID: ${request.sessionId}] failed to set new draft data. Message: $message")
            InternalServerError
          case Left(_) =>
            logger.warn(s"[$className][removeAtPath][Session ID: ${request.sessionId}] there was a problem, failed to set new draft data.")
            InternalServerError
        }

      case Right(_) => Future.successful(NotFound)
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][removeAtPath][Session ID: ${request.sessionId}] cannot remove at path. Message: $message")
        Future.successful(InternalServerError)
      case Left(_) =>
        logger.warn(s"[$className][removeAtPath][Session ID: ${request.sessionId}] there was a problem, cannot remove at path")
        Future.successful(InternalServerError)
    }
  }
}
