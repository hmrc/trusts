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
  def applyTransform(input: JsValue): JsResult[JsValue]
}

object DeltaTransform {
  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value => {
      value.as[JsObject] match {
        case json if json.keys.contains("SetLeadTrusteeIndTransform") => (json \ "SetLeadTrusteeIndTransform").validate[AmendLeadTrusteeIndTransform]
        case json if json.keys.contains("SetLeadTrusteeOrgTransform") => (json \ "SetLeadTrusteeOrgTransform").validate[AmendLeadTrusteeOrgTransform]
        case json if json.keys.contains("AddTrusteeIndTransform")      => (json \ "AddTrusteeIndTransform").validate[AddTrusteeIndTransform]
        case json if json.keys.contains("AddTrusteeOrgTransform")      => (json \ "AddTrusteeOrgTransform").validate[AddTrusteeOrgTransform]
        case json if json.keys.contains(RemoveTrusteeTransform.key)      => (json \ RemoveTrusteeTransform.key).validate[RemoveTrusteeTransform]
        case _ => throw new Exception(s"Don't know how to deserialise transform: $value")
      }
    }
  )

  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { deltaTransform =>
    val transformWrapper = deltaTransform match {
      case transform: AmendLeadTrusteeIndTransform  => Json.obj("SetLeadTrusteeIndTransform" -> Json.toJson(transform)(AmendLeadTrusteeIndTransform.format))
      case transform: AmendLeadTrusteeOrgTransform  => Json.obj("SetLeadTrusteeOrgTransform" -> Json.toJson(transform)(AmendLeadTrusteeOrgTransform.format))
      case transform: AddTrusteeIndTransform      => Json.obj("AddTrusteeIndTransform"-> Json.toJson(transform)(AddTrusteeIndTransform.format))
      case transform: AddTrusteeOrgTransform      => Json.obj("AddTrusteeOrgTransform"-> Json.toJson(transform)(AddTrusteeOrgTransform.format))
      case transform: RemoveTrusteeTransform      => Json.obj(RemoveTrusteeTransform.key -> Json.toJson(transform)(RemoveTrusteeTransform.format))
      case transform => throw new Exception(s"Don't know how to serialise transform: $transform")
    }
    Json.toJson(transformWrapper)
  }
}

case class ComposedDeltaTransform(deltaTransforms: Seq[DeltaTransform]) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyTransform))
  }

  def :+(transform: DeltaTransform) = ComposedDeltaTransform(deltaTransforms :+ transform)
}

object ComposedDeltaTransform {
  implicit val format: Format[ComposedDeltaTransform] = Json.format[ComposedDeltaTransform]
}
