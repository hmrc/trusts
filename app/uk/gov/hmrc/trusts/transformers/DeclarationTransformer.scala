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

import org.joda.time.DateTime
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.{DeclarationForApi, NameType}
import uk.gov.hmrc.trusts.utils.Implicits._

class DeclarationTransformer {

  def transform(response: TrustProcessedResponse,
                originalJson: JsValue,
                declaration: DeclarationForApi,
                date: DateTime): JsResult[JsValue] = {
    val responseJson = response.getTrust
    val responseHeader = response.responseHeader
    val agentTransformer = if (declaration.agentDetails.isDefined) {
      (__).json.update(
        (__ \ 'agentDetails).json.put(Json.toJson(declaration.agentDetails.get))
      )
    } else {
      (__).json.pick[JsObject]
    }
    responseJson.transform(
      (__ \ 'applicationType).json.prune andThen
        (__ \ 'declaration).json.prune andThen
        (__ \ 'yearsReturns).json.prune andThen
        removeEmptyLineNo(responseJson) andThen
        convertLeadTrustee(responseJson) andThen
        addPreviousLeadTrustee(responseJson, originalJson, date) andThen
        putNewValue(__ \ 'reqHeader \ 'formBundleNo, JsString(responseHeader.formBundleNo)) andThen
        putNewValue(__ \ 'declaration, Json.toJson(declaration.declaration)) andThen
        agentTransformer
    )
  }

  private val pathToLeadTrustees: JsPath = __ \ 'details \ 'trust \ 'entities \ 'leadTrustees
  private val pickLeadTrustee = pathToLeadTrustees.json.pick

  private def trusteeField(json: JsValue): String = determineTrusteeField(pathToLeadTrustees, json)

  private def determineTrusteeField(rootPath: JsPath, json: JsValue): String = {
    val namePath = (rootPath \ 'name).json.pick[JsObject]

    json.transform(namePath).flatMap(_.validate[NameType]) match {
      case JsSuccess(_, _) => "leadTrusteeInd"
      case _ => "leadTrusteeOrg"
    }
  }

  private def removeEmptyLineNo(json: JsValue): Reads[JsObject] = {
    val lineNoPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees \ 'lineNo)
    json.transform(lineNoPath.json.pick[JsString]) match {
      case JsSuccess(JsString(""), _) => lineNoPath.json.prune
      case _ => (__).json.pick[JsObject]
    }
  }

  private def addPreviousLeadTrusteeAsExpiredStep(previousLeadTrusteeJson: JsValue, date: DateTime): Reads[JsObject] = {
    val trusteeField = determineTrusteeField(__, previousLeadTrusteeJson)
    previousLeadTrusteeJson.transform(__.json.update(
      (__ \ 'entityEnd).json.put(Json.toJson(date))
    )).fold(
      errors => Reads(_ => JsError(errors)),
      endedJson => {
        pathToLeadTrustees.json.update(of[JsArray]
          .map { a => a :+ Json.obj(trusteeField -> endedJson) })
      })
  }

  private def addPreviousLeadTrustee(newJson: JsValue, originalJson: JsValue, date: DateTime): Reads[JsObject] = {
    val newLeadTrustee = newJson.transform(pickLeadTrustee)
    val originalLeadTrustee = originalJson.transform(pickLeadTrustee)

    (newLeadTrustee, originalLeadTrustee) match {
      case (JsSuccess(newLeadTrusteeJson, _), JsSuccess(originalLeadTrusteeJson, _))
        if (newLeadTrusteeJson != originalLeadTrusteeJson) =>
          addPreviousLeadTrusteeAsExpiredStep(originalLeadTrusteeJson, date)
      case _ => (__).json.pick[JsObject]
    }
  }

  private def convertLeadTrustee(json: JsValue): Reads[JsObject] = pathToLeadTrustees.json.update( of[JsObject]
    .map{ a => Json.arr(Json.obj(trusteeField(json) -> a )) })

  private def putNewValue(path: JsPath, value: JsValue ): Reads[JsObject] =
    (__).json.update(path.json.put(value))
}
