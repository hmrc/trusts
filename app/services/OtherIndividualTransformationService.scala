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

package services

import exceptions.InternalServerErrorException
import javax.inject.Inject
import models.Success
import models.variation.NaturalPersonType
import play.api.libs.json.{JsObject, JsValue, Json, __}
import transformers._
import transformers.remove.RemoveOtherIndividual
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class OtherIndividualTransformationService @Inject()(transformationService: TransformationService,
                                                     localDateService: LocalDateService
                                                    )(implicit ec:ExecutionContext) extends JsonOperations {

  def removeOtherIndividual(identifier: String, internalId: String, removeOtherIndividual: RemoveOtherIndividual)
                           (implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findOtherIndividualJson(_, removeOtherIndividual.index))
      .flatMap(Future.fromTry)
      .flatMap { otherIndividualJson =>
        transformationService.addNewTransform (identifier, internalId,
          RemoveOtherIndividualsTransform(
            removeOtherIndividual.index,
            otherIndividualJson,
            removeOtherIndividual.endDate
          )
        ).map(_ => Success)
      }
  }

  def amendOtherIndividualTransformer(identifier: String,
                                      index: Int,
                                      internalId: String,
                                      amended: NaturalPersonType)(implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findOtherIndividualJson(_, index))
      .flatMap(Future.fromTry)
      .flatMap { original =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendOtherIndividualTransform(
            index,
            Json.toJson(amended),
            original,
            localDateService.now
          )
        ).map(_ => Success)
      }
  }

  private def findOtherIndividualJson(json: JsValue, index: Int): Try[JsObject] = {
    val path = (__ \ 'details \ 'trust \ 'entities \ 'naturalPerson \ index).json
    json.transform(path.pick).fold(
      _ => Failure(InternalServerErrorException("Could not locate otherIndividual at index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

  def addOtherIndividualTransformer(identifier: String, internalId: String, newOtherIndividual: NaturalPersonType): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddOtherIndividualTransform(newOtherIndividual))
  }

}
