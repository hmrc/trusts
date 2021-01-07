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
import models.Success
import models.variation._
import play.api.libs.json.{JsObject, JsValue, Json, Writes, __}
import transformers._
import transformers.assets._
import transformers.remove.RemoveAsset
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class AssetsTransformationService @Inject()(transformationService: TransformationService,
                                            localDateService: LocalDateService)
                                           (implicit ec:ExecutionContext) extends JsonOperations {

  def addAsset[T <: AssetType](identifier: String, internalId: String, asset: T)
                              (implicit wts: Writes[T]): Future[Boolean] = {

    transformationService.addNewTransform(
      identifier = identifier,
      internalId = internalId,
      newTransform = AddAssetTransform(
        asset = Json.toJson(asset),
        assetType = asset.toString
      )
    )
  }

  def amendAsset[T <: AssetType](identifier: String, index: Int, internalId: String, asset: T)
                                (implicit hc: HeaderCarrier, wts: Writes[T]): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findAssetJson(_, asset.toString, index))
      .flatMap(Future.fromTry)
      .flatMap { assetJson =>
        transformationService.addNewTransform(
          identifier = identifier,
          internalId = internalId,
          newTransform = AmendAssetTransform(
            index = index,
            amended = Json.toJson(asset),
            original = assetJson,
            endDate = localDateService.now,
            assetType = asset.toString
          )
        ).map(_ => Success)
      }
  }

  def removeAsset(identifier: String, internalId: String, asset: RemoveAsset)
                 (implicit hc: HeaderCarrier): Future[Success.type] = {

    transformationService.getTransformedTrustJson(identifier, internalId)
      .map(findAssetJson(_, asset.`type`, asset.index))
      .flatMap(Future.fromTry)
      .flatMap { assetJson =>
        transformationService.addNewTransform(
          identifier = identifier,
          internalId = internalId,
          newTransform = RemoveAssetTransform(
            index = asset.index,
            asset = assetJson,
            endDate = asset.endDate,
            assetType = asset.`type`
          )
        ).map(_ => Success)
      }
  }

  private def findAssetJson(json: JsValue, assetType: String, index: Int): Try[JsObject] = {
    val assetPath = (__ \ 'details \ 'trust \ 'assets \ assetType \ index).json
    json.transform(assetPath.pick).fold(
      _ => Failure(InternalServerErrorException(s"Could not locate asset of type $assetType at index $index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }
}
