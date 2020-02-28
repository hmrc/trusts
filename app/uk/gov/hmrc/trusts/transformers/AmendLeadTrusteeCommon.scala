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

import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, __}

trait AmendLeadTrusteeCommon {
  def setLeadTrustee(input: JsValue, newLeadTrusteeDetails: JsValue): JsResult[JsValue] = {
    val leadTrusteesPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees)
    val entityStartPath = leadTrusteesPath \ 'entityStart

    val entityStartPick = entityStartPath.json.pick
    input.transform(entityStartPick) match {
      case JsSuccess(entityStart, _) =>
        input.transform(
          leadTrusteesPath.json.prune andThen
            (__).json.update(leadTrusteesPath.json.put(newLeadTrusteeDetails)) andThen
            (__).json.update(entityStartPath.json.put(entityStart)) andThen
            (leadTrusteesPath \ 'lineNo).json.prune andThen
            (leadTrusteesPath \ 'bpMatchStatus).json.prune
        )
      case e: JsError => e
    }
  }
}
