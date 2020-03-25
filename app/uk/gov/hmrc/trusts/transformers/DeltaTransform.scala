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
  def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = JsSuccess(input)
}

object DeltaTransform {
  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value => {
      value.as[JsObject] match {
        case json if json.keys.contains(PromoteTrusteeIndTransform.key) => (json \ PromoteTrusteeIndTransform.key).validate[PromoteTrusteeIndTransform]
        case json if json.keys.contains(PromoteTrusteeOrgTransform.key) => (json \ PromoteTrusteeOrgTransform.key).validate[PromoteTrusteeOrgTransform]
        case json if json.keys.contains(AmendLeadTrusteeIndTransform.key) => (json \ AmendLeadTrusteeIndTransform.key).validate[AmendLeadTrusteeIndTransform]
        case json if json.keys.contains(AmendLeadTrusteeOrgTransform.key) => (json \ AmendLeadTrusteeOrgTransform.key).validate[AmendLeadTrusteeOrgTransform]
        case json if json.keys.contains(AddTrusteeIndTransform.key)      => (json \ AddTrusteeIndTransform.key).validate[AddTrusteeIndTransform]
        case json if json.keys.contains(AddTrusteeOrgTransform.key)      => (json \ AddTrusteeOrgTransform.key).validate[AddTrusteeOrgTransform]
        case json if json.keys.contains(RemoveTrusteeTransform.key)      => (json \ RemoveTrusteeTransform.key).validate[RemoveTrusteeTransform]
        case json if json.keys.contains(AmendTrusteeIndTransform.key)    => (json \ AmendTrusteeIndTransform.key).validate[AmendTrusteeIndTransform]
        case json if json.keys.contains(AmendTrusteeOrgTransform.key)    => (json \ AmendTrusteeOrgTransform.key).validate[AmendTrusteeOrgTransform]
        case json if json.keys.contains(AmendUnidentifiedBeneficiaryTransform.key)    => (json \ AmendUnidentifiedBeneficiaryTransform.key).validate[AmendUnidentifiedBeneficiaryTransform]
        case json if json.keys.contains(RemoveBeneficiariesTransform.key)    => (json \ RemoveBeneficiariesTransform.key).validate[RemoveBeneficiariesTransform]
        case json if json.keys.contains(AddUnidentifiedBeneficiaryTransform.key)    => (json \ AddUnidentifiedBeneficiaryTransform.key).validate[AddUnidentifiedBeneficiaryTransform]
        case json if json.keys.contains(AmendIndividualBeneficiaryTransform.key) => (json \ AmendIndividualBeneficiaryTransform.key).validate[AmendIndividualBeneficiaryTransform]
        case _ => throw new Exception(s"Don't know how to deserialise transform: $value")
      }
    }
  )

  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { deltaTransform =>
    val transformWrapper = deltaTransform match {
      case transform: PromoteTrusteeIndTransform  => Json.obj(PromoteTrusteeIndTransform.key -> Json.toJson(transform)(PromoteTrusteeIndTransform.format))
      case transform: PromoteTrusteeOrgTransform  => Json.obj(PromoteTrusteeOrgTransform.key -> Json.toJson(transform)(PromoteTrusteeOrgTransform.format))
      case transform: AmendLeadTrusteeIndTransform  => Json.obj(AmendLeadTrusteeIndTransform.key -> Json.toJson(transform)(AmendLeadTrusteeIndTransform.format))
      case transform: AmendLeadTrusteeOrgTransform  => Json.obj(AmendLeadTrusteeOrgTransform.key -> Json.toJson(transform)(AmendLeadTrusteeOrgTransform.format))
      case transform: AddTrusteeIndTransform      => Json.obj(AddTrusteeIndTransform.key -> Json.toJson(transform)(AddTrusteeIndTransform.format))
      case transform: AddTrusteeOrgTransform      => Json.obj(AddTrusteeOrgTransform.key -> Json.toJson(transform)(AddTrusteeOrgTransform.format))
      case transform: RemoveTrusteeTransform      => Json.obj(RemoveTrusteeTransform.key -> Json.toJson(transform)(RemoveTrusteeTransform.format))
      case transform: AmendTrusteeIndTransform    => Json.obj(AmendTrusteeIndTransform.key -> Json.toJson(transform)(AmendTrusteeIndTransform.format))
      case transform: AmendTrusteeOrgTransform    => Json.obj(AmendTrusteeOrgTransform.key -> Json.toJson(transform)(AmendTrusteeOrgTransform.format))
      case transform: AmendUnidentifiedBeneficiaryTransform    => Json.obj(AmendUnidentifiedBeneficiaryTransform.key -> Json.toJson(transform)(AmendUnidentifiedBeneficiaryTransform.format))
      case transform: RemoveBeneficiariesTransform    => Json.obj(RemoveBeneficiariesTransform.key -> Json.toJson(transform)(RemoveBeneficiariesTransform.format))
      case transform: AddUnidentifiedBeneficiaryTransform    => Json.obj(AddUnidentifiedBeneficiaryTransform.key -> Json.toJson(transform)(AddUnidentifiedBeneficiaryTransform.format))
      case transform: AmendIndividualBeneficiaryTransform    => Json.obj(AmendIndividualBeneficiaryTransform.key -> Json.toJson(transform)(AmendIndividualBeneficiaryTransform.format))
      case transform => throw new Exception(s"Don't know how to serialise transform: $transform")
    }
    Json.toJson(transformWrapper)
  }
}

case class ComposedDeltaTransform(deltaTransforms: Seq[DeltaTransform]) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyTransform))
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyDeclarationTransform))
  }
  def :+(transform: DeltaTransform) = ComposedDeltaTransform(deltaTransforms :+ transform)
}

object ComposedDeltaTransform {
  implicit val format: Format[ComposedDeltaTransform] = Json.format[ComposedDeltaTransform]
}
