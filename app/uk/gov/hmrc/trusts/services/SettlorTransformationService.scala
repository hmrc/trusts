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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustSettlor, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.variation._
import uk.gov.hmrc.trusts.models.{RemoveSettlor, Success}
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class SettlorTransformationService @Inject()(
                                                  transformationService: TransformationService,
                                                  localDateService: LocalDateService
                                                )
                                            (implicit ec:ExecutionContext)
  extends JsonOperations {

  def removeSettlor(utr: String, internalId: String, removeSettlor: RemoveSettlor)
                       (implicit hc: HeaderCarrier) : Future[Success.type] = {

    getTransformedTrustJson(utr, internalId)
      .map(findSettlorJson(_, removeSettlor.`type`, removeSettlor.index))
      .flatMap(Future.fromTry)
      .flatMap { settlorJson =>
        transformationService.addNewTransform (utr, internalId,
          RemoveSettlorsTransform(
            removeSettlor.index,
            settlorJson,
            removeSettlor.endDate,
            removeSettlor.`type`
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

  private def findSettlorJson(json: JsValue, settlorType: String, index: Int): Try[JsObject] = {
    val path = (__ \ 'details \ 'trust \ 'entities \ 'settlors \ settlorType \ index).json
    json.transform(path.pick).fold(
      _ => Failure(InternalServerErrorException("Could not locate settlor at index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

  private def getDeceasedJson(json: JsValue): Try[JsObject] = {
    val path = (__ \ 'details \ 'trust \ 'entities \ 'deceased).json
    json.transform(path.pick).fold(
      _ => Failure(InternalServerErrorException("Could not locate settlor at index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

  def amendIndividualSettlorTransformer(utr: String,
                                        index: Int,
                                        internalId: String,
                                        settlor: Settlor)
                                       (implicit hc: HeaderCarrier): Future[Success.type] =
  {
    getTransformedTrustJson(utr, internalId)
    .map(findSettlorJson(_, "settlor", index))
      .flatMap(Future.fromTry)
      .flatMap { original =>
        transformationService.addNewTransform(
          utr,
          internalId,
          AmendIndividualSettlorTransform(index, Json.toJson(settlor), original, localDateService.now)
        ).map(_ => Success)
      }
  }

  def addIndividualSettlorTransformer(utr: String, internalId: String, newSettlor: DisplayTrustSettlor): Future[Success.type] = {
    transformationService.addNewTransform(utr, internalId, AddIndividualSettlorTransform(newSettlor)).map(_ => Success)
  }


  def amendBusinessSettlorTransformer(utr: String,
                                        index: Int,
                                        internalId: String,
                                        settlor: SettlorCompany)
                                       (implicit hc: HeaderCarrier): Future[Success.type] =
  {
    getTransformedTrustJson(utr, internalId)
      .map(findSettlorJson(_, "settlorCompany", index))
      .flatMap(Future.fromTry)
      .flatMap { original =>
        transformationService.addNewTransform(
          utr,
          internalId,
          AmendBusinessSettlorTransform(index, Json.toJson(settlor), original, localDateService.now)
        ).map(_ => Success)
      }
  }

  def amendDeceasedSettlor(utr: String,
                           internalId : String,
                           deceased: AmendDeceasedSettlor)
                          (implicit hc: HeaderCarrier): Future[Success.type] =
    {
      getTransformedTrustJson(utr, internalId)
        .map(getDeceasedJson)
        .flatMap(Future.fromTry)
        .flatMap { original =>
          transformationService.addNewTransform(
            utr,
            internalId,
            AmendDeceasedSettlorTransform(Json.toJson(deceased), original)
          ).map(_ => Success)
        }
    }
}
