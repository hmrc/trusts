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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse

class DeclareNoChangeTransformer {
  def transform(response: TrustProcessedResponse, declaration: Declaration): JsResult[JsValue] = {

    val trusteeField: String = {
      val namePath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees \ 'name).json.pick[JsObject]

      response.getTrust.transform(namePath) match {
        case JsSuccess(_, _) => "leadTrusteeInd"
        case _ => "leadTrusteeOrg"
      }
    }

    val trustFromPath = (__ \ 'applicationType ).json.prune andThen
      (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees).json.update( of[JsObject]
        .map{ a => Json.arr(Json.obj(trusteeField -> a )) }) andThen
      (__ \ 'declaration).json.prune andThen
      (__ \ 'yearsReturns).json.prune andThen
      (__).json.pick

    val trustToPath = (__).json

    val setReqFormBundleNo = (__ \ 'reqHeader \ 'formBundleNo ).json.put(JsString(response.responseHeader.formBundleNo))

    val insertDeclaration = (__ \ 'declaration).json.put(Json.toJson(declaration))

    val formBundleNoTransformer: Reads[JsObject] = {
      trustToPath.copyFrom(trustFromPath) and
        setReqFormBundleNo and
        insertDeclaration
      }.reduce

    response.getTrust.transform(formBundleNoTransformer)
  }
}
