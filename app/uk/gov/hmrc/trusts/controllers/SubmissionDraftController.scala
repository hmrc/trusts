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

import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.{RegistrationSubmissionDraft, RegistrationSubmissionDraftData}
import uk.gov.hmrc.trusts.models.requests.IdentifierRequest
import uk.gov.hmrc.trusts.repositories.RegistrationSubmissionRepository
import uk.gov.hmrc.trusts.services.{AuditService, LocalDateTimeService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionDraftController @Inject()(submissionRepository: RegistrationSubmissionRepository,
                                          identify: IdentifierAction,
                                          auditService: AuditService,
                                          localDateTimeService: LocalDateTimeService
                                       ) extends TrustsBaseController {

  def setSection(draftId: String, sectionKey: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RegistrationSubmissionDraftData] match {
        case JsSuccess(draftData, _) =>
          submissionRepository.getDraft(draftId, request.identifier).flatMap(
            result => {
              val draft: RegistrationSubmissionDraft = result match {
                case Some(draft) => draft
                case None => RegistrationSubmissionDraft(draftId, request.identifier, localDateTimeService.now, Json.obj(), None, Some(true))
              }

              val body: JsValue = draftData.data

              val path = JsPath() \ sectionKey

              draft.draftData.transform(
                path.json.prune andThen
                  JsPath.json.update {
                    path.json.put(Json.toJson(body))
                  }
              ) match {
                case JsSuccess(newDraftData, _) =>
                  val newReference = draftData.reference orElse draft.reference
                  val newInProgress = draftData.inProgress orElse draft.inProgress
                  val newDraft = draft.copy(
                    draftData = newDraftData,
                    reference = newReference,
                    inProgress = newInProgress)
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
        case _ => Future.successful(BadRequest)
      }
    }
  }

  def getSection(draftId: String, sectionKey: String): Action[AnyContent] = identify.async {
    implicit request: IdentifierRequest[AnyContent] =>
      submissionRepository.getDraft(draftId, request.identifier).map {
        case Some(draft) =>
          val path = JsPath() \ sectionKey
          draft.draftData.transform(path.json.pick) match {
            case JsSuccess(data, _) => Ok(buildResponseJson(draft, data))
            case _: JsError => NoContent
          }
        case None => NotFound
      }
  }

  def getDrafts: Action[AnyContent] = identify.async { request =>
    submissionRepository.getAllDrafts(request.identifier).map {
      drafts =>

        implicit val draftWrites: Writes[RegistrationSubmissionDraft] = new Writes[RegistrationSubmissionDraft] {
          override def writes(draft: RegistrationSubmissionDraft): JsValue =
            if (draft.reference.isDefined) {
              Json.obj(
                "createdAt" -> draft.createdAt,
                "draftId" -> draft.draftId,
                "reference" -> draft.reference)
            } else {
              Json.obj(
                "createdAt" -> draft.createdAt,
                "draftId" -> draft.draftId)
            }
        }

        Ok(Json.toJson(drafts))
    }
  }

  def removeDraft(draftId: String): Action[AnyContent] = identify.async { request =>
    submissionRepository.removeDraft(draftId, request.identifier).map { _ => Ok }
  }

  private def buildResponseJson(draft: RegistrationSubmissionDraft, data: JsValue) = {
    if (draft.reference.isDefined) {
      Json.obj(
        "createdAt" -> draft.createdAt,
        "data" -> data,
        "reference" -> draft.reference
      )
    } else {
      Json.obj(
        "createdAt" -> draft.createdAt,
        "data" -> data
      )
    }
  }
}
