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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustLeadTrusteeIndType

case class SetLeadTrusteeIndTransform( leadTrustee: DisplayTrustLeadTrusteeIndType) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    setLeadTrustee(input, leadTrustee)
  }

  private def setLeadTrustee[A](input: JsValue, lead: A)(implicit writes: Writes[A]) = {
    val leadTrusteesPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees)

    input.transform(
        leadTrusteesPath.json.prune andThen
        (__).json.update(leadTrusteesPath.json.put(Json.toJson(lead))) andThen
        (leadTrusteesPath \ 'lineNo).json.prune andThen
        (leadTrusteesPath \ 'bpMatchStatus).json.prune andThen
        (leadTrusteesPath \ 'entityStart).json.prune
    )
  }
}

object SetLeadTrusteeIndTransform {
  implicit val format: Format[SetLeadTrusteeIndTransform] = Json.format[SetLeadTrusteeIndTransform]
}
