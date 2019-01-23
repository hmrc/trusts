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

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.models.{ErrorRegistrationTrustsResponse, ExistingTrustCheckRequest, Registration, SuccessRegistrationResponse}
import uk.gov.hmrc.trusts.models.ExistingTrustResponse.{AlreadyRegistered, Matched, NotMatched}
import uk.gov.hmrc.trusts.services.{DesService, ValidationService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton()
class TrustsController @Inject()(desService: DesService, config: AppConfig, validationService: ValidationService) extends TrustsBaseController {


  def checkExistingTrust() = Action.async(parse.json) { implicit request =>
    withJsonBody[ExistingTrustCheckRequest] {
      trustsCheckRequest =>
        desService.checkExistingTrust(trustsCheckRequest).map {
          result =>
            Logger.info(s"[TrustsController][checkExistingTrust] response type :${result}")
            result match {
              case Matched => Ok(matchResponse)
              case NotMatched => Ok(noMatchResponse)
              case AlreadyRegistered => alreadyRegisteredResponse
              case _ => internalServerErrorResponse
            }
        }
    }

  }//checkExistingTrust


  def registration() = Action.async(parse.json) { implicit request =>

    val registrationJsonString = request.body.toString()
    val isValid = validationService.get(config.trustsApiRegistrationSchema)
                  .validate(registrationJsonString).isEmpty


    isValid match {
      case true => {
        request.body.validate[Registration].fold(
          errors => Future.successful(invalidRequestErrorResponse),
          trustsRegistrationRequest => {
            desService.registerTrust(trustsRegistrationRequest).map {
              response => {
                response match {
                  case success: SuccessRegistrationResponse =>
                    Ok(Json.toJson(success))
                  case failure: ErrorRegistrationTrustsResponse
                    if failure.code == "ALREADY_REGISTERED"=>  Conflict(doErrorResponse(failure.reason, failure.code))
                  case failure: ErrorRegistrationTrustsResponse => internalServerErrorResponse

                }
              }
            }
          }
        )
      }
      case false => Future.successful(invalidRequestErrorResponse)
    }

  } //registration



}


