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
import uk.gov.hmrc.http.ServiceUnavailableException
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.models.{AlreadyRegisteredException, ExistingTrustCheckRequest, Registration}
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
    validationService.get(config.trustsApiRegistrationSchema)
      .validate[Registration](registrationJsonString)


    validationService.get(config.trustsApiRegistrationSchema)
      .validate[Registration](registrationJsonString) match {

      case Right(trustsRegistrationRequest) => {
        desService.registerTrust(trustsRegistrationRequest).map {
          response => Ok(Json.toJson(response))
        } recover {
          case alreadyRegisterd: AlreadyRegisteredException => {
            Logger.info("[TrustsController][registration] Returning already registered response.")
            Conflict(doErrorResponse("The trust is already registered.", "ALREADY_REGISTERED"))
          }
          case exception: Exception => {
            Logger.error(s"[TrustsController][registration] Exception received : ${exception}.")
            internalServerErrorResponse
          }
        }
      }
      case Left(validationErros) =>
        Future.successful(invalidRequestErrorResponse)
    }

  } //registration



}


