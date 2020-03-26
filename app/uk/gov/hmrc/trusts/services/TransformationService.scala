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

package uk.gov.hmrc.trusts.services

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsObject, JsResult, JsSuccess, JsValue, Json, __, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.TransformationErrorResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationService @Inject()(repository: TransformationRepository,
                                      desService: DesService,
                                      auditService: AuditService){
  def getTransformedData(utr: String, internalId: String)(implicit hc : HeaderCarrier): Future[GetTrustResponse] = {
    desService.getTrustInfo(utr, internalId).flatMap {
      case response: TrustProcessedResponse =>
        populateLeadTrusteeAddress(response.getTrust) match {
          case JsSuccess(fixed, _) =>
            applyTransformations(utr, internalId, fixed).map {
              case JsSuccess(transformed, _) => TrustProcessedResponse(transformed, response.responseHeader)
              case JsError(errors) => TransformationErrorResponse(errors.toString)
            }
          case JsError(errors) => Future.successful(TransformationErrorResponse(errors.toString))
        }
      case response => Future.successful(response)
    }
  }

  private def applyTransformations(utr: String, internalId: String, json: JsValue)(implicit hc : HeaderCarrier): Future[JsResult[JsValue]] = {
    repository.get(utr, internalId).map {
      case None => JsSuccess(json)
      case Some(transformations) => transformations.applyTransform(json)
    }
  }

  def applyDeclarationTransformations(utr: String, internalId: String, json: JsValue)(implicit hc : HeaderCarrier): Future[JsResult[JsValue]] = {
    repository.get(utr, internalId).map {
      case None =>
        Logger.info(s"[TransformationService] no transformations to apply")
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
            Logger.info(s"[TransformationService] applying transformations")
            transformations.applyTransform(json)
          }
          transformed <- {
            Logger.info(s"[TransformationService] applying declaration transformations")
            transformations.applyDeclarationTransform(initial)
          }
        } yield transformed
    }
  }

  def populateLeadTrusteeAddress(beforeJson: JsValue): JsResult[JsValue] = {
    val pathToLeadTrusteeAddress = __ \ 'details \ 'trust \ 'entities \ 'leadTrustees \ 'identification \ 'address

    if (beforeJson.transform(pathToLeadTrusteeAddress.json.pick).isSuccess)
      JsSuccess(beforeJson)
    else {
      val pathToCorrespondenceAddress = __ \ 'correspondence \ 'address
      val copyAddress = __.json.update(pathToLeadTrusteeAddress.json.copyFrom(pathToCorrespondenceAddress.json.pick))
      beforeJson.transform(copyAddress)
    }
  }

  def addNewTransform(utr: String, internalId: String, newTransform: DeltaTransform) : Future[Boolean] = {
    repository.get(utr, internalId).map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    }.flatMap(newTransforms =>
      repository.set(utr, internalId, newTransforms)).recoverWith {
      case e =>
        Logger.error(s"[TransformationService] exception adding new transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def removeAllTransformations(utr: String, internalId: String): Future[Option[JsObject]] = {
    repository.resetCache(utr, internalId)
  }
}
