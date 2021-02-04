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
import models._
import models.registration.RegistrationSubmission.{AnswerSection, MappedPiece}
import models.registration.{RegistrationSubmission, RegistrationSubmissionDraft, RegistrationSubmissionDraftData}
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.RegistrationSubmissionRepository
import services.{BackwardsCompatibilityService, LocalDateTimeService}
import utils.Constants._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionDraftController @Inject()(submissionRepository: RegistrationSubmissionRepository,
                                          identify: IdentifierAction,
                                          localDateTimeService: LocalDateTimeService,
                                          cc: ControllerComponents,
                                          backwardsCompatibilityService: BackwardsCompatibilityService
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

  // Play 2.5 throws if the path to be pruned does not exist.
  // So we do this hacky thing to keep it all self-contained.
  // If upgraded to play 2.6, this can turn into simply "path.json.prune".
  private def prunePath(path: JsPath): Reads[JsObject] = {
    JsPath.json.update {
      path.json.put(Json.toJson(Json.obj()))
    } andThen path.json.prune
  }

  private def setRegistrationSection(path: String, registrationSectionData: JsValue) : Reads[JsObject] = {
    val sectionPath = MappedPiece.path \ path
    prunePath(sectionPath) andThen JsPath.json.update(sectionPath.json.put(registrationSectionData))
  }

  private def setRegistrationSections(pieces: List[RegistrationSubmission.MappedPiece]) : List[Reads[JsObject]] = {
    pieces.map {
      case RegistrationSubmission.MappedPiece(path, JsNull) =>
        prunePath(MappedPiece.path \ path)
      case piece =>
        setRegistrationSection(piece.elementPath, piece.data)
    }
  }

  private def setStatus(key: String, statusOpt: Option[registration.Status]): Reads[JsObject] = {
    val sectionPath = registration.Status.path \ key

    statusOpt match {
      case Some(status) =>
        prunePath(sectionPath) andThen JsPath.json.update(sectionPath.json.put(Json.toJson(status.toString)))
      case _ => prunePath(sectionPath)
    }
  }

  private def setAnswerSections(key: String, answerSections: List[RegistrationSubmission.AnswerSection]): Reads[JsObject] = {
    val sectionPath = AnswerSection.path \ key

    prunePath(sectionPath) andThen JsPath.json.update(sectionPath.json.put(Json.toJson(answerSections)))
  }

  def setSectionSet(draftId: String, sectionKey: String): Action[JsValue] = identify.async(parse.json) {
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
      setStatus(sectionKey, incomingDraftData.status),
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
        implicit val draftWrites: Writes[RegistrationSubmissionDraft] = new Writes[RegistrationSubmissionDraft] {
          override def writes(draft: RegistrationSubmissionDraft): JsValue = {

            val obj = Json.obj(
              CREATED_AT -> draft.createdAt,
              DRAFT_ID -> draft.draftId
            )

            addReferenceIfDefined(obj, draft.reference)
          }
        }

        Ok(Json.toJson(drafts))
    }
  }

  def adjustDraft(draftId: String): Action[AnyContent] = identify.async { request =>
    submissionRepository.getDraft(draftId, request.internalId) flatMap {
      case Some(draft) =>
        val updatedDraft = backwardsCompatibilityService.adjustData(draft)
        submissionRepository.setDraft(draft.copy(draftData = updatedDraft)) map { _ =>
          Ok
        }
      case _ =>
        Future.successful(NotFound)
    }
  }

  def removeDraft(draftId: String): Action[AnyContent] = identify.async { request =>
    submissionRepository.removeDraft(draftId, request.internalId).map { _ => Ok }
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

  def getWhenTrustSetup(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>

      val reads: Reads[LocalDate] = Reads.DefaultLocalDateReads
      val writes: Writes[LocalDate] = (date: LocalDate) => Json.obj("startDate" -> date)

      val path = JsPath \ "trustDetails" \ "data" \ "trustDetails" \ "whenTrustSetup"
      getAtPath[LocalDate](draftId, path)(request, reads, writes)
  }

  def getTrustTaxable(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>

      val path = JsPath \ "main" \ "data" \ "trustTaxable"
      getAtPath[Boolean](draftId, path)
  }

  def getTrustName(draftId: String) : Action[AnyContent] = identify.async {
    implicit request =>
      submissionRepository.getDraft(draftId, request.internalId).map {
        case Some(draft) =>
          val matchingPath = JsPath \ "main" \ "data" \ "matching" \ "trustName"
          val detailsPath = JsPath \ "trustDetails" \ "data" \ "trustDetails" \ "trustName"

          val matchingName = get[String](draft.draftData, matchingPath)
          val detailsName = get[String](draft.draftData, detailsPath)

          (matchingName, detailsName) match {
            case (JsSuccess(date, _), JsError(_)) =>
              logger.info(s"[Session ID: ${request.sessionId}]" +
                s" found trust name in matching")
              Ok(Json.obj("trustName" -> date))
            case (JsError(_), JsSuccess(date, _)) =>
              logger.info(s"[Session ID: ${request.sessionId}]" +
                s" found trust name in trust details")
              Ok(Json.obj("trustName" -> date))
            case _ =>
              logger.info(s"[Session ID: ${request.sessionId}]" +
                s" no trust name found")
              NotFound
          }
        case None =>
          logger.info(s"[Session ID: ${request.sessionId}]" +
            s" no draft, cannot return trust name")
          NotFound
      }
  }

  private def get[T](draftData: JsValue, path: JsPath)(implicit rds: Reads[T]): JsResult[T] = {
    draftData.transform(path.json.pick).map(_.as[T])
  }

  def getLeadTrustee(draftId: String) : Action[AnyContent] = identify.async {
    implicit request =>
      val path = JsPath \ "registration" \ "trust/entities/leadTrustees"
      getAtPath[LeadTrusteeType](draftId, path)(request, LeadTrusteeType.leadTrusteeTypeReads, LeadTrusteeType.writes)
  }

  def reset(draftId: String, section: String, mappedDataKey : String) : Action[AnyContent] = identify.async {
    implicit request =>
      submissionRepository.getDraft(draftId, request.internalId).flatMap {
        case Some(draft) =>
          val statusPath = registration.Status.path \ section
          val userAnswersPath = __ \ section
          val rowsPath = AnswerSection.path \ section
          val mappedDataPath = MappedPiece.path \ mappedDataKey

          draft.draftData.transform {
            prunePath(statusPath) andThen
              prunePath(userAnswersPath) andThen
              prunePath(rowsPath) andThen
              prunePath(mappedDataPath)
          } match {
            case JsSuccess(data, _) =>
              val draftWithStatusRemoved = draft.copy(draftData = data)
              logger.info(s"[reset][Session ID: ${request.sessionId}]" +
                s" removed status, mapped data, answers and status for $section")
              submissionRepository.setDraft(draftWithStatusRemoved).map(x => if (x) Ok else InternalServerError)
            case _ : JsError =>
              logger.info(s"[reset][Session ID: ${request.sessionId}]" +
                s" failed to reset for $section")
              Future.successful(InternalServerError)
          }
        case None =>
          logger.info(s"[reset][Session ID: ${request.sessionId}]" +
            s" no draft, cannot reset status for $section")
          Future.successful(InternalServerError)
      }
  }

  def getCorrespondenceAddress(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>
      val path: JsPath = JsPath \ "registration" \ "correspondence/address"
      getAtPath[AddressType](draftId, path)
  }

  def getAgentAddress(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>
      val path: JsPath = JsPath \ "registration" \ "agentDetails" \ "agentAddress"
      getAtPath[AddressType](draftId, path)
  }

  def getClientReference(draftId: String): Action[AnyContent] = identify.async {
    implicit request =>
      val path: JsPath = JsPath \ "registration" \ "agentDetails" \ "clientReference"
      getAtPath[String](draftId, path)
  }

  private def getAtPath[T](draftId: String, path: JsPath)
                          (implicit request: IdentifierRequest[AnyContent], rds: Reads[T], wts: Writes[T]): Future[Result] = {
    submissionRepository.getDraft(draftId, request.internalId).map {
      case Some(draft) =>
        draft.draftData.transform(path.json.pick).map(_.as[T]) match {
          case JsSuccess(value, _) =>
            logger.info(s"[Session ID: ${request.sessionId}] value found at $path")
            Ok(Json.toJson(value))
          case _ : JsError =>
            logger.info(s"[Session ID: ${request.sessionId}] value not found at $path")
            NotFound
        }
      case None =>
        logger.info(s"[Session ID: ${request.sessionId}] no draft, cannot return value at $path")
        NotFound
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
