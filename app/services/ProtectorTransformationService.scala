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

import javax.inject.Inject
import play.api.libs.json.{JsObject, JsValue, Json, __}
import exceptions.InternalServerErrorException
import models.Success
import models.get_trust.TrustProcessedResponse
import models.variation._
import transformers._
import transformers.remove.RemoveProtector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class ProtectorTransformationService @Inject()(transformationService: TransformationService,
                                               localDateService: LocalDateService
                                              )(implicit ec:ExecutionContext) extends JsonOperations {

  def removeProtector(identifier: String, internalId: String, removeProtector: RemoveProtector)(implicit hc: HeaderCarrier): Future[Success.type] = {

    getTransformedTrustJson(identifier, internalId)
      .map(findProtectorJson(_, removeProtector.`type`, removeProtector.index))
      .flatMap(Future.fromTry)
      .flatMap { protectorJson =>
        transformationService.addNewTransform (identifier, internalId,
          RemoveProtectorsTransform(
            removeProtector.index,
            protectorJson,
            removeProtector.endDate,
            removeProtector.`type`
          )
        ).map(_ => Success)
      }
  }

  def addBusinessProtectorTransformer(identifier: String, internalId: String, newProtectorCompany: ProtectorCompany): Future[Boolean] = {
    transformationService.addNewTransform(identifier, internalId, AddCompanyProtectorTransform(newProtectorCompany))
  }

  private def getTransformedTrustJson(identifier: String, internalId: String)(implicit hc: HeaderCarrier): Future[JsObject] = {

    transformationService.getTransformedData(identifier, internalId).flatMap {
      case TrustProcessedResponse(json, _) => Future.successful(json.as[JsObject])
      case _ => Future.failed(InternalServerErrorException("Trust is not in processed state."))
    }
  }

  private def findProtectorJson(json: JsValue, protectorType: String, index: Int): Try[JsObject] = {
    val path = (__ \ 'details \ 'trust \ 'entities \ 'protectors \ protectorType \ index).json
    json.transform(path.pick).fold(
      _ => Failure(InternalServerErrorException("Could not locate protector at index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

  def addIndividualProtectorTransformer(identifier: String, internalId: String, newProtector: Protector): Future[Success.type] = {
    transformationService.addNewTransform(identifier, internalId, AddIndividualProtectorTransform(newProtector)).map(_ => Success)
  }


  def amendIndividualProtectorTransformer(identifier: String,
                                          index: Int,
                                          internalId: String,
                                          amended: Protector)(implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(identifier, internalId)
      .map(findProtectorJson(_, "protector", index))
      .flatMap(Future.fromTry)
      .flatMap { original =>
        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendIndividualProtectorTransform(index, Json.toJson(amended), original, localDateService.now)
        ).map(_ => Success)
      }
  }

  def amendBusinessProtectorTransformer(identifier: String,
                                        index: Int,
                                        internalId: String,
                                        amended: ProtectorCompany)(implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(identifier, internalId)
      .map(findProtectorJson(_, "protectorCompany", index))
      .flatMap(Future.fromTry)
      .flatMap { protectorJson =>

        transformationService.addNewTransform(
          identifier,
          internalId,
          AmendBusinessProtectorTransform(index, Json.toJson(amended), protectorJson, localDateService.now)
        ).map(_ => Success)
      }
  }

}
