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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustLeadTrusteeType

trait DeltaTransform {
  def applyTransform(input: JsValue): JsValue
}

object DeltaTransform {
  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value => {
      JsSuccess(value.as[SetLeadTrusteeIndTransform])
    }
  )
  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] {
    case transform: SetLeadTrusteeIndTransform => Json.toJson(transform)(SetLeadTrusteeIndTransform.format)
    case _ => throw new Exception("something went wrong")
  }
}

case class ComposedDeltaTransform(deltaTransforms: Seq[DeltaTransform]) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsValue = {
    deltaTransforms.foldLeft(input)((cur, xform) => xform.applyTransform(cur))
  }

  def :+(transform: DeltaTransform) = ComposedDeltaTransform(deltaTransforms :+ transform)
}

object ComposedDeltaTransform {
  implicit val format: Format[ComposedDeltaTransform] = Json.format[ComposedDeltaTransform]
}
