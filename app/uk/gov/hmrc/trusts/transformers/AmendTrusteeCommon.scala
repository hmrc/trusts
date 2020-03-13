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
import uk.gov.hmrc.trusts.models.TrusteeIndOrOrg

trait AmendTrusteeCommon {

  def transform(index: Int, input: JsValue, newTrusteeDetails: JsValue, trusteeType: TrusteeIndOrOrg): JsResult[JsValue] = {
    val trusteePath = __ \ 'details \ 'trust \ 'entities \ 'trustees

    input.transform(trusteePath.json.pick) match {

      case JsSuccess(json, _) =>

        val array = json.as[JsArray]

        val updated = (array.value.take(index) :+ Json.obj(trusteeType.toString -> newTrusteeDetails)) ++ array.value.drop(index + 1)

        input.transform(
          trusteePath.json.prune andThen
            JsPath.json.update {
              trusteePath.json.put(Json.toJson(updated))
            }
        )

      case e: JsError => e
    }
  }
}