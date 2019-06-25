/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.ApiResponse._
import uk.gov.hmrc.trusts.models.RegistrationResponse.formats
import uk.gov.hmrc.trusts.models.{TaxEnrolmentSuccess, _}
import uk.gov.hmrc.trusts.services.{AuthService, DesService, RosmPatternService, ValidationService}
import uk.gov.hmrc.trusts.utils.Headers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal


class RegisterTrustController @Inject()(desService: DesService, config: AppConfig,
                                        validationService: ValidationService,
                                        authService: AuthService,
                                        rosmPatternService: RosmPatternService) extends TrustsBaseController {

  def registration() = Action.async(parse.json) {
    implicit request =>

      val draftIdOption = request.headers.get(Headers.DraftRegistrationId)

      authService.authorisedUser() {
        userAffinityGroup: Option[AffinityGroup] =>

          val payload = request.body.toString()

          validationService
            .get(config.trustsApiRegistrationSchema)
            .validate[Registration](payload) match {

            case Right(trustsRegistrationRequest) =>

              draftIdOption match {
                case Some(draftId) =>
                  desService.registerTrust(trustsRegistrationRequest).map {
                    case response: RegistrationTrnResponse =>
                      completeRosmPatternWithTaxEnrolments(response.trn, userAffinityGroup)
                      Ok(Json.toJson(response))
                  } recover {
                    case AlreadyRegisteredException =>
                      Logger.info("[RegisterTrustController][registration] Returning already registered response.")
                      Conflict(Json.toJson(alreadyRegisteredTrustsResponse))
                    case NoMatchException =>
                      Logger.info("[RegisterTrustController][registration] Returning no match response.")
                      Forbidden(Json.toJson(noMatchRegistrationResponse))
                    case NonFatal(e) =>
                      Logger.error(s"[RegisterTrustController][registration] Exception received : $e.")
                      InternalServerError(Json.toJson(internalServerErrorResponse))
                  }
                case None =>
                  Future.successful(BadRequest(Json.toJson(noDraftIdProvided)))
              }
            case Left(_) =>
              Logger.error(s"[registration] trusts validation errors, returning bad request.")
              Future.successful(invalidRequestErrorResponse)
          }

      }
  }


  private def completeRosmPatternWithTaxEnrolments(trn: String,
                                                   userAffinityGroup: Option[AffinityGroup])
                                                  (implicit hc: HeaderCarrier) : Unit  = {
    userAffinityGroup match {
      case Some(AffinityGroup.Organisation) =>
        rosmPatternService.completeRosmTransaction(trn) map {
          case TaxEnrolmentSuccess =>
            Logger.info(s"Rosm completed successfully for provided trn : ${trn}.")
          case TaxEnrolmentFailure =>
            Logger.error(s"Rosm pattern is not completed for trn:  ${trn}.")
        } recover {
          case NonFatal(exception) => {
            Logger.error(s"Rosm pattern is not completed for trn:  ${trn}.")
            Logger.error(s"[completeRosmPatternWithTaxEnrolments] Exception received : ${exception}.")
          }
        }
      case _ =>
        Logger.info("Tax enrolments is not required for Agent.")
    }
  }


}
