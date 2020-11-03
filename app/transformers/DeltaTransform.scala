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

package transformers

import play.api.libs.json.{JsValue, _}
import transformers.trustDetails.{SetExpressTransform, SetPropertyTransform, SetRecordedTransform, SetResidentTransform, SetTaxableTransform}

trait DeltaTransform {
  def applyTransform(input: JsValue): JsResult[JsValue]

  def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = JsSuccess(input)
}

object DeltaTransform {

  private def readsForTransform[T](key: String)(implicit reads: Reads[T]): PartialFunction[JsObject, JsResult[T]] = {
    case json if json.keys.contains(key) =>
      (json \ key).validate[T]
  }

  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value =>
      (
        trusteeReads orElse
        beneficiaryReads orElse
        settlorReads orElse
        protectorReads orElse
        otherIndividualReads orElse
        trustDetailsTransformsReads
      ) (value.as[JsObject]) orElse (throw new Exception(s"Don't know how to deserialise transform"))
  )

  def trusteeReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[PromoteTrusteeIndTransform](PromoteTrusteeIndTransform.key) orElse
    readsForTransform[PromoteTrusteeOrgTransform](PromoteTrusteeOrgTransform.key) orElse
    readsForTransform[AmendLeadTrusteeIndTransform](AmendLeadTrusteeIndTransform.key) orElse
    readsForTransform[AmendLeadTrusteeOrgTransform](AmendLeadTrusteeOrgTransform.key) orElse
    readsForTransform[AddTrusteeIndTransform](AddTrusteeIndTransform.key) orElse
    readsForTransform[AddTrusteeOrgTransform](AddTrusteeOrgTransform.key) orElse
    readsForTransform[AmendTrusteeIndTransform](AmendTrusteeIndTransform.key) orElse
    readsForTransform[AmendTrusteeOrgTransform](AmendTrusteeOrgTransform.key) orElse
    readsForTransform[RemoveTrusteeTransform](RemoveTrusteeTransform.key)
  }

  def beneficiaryReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddUnidentifiedBeneficiaryTransform](AddUnidentifiedBeneficiaryTransform.key) orElse
    readsForTransform[AmendUnidentifiedBeneficiaryTransform](AmendUnidentifiedBeneficiaryTransform.key) orElse
    readsForTransform[AddIndividualBeneficiaryTransform](AddIndividualBeneficiaryTransform.key) orElse
    readsForTransform[AmendIndividualBeneficiaryTransform](AmendIndividualBeneficiaryTransform.key) orElse
    readsForTransform[AddCharityBeneficiaryTransform](AddCharityBeneficiaryTransform.key) orElse
    readsForTransform[AmendCharityBeneficiaryTransform](AmendCharityBeneficiaryTransform.key) orElse
    readsForTransform[AddTrustBeneficiaryTransform](AddTrustBeneficiaryTransform.key) orElse
    readsForTransform[AmendTrustBeneficiaryTransform](AmendTrustBeneficiaryTransform.key) orElse
    readsForTransform[AddCompanyBeneficiaryTransform](AddCompanyBeneficiaryTransform.key) orElse
    readsForTransform[AmendCompanyBeneficiaryTransform](AmendCompanyBeneficiaryTransform.key) orElse
    readsForTransform[AddLargeBeneficiaryTransform](AddLargeBeneficiaryTransform.key) orElse
    readsForTransform[AmendLargeBeneficiaryTransform](AmendLargeBeneficiaryTransform.key) orElse
    readsForTransform[AddOtherBeneficiaryTransform](AddOtherBeneficiaryTransform.key) orElse
    readsForTransform[AmendOtherBeneficiaryTransform](AmendOtherBeneficiaryTransform.key) orElse
    readsForTransform[RemoveBeneficiariesTransform](RemoveBeneficiariesTransform.key)
  }

  def settlorReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddIndividualSettlorTransform](AddIndividualSettlorTransform.key) orElse
    readsForTransform[AmendIndividualSettlorTransform](AmendIndividualSettlorTransform.key) orElse
    readsForTransform[AddBusinessSettlorTransform](AddBusinessSettlorTransform.key) orElse
    readsForTransform[AmendBusinessSettlorTransform](AmendBusinessSettlorTransform.key) orElse
    readsForTransform[AmendDeceasedSettlorTransform](AmendDeceasedSettlorTransform.key) orElse
    readsForTransform[RemoveSettlorsTransform](RemoveSettlorsTransform.key)
  }

  def protectorReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddIndividualProtectorTransform](AddIndividualProtectorTransform.key) orElse
    readsForTransform[AddCompanyProtectorTransform](AddCompanyProtectorTransform.key) orElse
    readsForTransform[AmendIndividualProtectorTransform](AmendIndividualProtectorTransform.key) orElse
    readsForTransform[AmendBusinessProtectorTransform](AmendBusinessProtectorTransform.key) orElse
    readsForTransform[RemoveProtectorsTransform](RemoveProtectorsTransform.key)
  }

  def otherIndividualReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AmendOtherIndividualTransform](AmendOtherIndividualTransform.key) orElse
    readsForTransform[RemoveOtherIndividualsTransform](RemoveOtherIndividualsTransform.key) orElse
    readsForTransform[AddOtherIndividualTransform](AddOtherIndividualTransform.key)
  }

  def trustDetailsTransformsReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[SetExpressTransform](SetExpressTransform.key) orElse
    readsForTransform[SetPropertyTransform](SetPropertyTransform.key) orElse
    readsForTransform[SetRecordedTransform](SetRecordedTransform.key) orElse
    readsForTransform[SetResidentTransform](SetResidentTransform.key) orElse
    readsForTransform[SetTaxableTransform](SetTaxableTransform.key)
  }

  def trusteeWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: PromoteTrusteeIndTransform =>
      Json.obj(PromoteTrusteeIndTransform.key -> Json.toJson(transform)(PromoteTrusteeIndTransform.format))
    case transform: PromoteTrusteeOrgTransform =>
      Json.obj(PromoteTrusteeOrgTransform.key -> Json.toJson(transform)(PromoteTrusteeOrgTransform.format))
    case transform: AmendLeadTrusteeIndTransform =>
      Json.obj(AmendLeadTrusteeIndTransform.key -> Json.toJson(transform)(AmendLeadTrusteeIndTransform.format))
    case transform: AmendLeadTrusteeOrgTransform =>
      Json.obj(AmendLeadTrusteeOrgTransform.key -> Json.toJson(transform)(AmendLeadTrusteeOrgTransform.format))
    case transform: AmendTrusteeIndTransform =>
      Json.obj(AmendTrusteeIndTransform.key -> Json.toJson(transform)(AmendTrusteeIndTransform.format))
    case transform: AmendTrusteeOrgTransform =>
      Json.obj(AmendTrusteeOrgTransform.key -> Json.toJson(transform)(AmendTrusteeOrgTransform.format))
    case transform: AddTrusteeIndTransform =>
      Json.obj(AddTrusteeIndTransform.key -> Json.toJson(transform)(AddTrusteeIndTransform.format))
    case transform: AddTrusteeOrgTransform =>
      Json.obj(AddTrusteeOrgTransform.key -> Json.toJson(transform)(AddTrusteeOrgTransform.format))
    case transform: RemoveTrusteeTransform =>
      Json.obj(RemoveTrusteeTransform.key -> Json.toJson(transform)(RemoveTrusteeTransform.format))
  }

  def addBeneficiariesWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddUnidentifiedBeneficiaryTransform =>
      Json.obj(AddUnidentifiedBeneficiaryTransform.key -> Json.toJson(transform)(AddUnidentifiedBeneficiaryTransform.format))
    case transform: AddIndividualBeneficiaryTransform =>
      Json.obj(AddIndividualBeneficiaryTransform.key -> Json.toJson(transform)(AddIndividualBeneficiaryTransform.format))
    case transform: AddCharityBeneficiaryTransform =>
      Json.obj(AddCharityBeneficiaryTransform.key -> Json.toJson(transform)(AddCharityBeneficiaryTransform.format))
    case transform: AddOtherBeneficiaryTransform =>
      Json.obj(AddOtherBeneficiaryTransform.key -> Json.toJson(transform)(AddOtherBeneficiaryTransform.format))
    case transform: AddCompanyBeneficiaryTransform =>
      Json.obj(AddCompanyBeneficiaryTransform.key -> Json.toJson(transform)(AddCompanyBeneficiaryTransform.format))
    case transform: AddTrustBeneficiaryTransform =>
      Json.obj(AddTrustBeneficiaryTransform.key -> Json.toJson(transform)(AddTrustBeneficiaryTransform.format))
    case transform: AddLargeBeneficiaryTransform =>
      Json.obj(AddLargeBeneficiaryTransform.key -> Json.toJson(transform)(AddLargeBeneficiaryTransform.format))
  }

  def amendBeneficiariesWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AmendUnidentifiedBeneficiaryTransform =>
      Json.obj(AmendUnidentifiedBeneficiaryTransform.key -> Json.toJson(transform)(AmendUnidentifiedBeneficiaryTransform.format))
    case transform: AmendIndividualBeneficiaryTransform =>
      Json.obj(AmendIndividualBeneficiaryTransform.key -> Json.toJson(transform)(AmendIndividualBeneficiaryTransform.format))
    case transform: AmendCharityBeneficiaryTransform =>
      Json.obj(AmendCharityBeneficiaryTransform.key -> Json.toJson(transform)(AmendCharityBeneficiaryTransform.format))
    case transform: AmendCompanyBeneficiaryTransform =>
      Json.obj(AmendCompanyBeneficiaryTransform.key -> Json.toJson(transform)(AmendCompanyBeneficiaryTransform.format))
    case transform: AmendOtherBeneficiaryTransform =>
      Json.obj(AmendOtherBeneficiaryTransform.key -> Json.toJson(transform)(AmendOtherBeneficiaryTransform.format))
    case transform: AmendTrustBeneficiaryTransform =>
      Json.obj(AmendTrustBeneficiaryTransform.key -> Json.toJson(transform)(AmendTrustBeneficiaryTransform.format))
    case transform: AmendLargeBeneficiaryTransform =>
      Json.obj(AmendLargeBeneficiaryTransform.key -> Json.toJson(transform)(AmendLargeBeneficiaryTransform.format))
  }

  def removeBeneficiariesWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: RemoveBeneficiariesTransform =>
      Json.obj(RemoveBeneficiariesTransform.key -> Json.toJson(transform)(RemoveBeneficiariesTransform.format))
  }

  def addSettlorsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddIndividualSettlorTransform =>
      Json.obj(AddIndividualSettlorTransform.key -> Json.toJson(transform)(AddIndividualSettlorTransform.format))
    case transform: AddBusinessSettlorTransform =>
      Json.obj(AddBusinessSettlorTransform.key -> Json.toJson(transform)(AddBusinessSettlorTransform.format))
  }

  def amendSettlorsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AmendIndividualSettlorTransform =>
      Json.obj(AmendIndividualSettlorTransform.key -> Json.toJson(transform)(AmendIndividualSettlorTransform.format))
    case transform: AmendBusinessSettlorTransform =>
      Json.obj(AmendBusinessSettlorTransform.key -> Json.toJson(transform)(AmendBusinessSettlorTransform.format))
    case transform: AmendDeceasedSettlorTransform =>
      Json.obj(AmendDeceasedSettlorTransform.key -> Json.toJson(transform)(AmendDeceasedSettlorTransform.format))
  }

  def removeSettlorsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: RemoveSettlorsTransform =>
      Json.obj(RemoveSettlorsTransform.key -> Json.toJson(transform)(RemoveSettlorsTransform.format))
  }

  def addProtectorsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddIndividualProtectorTransform =>
      Json.obj(AddIndividualProtectorTransform.key -> Json.toJson(transform)(AddIndividualProtectorTransform.format))
    case transform: AddCompanyProtectorTransform =>
      Json.obj(AddCompanyProtectorTransform.key -> Json.toJson(transform)(AddCompanyProtectorTransform.format))  }

  def amendProtectorsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AmendIndividualProtectorTransform =>
      Json.obj(AmendIndividualProtectorTransform.key -> Json.toJson(transform)(AmendIndividualProtectorTransform.format))
    case transform: AmendBusinessProtectorTransform =>
      Json.obj(AmendBusinessProtectorTransform.key -> Json.toJson(transform)(AmendBusinessProtectorTransform.format))
  }

  def removeProtectorsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: RemoveProtectorsTransform =>
      Json.obj(RemoveProtectorsTransform.key -> Json.toJson(transform)(RemoveProtectorsTransform.format))
  }

  def otherIndividualsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AmendOtherIndividualTransform =>
      Json.obj(AmendOtherIndividualTransform.key -> Json.toJson(transform)(AmendOtherIndividualTransform.format))
  }

  def removeOtherIndividualsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: RemoveOtherIndividualsTransform =>
      Json.obj(RemoveOtherIndividualsTransform.key -> Json.toJson(transform)(RemoveOtherIndividualsTransform.format))
  }

  def addOtherIndividualsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: AddOtherIndividualTransform =>
      Json.obj(AddOtherIndividualTransform.key -> Json.toJson(transform)(AddOtherIndividualTransform.format))
  }

  def trustDetailsTransformsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: SetExpressTransform =>
      Json.obj(SetExpressTransform.key -> Json.toJson(transform)(SetExpressTransform.format))
    case transform: SetPropertyTransform =>
      Json.obj(SetPropertyTransform.key -> Json.toJson(transform)(SetPropertyTransform.format))
    case transform: SetRecordedTransform =>
      Json.obj(SetRecordedTransform.key -> Json.toJson(transform)(SetRecordedTransform.format))
    case transform: SetResidentTransform =>
      Json.obj(SetResidentTransform.key -> Json.toJson(transform)(SetResidentTransform.format))
    case transform: SetTaxableTransform =>
      Json.obj(SetTaxableTransform.key -> Json.toJson(transform)(SetTaxableTransform.format))
  }

  def defaultWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform => throw new Exception(s"Don't know how to serialise transform - $transform")
  }

  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { deltaTransform =>
    (trusteeWrites orElse
      addBeneficiariesWrites orElse
      amendBeneficiariesWrites orElse
      removeBeneficiariesWrites orElse
      addSettlorsWrites orElse
      amendSettlorsWrites orElse
      removeSettlorsWrites orElse
      addProtectorsWrites orElse
      amendProtectorsWrites orElse
      removeProtectorsWrites orElse
      otherIndividualsWrites orElse
      removeOtherIndividualsWrites orElse
      addOtherIndividualsWrites orElse
      trustDetailsTransformsWrites orElse
      defaultWrites
      ).apply(deltaTransform)
  }

}

case class ComposedDeltaTransform(deltaTransforms: Seq[DeltaTransform]) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyTransform))
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyDeclarationTransform))
  }

  def :+(transform: DeltaTransform): ComposedDeltaTransform = ComposedDeltaTransform(deltaTransforms :+ transform)
}

object ComposedDeltaTransform {
  implicit val format: Format[ComposedDeltaTransform] = Json.format[ComposedDeltaTransform]
}
