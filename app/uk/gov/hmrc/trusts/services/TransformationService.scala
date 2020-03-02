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
import play.api.libs.json.{JsPath, JsResult, JsSuccess, JsValue, Json, __}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.RemoveTrustee
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustLeadTrusteeType, DisplayTrustTrusteeType}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationService @Inject()(repository: TransformationRepository,
                                      auditService: AuditService){

  def applyTransformations(utr: String, internalId: String, json: JsValue)(implicit hc : HeaderCarrier): Future[JsResult[JsValue]] = {
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

        Logger.info(s"[TransformationService] applying transformations")
        transformations.applyTransform(json)
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

  def addAmendLeadTrusteeTransformer(utr: String, internalId: String, newLeadTrustee: DisplayTrustLeadTrusteeType): Future[Unit] = {
    addNewTransform(utr, internalId, newLeadTrustee match {
      case DisplayTrustLeadTrusteeType(Some(trusteeInd), None) => AmendLeadTrusteeIndTransform(trusteeInd)
      case DisplayTrustLeadTrusteeType(None, Some(trusteeOrg)) => AmendLeadTrusteeOrgTransform(trusteeOrg)
    })
  }

  def addAddTrusteeTransformer(utr: String, internalId: String, newTrustee: DisplayTrustTrusteeType): Future[Unit] = {
    addNewTransform(utr, internalId, newTrustee match {
      case DisplayTrustTrusteeType(Some(trusteeInd), None) => AddTrusteeIndTransform(trusteeInd)
    })
  }

  def addRemoveTrusteeTransformer(utr: String, internalId: String, remove: RemoveTrustee) : Future[Unit] = {
    addNewTransform(utr, internalId, RemoveTrusteeTransform(remove.endDate, remove.index, Json.obj()))
  }

  private def addNewTransform(utr: String, internalId: String, newTransform: DeltaTransform) = {
    repository.get(utr, internalId).map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    }.flatMap(newTransforms => repository.set(utr, internalId, newTransforms).map(_ => ()))
  }
}
