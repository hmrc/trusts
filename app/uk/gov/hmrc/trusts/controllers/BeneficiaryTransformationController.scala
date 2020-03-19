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
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsString, JsSuccess, JsValue}
import play.api.mvc.Action
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.RemoveBeneficiary
import uk.gov.hmrc.trusts.services.BeneficiaryTransformationService
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class BeneficiaryTransformationController @Inject()(
                                          identify: IdentifierAction,
                                          beneficiaryTransformationService: BeneficiaryTransformationService
                                        )(implicit val executionContext: ExecutionContext) extends TrustsBaseController with ValidationUtil {
  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

  def amendUnidentifiedBeneficiary(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[JsString] match {
        case JsSuccess(description, _) =>
          beneficiaryTransformationService.addAmendUnidentifiedBeneficiaryTransformer(utr, index, request.identifier, description.value) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"Supplied description could not be read as a JsString - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def removeBeneficiary(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveBeneficiary] match {
        case JsSuccess(beneficiary, _) =>
          beneficiaryTransformationService.removeBeneficiary(utr, request.identifier, beneficiary) map { _ =>
          Ok
        }
        case JsError(_) => Future.successful(BadRequest)
      }
    }
  }
}
