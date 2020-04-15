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
import play.api.libs.json.{JsObject, JsValue, Json, __}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.exceptions.InternalServerErrorException
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.variation.{BeneficiaryCharityType, BeneficiaryCompanyType, BeneficiaryTrustType, IndividualDetailsType, OtherType, UnidentifiedType}
import uk.gov.hmrc.trusts.models.{RemoveBeneficiary, Success}
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class BeneficiaryTransformationService @Inject()(
                                                  transformationService: TransformationService,
                                                  localDateService: LocalDateService
                                                )
                                                (implicit ec:ExecutionContext)
  extends JsonOperations {

  def removeBeneficiary(utr: String, internalId: String, removeBeneficiary: RemoveBeneficiary)
                       (implicit hc: HeaderCarrier) : Future[Success.type] = {

    getTransformedTrustJson(utr, internalId)
      .map(findBeneficiaryJson(_, removeBeneficiary.`type`, removeBeneficiary.index))
      .flatMap(Future.fromTry)
      .flatMap {beneficiaryJson =>
        transformationService.addNewTransform (utr, internalId,
            RemoveBeneficiariesTransform(
              removeBeneficiary.index,
              beneficiaryJson,
              removeBeneficiary.endDate,
              removeBeneficiary.`type`
            )
        ).map(_ => Success)
      }
  }

  private def getTransformedTrustJson(utr: String, internalId: String)
                                     (implicit hc:HeaderCarrier) = {

    transformationService.getTransformedData(utr, internalId).flatMap {
      case TrustProcessedResponse(json, _) => Future.successful(json.as[JsObject])
      case _ => Future.failed(InternalServerErrorException("Trust is not in processed state."))
    }
  }

  private def findBeneficiaryJson(json: JsValue, beneficiaryType: String, index: Int): Try[JsObject] = {
    val beneficiaryPath = (__ \ 'details \ 'trust \ 'entities \ 'beneficiary \ beneficiaryType \ index).json
    json.transform(beneficiaryPath.pick).fold(
      _ => Failure(InternalServerErrorException("Could not locate beneficiary at index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

  def amendUnidentifiedBeneficiaryTransformer(utr: String, index: Int, internalId: String, description: String)
                                             (implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(utr, internalId)
    .map(findBeneficiaryJson(_, "unidentified", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>

      transformationService.addNewTransform(utr, internalId, AmendUnidentifiedBeneficiaryTransform(index, description, beneficiaryJson, localDateService.now))
        .map(_ => Success)
      }
  }

  def addUnidentifiedBeneficiaryTransformer(utr: String, internalId: String, newBeneficiary: UnidentifiedType): Future[Boolean] = {
    transformationService.addNewTransform(utr, internalId, AddUnidentifiedBeneficiaryTransform(newBeneficiary))
  }

  def amendIndividualBeneficiaryTransformer(utr: String,
                                            index: Int,
                                            internalId: String,
                                            amend: IndividualDetailsType)
                                           (implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(utr, internalId)
      .map(findBeneficiaryJson(_, "individualDetails", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>

        transformationService.addNewTransform(
          utr,
          internalId,
          AmendIndividualBeneficiaryTransform(index, Json.toJson(amend), beneficiaryJson, localDateService.now)
        ).map(_ => Success)
      }
  }

  def addIndividualBeneficiaryTransformer(utr: String, internalId: String, newBeneficiary: IndividualDetailsType): Future[Boolean] = {
    transformationService.addNewTransform(utr, internalId, AddIndividualBeneficiaryTransform(newBeneficiary))
  }

  def addCharityBeneficiaryTransformer(utr: String, internalId: String, newBeneficiary: BeneficiaryCharityType): Future[Boolean] = {
    transformationService.addNewTransform(utr, internalId, AddCharityBeneficiaryTransform(newBeneficiary))
  }

  def amendCharityBeneficiaryTransformer(utr: String,
                                         index: Int,
                                         internalId: String,
                                         amended: BeneficiaryCharityType)
                                           (implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(utr, internalId)
      .map(findBeneficiaryJson(_, "charity", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>

        transformationService.addNewTransform(
          utr,
          internalId,
          AmendCharityBeneficiaryTransform(index, Json.toJson(amended), beneficiaryJson, localDateService.now)
        ).map(_ => Success)
      }
  }

  def addOtherBeneficiaryTransformer(utr: String, internalId: String, newBeneficiary: OtherType): Future[Boolean] = {
    transformationService.addNewTransform(utr, internalId, AddOtherBeneficiaryTransform(newBeneficiary))
  }

  def amendOtherBeneficiaryTransformer(utr: String,
                                         index: Int,
                                         internalId: String,
                                         amended: OtherType)
                                        (implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(utr, internalId)
      .map(findBeneficiaryJson(_, "other", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>

        transformationService.addNewTransform(
          utr,
          internalId,
          AmendOtherBeneficiaryTransform(index, Json.toJson(amended), beneficiaryJson, localDateService.now)
        ).map(_ => Success)
      }
  }

  def addCompanyBeneficiaryTransformer(utr: String, internalId: String, newBeneficiary: BeneficiaryCompanyType): Future[Boolean] = {
    transformationService.addNewTransform(utr, internalId, AddCompanyBeneficiaryTransform(newBeneficiary))
  }

  def amendCompanyBeneficiaryTransformer(utr: String,
                                       index: Int,
                                       internalId: String,
                                       amended: BeneficiaryCompanyType)
                                      (implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(utr, internalId)
      .map(findBeneficiaryJson(_, "company", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>

        transformationService.addNewTransform(
          utr,
          internalId,
          AmendCompanyBeneficiaryTransform(index, Json.toJson(amended), beneficiaryJson, localDateService.now)
        ).map(_ => Success)
      }
  }

  def amendTrustBeneficiaryTransformer(utr: String,
                                         index: Int,
                                         internalId: String,
                                         amended: BeneficiaryTrustType)
                                        (implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(utr, internalId)
      .map(findBeneficiaryJson(_, "trust", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>

        transformationService.addNewTransform(
          utr,
          internalId,
          AmendTrustBeneficiaryTransform(index, Json.toJson(amended), beneficiaryJson, localDateService.now)
        ).map(_ => Success)
      }
  }
}
