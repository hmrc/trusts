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

import play.api.libs.json.{JsValue, _}

trait DeltaTransform {
  def applyTransform(input: JsValue): JsValue
}

object DeltaTransform {
  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value => {
      value.as[JsObject] match {
        case json if json.keys.contains("SetLeadTrusteeIndTransform") => (json \ "SetLeadTrusteeIndTransform").validate[SetLeadTrusteeIndTransform]
        case json if json.keys.contains("SetLeadTrusteeOrgTransform") => (json \ "SetLeadTrusteeOrgTransform").validate[SetLeadTrusteeOrgTransform]
        case json if json.keys.contains("AddTrusteeTransformer")      => (json \ "AddTrusteeTransformer").validate[AddTrusteeTransformer]
        case _ => throw new Exception(s"Don't know how to deserialise transform: $value")
      }
    }
  )

  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { deltaTransform =>
    val transformWrapper = deltaTransform match {
      case transform: SetLeadTrusteeIndTransform  => Json.obj("SetLeadTrusteeIndTransform" -> Json.toJson(transform)(SetLeadTrusteeIndTransform.format))
      case transform: SetLeadTrusteeOrgTransform  => Json.obj("SetLeadTrusteeOrgTransform" -> Json.toJson(transform)(SetLeadTrusteeOrgTransform.format))
      case transform: AddTrusteeTransformer       => Json.obj("AddTrusteeTransformer"-> Json.toJson(transform)(AddTrusteeTransformer.format))
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
