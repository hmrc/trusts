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

import javax.inject.Inject
import models.Success
import models.variation._
import play.api.libs.json.{JsObject, JsValue, Json, __}
import transformers._
import transformers.beneficiaries.{AddCharityBeneficiaryTransform, AddCompanyBeneficiaryTransform, AddIndividualBeneficiaryTransform, AddLargeBeneficiaryTransform, AddOtherBeneficiaryTransform, AddTrustBeneficiaryTransform, AddUnidentifiedBeneficiaryTransform, AmendCharityBeneficiaryTransform, AmendCompanyBeneficiaryTransform, AmendIndividualBeneficiaryTransform, AmendLargeBeneficiaryTransform, AmendOtherBeneficiaryTransform, AmendTrustBeneficiaryTransform, AmendUnidentifiedBeneficiaryTransform, RemoveBeneficiariesTransform}
import transformers.remove.RemoveBeneficiary
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class BeneficiaryTransformationService @Inject()(transformationService: TransformationService,
                                                 localDateService: LocalDateService
                                                )(implicit ec:ExecutionContext) extends JsonOperations {

  def removeBeneficiary(identifier: String, internalId: String, removeBeneficiary: RemoveBeneficiary)
                       (implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findBeneficiaryJson(_, removeBeneficiary.`type`, removeBeneficiary.index))
      .flatMap(Future.fromTry)
      .flatMap {beneficiaryJson =>
        transformationService.addNewTransform (identifier, internalId,
            RemoveBeneficiariesTransform(
              removeBeneficiary.index,
              beneficiaryJson,
              removeBeneficiary.endDate,
              removeBeneficiary.`type`
            )
        ).map(_ => Success)
      }
  }

  private def findBeneficiaryJson(json: JsValue, beneficiaryType: String, index: Int): Try[JsObject] = {
    val beneficiaryPath = (__ \ 'details \ 'trust \ 'entities \ 'beneficiary \ beneficiaryType \ index).json
    json.transform(beneficiaryPath.pick).fold(
      _ => Failure(InternalServerErrorException("Could not locate beneficiary at index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

  def amendUnidentifiedBeneficiaryTransformer(identifier: String, index: Int, internalId: String, description: String)
                                             (implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findBeneficiaryJson(_, "unidentified", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendUnidentifiedBeneficiaryTransform(
            index,
            description,
            beneficiaryJson,
            localDateService.now)
        ).map(_ => Success)
      }
  }

  def addUnidentifiedBeneficiaryTransformer(identifier: String, internalId: String, newBeneficiary: UnidentifiedType): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddUnidentifiedBeneficiaryTransform(newBeneficiary))
  }

  def amendIndividualBeneficiaryTransformer(identifier: String,
                                            index: Int,
                                            internalId: String,
                                            amend: IndividualDetailsType)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findBeneficiaryJson(_, "individualDetails", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendIndividualBeneficiaryTransform(
            index,
            Json.toJson(amend),
            beneficiaryJson,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }

  def addIndividualBeneficiaryTransformer(identifier: String, internalId: String, newBeneficiary: IndividualDetailsType): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddIndividualBeneficiaryTransform(newBeneficiary))
  }

  def addCharityBeneficiaryTransformer(identifier: String, internalId: String, newBeneficiary: BeneficiaryCharityType): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddCharityBeneficiaryTransform(newBeneficiary))
  }

  def amendCharityBeneficiaryTransformer(identifier: String,
                                         index: Int,
                                         internalId: String,
                                         amended: BeneficiaryCharityType)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findBeneficiaryJson(_, "charity", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendCharityBeneficiaryTransform(
            index,
            Json.toJson(amended),
            beneficiaryJson,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }

  def addOtherBeneficiaryTransformer(identifier: String, internalId: String, newBeneficiary: OtherType): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddOtherBeneficiaryTransform(newBeneficiary))
  }

  def amendOtherBeneficiaryTransformer(identifier: String,
                                       index: Int,
                                       internalId: String,
                                       amended: OtherType)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findBeneficiaryJson(_, "other", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendOtherBeneficiaryTransform(
            index,
            Json.toJson(amended),
            beneficiaryJson,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }

  def addCompanyBeneficiaryTransformer(identifier: String, internalId: String, newBeneficiary: BeneficiaryCompanyType): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddCompanyBeneficiaryTransform(newBeneficiary))
  }

  def addTrustBeneficiaryTransformer(identifier: String, internalId: String, newBeneficiary: BeneficiaryTrustType): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddTrustBeneficiaryTransform(newBeneficiary))
  }

  def amendCompanyBeneficiaryTransformer(identifier: String,
                                         index: Int,
                                         internalId: String,
                                         amended: BeneficiaryCompanyType)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findBeneficiaryJson(_, "company", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendCompanyBeneficiaryTransform(
            index,
            Json.toJson(amended),
            beneficiaryJson,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }

  def amendTrustBeneficiaryTransformer(identifier: String,
                                       index: Int,
                                       internalId: String,
                                       amended: BeneficiaryTrustType)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findBeneficiaryJson(_, "trust", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendTrustBeneficiaryTransform(
            index,
            Json.toJson(amended),
            beneficiaryJson,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }

  def addLargeBeneficiaryTransformer(identifier: String, internalId: String, newBeneficiary: LargeType): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddLargeBeneficiaryTransform(newBeneficiary))
  }

  def amendLargeBeneficiaryTransformer(identifier: String,
                                       index: Int,
                                       internalId: String,
                                       amended: LargeType)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findBeneficiaryJson(_, "large", index))
      .flatMap(Future.fromTry)
      .flatMap { beneficiaryJson =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendLargeBeneficiaryTransform(
            index,
            Json.toJson(amended),
            beneficiaryJson,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }
}
