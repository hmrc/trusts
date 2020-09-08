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
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.Action
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustNaturalPersonType
import uk.gov.hmrc.trusts.models.RemoveOtherIndividual
import uk.gov.hmrc.trusts.models.variation.NaturalPersonType
import uk.gov.hmrc.trusts.services.OtherIndividualTransformationService
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class OtherIndividualTransformationController @Inject()(
                                          identify: IdentifierAction,
                                          transformService: OtherIndividualTransformationService
                                        )(implicit val executionContext: ExecutionContext) extends TrustsBaseController with ValidationUtil {
  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

    def removeOtherIndividual(utr: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[RemoveOtherIndividual] match {
          case JsSuccess(otherIndividual, _) =>
            transformService.removeOtherIndividual(utr, request.identifier, otherIndividual) map { _ =>
              Ok
            }
          case JsError(_) => Future.successful(BadRequest)
        }
      }
    }

    def amendOtherIndividual(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[NaturalPersonType] match {
          case JsSuccess(otherIndividual, _) =>
            transformService.amendOtherIndividualTransformer(
              utr,
              index,
              request.identifier,
              otherIndividual
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[OtherIndividualTransformationController][amendOtherIndividual] " +
              s"Supplied json could not be read as an Other Individual - $errors")
            Future.successful(BadRequest)
        }
      }
    }

    def addOtherIndividual(utr: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[DisplayTrustNaturalPersonType] match {
          case JsSuccess(otherIndividual, _) =>

            transformService.addOtherIndividualTransformer(
              utr,
              request.identifier,
              otherIndividual
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[OtherIndividualTransformationController][addOtherIndividualTransformer] " +
              s"Supplied json could not be read as an Other Individual - $errors")
            Future.successful(BadRequest)
        }
      }
    }

}
