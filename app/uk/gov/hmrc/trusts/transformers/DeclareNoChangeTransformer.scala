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

  def transform(response: TrustProcessedResponse, originalJson: JsValue, declaration: Declaration): JsResult[JsValue] = {

    val responseJson = response.getTrust

    val trustFromPath = (__ \ 'applicationType ).json.prune andThen
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

    for {
      converted <- responseJson.transform(convertLeadTrustee(responseJson))
      added <- converted.transform(addPreviousLeadTrustee(responseJson, originalJson))
      result <- added.transform(formBundleNoTransformer)
    } yield result
  }

  private val doNothing = (__).json.pick
  private val pickLeadTrustee = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees).json.pick

  private def getLeadTrustee(json: JsValue): JsResult[JsValue] = {
    json.transform(pickLeadTrustee)
  }

  private def trusteeField(json: JsValue): String = determineTrusteeField(__ \ 'details \ 'trust \ 'entities \ 'leadTrustees, json)

  private def determineTrusteeField(rootPath: JsPath, json: JsValue): String = {
    val namePath = (rootPath \ 'name).json.pick[JsObject]

    json.transform(namePath) match {
      case JsSuccess(_, _) => "leadTrusteeInd"
      case _ => "leadTrusteeOrg"
    }
  }

  private def addPreviousLeadTrusteeAsExpiredStep(oldJson: JsValue) = {
    val trusteeField = determineTrusteeField(__, oldJson)
    (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees).json.update( of[JsArray]
      .map{ a => a:+ Json.obj(trusteeField -> oldJson ) })
  }

  private def addPreviousLeadTrustee(newJson: JsValue, originalJson: JsValue) = {
    val newLeadTrustee: JsResult[JsValue] = getLeadTrustee(newJson)
    val originalLeadTrustee: JsResult[JsValue] = getLeadTrustee(originalJson)
    (newLeadTrustee, originalLeadTrustee) match {
      case (JsSuccess(newLeadTrusteeJson, _), JsSuccess(originalLeadTrusteeJson, _))
        if (newLeadTrusteeJson != originalLeadTrusteeJson) =>
          addPreviousLeadTrusteeAsExpiredStep(originalLeadTrusteeJson)
      case _ => doNothing
    }
  }

  private def convertLeadTrustee(json: JsValue): Reads[JsObject] = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees).json.update( of[JsObject]
    .map{ a => Json.arr(Json.obj(trusteeField(json) -> a )) })
}
