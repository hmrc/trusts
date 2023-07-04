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

import cats.data.EitherT
import controllers.actions.IdentifierAction
import errors.{ErrorWithResult, NotFoundError, ServerError}
import models.registration.RegistrationSubmission.{AnswerSection, MappedPiece}
import models.registration.{RegistrationSubmission, RegistrationSubmissionDraft, RegistrationSubmissionDraftData}
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.RegistrationSubmissionRepository
import services.dates.LocalDateTimeService
import utils.Constants._
import utils.JsonOps.prunePath
import utils.TrustEnvelope.TrustEnvelope

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionDraftController @Inject()(
                                           submissionRepository: RegistrationSubmissionRepository,
                                           identify: IdentifierAction,
                                           localDateTimeService: LocalDateTimeService,
                                           cc: ControllerComponents
                                         )(implicit ec: ExecutionContext) extends TrustsBaseController(cc) with Logging {

  private val className = this.getClass.getSimpleName

  def setSection(draftId: String, sectionKey: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RegistrationSubmissionDraftData] match {
        case JsSuccess(draftData, _) =>
          val expectedResult = for {
            optionalDraftData <- submissionRepository.getDraft(draftId, request.internalId)
            draft = optionalDraftData.getOrElse(
              RegistrationSubmissionDraft(draftId, request.internalId, localDateTimeService.now, Json.obj(), None, Some(true))
            )
            body: JsValue = draftData.data
            path = JsPath() \ sectionKey
            dataTransformationResult <- draftDataTransformationForSetSection(draftData, draft, path, body)
          } yield dataTransformationResult

          expectedResult.value.map {
            case Right(dataTransformationResult) => dataTransformationResult
            case Left(ErrorWithResult(result)) => result
            case Left(_) =>
              logger.warn(s"[$className][setSection][Session ID: ${request.sessionId}] failed to set section.")
              InternalServerError
          }
        case _ => Future.successful(BadRequest)
      }
    }
  }

  private def draftDataTransformationForSetSection(draftData: RegistrationSubmissionDraftData,
                                                   draft: RegistrationSubmissionDraft,
                                                   path: JsPath,
                                                   body: JsValue)(implicit request: IdentifierRequest[JsValue]): TrustEnvelope[Result] = EitherT {
    draft.draftData.transform(
      prunePath(path) andThen
        JsPath.json.update {
          path.json.put(Json.toJson(body))
        }
    ) match {
      case JsSuccess(newDraftData, _) =>
        val newReference = draftData.reference orElse draft.reference
        val newInProgress = draftData.inProgress orElse draft.inProgress
        val newDraft = draft.copy(draftData = newDraftData, reference = newReference, inProgress = newInProgress)
        submissionRepository.setDraft(newDraft).value.map {
          case Right(true) => Right(Ok)
          case Right(_) => Left(ErrorWithResult(InternalServerError))
          case Left(ServerError(message)) if message.nonEmpty =>
            logger.warn(s"[$className][draftDataTransformationForSetSection][Session ID: ${request.sessionId}] " +
              s" cannot transform data for set section. Message: $message")
            Left(ServerError())
          case Left(_) =>
            logger.warn(s"[$className][draftDataTransformationForSetSection][Session ID: ${request.sessionId}] " +
              s"there was a problem, cannot transform data for set section.")
            Left(ServerError())
        }
      case e: JsError => Future.successful(Left(ErrorWithResult(InternalServerError(e.errors.toString()))))
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

          val expectedResult = for {
            optionalDraftData <- submissionRepository.getDraft(draftId, request.internalId)
            draft = optionalDraftData.getOrElse(
              RegistrationSubmissionDraft(draftId, request.internalId, localDateTimeService.now, Json.obj(), None, Some(true))
            )
            appliedDataSetResult <- applyDataSet(draft, dataSetOperations(sectionKey, dataSet))
          } yield appliedDataSetResult

          expectedResult.value.map {
            case Right(appliedDataSetResult) => appliedDataSetResult
            case Left(ErrorWithResult(result)) => result
            case Left(_) =>
              logger.warn(s"[$className][setDataset][Session ID: ${request.sessionId}] failed to set dataset.")
              InternalServerError
          }

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
                          (implicit request: IdentifierRequest[JsValue]): TrustEnvelope[Result] = EitherT {
    operations.foldLeft[JsResult[JsValue]](JsSuccess(draft.draftData))((cur, xform) =>
      cur.flatMap(_.transform(xform))) match {
      case JsSuccess(newDraftData, _) =>

        val newDraft = draft.copy(draftData = newDraftData)

        submissionRepository.setDraft(newDraft).value.map {
          case Right(true) => Right(Ok)
          case Right(_) => Left(ErrorWithResult(InternalServerError))
          case Left(ServerError(message)) if message.nonEmpty =>
            logger.warn(s"[$className][applyDataSet][Session ID: ${request.sessionId}] can't apply operations to draft data. Message: $message")
            Left(ServerError())
          case Left(_) =>
            logger.warn(s"[$className][applyDataSet][Session ID: ${request.sessionId}] there was a problem, can't apply operations to draft data.")
            Left(ServerError())
        }
      case e: JsError =>
        logger.error(s"[$className][applyDataSet][Session ID: ${request.sessionId}] Can't apply operations to draft data: ${e.errors}.")
        Future.successful(Left(ErrorWithResult(InternalServerError(e.errors.toString()))))
    }
  }

  def getSection(draftId: String, sectionKey: String): Action[AnyContent] = identify.async {
    implicit request: IdentifierRequest[AnyContent] =>
      submissionRepository.getDraft(draftId, request.internalId).value.map {
        case Right(Some(draft)) =>
          val path = JsPath() \ sectionKey
          draft.draftData.transform(path.json.pick) match {
            case JsSuccess(data, _) => Ok(buildResponseJson(draft, data))
            case _: JsError => Ok(buildResponseJson(draft, Json.obj()))
          }
        case Right(None) => NotFound
        case Left(ServerError(message)) if message.nonEmpty =>
          logger.warn(s"[$className][getSection][Session ID: ${request.sessionId}] failed to get section. Message: $message")
          InternalServerError
        case Left(_) =>
          logger.warn(s"[$className][getSection][Session ID: ${request.sessionId}] there was a problem, failed to get section.")
          InternalServerError
      }
  }

  def getDrafts: Action[AnyContent] = identify.async { request =>
    submissionRepository.getRecentDrafts(request.internalId, request.affinityGroup).value.map {
      case Right(drafts) =>

        implicit val draftWrites: Writes[RegistrationSubmissionDraft] = (draft: RegistrationSubmissionDraft) => {
          val obj = Json.obj(
            CREATED_AT -> draft.createdAt,
            DRAFT_ID -> draft.draftId
          )
          addReferenceIfDefined(obj, draft.reference)
        }

        Ok(Json.toJson(drafts))

      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][getDrafts][Session ID: ${request.sessionId}] failed to get drafts. Message: $message")
        InternalServerError
      case Left(_) =>
        logger.warn(s"[$className][getDrafts][Session ID: ${request.sessionId}] there was a problem, failed to get drafts.")
        InternalServerError
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
    getAtPath[A](draftId, path).value.map {
      case Right(value) =>
        Ok(Json.toJson(f(value)))
      case Left(NotFoundError(message)) =>
        logger.warn(message)
        NotFound
      case Left(_) =>
        InternalServerError
    }
  }

  private def getAtPath[T](draftId: String, path: JsPath)
                          (implicit request: IdentifierRequest[AnyContent], rds: Reads[T]): TrustEnvelope[T] = EitherT {
    submissionRepository.getDraft(draftId, request.internalId).value.map {
      case Right(Some(draft)) =>
        draft.draftData.transform(path.json.pick).map(_.as[T]) match {
          case JsSuccess(value, _) =>
            Right(value)
          case _: JsError =>
            Left(NotFoundError(s"[$className][getAtPath][Session ID: ${request.sessionId}] value not found at $path"))
        }
      case Right(None) =>
        Left(NotFoundError(s"[$className][getAtPath][Session ID: ${request.sessionId}] no draft, cannot return value at $path"))
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][getAtPath][Session ID: ${request.sessionId}] cannot return value at $path. Message: $message")
        Left(ServerError())
      case Left(_) =>
        logger.warn(s"[$className][getAtPath][Session ID: ${request.sessionId}] there was a problem, cannot return value at $path")
        Left(ServerError())
    }
  }

}
