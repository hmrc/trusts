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
import play.api.libs.json.{Json, __}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.exceptions.InternalServerErrorException
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse

import uk.gov.hmrc.trusts.models.variation.UnidentifiedType
import uk.gov.hmrc.trusts.models.{RemoveBeneficiary, Success}
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.{ExecutionContext, Future}

class BeneficiaryTransformationService @Inject()(transformationService: TransformationService)(implicit ec:ExecutionContext) {
  def removeBeneficiary(utr: String, internalId: String, removeBeneficiary: RemoveBeneficiary) : Future[Success.type] = {
    //val beneficiaryData = getBeneficiaryAtIndex(utr, )

    transformationService.addNewTransform (utr, internalId,
      removeBeneficiary match {
        case RemoveBeneficiary.Unidentified(endDate, index) => RemoveBeneficiariesTransform.Unidentified(endDate, index, Json.obj())
        case RemoveBeneficiary.Individual(endDate, index)   => RemoveBeneficiariesTransform.Individual(endDate, index, Json.obj())
      }
    ).map(_ => Success)
  }

  def addAmendUnidentifiedBeneficiaryTransformer(utr: String, index: Int, internalId: String, description: String): Future[Unit] = {
    transformationService.addNewTransform(utr, internalId, AmendUnidentifiedBeneficiaryTransform(index, description))
  }

//  private def getBeneficiaryAtIndex(utr: String, internalId: String, index: Int)(implicit hc: HeaderCarrier) = {
//    transformationService.getTransformedData(utr, internalId).map {
//      case TrustProcessedResponse(transformedJson, _) =>
//        val beneficiaryPath = (__ \ 'details \ 'trust \ 'entities \ 'beneficiary \ index).json
//        transformedJson.transform(beneficiaryPath.pick)
//      case _ => Future.failed(InternalServerErrorException("Trust is not in processed state."))
//    }
//  }

  def addAddUnidentifiedBeneficiaryTransformer(utr: String, internalId: String, newBeneficiary: UnidentifiedType): Future[Unit] = {
    transformationService.addNewTransform(utr, internalId, AddUnidentifiedBeneficiaryTransform(newBeneficiary))
  }


}
