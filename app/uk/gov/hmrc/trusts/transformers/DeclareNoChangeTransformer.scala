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

package uk.gov.hmrc.trusts.transformers

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import uk.gov.hmrc.trusts.models.Declaration

class DeclareNoChangeTransformer {
  def transform(beforeJson: JsValue, declaration: Declaration): JsResult[JsValue] = {

    val trusteeField: String = {
      val namePath = (__ \ 'trustOrEstateDisplay \ 'details \ 'trust \ 'entities \ 'leadTrustees \ 'name).json.pick[JsObject]

      beforeJson.transform(namePath) match {
        case JsSuccess(_, _) => "leadTrusteeInd"
        case _ => "leadTrusteeOrg"
      }
    }

    val trustFromPath = (__ \ 'trustOrEstateDisplay \ 'applicationType ).json.prune andThen
      (__ \ 'trustOrEstateDisplay \ 'details \ 'trust \ 'entities \ 'leadTrustees).json.update( of[JsObject]
        .map{ a => Json.arr(Json.obj(trusteeField -> a )) }) andThen
      (__ \ 'trustOrEstateDisplay \ 'declaration).json.prune andThen
      (__ \ 'trustOrEstateDisplay ).json.pick

    val trustToPath = (__ ).json

    val headerFromPath = (__ \ 'responseHeader \ 'formBundleNo ).json.pick
    val headerToPath = (__ \ 'reqHeader \ 'formBundleNo ).json

    val declarationInsert = (__ \ 'declaration).json.put(Json.toJson(declaration))

    val formBundleNoTransformer: Reads[JsObject] = {
      trustToPath.copyFrom(trustFromPath) and
        headerToPath.copyFrom(headerFromPath) and
        declarationInsert
      }.reduce

    beforeJson.transform(formBundleNoTransformer)
  }
}
