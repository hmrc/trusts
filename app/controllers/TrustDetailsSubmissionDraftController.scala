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
import models.AddressType
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.RegistrationSubmissionRepository
import services.dates.TimeService

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TrustDetailsSubmissionDraftController @Inject() (
  submissionRepository: RegistrationSubmissionRepository,
  identify: IdentifierAction,
  timeService: TimeService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends SubmissionDraftController(submissionRepository, identify, timeService, cc) {

  private val className                  = this.getClass.getSimpleName
  private val whenTrustSetupPath: JsPath = JsPath \ "trustDetails" \ "data" \ "trustDetails" \ "whenTrustSetup"

  def getWhenTrustSetup(draftId: String): Action[AnyContent] = identify.async { implicit request =>
    getResult[LocalDate, JsValue](draftId, whenTrustSetupPath)(x => Json.obj("startDate" -> x))
  }

  def getCorrespondenceAddress(draftId: String): Action[AnyContent] = identify.async { implicit request =>
    val path: JsPath = JsPath \ "registration" \ "correspondence/address"
    getResult[AddressType](draftId, path)
  }

  def getTrustUtr(draftId: String): Action[AnyContent] = identify.async { implicit request =>
    val path = JsPath \ "main" \ "data" \ "matching" \ "whatIsTheUTR"
    getResult[String](draftId, path)
  }

  def getTrustTaxable(draftId: String): Action[AnyContent] = identify.async { implicit request =>
    val path = JsPath \ "main" \ "data" \ "trustTaxable"
    getResult[Boolean](draftId, path)
  }

  def getIsExpressTrust(draftId: String): Action[AnyContent] = identify.async { implicit request =>
    val path = JsPath \ "main" \ "data" \ "expressTrust"
    getResult[Boolean](draftId, path)
  }

  def getTrustName(draftId: String): Action[AnyContent] = identify.async { implicit request =>
    submissionRepository.getDraft(draftId, request.internalId).value.map {
      case Right(Some(draft)) =>
        val matchingPath = JsPath \ "main" \ "data" \ "matching" \ "trustName"
        val detailsPath  = JsPath \ "trustDetails" \ "data" \ "trustDetails" \ "trustName"

        val matchingName = get[String](draft.draftData, matchingPath)
        val detailsName  = get[String](draft.draftData, detailsPath)

        (matchingName, detailsName) match {
          case (JsSuccess(date, _), JsError(_)) =>
            logger.info(
              s"[$className][getTrustName][Session ID: ${request.sessionId}]" +
                s" found trust name in matching"
            )
            Ok(Json.obj("trustName" -> date))
          case (JsError(_), JsSuccess(date, _)) =>
            logger.info(
              s"[$className][getTrustName][Session ID: ${request.sessionId}]" +
                s" found trust name in trust details"
            )
            Ok(Json.obj("trustName" -> date))
          case _                                =>
            logger.info(
              s"[$className][getTrustName][Session ID: ${request.sessionId}]" +
                s" no trust name found"
            )
            NotFound
        }
      case Right(None)        =>
        logger.info(
          s"[$className][getTrustName][Session ID: ${request.sessionId}]" +
            s" no draft, cannot return trust name"
        )
        NotFound
      case Left(_)            =>
        logger.warn(
          s"[$className][getTrustName][Session ID: ${request.sessionId}] " +
            s"error while retrieving draft from repository"
        )
        InternalServerError
    }
  }

}
