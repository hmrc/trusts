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
import models.registration.{RegistrationSubmission, RegistrationSubmissionDraft, RegistrationSubmissionDraftData}
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.RegistrationSubmissionRepository
import services.dates.LocalDateTimeService
import uk.gov.hmrc.http.NotFoundException
import utils.Constants._
import utils.JsonOps.prunePath

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SubmissionDraftController @Inject()(
                                           submissionRepository: RegistrationSubmissionRepository,
                                           identify: IdentifierAction,
                                           localDateTimeService: LocalDateTimeService,
                                           cc: ControllerComponents
                                         ) extends TrustsBaseController(cc) with Logging {

  def setSection(draftId: String, sectionKey: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RegistrationSubmissionDraftData] match {
        case JsSuccess(draftData, _) =>
          submissionRepository.getDraft(draftId, request.internalId).flatMap(
            result => {
              val draft: RegistrationSubmissionDraft = result match {
                case Some(draft) => draft
                case None => RegistrationSubmissionDraft(draftId, request.internalId, localDateTimeService.now, Json.obj(), None, Some(true))
              }

              val body: JsValue = draftData.data

              val path = JsPath() \ sectionKey

              draft.draftData.transform(
                prunePath(path) andThen
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

  private def setRegistrationSection(path: String, registrationSectionData: JsValue): Reads[JsObject] = {
    val sectionPath = MappedPiece.path \ path
    prunePath(sectionPath) andThen JsPath.json.update(sectionPath.json.put(registrationSectionData))
  }

  private def setRegistrationSections(pieces: List[RegistrationSubmission.MappedPiece]): List[Reads[JsObject]] = {
    pieces.map {
      case RegistrationSubmission.MappedPiece(path, JsNull) =>
        prunePath(MappedPiece.path \ path)
      case piece =>
        setRegistrationSection(piece.elementPath, piece.data)
    }
  }

  private def setAnswerSections(key: String, answerSections: List[RegistrationSubmission.AnswerSection]): Reads[JsObject] = {
    val sectionPath = AnswerSection.path \ key

    prunePath(sectionPath) andThen JsPath.json.update(sectionPath.json.put(Json.toJson(answerSections)))
  }

  def setDataset(draftId: String, sectionKey: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RegistrationSubmission.DataSet] match {
        case JsSuccess(dataSet, _) =>
          submissionRepository.getDraft(draftId, request.internalId).flatMap(
            result => {
              val draft: RegistrationSubmissionDraft = result match {
                case Some(draft) => draft
                case None => RegistrationSubmissionDraft(draftId, request.internalId, localDateTimeService.now, Json.obj(), None, Some(true))
              }

              applyDataSet(draft, dataSetOperations(sectionKey, dataSet))
            }
          )
        case _ => Future.successful(BadRequest)
      }
    }
  }

  private def dataSetOperations(sectionKey: String, incomingDraftData: RegistrationSubmission.DataSet): List[Reads[JsObject]] = {
    val sectionPath = JsPath() \ sectionKey

    List(
      prunePath(sectionPath),
      JsPath.json.update {
        sectionPath.json.put(Json.toJson(incomingDraftData.data))
      },
      setAnswerSections(sectionKey, incomingDraftData.answerSections)
    ) ++ setRegistrationSections(incomingDraftData.registrationPieces)
  }

  private def applyDataSet(draft: RegistrationSubmissionDraft, operations: List[Reads[JsObject]])
                          (implicit request: IdentifierRequest[JsValue]): Future[Result] = {
    operations.foldLeft[JsResult[JsValue]](JsSuccess(draft.draftData))((cur, xform) =>
      cur.flatMap(_.transform(xform))) match {
      case JsSuccess(newDraftData, _) =>

        val newDraft = draft.copy(draftData = newDraftData)

        submissionRepository.setDraft(newDraft).map(
          result => if (result) {
            Ok
          } else {
            InternalServerError
          }
        )
      case e: JsError =>
        logger.error(s"[applyDataSet][Session ID: ${request.sessionId}]" +
          s" Can't apply operations to draft data: $e.errors.")
        Future.successful(InternalServerError(e.errors.toString()))
    }
  }

  def getSection(draftId: String, sectionKey: String): Action[AnyContent] = identify.async {
    implicit request: IdentifierRequest[AnyContent] =>
      submissionRepository.getDraft(draftId, request.internalId).map {
        case Some(draft) =>
          val path = JsPath() \ sectionKey
          draft.draftData.transform(path.json.pick) match {
            case JsSuccess(data, _) => Ok(buildResponseJson(draft, data))
            case _: JsError => Ok(buildResponseJson(draft, Json.obj()))
          }
        case None => NotFound
      }
  }

  def getDrafts: Action[AnyContent] = identify.async { request =>
    submissionRepository.getRecentDrafts(request.internalId, request.affinityGroup).map {
      drafts =>
        implicit val draftWrites: Writes[RegistrationSubmissionDraft] = (draft: RegistrationSubmissionDraft) => {
          val obj = Json.obj(
            CREATED_AT -> draft.createdAt,
            DRAFT_ID -> draft.draftId
          )

          addReferenceIfDefined(obj, draft.reference)
        }

        Ok(Json.toJson(drafts))
    }
  }

  private def buildResponseJson(draft: RegistrationSubmissionDraft, data: JsValue): JsObject = {

    val obj = Json.obj(
      CREATED_AT -> draft.createdAt,
      DATA -> data
    )

    addReferenceIfDefined(obj, draft.reference)
  }

  private def addReferenceIfDefined(obj: JsObject, reference: Option[String]): JsObject = {
    reference match {
      case Some(r) => obj + (REFERENCE -> JsString(r))
      case _ => obj
    }
  }

  protected def get[T](draftData: JsValue, path: JsPath)(implicit rds: Reads[T]): JsResult[T] = {
    draftData.transform(path.json.pick).map(_.as[T])
  }

  protected def getResult[T](draftId: String, path: JsPath)
                          (implicit request: IdentifierRequest[AnyContent], rds: Reads[T], wts: Writes[T]): Future[Result] = {
    getResult[T, T](draftId, path)(x => x)
  }

  protected def getResult[A, B](draftId: String, path: JsPath)
                             (f: A => B)
                             (implicit request: IdentifierRequest[AnyContent], rds: Reads[A], wts: Writes[B]): Future[Result] = {
    getAtPath[A](draftId, path) map {
      case Success(value) =>
        logger.info(s"[Session ID: ${request.sessionId}] value found at $path")
        Ok(Json.toJson(f(value)))
      case Failure(exception) =>
        logger.info(exception.getMessage)
        NotFound
    }
  }

  private def getAtPath[T](draftId: String, path: JsPath)
                          (implicit request: IdentifierRequest[AnyContent], rds: Reads[T]): Future[Try[T]] = {
    submissionRepository.getDraft(draftId, request.internalId).map {
      case Some(draft) =>
        draft.draftData.transform(path.json.pick).map(_.as[T]) match {
          case JsSuccess(value, _) =>
            Success(value)
          case _: JsError =>
            Failure(new NotFoundException(s"[Session ID: ${request.sessionId}] value not found at $path"))
        }
      case None =>
        Failure(new NotFoundException(s"[Session ID: ${request.sessionId}] no draft, cannot return value at $path"))
    }
  }

}
