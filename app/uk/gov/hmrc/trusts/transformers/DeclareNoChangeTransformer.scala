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

    val matchFromPath = (__ \ 'getTrust \ 'matchData ).json.pick
    val matchToPath = (__ \ 'matchData ).json

    val corrFromPath = (__ \ 'getTrust \ 'correspondence ).json.pick
    val corrToPath = (__ \ 'correspondence ).json

    val leadTrusteeFromPath = (__ \ 'getTrust \ 'trust \ 'entities \ 'leadTrustee).json.update( of[JsObject]
      .map{ leadTrustee => Json.arr( leadTrustee ) }) andThen
      (__ \ 'getTrust \ 'trust \ 'entities \ 'leadTrustee).json.pick
    val leadTrusteeToPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees).json

    val trustFromPath =
      (__ \ 'getTrust \ 'declaration).json.prune andThen
      (__ \ 'getTrust \ 'matchData).json.prune andThen
      (__ \ 'getTrust \ 'correspondence).json.prune andThen
        (__ \ 'getTrust \ 'trust \ 'entities \ 'leadTrustee).json.prune andThen
      (__ \ 'getTrust ).json.pick

    val trustToPath = (__ \ 'details ).json

    val headerFromPath = (__ \ 'responseHeader \ 'formBundleNo ).json.pick
    val headerToPath = (__ \ 'reqHeader \ 'formBundleNo ).json

    val declarationInsert = (__ \ 'declaration ).json.put(Json.toJson(declaration))

    val formBundleNoTransformer: Reads[JsObject] = {
      matchToPath.copyFrom(matchFromPath) and
      corrToPath.copyFrom(corrFromPath) and
      declarationInsert and
      leadTrusteeToPath.copyFrom(leadTrusteeFromPath) and
      trustToPath.copyFrom(trustFromPath) and
      headerToPath.copyFrom(headerFromPath)
    }.reduce

    beforeJson.transform(formBundleNoTransformer)
  }
}
