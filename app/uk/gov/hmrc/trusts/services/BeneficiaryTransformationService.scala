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
import play.api.libs.json.{JsObject, __}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.exceptions.InternalServerErrorException
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.variation.UnidentifiedType
import uk.gov.hmrc.trusts.models.{RemoveBeneficiary, Success}
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.{ExecutionContext, Future}

class BeneficiaryTransformationService @Inject()(transformationService: TransformationService)(implicit ec:ExecutionContext) {
  def removeBeneficiary(utr: String, internalId: String, removeBeneficiary: RemoveBeneficiary)(implicit hc: HeaderCarrier) : Future[Success.type] = {
    transformationService.getTransformedData(utr, internalId).flatMap {
      case TrustProcessedResponse(json, _) =>
        println(json)
        val beneficiaryPath = (__ \ 'details \ 'trust \ 'entities \ 'beneficiary \ removeBeneficiary.beneficiaryType \ removeBeneficiary.index).json
        json.transform(beneficiaryPath.pick).fold(
          _ => Future.failed(InternalServerErrorException("Could not locate beneficiary at index")),
          value => Future.successful(value.as[JsObject])
        )
      case _ => Future.failed(InternalServerErrorException("Trust is not in processed state."))
    }.flatMap {beneficiaryJson =>
      transformationService.addNewTransform (utr, internalId,
        removeBeneficiary match {
          case RemoveBeneficiary.Unidentified(endDate, index) => RemoveBeneficiariesTransform.Unidentified(endDate, index, beneficiaryJson)
          case RemoveBeneficiary.Individual(endDate, index)   => RemoveBeneficiariesTransform.Individual(endDate, index, beneficiaryJson)
        }
      ).map(_ => Success)
    }
  }

  def addAmendUnidentifiedBeneficiaryTransformer(utr: String, index: Int, internalId: String, description: String): Future[Unit] = {
    transformationService.addNewTransform(utr, internalId, AmendUnidentifiedBeneficiaryTransform(index, description))
  }


  def addAddUnidentifiedBeneficiaryTransformer(utr: String, internalId: String, newBeneficiary: UnidentifiedType): Future[Unit] = {
    transformationService.addNewTransform(utr, internalId, AddUnidentifiedBeneficiaryTransform(newBeneficiary))
  }


}
