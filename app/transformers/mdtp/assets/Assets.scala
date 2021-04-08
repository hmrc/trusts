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

package transformers.mdtp.assets

import models.variation.Asset
import play.api.libs.json._
import transformers.mdtp.Entities

trait Assets[T <: Asset[T]] extends Entities[T]

object Assets {

  def transform(response: JsValue): Reads[JsObject] = {
    AssetMonetaryAmount.transform(response) andThen
    PropertyLandAsset.transform(response) andThen
    SharesAsset.transform(response) andThen
    BusinessAsset.transform(response) andThen
    PartnershipAsset.transform(response) andThen
    OtherAsset.transform(response) andThen
    NonEEABusiness.transform(response)
  }
}
