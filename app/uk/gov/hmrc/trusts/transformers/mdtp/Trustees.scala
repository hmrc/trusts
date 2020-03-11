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

package uk.gov.hmrc.trusts.transformers.mdtp

import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeOrgType, DisplayTrustTrusteeType}

object Trustees {

  private val pathToTrustees = JsPath \ 'details \ 'trust \ 'entities \ 'trustees

  def transform(response : JsValue) : Reads[JsObject] = {
    response.transform(pathToTrustees.json.pick).fold(
      _ => {
        JsPath.json.update(
          pathToTrustees.json.put(JsArray())
        )
      },
      trustees => {

        val trusteesUpdated = JsArray(trustees.as[List[DisplayTrustTrusteeType]].map {
          case DisplayTrustTrusteeType(Some(trusteeInd), None) =>
            Json.obj(
              "trusteeInd" -> Json.toJson(trusteeInd)(DisplayTrustTrusteeIndividualType.writeToMaintain)
            )
          case DisplayTrustTrusteeType(None, Some(trusteeOrg)) =>
            Json.obj(
              "trusteeOrg" -> Json.toJson(trusteeOrg)(DisplayTrustTrusteeOrgType.writeToMaintain)
            )
        })

        JsPath.json.update(
          pathToTrustees.json.prune andThen
            pathToTrustees.json.put(trusteesUpdated)
        )
      }
    )
  }

}
