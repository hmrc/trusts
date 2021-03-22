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

package services

import exceptions.InternalServerErrorException
import models.auditing.TrustAuditing
import models.get_trust.{GetTrustResponse, TransformationErrorResponse, TrustProcessedResponse}
import play.api.Logging
import play.api.libs.json.{JsObject, JsResult, JsSuccess, JsValue, Json, __, _}
import repositories.TransformationRepository
import transformers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationService @Inject()(repository: TransformationRepository,
                                      trustsService: TrustsService,
                                      auditService: AuditService) extends Logging {

  def getTransformedTrustJson(identifier: String, internalId: String)(implicit hc: HeaderCarrier): Future[JsObject] = {
    getTransformedData(identifier, internalId).flatMap {
      case TrustProcessedResponse(json, _) => Future.successful(json.as[JsObject])
      case _ => Future.failed(InternalServerErrorException("Trust is not in a processed state."))
    }
  }

  def getTransformedData(identifier: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetTrustResponse] = {
    trustsService.getTrustInfo(identifier, internalId).flatMap {
      case response: TrustProcessedResponse =>
        populateLeadTrusteeAddress(response.getTrust) match {
          case JsSuccess(fixed, _) =>

            applyTransformations(identifier, internalId, fixed).map {
              case JsSuccess(transformed, _) =>
                TrustProcessedResponse(transformed, response.responseHeader)
              case JsError(errors) => TransformationErrorResponse(errors.toString)
            }
          case JsError(errors) => Future.successful(TransformationErrorResponse(errors.toString))
        }
      case response => Future.successful(response)
    }
  }

  private def applyTransformations(identifier: String, internalId: String, json: JsValue): Future[JsResult[JsValue]] = {
    repository.get(identifier, internalId).map {
      case None =>
        JsSuccess(json)
      case Some(transformations) =>
        transformations.applyTransform(json)
    }
  }

  def applyDeclarationTransformations(identifier: String, internalId: String, json: JsValue)(implicit hc: HeaderCarrier): Future[JsResult[JsValue]] = {
    repository.get(identifier, internalId).map {
      case None =>
        logger.info(s"[Session ID: ${Session.id(hc)}]" +
          s" no transformations to apply")
        JsSuccess(json)
      case Some(transformations) =>

        auditService.audit(
          event = TrustAuditing.TRUST_TRANSFORMATIONS,
          request = Json.toJson(Json.obj()),
          internalId = internalId,
          response = Json.obj(
            "transformations" -> transformations,
            "data" -> json
          )
        )

        for {
          initial <- {
            logger.debug(s"[Session ID: ${Session.id(hc)}] applying transformations $transformations")
            logger.info(s"[Session ID: ${Session.id(hc)}]" +
              s" applying transformations")
            transformations.applyTransform(json)
          }
          transformed <- {
            logger.info(s"[Session ID: ${Session.id(hc)}]" +
              s" applying declaration transformations")
            transformations.applyDeclarationTransform(initial)
          }
        } yield {
          logger.debug(s"[Session ID: ${Session.id(hc)}] transformations have been applied, final output $transformed")
          transformed
        }
    }
  }

  def populateLeadTrusteeAddress(beforeJson: JsValue)(implicit hc: HeaderCarrier): JsResult[JsValue] = {
    val pathToLeadTrusteeAddress = __ \ 'details \ 'trust \ 'entities \ 'leadTrustees \ 'identification \ 'address

    logger.debug(s"[Session ID: ${Session.id(hc)}] setting address on lead trustee")

    if (beforeJson.transform(pathToLeadTrusteeAddress.json.pick).isSuccess) {
      logger.debug(s"[Session ID: ${Session.id(hc)}] lead trustee already had an address, no modification")
      JsSuccess(beforeJson)
    } else {
      logger.debug(s"[Session ID: ${Session.id(hc)}] copying address from correspondence and setting on lead trustee")
      val pathToCorrespondenceAddress = __ \ 'correspondence \ 'address
      val copyAddress = __.json.update(pathToLeadTrusteeAddress.json.copyFrom(pathToCorrespondenceAddress.json.pick))
      beforeJson.transform(copyAddress)
    }
  }

  def addNewTransform(identifier: String, internalId: String, newTransform: DeltaTransform): Future[Boolean] = {
    repository.get(identifier, internalId).map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    }.flatMap(newTransforms =>
      repository.set(identifier, internalId, newTransforms)).recoverWith {
        case e =>
        logger.error(s"Exception adding new transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def removeTaxableMigrationTransforms(identifier: String, internalId: String): Future[Boolean] = {
    repository.get(identifier, internalId).map {
      case Some(composedTransform) =>
        composedTransform.deltaTransforms.filterNot(_.isTaxableMigrationTransform)
      case None =>
        Nil
    }.flatMap { updatedTransforms =>
      repository.set(identifier, internalId, ComposedDeltaTransform(updatedTransforms))
    }.recoverWith {
      case e =>
        logger.error(s"Exception removing taxable migration transforms: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def migratingFromNonTaxableToTaxable(identifier: String, internalId: String): Future[Boolean] = {
    repository.get(identifier, internalId).map {
      case Some(composedTransform) =>
        composedTransform.deltaTransforms.exists(_.isTaxableMigrationTransform)
      case None =>
        false
    }.recoverWith {
      case e =>
        logger.error(s"Exception getting transforms: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def removeAllTransformations(identifier: String, internalId: String): Future[Option[JsObject]] = {
    repository.resetCache(identifier, internalId)
  }
}
