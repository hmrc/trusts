/*
 * Copyright 2023 HM Revenue & Customs
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
import services.auditing.AuditService
import transformers._
import transformers.beneficiaries.{AddBeneficiaryTransform, AmendBeneficiaryTransform}
import transformers.settlors.{AddSettlorTransform, AmendSettlorTransform}
import transformers.trustdetails.SetTrustDetailTransform
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants._
import utils.Session

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransformationService @Inject()(repository: TransformationRepository,
                                      trustsService: TrustsService,
                                      auditService: AuditService)(implicit ec: ExecutionContext) extends Logging {

  def getTransformedTrustJson(identifier: String, internalId: String, sessionId: String)(implicit hc: HeaderCarrier): Future[JsObject] = {
    getTransformedData(identifier, internalId, sessionId).flatMap {
      case TrustProcessedResponse(json, _) => Future.successful(json.as[JsObject])
      case _ => Future.failed(InternalServerErrorException("Trust is not in a processed state."))
    }
  }

  def getTransformedData(identifier: String, internalId: String, sessionId: String)(implicit hc: HeaderCarrier): Future[GetTrustResponse] = {
    trustsService.getTrustInfo(identifier, internalId, sessionId).flatMap {
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

  private def applyTransformations(identifier: String, internalId: String, json: JsValue)(implicit hc: HeaderCarrier): Future[JsResult[JsValue]] = {
    repository.get(identifier, internalId, Session.id(hc)).map {
      case None =>
        JsSuccess(json)
      case Some(transformations) =>
        transformations.applyTransform(json)
    }
  }

  def applyDeclarationTransformations(identifier: String, internalId: String, json: JsValue)(implicit hc: HeaderCarrier): Future[JsResult[JsValue]] = {
    repository.get(identifier, internalId, Session.id(hc)).map {
      case None =>
        logger.info(s"[TransformationService][applyDeclarationTransformations][Session ID: ${Session.id(hc)}]" +
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
            logger.info(s"[TransformationService][applyDeclarationTransformations][Session ID: ${Session.id(hc)}]" +
              s" applying transformations")
            transformations.applyTransform(json)
          }
          transformed <- {
            logger.info(s"[TransformationService][applyDeclarationTransformations][Session ID: ${Session.id(hc)}]" +
              s" applying declaration transformations")
            transformations.applyDeclarationTransform(initial)
          }
        } yield {
          transformed
        }
    }
  }

  def populateLeadTrusteeAddress(beforeJson: JsValue): JsResult[JsValue] = {
    val pathToLeadTrusteeAddress = __ \ Symbol("details") \ Symbol("trust") \ Symbol("entities") \
      Symbol("leadTrustees") \ Symbol("identification") \ Symbol("address")
    if (beforeJson.transform(pathToLeadTrusteeAddress.json.pick).isSuccess) {
      JsSuccess(beforeJson)
    } else {
      val pathToCorrespondenceAddress = __ \ Symbol("correspondence") \ Symbol("address")
      val copyAddress = __.json.update(pathToLeadTrusteeAddress.json.copyFrom(pathToCorrespondenceAddress.json.pick))
      beforeJson.transform(copyAddress)
    }
  }

  def addNewTransform(identifier: String, internalId: String, newTransform: DeltaTransform)(implicit hc: HeaderCarrier): Future[Boolean] = {
    repository.get(identifier, internalId, Session.id(hc)).map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    }.flatMap(newTransforms =>
      repository.set(identifier, internalId, Session.id(hc), newTransforms)).recoverWith {
      case e =>
        logger.error(s"[TransformationService][addNewTransform] Exception adding new transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def removeAllTransformations(identifier: String, internalId: String, sessionId: String): Future[Boolean] = {
    repository.resetCache(identifier, internalId, sessionId)
  }

  def removeTrustTypeDependentTransformFields(identifier: String, internalId: String, sessionId: String): Future[Boolean] = {
    for {
      transforms <- repository.get(identifier, internalId, sessionId)
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
      result <- repository.set(identifier, internalId, sessionId, updatedTransforms)
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
  def removeOptionalTrustDetailTransforms(identifier: String, internalId: String, sessionId: String): Future[Boolean] = {

    val optionalTrustDetails = Seq(LAW_COUNTRY, UK_RELATION, DEED_OF_VARIATION, INTER_VIVOS, EFRBS_START_DATE)

    for {
      transforms <- repository.get(identifier, internalId, sessionId)
      updatedTransforms = transforms match {
        case Some(value) => ComposedDeltaTransform(value.deltaTransforms.filter {
          case x: SetTrustDetailTransform if optionalTrustDetails.contains(x.`type`) => false
          case _ => true
        })
        case None => ComposedDeltaTransform()
      }
      result <- repository.set(identifier, internalId, sessionId, updatedTransforms)
    } yield {
      result
    }
  }
}
