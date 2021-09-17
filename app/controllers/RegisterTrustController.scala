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

import config.AppConfig
import controllers.actions.IdentifierAction
import models._
import models.registration.ApiResponse._
import models.registration.RegistrationTrnResponse._
import models.registration._
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import services._
import utils.ErrorResponses._
import utils.Headers

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegisterTrustController @Inject()(
                                         trustsService: TrustsService,
                                         config: AppConfig,
                                         validationService: ValidationService,
                                         identify: IdentifierAction,
                                         rosmPatternService: RosmPatternService,
                                         cc: ControllerComponents,
                                         amendSubmissionDataService: AmendSubmissionDataService
                                       ) extends TrustsBaseController(cc) with Logging {

  def registration(): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.headers.get(Headers.DRAFT_REGISTRATION_ID) match {
        case Some(_) =>
          amendRegistration
        case _ =>
          logger.error(s"[Session ID: ${request.sessionId}] no draft id provided in headers")
          Future.successful(BadRequest(Json.toJson(noDraftIdProvided)))
      }
  }

  private def amendRegistration(implicit request: IdentifierRequest[JsValue]): Future[Result] = {
    val payload: JsValue = amendSubmissionDataService
      .applyRulesAndAddSubmissionDate(request.body)

    val schema: String = config.trustsApiRegistrationSchema5MLD
    validateRegistration(schema, payload)
  }

  private def validateRegistration(schema: String, data: JsValue)
                                  (implicit request: IdentifierRequest[JsValue]): Future[Result] = {
    validationService.get(schema)
      .validate[Registration](data.toString()) match {
        case Right(registration) =>
          register(registration)
        case Left(validationErrors) =>
          logger.error(s"[Session ID: ${request.sessionId}] problem validating submission: $validationErrors")
          Future.successful(invalidRequestErrorResponse)
      }
  }

  private def register(registration: Registration)
                      (implicit request: IdentifierRequest[JsValue]): Future[Result] = {
    trustsService.registerTrust(registration).flatMap {
      case response: RegistrationTrnResponse =>
        enrol(response, registration)
      case AlreadyRegisteredResponse =>
        handleAlreadyRegisteredResponse
      case NoMatchResponse =>
        handleNoMatchResponse
      case BadRequestResponse =>
        handleBadRequestResponse
      case ServiceUnavailableResponse =>
        handleServiceUnavailableResponse
      case _ =>
        handleInternalServerErrorResponse()
    }
  }

  private def enrol(response: RegistrationTrnResponse, registration: Registration)
                                   (implicit request: IdentifierRequest[JsValue]): Future[Result] = {

    rosmPatternService.enrolAndLogResult(
      trn = response.trn,
      affinityGroup = request.affinityGroup,
      taxable = registration.trust.details.trustTaxable.getOrElse(true)
    ) map { _ =>
      Ok(Json.toJson(response))
    }
  }

  private def handleAlreadyRegisteredResponse()(implicit request: IdentifierRequest[JsValue]): Future[Result] = {

    logger.info(s"[registration][Session ID: ${request.sessionId}] Returning already registered response.")
    Future.successful(Conflict(Json.toJson(alreadyRegisteredTrustsResponse)))
  }

  private def handleNoMatchResponse()(implicit request: IdentifierRequest[JsValue]): Future[Result] = {

    logger.info(s"[registration][Session ID: ${request.sessionId}] Returning no match response.")
    Future.successful(Forbidden(Json.toJson(noMatchRegistrationResponse)))
  }

  private def handleBadRequestResponse()(implicit request: IdentifierRequest[JsValue]): Future[Result] = {

    logger.error(s"[registration][Session ID: ${request.sessionId}] bad request response from DES")
    Future.successful(InternalServerError(Json.toJson(internalServerErrorResponse)))
  }

  private def handleServiceUnavailableResponse()(implicit request: IdentifierRequest[JsValue]): Future[Result] = {

    logger.error(s"[registration][Session ID: ${request.sessionId}] Service unavailable response from DES")
    Future.successful(InternalServerError(Json.toJson(internalServerErrorResponse)))
  }

  private def handleInternalServerErrorResponse()(implicit request: IdentifierRequest[JsValue]): Future[Result] = {
    logger.error(s"[registration][Session ID: ${request.sessionId}] Internal server error.")
    Future.successful(InternalServerError(Json.toJson(internalServerErrorResponse)))
  }

}
