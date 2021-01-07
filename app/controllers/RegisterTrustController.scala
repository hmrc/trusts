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

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsBoolean, JsObject, JsPath, JsString, JsValue, Json, Reads, __}
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
import services.{AuditService, RosmPatternService, TrustsService, ValidationService, _}
import utils.ErrorResponses._
import utils.Headers
import utils.JsonOps._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class RegisterTrustController @Inject()(trustsService: TrustsService, config: AppConfig,
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

//      val payload = request.body.applyRules.toString //TODO: Uncomment this line when all 5mld data is available
      val draftIdOption = request.headers.get(Headers.DraftRegistrationId)

      schemaF.flatMap {
        schema =>

//TODO: Remove the defaulted data for 5mld below when the services are updated to add real data for 5mld
          val payload = if (schema == config.trustsApiRegistrationSchema5MLD) {

            def putNewValue(path: JsPath, value: JsValue ): Reads[JsObject] =
              __.json.update(path.json.put(value))

            val leadTrusteeIndPath = (__ \ 'trust \ 'entities \ 'leadTrustees \ 'leadTrusteeInd).json

            request.body.applyRules.transform(
              if (request.body.transform(leadTrusteeIndPath.pick).isSuccess) {
                putNewValue((__ \ 'trust \ 'entities \ 'leadTrustees \ 'leadTrusteeInd \ 'countryOfResidence), JsString("GB")) andThen
                putNewValue((__ \ 'trust \ 'entities \ 'leadTrustees \ 'leadTrusteeInd \ 'nationality), JsString("GB")) andThen
                putNewValue((__ \ 'trust \ 'details \ 'expressTrust), JsBoolean(false)) andThen
                putNewValue((__ \ 'trust \ 'details \ 'trustTaxable), JsBoolean(true)) andThen
                putNewValue((__ \ 'trust \ 'details \ 'trustUKResident), JsBoolean(true)) andThen
                putNewValue((__ \ 'submissionDate), JsString("2021-01-01"))
              } else {
                putNewValue((__ \ 'trust \ 'entities \ 'leadTrustees \ 'leadTrusteeOrg \ 'countryOfResidence), JsString("GB")) andThen
                putNewValue((__ \ 'trust \ 'details \ 'expressTrust), JsBoolean(false)) andThen
                putNewValue((__ \ 'trust \ 'details \ 'trustTaxable), JsBoolean(true)) andThen
                putNewValue((__ \ 'trust \ 'details \ 'trustUKResident), JsBoolean(true)) andThen
                putNewValue((__ \ 'submissionDate), JsString("2021-01-01"))
              }
            ).fold(
              _ => {
                logger.error("[registration] Could not add temporary data for 5mld tests")
                request.body.applyRules.toString
              },
              value => value.toString)
          } else {
            request.body.applyRules.toString
          }
//TODO: When ALL services are updated with real 5mld data delete all of the above and uncomment val payload above

          validationService
            .get(schema)
            .validate[Registration](payload) match {

            case Right(trustsRegistrationRequest) =>

              draftIdOption match {
                case Some(draftId) =>
                  trustsService.registerTrust(trustsRegistrationRequest).flatMap {
                    case response: RegistrationTrnResponse =>

                      auditService.audit(
                        event = TrustAuditing.TRUST_REGISTRATION_SUBMITTED,
                        registration = trustsRegistrationRequest,
                        draftId = draftId,
                        internalId = request.internalId,
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
                        internalId = request.internalId,
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
                        internalId = request.internalId,
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
                        internalId = request.internalId,
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
                        internalId = request.internalId,
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
