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

sealed class DeltaType(val myType: String)

object DeltaType {
  implicit val writes: Writes[DeltaType] = Writes[DeltaType] {
    t => JsString(t.myType)
  }
  implicit val reads: Reads[DeltaType] = Reads[DeltaType] {
    case JsString(AddTrusteeDeltaType.myType) => JsSuccess(AddTrusteeDeltaType)
    case JsString(SetLeadTrusteeIndDeltaType.myType) => JsSuccess(SetLeadTrusteeIndDeltaType)
  }
}

case object AddTrusteeDeltaType extends DeltaType("AddTrusteeDeltaType")
case object SetLeadTrusteeIndDeltaType extends DeltaType("SetLeadTrusteeIndDeltaType")

case class SerialisedDeltaTransform(serialisedType: DeltaType)

object SerialisedDeltaTransform {
  implicit val format: Format[SerialisedDeltaTransform] = Json.format[SerialisedDeltaTransform]
}

object DeltaTransform {
  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value => {
      value.as[SerialisedDeltaTransform].serialisedType match {
        case AddTrusteeDeltaType => JsSuccess(value.as[AddTrusteeTransformer])
        case SetLeadTrusteeIndDeltaType => JsSuccess(value.as[SetLeadTrusteeIndTransform])
        case _ => throw new Exception(s"Don't know how to deserialise transform: $value")
      }
    }
  )
  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] {
    case transform: SetLeadTrusteeIndTransform => Json.toJson(transform)(SetLeadTrusteeIndTransform.format)
    case transform: AddTrusteeTransformer => Json.toJson(transform)(AddTrusteeTransformer.format)
    case transform => throw new Exception(s"Don't know how to serialise transform: $transform")
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
