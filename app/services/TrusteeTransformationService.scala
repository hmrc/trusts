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

import java.time.LocalDate

import javax.inject.Inject
import play.api.libs.json.{__, _}
import exceptions.InternalServerErrorException
import models.Success
import models.get_trust.TrustProcessedResponse
import models.variation.{AmendedLeadTrusteeIndType, AmendedLeadTrusteeOrgType, TrusteeType}
import transformers._
import transformers.remove.RemoveTrustee
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class TrusteeTransformationService @Inject()(
                                              transformationService: TransformationService,
                                              localDateService: LocalDateService) {

  def addAmendLeadTrusteeIndTransformer(identifier: String, internalId: String, newLeadTrustee: AmendedLeadTrusteeIndType): Future[Success.type] = {
    transformationService.addNewTransform(
      identifier,
      internalId,
      AmendLeadTrusteeIndTransform(newLeadTrustee)
    ).map(_ => Success)
  }

  def addAmendLeadTrusteeOrgTransformer(identifier: String, internalId: String, newLeadTrustee: AmendedLeadTrusteeOrgType): Future[Success.type] = {
    transformationService.addNewTransform(
      identifier,
      internalId,
      AmendLeadTrusteeOrgTransform(newLeadTrustee)
    ).map(_ => Success)
  }

  def addAmendTrusteeTransformer(identifier: String,
                                 index: Int,
                                 internalId: String,
                                 newTrustee: TrusteeType)(implicit hc: HeaderCarrier): Future[Success.type] = {

    getTrusteeAtIndex(identifier, internalId, index).flatMap {
      case scala.util.Success(trusteeJson) =>
        transformationService.addNewTransform(identifier, internalId, newTrustee match {
          case TrusteeType(Some(trusteeInd), None) =>
            AmendTrusteeIndTransform(index, trusteeInd, trusteeJson, localDateService.now)
          case TrusteeType(None, Some(trusteeOrg)) =>
            AmendTrusteeOrgTransform(index, trusteeOrg, trusteeJson, localDateService.now)
        }).map(_ => Success)
      case scala.util.Failure(_) => Future.failed(InternalServerErrorException(s"Could not pick trustee at index $index."))
    }
  }

  def addPromoteTrusteeIndTransformer(
                                       identifier: String,
                                       internalId: String,
                                       index: Int,
                                       newLeadTrustee: AmendedLeadTrusteeIndType,
                                       endDate: LocalDate)(implicit hc: HeaderCarrier): Future[Success.type] = {

    getTrusteeAtIndex(identifier, internalId, index).flatMap {
      case scala.util.Success(trusteeJson) =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          PromoteTrusteeIndTransform(index, newLeadTrustee, endDate, trusteeJson, localDateService.now)
        ).map(_ => Success)
      case scala.util.Failure(_) => Future.failed(InternalServerErrorException(s"Could not pick trustee at index $index."))
    }
  }

  def addPromoteTrusteeOrgTransformer(
                                       identifier: String,
                                       internalId: String,
                                       index: Int,
                                       newLeadTrustee: AmendedLeadTrusteeOrgType,
                                       endDate: LocalDate)(implicit hc: HeaderCarrier): Future[Success.type] = {

    getTrusteeAtIndex(identifier, internalId, index).flatMap {
      case scala.util.Success(trusteeJson) =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          PromoteTrusteeOrgTransform(index, newLeadTrustee, endDate, trusteeJson, localDateService.now)
        ).map(_ => Success)
      case scala.util.Failure(_) => Future.failed(InternalServerErrorException(s"Could not pick trustee at index $index."))
    }
  }

  def addAddTrusteeTransformer(identifier: String, internalId: String, newTrustee: TrusteeType): Future[Success.type] = {
    transformationService.addNewTransform(identifier, internalId, newTrustee match {
      case TrusteeType(Some(trusteeInd), None) => AddTrusteeIndTransform(trusteeInd)
      case TrusteeType(None, Some(trusteeOrg)) => AddTrusteeOrgTransform(trusteeOrg)
    }).map(_ => Success)
  }

  def addRemoveTrusteeTransformer(identifier: String, internalId: String, remove: RemoveTrustee)(implicit hc: HeaderCarrier): Future[Success.type] = {

    getTrusteeAtIndex(identifier, internalId, remove.index).flatMap {
      case scala.util.Success(trusteeJson) =>
        transformationService.addNewTransform(identifier, internalId, RemoveTrusteeTransform(remove.endDate, remove.index, trusteeJson)).map(_ => Success)
      case scala.util.Failure(_) => Future.failed(InternalServerErrorException(s"Could not pick trustee at index ${remove.index}."))
    }
  }

  private def getTrusteeAtIndex(identifier: String, internalId: String, index: Int)(implicit hc: HeaderCarrier): Future[Try[JsObject]] = {

    transformationService.getTransformedData(identifier, internalId).map {
      case TrustProcessedResponse(transformedJson, _) =>
        val trusteePath = (__ \ 'details \ 'trust \ 'entities \ 'trustees \ index).json
        transformedJson.transform(trusteePath.pick).fold(
          _ => scala.util.Failure(InternalServerErrorException("Could not locate trustee at index")),
          value => scala.util.Success(value.as[JsObject])
        )
      case _ => scala.util.Failure(InternalServerErrorException("Trust is not in processed state."))
    }
  }

}
