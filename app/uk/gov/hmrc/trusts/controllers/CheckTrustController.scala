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

package uk.gov.hmrc.trusts.controllers

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.ApiResponse._
import uk.gov.hmrc.trusts.models.ExistingCheckRequest
import uk.gov.hmrc.trusts.models.ExistingCheckResponse.{AlreadyRegistered, Matched, NotMatched}
import uk.gov.hmrc.trusts.services.DesService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton()
class CheckTrustController @Inject()(desService: DesService,
                                     cc: ControllerComponents,
                                     identify: IdentifierAction) extends TrustsBaseController(cc) {

  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

  def checkExistingTrust() = identify.async(parse.json) { implicit request =>
    withJsonBody[ExistingCheckRequest] {
      trustsCheckRequest =>
        desService.checkExistingTrust(trustsCheckRequest).map {
          result =>
            logger.info(s"[CheckTrustController][checkExistingTrust] response: $result")
            result match {
              case Matched => Ok(matchResponse)
              case NotMatched => Ok(noMatchResponse)
              case AlreadyRegistered => Conflict(Json.toJson(alreadyRegisteredTrustsResponse))
              case _ => InternalServerError(Json.toJson(internalServerErrorResponse))
            }
        }
    }

  }

}