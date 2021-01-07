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
import transformers.remove.RemoveSettlor
import transformers.settlors.{AddBusinessSettlorTransform, AddIndividualSettlorTransform, AmendBusinessSettlorTransform, AmendDeceasedSettlorTransform, AmendIndividualSettlorTransform, RemoveSettlorsTransform}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class SettlorTransformationService @Inject()(transformationService: TransformationService,
                                             localDateService: LocalDateService
                                            )(implicit ec:ExecutionContext) extends JsonOperations {

  def removeSettlor(identifier: String, internalId: String, removeSettlor: RemoveSettlor)
                   (implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findSettlorJson(_, removeSettlor.`type`, removeSettlor.index))
      .flatMap(Future.fromTry)
      .flatMap { settlorJson =>
        transformationService.addNewTransform (identifier, internalId,
          RemoveSettlorsTransform(
            removeSettlor.index,
            settlorJson,
            removeSettlor.endDate,
            removeSettlor.`type`
          )
        ).map(_ => Success)
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

  def amendIndividualSettlorTransformer(identifier: String,
                                        index: Int,
                                        internalId: String,
                                        settlor: Settlor)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
    .map(findSettlorJson(_, "settlor", index))
      .flatMap(Future.fromTry)
      .flatMap { original =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendIndividualSettlorTransform(
            index,
            Json.toJson(settlor),
            original,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }

  def addIndividualSettlorTransformer(identifier: String, internalId: String, newSettlor: Settlor): Future[Success.type] = {
    transformationService.addNewTransform(identifier, internalId, AddIndividualSettlorTransform(newSettlor)).map(_ => Success)
  }

  def addBusinessSettlorTransformer(identifier: String, internalId: String, newCompanySettlor: SettlorCompany): Future[Success.type] = {
    transformationService.addNewTransform(identifier, internalId, AddBusinessSettlorTransform(newCompanySettlor)).map(_ => Success)
  }

  def amendBusinessSettlorTransformer(identifier: String,
                                      index: Int,
                                      internalId: String,
                                      settlor: SettlorCompany)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findSettlorJson(_, "settlorCompany", index))
      .flatMap(Future.fromTry)
      .flatMap { original =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendBusinessSettlorTransform(
            index,
            Json.toJson(settlor),
            original,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }

  def amendDeceasedSettlor(identifier: String,
                           internalId : String,
                           deceased: AmendDeceasedSettlor)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(getDeceasedJson)
      .flatMap(Future.fromTry)
      .flatMap { original =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendDeceasedSettlorTransform(
            Json.toJson(deceased),
            original
          )
        ).map(_ => Success)
      }
  }
}
