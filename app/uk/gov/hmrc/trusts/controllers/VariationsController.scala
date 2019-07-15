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
import uk.gov.hmrc.trusts.controllers.actions.{IdentifierAction, ValidateHeadersAction}
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.variation.Variation
import uk.gov.hmrc.trusts.services.DesService
import uk.gov.hmrc.trusts.utils.{Headers, ValidationUtil}
import uk.gov.hmrc.trusts.utils.ErrorResponses._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VariationsController @Inject()(
                                      identify: IdentifierAction,
                                      desService: DesService
                                    ) extends TrustsBaseController with ValidationUtil {

  def variation() = (identify andThen ValidateHeadersAction(Headers.VARIATION_CORRELATION_REGEX)).async(parse.json) {
    implicit request =>

      request.body.validate[Variation].fold(
        errors => {
          Logger.error(s"[variations] trusts validation errors from request body $errors.")
          Future.successful(invalidRequestErrorResponse)
        },
        desService.variation(_) map { response =>
          Ok(Json.toJson(response))
        } recover {
          case InvalidCorrelationIdException =>
            invalidCorrelationIdErrorResponse
          case DuplicateSubmissionException =>
            duplicateSubmissionErrorResponse
          case ServiceNotAvailableException(_) =>
            serviceUnavailableErrorResponse
          case _ =>
            internalServerErrorErrorResponse
        }
      )

  }
}

