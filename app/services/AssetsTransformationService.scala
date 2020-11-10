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
import models.get_trust.TrustProcessedResponse
import models.variation._
import play.api.libs.json.{JsObject, JsValue, Json, __}
import transformers._
import transformers.remove.{RemoveAsset, RemoveBeneficiary}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class AssetsTransformationService @Inject()(
                                                  transformationService: TransformationService,
                                                  localDateService: LocalDateService
                                                )
                                           (implicit ec:ExecutionContext)
  extends JsonOperations {

  def removeAsset(utr: String, internalId: String, removeAsset: RemoveAsset)(implicit hc: HeaderCarrier): Future[Success.type] = {

    getTransformedTrustJson(utr, internalId)
      .map(findAssetJson(_, removeAsset.`type`, removeAsset.index))
      .flatMap(Future.fromTry)
      .flatMap {assetJson =>
        transformationService.addNewTransform (utr, internalId,
            RemoveNonEeaBusinessAssetTransform(
              removeAsset.index,
              assetJson,
              removeAsset.endDate,
              removeAsset.`type`
            )
        ).map(_ => Success)
      }
  }

  private def getTransformedTrustJson(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[JsObject] = {

    transformationService.getTransformedData(utr, internalId).flatMap {
      case TrustProcessedResponse(json, _) => Future.successful(json.as[JsObject])
      case _ => Future.failed(InternalServerErrorException("Trust is not in processed state."))
    }
  }

  private def findAssetJson(json: JsValue, nonEEABusinessType: String, index: Int): Try[JsObject] = {
    val nonEEABusinessAssetPath = (__ \ 'details \ 'trust \ 'Assets \ nonEEABusinessType \ index).json
    json.transform(nonEEABusinessAssetPath.pick).fold(
      _ => Failure(InternalServerErrorException("Could not locate nonEeaBusinessAsset at index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

  def amendNonEeaBusinessAssetTransformer(utr: String, index: Int, internalId: String, description: String)(implicit hc: HeaderCarrier): Future[Success.type] = {
    getTransformedTrustJson(utr, internalId)
    .map(findAssetJson(_, "nonEEABusinessType", index))
      .flatMap(Future.fromTry)
      .flatMap { assetJson =>

      transformationService.addNewTransform(utr, internalId, AmendNonEeaBusinessAssetTransform(index, description, assetJson, localDateService.now))
        .map(_ => Success)
      }
  }

  def addNonEeaBusinessAssetTransformer(utr: String, internalId: String, newNonEEABusinessAsset: NonEEABusinessType): Future[Boolean] = {
    transformationService.addNewTransform(utr, internalId, AddNonEeaBusinessAssetTransform(newNonEEABusinessAsset))
  }
 }
