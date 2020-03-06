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

import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustTrusteeIndividualType

trait AddTrusteeCommon {

  sealed abstract class TrusteeType(trustee: String) { override val toString: String = trustee }
  final case object TrusteeInd extends TrusteeType("trusteeInd")
  final case object TrusteeOrg extends TrusteeType("trusteeOrg")

  def addTrustee(input: JsValue, newTrustee: JsValue, trusteeType: TrusteeType): JsResult[JsValue] = {

    val path = (__ \ 'details \ 'trust \ 'entities \ 'trustees).json

    input.transform(path.pick[JsArray]) match {
      case JsSuccess(value, _) =>
        if (value.value.size < 25) {
          val trustees: Reads[JsObject] =
            path.update( of[JsArray]
              .map {
                trustees => trustees :+ Json.obj(trusteeType.toString -> newTrustee)
              }
            )
          input.transform(trustees)
        }
        else {
          throw new Exception("Adding a trustee would exceed the maximum allowed amount of 25")
        }
      case JsError(_) =>
        input.transform(__.json.update {
          path.put(JsArray(
            Seq(Json.obj(trusteeType.toString -> newTrustee)))
          )
        })

    }
  }
}
