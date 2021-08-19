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
import transformers.beneficiaries.{AddBeneficiaryTransform, AmendBeneficiaryTransform}
import transformers.settlors.{AddSettlorTransform, AmendSettlorTransform}
import transformers.trustdetails.SetTrustDetailTransform
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants._
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

    }.flatMap { newTransforms =>

      repository.set(identifier, internalId, newTransforms)

    }.recoverWith {
      case e =>
        logger.error(s"Exception adding new transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def removeAllTransformations(identifier: String, internalId: String): Future[Option[JsObject]] = {
    repository.resetCache(identifier, internalId)
  }

  def removeTrustTypeDependentTransformFields(identifier: String, internalId: String): Future[Boolean] = {
    for {
      transforms <- repository.get(identifier, internalId)
      updatedTransforms = transforms match {
        case Some(value) => ComposedDeltaTransform(value.deltaTransforms.map {
          case x: AddBeneficiaryTransform => x.copy(entity = x.removeTrustTypeDependentFields(x.entity))
          case x: AmendBeneficiaryTransform => x.copy(amended = x.removeTrustTypeDependentFields(x.amended))
          case x: AddSettlorTransform => x.copy(entity = x.removeTrustTypeDependentFields(x.entity))
          case x: AmendSettlorTransform => x.copy(amended = x.removeTrustTypeDependentFields(x.amended))
          case x => x
        })
        case None => ComposedDeltaTransform()
      }
      result <- repository.set(identifier, internalId, updatedTransforms)
    } yield {
      result
    }
  }

  /**
   * Need to remove any <i>SetTrustDetailTransform</i> transforms corresponding to optional trust detail fields
   * before we call <i>setMigratingTrustDetails</i> from the frontend. This is done to avoid a few scenarios:
   * <ol>
   *   <li>so we don't end up with transforms for setting more than one of <i>deedOfVariation</i>, <i>interVivos</i>, and
   *  <i>efrbsStartDate</i>, as these fields are mutually exclusive</li>
   *  <li>so we don't end up with a transform for <i>lawCountry</i> if the user changes their answer to the 'governed by UK law'
   *  question from 'No' to 'Yes'</li>
   *  <li>so we don't end up with a transform for <i>trustUKRelation</i> if the user changes their answers from being that of a
   *  non-UK-resident trust to that of a UK-resident trust</li>
   * </ol>
   */
  def removeOptionalTrustDetailTransforms(identifier: String, internalId: String): Future[Boolean] = {

    val optionalTrustDetails = Seq(LAW_COUNTRY, UK_RELATION, DEED_OF_VARIATION, INTER_VIVOS, EFRBS_START_DATE)

    for {
      transforms <- repository.get(identifier, internalId)
      updatedTransforms = transforms match {
        case Some(value) => ComposedDeltaTransform(value.deltaTransforms.filter {
          case x: SetTrustDetailTransform if optionalTrustDetails.contains(x.`type`) => false
          case _ => true
        })
        case None => ComposedDeltaTransform()
      }
      result <- repository.set(identifier, internalId, updatedTransforms)
    } yield {
      result
    }
  }
}
