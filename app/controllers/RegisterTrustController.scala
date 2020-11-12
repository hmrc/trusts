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

package controllers

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.BadRequestException
import config.AppConfig
import controllers.actions.IdentifierAction
import exceptions._
import models._
import models.auditing.TrustAuditing
import models.registration.ApiResponse._
import models.registration.RegistrationTrnResponse._
import models.registration.{RegistrationFailureResponse, RegistrationTrnResponse}
import models.requests.IdentifierRequest
import services.{AuditService, DesService, RosmPatternService, ValidationService, _}
import utils.ErrorResponses._
import utils.Headers
import utils.JsonOps._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class RegisterTrustController @Inject()(desService: DesService, config: AppConfig,
                                        validationService: ValidationService,
                                        identify: IdentifierAction,
                                        rosmPatternService: RosmPatternService,
                                        auditService: AuditService,
                                        cc: ControllerComponents,
                                        trustsStoreService: TrustsStoreService
                                        ) extends TrustsBaseController(cc) with Logging {

  private def schemaF(implicit request: IdentifierRequest[JsValue]): Future[String] = {
    trustsStoreService.is5mldEnabled.map {
      case true => config.trustsApiRegistrationSchema5MLD
      case _ => config.trustsApiRegistrationSchema4MLD
    }
  }

  def registration(): Action[JsValue] = identify.async(parse.json) {
    implicit request =>

      val payload = request.body.applyRules.toString
      val draftIdOption = request.headers.get(Headers.DraftRegistrationId)

      schemaF.flatMap {
        schema =>
          validationService
            .get(schema)
            .validate[Registration](payload) match {

            case Right(trustsRegistrationRequest) =>

              draftIdOption match {
                case Some(draftId) =>
                  desService.registerTrust(trustsRegistrationRequest).flatMap {
                    case response: RegistrationTrnResponse =>

                      auditService.audit(
                        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
                        registration = trustsRegistrationRequest,
                        draftId = draftId,
                        internalId = request.identifier,
                        response = response
                      )

                      rosmPatternService.enrolAndLogResult(response.trn,
                        request.affinityGroup,
                        trustsRegistrationRequest.trust.details.trustTaxable.getOrElse(true)
                      ) map {
                        _ =>
                          Ok(Json.toJson(response))
                      }
                  } recover {
                    case AlreadyRegisteredException =>

                      auditService.audit(
                        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
                        registration = trustsRegistrationRequest,
                        draftId = draftId,
                        internalId = request.identifier,
                        response = RegistrationFailureResponse(403, "ALREADY_REGISTERED", "Trust is already registered.")
                      )

                      logger.info(s"[registration][Session ID: ${request.sessionId}]" +
                        " Returning already registered response.")
                      Conflict(Json.toJson(alreadyRegisteredTrustsResponse))
                    case NoMatchException =>

                      auditService.audit(
                        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
                        registration = trustsRegistrationRequest,
                        draftId = draftId,
                        internalId = request.identifier,
                        response = RegistrationFailureResponse(403, "NO_MATCH", "There is no match in HMRC records.")
                      )

                      logger.info(s"[registration][Session ID: ${request.sessionId}]" +
                        s" Returning no match response.")
                      Forbidden(Json.toJson(noMatchRegistrationResponse))
                    case _: ServiceNotAvailableException =>

                      auditService.audit(
                        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
                        registration = trustsRegistrationRequest,
                        draftId = draftId,
                        internalId = request.identifier,
                        response = RegistrationFailureResponse(503, "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.")
                      )

                      logger.error(s"[registration][Session ID: ${request.sessionId}]" +
                        s" Service unavailable response from DES")
                      InternalServerError(Json.toJson(internalServerErrorResponse))
                    case _: BadRequestException =>

                      auditService.audit(
                        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
                        registration = trustsRegistrationRequest,
                        draftId = draftId,
                        internalId = request.identifier,
                        response = RegistrationFailureResponse(400, "INVALID_PAYLOAD", "Submission has not passed validation. Invalid payload..")
                      )

                      logger.error(s"[registration][Session ID: ${request.sessionId}]" +
                        s" bad request response from DES")
                      InternalServerError(Json.toJson(internalServerErrorResponse))
                    case NonFatal(e) =>
                      logger.error(s"[registration][Session ID: ${request.sessionId}]" +
                        s" Exception received : $e.")
                      InternalServerError(Json.toJson(internalServerErrorResponse))
                  }
                case None =>
                  Future.successful(BadRequest(Json.toJson(noDraftIdProvided)))
              }
            case Left(_) =>
              logger.error(s"[Session ID: ${request.sessionId}]" +
                s" trusts validation errors, returning bad request.")
              Future.successful(invalidRequestErrorResponse)
          }
      }
  }

}
