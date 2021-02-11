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
import exceptions._
import models._
import models.auditing.TrustAuditing
import models.registration.ApiResponse._
import models.registration.RegistrationFailureResponse
import models.registration.RegistrationTrnResponse._
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import services.{AuditService, RosmPatternService, TrustsService, ValidationService, _}
import uk.gov.hmrc.http.BadRequestException
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
                                         auditService: AuditService,
                                         cc: ControllerComponents,
                                         trustsStoreService: TrustsStoreService,
                                         amendSubmissionDataService: AmendSubmissionDataService
                                       ) extends TrustsBaseController(cc) with Logging {

  def registration(): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.headers.get(Headers.DraftRegistrationId).fold {
        logger.error(s"[Session ID: ${request.sessionId}] no draft id provided in headers")
        Future.successful(BadRequest(Json.toJson(noDraftIdProvided)))
      } { draftId =>

        schemaF.flatMap {
          schema =>

            val is5mldEnabled: Boolean = schema == config.trustsApiRegistrationSchema5MLD
            val payload = amendSubmissionDataService.applyRulesAndAddSubmissionDate(is5mldEnabled, request.body)

            val validation = validationService.get(schema).validate[Registration](payload.toString())

            validation.fold(
              _ => {
                logger.error(s"[Session ID: ${request.sessionId}] problem validating submission")
                Future.successful(invalidRequestErrorResponse)
              },
              v =>
                trustsService.registerTrust(v).flatMap {
                  response =>
                    auditService.audit(
                      event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
                      registration = v,
                      draftId = draftId,
                      internalId = request.internalId,
                      response = response
                    )

                    rosmPatternService.enrolAndLogResult(
                      trn = response.trn,
                      affinityGroup = request.affinityGroup,
                      taxable = v.trust.details.trustTaxable.getOrElse(true)
                    ) map {
                      _ =>
                        Ok(Json.toJson(response))
                    }
                } recover[Result](
                  handleBusinessErrors(v, draftId) orElse
                    handleHttpError(v, draftId)
                  )
            )
        }
      }
  }

  private def handleHttpError(v: Registration, draftId: String)(implicit request: IdentifierRequest[_]): PartialFunction[Throwable, Result] = {
    case _: ServiceNotAvailableException =>
      auditService.audit(
        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
        registration = v,
        draftId = draftId,
        internalId = request.internalId,
        response = RegistrationFailureResponse(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.")
      )
      logger.error(s"[registration][Session ID: ${request.sessionId}] Service unavailable response from DES")
      InternalServerError(Json.toJson(internalServerErrorResponse))

    case _: BadRequestException =>
      auditService.audit(
        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
        registration = v,
        draftId = draftId,
        internalId = request.internalId,
        response = RegistrationFailureResponse(BAD_REQUEST, "INVALID_PAYLOAD", "Submission has not passed validation. Invalid payload.")
      )
      logger.error(s"[registration][Session ID: ${request.sessionId}] bad request response from DES")
      InternalServerError(Json.toJson(internalServerErrorResponse))

    case e =>
      logger.error(s"[registration][Session ID: ${request.sessionId}] Exception received: $e.")
      InternalServerError(Json.toJson(internalServerErrorResponse))
  }

  def handleBusinessErrors(v: Registration, draftId: String)(implicit request: IdentifierRequest[_]): PartialFunction[Throwable, Result] = {
    case AlreadyRegisteredException =>
      auditService.audit(
        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
        registration = v,
        draftId = draftId,
        internalId = request.internalId,
        response = RegistrationFailureResponse(FORBIDDEN, "ALREADY_REGISTERED", "Trust is already registered.")
      )
      logger.info(s"[registration][Session ID: ${request.sessionId}] Returning already registered response.")
      Conflict(Json.toJson(alreadyRegisteredTrustsResponse))

    case NoMatchException =>
      auditService.audit(
        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
        registration = v,
        draftId = draftId,
        internalId = request.internalId,
        response = RegistrationFailureResponse(FORBIDDEN, "NO_MATCH", "There is no match in HMRC records.")
      )
      logger.info(s"[registration][Session ID: ${request.sessionId}] Returning no match response.")
      Forbidden(Json.toJson(noMatchRegistrationResponse))
  }

  private def schemaF(implicit request: IdentifierRequest[JsValue]): Future[String] = {
    trustsStoreService.is5mldEnabled.map {
      case true => config.trustsApiRegistrationSchema5MLD
      case _ => config.trustsApiRegistrationSchema4MLD
    }
  }

}
