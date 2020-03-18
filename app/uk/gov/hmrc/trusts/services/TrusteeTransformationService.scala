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

import java.time.LocalDate

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsObject, JsResult, JsSuccess, JsValue, Json, __, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.exceptions.InternalServerErrorException
import uk.gov.hmrc.trusts.models.RemoveTrustee
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.TransformationErrorResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustLeadTrusteeType, DisplayTrustTrusteeType, GetTrustResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrusteeTransformationService @Inject()(repository: TransformationRepository,
                                             desService: DesService,
                                             auditService: AuditService) extends TransformationService(repository, desService, auditService){

  def addAmendLeadTrusteeTransformer(utr: String, internalId: String, newLeadTrustee: DisplayTrustLeadTrusteeType): Future[Unit] = {
    addNewTransform(utr, internalId, newLeadTrustee match {
      case DisplayTrustLeadTrusteeType(Some(trusteeInd), None) => AmendLeadTrusteeIndTransform(trusteeInd)
      case DisplayTrustLeadTrusteeType(None, Some(trusteeOrg)) => AmendLeadTrusteeOrgTransform(trusteeOrg)
    })
  }

  def addAmendTrusteeTransformer(utr: String, index: Int, internalId: String, trustee: DisplayTrustTrusteeType): Future[Unit] = {
    addNewTransform(utr, internalId, trustee match {
      case DisplayTrustTrusteeType(Some(trusteeInd), None) => AmendTrusteeIndTransform(index, trusteeInd)
      case DisplayTrustTrusteeType(None, Some(trusteeOrg)) => AmendTrusteeOrgTransform(index, trusteeOrg)
    })
  }

  def addPromoteTrusteeTransformer(
                                    utr: String,
                                    internalId: String,
                                    index: Int,
                                    newLeadTrustee: DisplayTrustLeadTrusteeType,
                                    endDate: LocalDate)(implicit hc: HeaderCarrier): Future[Unit] = {
    getTrusteeAtIndex(utr, internalId, index).map {
      case JsSuccess(trusteeJson: JsValue, _) =>
        addNewTransform(utr, internalId, newLeadTrustee match {
          case DisplayTrustLeadTrusteeType(Some(trusteeInd), None) => PromoteTrusteeIndTransform(index, trusteeInd, endDate, trusteeJson)
          case DisplayTrustLeadTrusteeType(None, Some(trusteeOrg)) => PromoteTrusteeOrgTransform(index, trusteeOrg, endDate, trusteeJson)
        })
      case JsError(_) => Future.failed(InternalServerErrorException(s"Could not pick trustee at index ${index}."))
      }
  }

  def addAddTrusteeTransformer(utr: String, internalId: String, newTrustee: DisplayTrustTrusteeType): Future[Unit] = {
    addNewTransform(utr, internalId, newTrustee match {
      case DisplayTrustTrusteeType(Some(trusteeInd), None) => AddTrusteeIndTransform(trusteeInd)
      case DisplayTrustTrusteeType(None, Some(trusteeOrg)) => AddTrusteeOrgTransform(trusteeOrg)
    })
  }

  def addRemoveTrusteeTransformer(utr: String, internalId: String, remove: RemoveTrustee)(implicit hc: HeaderCarrier) : Future[Unit] = {
    getTrusteeAtIndex(utr, internalId, remove.index).map {
      case JsSuccess(trusteeJson: JsValue, _) => addNewTransform(utr, internalId, RemoveTrusteeTransform(remove.endDate, remove.index, trusteeJson))
      case JsError(_) => Future.failed(InternalServerErrorException(s"Could not pick trustee at index ${remove.index}."))
    }
  }

  private def getTrusteeAtIndex(utr: String, internalId: String, index: Int)(implicit hc: HeaderCarrier) = {
    getTransformedData(utr, internalId).map {
      case TrustProcessedResponse(transformedJson, _) =>
        val trusteePath = (__ \ 'details \ 'trust \ 'entities \ 'trustees \ index).json
        transformedJson.transform(trusteePath.pick)
      case _ => Future.failed(InternalServerErrorException("Trust is not in processed state."))
    }
  }

}
