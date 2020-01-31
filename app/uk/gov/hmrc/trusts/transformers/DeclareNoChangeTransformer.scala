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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.Declaration
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse

class DeclareNoChangeTransformer {

  def transform(response: TrustProcessedResponse, originalJson: JsValue, declaration: Declaration): JsResult[JsValue] = {
    val responseJson = response.getTrust
    val responseHeader = response.responseHeader

    responseJson.transform(
      (__ \ 'applicationType).json.prune andThen
        (__ \ 'declaration).json.prune andThen
        (__ \ 'yearsReturns).json.prune andThen
        convertLeadTrustee(responseJson) andThen
        addPreviousLeadTrustee(responseJson, originalJson) andThen
        putNewValue(__ \ 'reqHeader \ 'formBundleNo, JsString(responseHeader.formBundleNo)) andThen
        putNewValue(__ \ 'declaration, Json.toJson(declaration))
    )
  }

  private val allContent = (__).json
  private val pickAllContent = allContent.pick
  private val pathToLeadTrustees: JsPath = __ \ 'details \ 'trust \ 'entities \ 'leadTrustees
  private val pickLeadTrustee = pathToLeadTrustees.json.pick

  private def trusteeField(json: JsValue): String = determineTrusteeField(pathToLeadTrustees, json)

  private def determineTrusteeField(rootPath: JsPath, json: JsValue): String = {
    val namePath = (rootPath \ 'name).json.pick[JsObject]

    json.transform(namePath) match {
      case JsSuccess(_, _) => "leadTrusteeInd"
      case _ => "leadTrusteeOrg"
    }
  }

  private def addPreviousLeadTrusteeAsExpiredStep(oldJson: JsValue) = {
    val trusteeField = determineTrusteeField(__, oldJson)
    pathToLeadTrustees.json.update( of[JsArray]
      .map{ a => a:+ Json.obj(trusteeField -> oldJson ) })
  }

  private def addPreviousLeadTrustee(newJson: JsValue, originalJson: JsValue) = {
    val newLeadTrustee = newJson.transform(pickLeadTrustee)
    val originalLeadTrustee = originalJson.transform(pickLeadTrustee)

    (newLeadTrustee, originalLeadTrustee) match {
      case (JsSuccess(newLeadTrusteeJson, _), JsSuccess(originalLeadTrusteeJson, _))
        if (newLeadTrusteeJson != originalLeadTrusteeJson) =>
          addPreviousLeadTrusteeAsExpiredStep(originalLeadTrusteeJson)
      case _ => pickAllContent
    }
  }

  private def convertLeadTrustee(json: JsValue): Reads[JsObject] = pathToLeadTrustees.json.update( of[JsObject]
    .map{ a => Json.arr(Json.obj(trusteeField(json) -> a )) })

  private def putNewValue(path: JsPath, value: JsValue ): Reads[JsObject] =
    { allContent.copyFrom(pickAllContent) and path.json.put(value) }.reduce
}
