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

import javax.inject.Inject
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustLeadTrusteeType
import uk.gov.hmrc.trusts.services.{AuditService, DesService, TransformationService, ValidationService, VariationService}
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.Future

class ChangeController @Inject()(
                                  identify: IdentifierAction,
                                  transformationService: TransformationService
                                  ) extends TrustsBaseController with ValidationUtil {

  def amendLeadTrustee(utr: String) = identify.async(parse.json) {
    implicit request => {

      val payload = request.body

      payload.validate[DisplayTrustLeadTrusteeType] match {
        case JsSuccess(model, _) =>
          transformationService.addAmendLeadTrustee(utr, request.identifier, model)
          Future.successful(Ok)

        case JsError(errors) =>
          Future.successful(BadRequest)

      }
    }
  }
}
