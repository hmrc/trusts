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

trait DeltaTransform {
  def applyTransform(input: JsValue): JsValue
}

case class SerialisedDeltaTransformWrapper(serialisedType: String, json: JsValue)

object SerialisedDeltaTransformWrapper {
  implicit val format: Format[SerialisedDeltaTransformWrapper] = Json.format[SerialisedDeltaTransformWrapper]
}

object DeltaTransform {
  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value => {
      value.as[SerialisedDeltaTransformWrapper] match {
        case SerialisedDeltaTransformWrapper("AddTrusteeTransformer", json) => JsSuccess(json.as[AddTrusteeTransformer])
        case SerialisedDeltaTransformWrapper("SetLeadTrusteeIndTransform", json) => JsSuccess(json.as[SetLeadTrusteeIndTransform])
        case SerialisedDeltaTransformWrapper("SetLeadTrusteeOrgTransform", json) => JsSuccess(json.as[SetLeadTrusteeOrgTransform])
        case _ => throw new Exception(s"Don't know how to deserialise transform: $value")
      }
    }
  )
  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { t =>
    val transformWrapper = t match {
      case transform: SetLeadTrusteeIndTransform => SerialisedDeltaTransformWrapper("SetLeadTrusteeIndTransform", Json.toJson(transform)(SetLeadTrusteeIndTransform.format))
      case transform: SetLeadTrusteeOrgTransform => SerialisedDeltaTransformWrapper("SetLeadTrusteeOrgTransform", Json.toJson(transform)(SetLeadTrusteeOrgTransform.format))
      case transform: AddTrusteeTransformer => SerialisedDeltaTransformWrapper("AddTrusteeTransformer", Json.toJson(transform)(AddTrusteeTransformer.format))
      case transform => throw new Exception(s"Don't know how to serialise transform: $transform")
    }
    Json.toJson(transformWrapper)
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
