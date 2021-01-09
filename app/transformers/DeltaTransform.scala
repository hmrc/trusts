/*
 * Copyright 2021 HM Revenue & Customs
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
import transformers.assets.{AddAssetTransform, AmendAssetTransform, RemoveAssetTransform}
import transformers.beneficiaries._
import transformers.otherindividuals._
import transformers.protectors._
import transformers.settlors._
import transformers.taxliability.SetTaxLiabilityTransform
import transformers.trustdetails._
import transformers.trustees._

trait DeltaTransform {
  def applyTransform(input: JsValue): JsResult[JsValue]

  def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = JsSuccess(input)

  val isTaxableMigrationTransform: Boolean = false
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
          trustDetailsReads orElse
          assetReads orElse
          taxLiabilityReads
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
    readsForTransform[AddBeneficiaryTransform](AddBeneficiaryTransform.key) orElse
    readsForTransform[AmendBeneficiaryTransform](AmendBeneficiaryTransform.key) orElse
    readsForTransform[RemoveBeneficiaryTransform](RemoveBeneficiaryTransform.key)
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
    readsForTransform[RemoveOtherIndividualTransform](RemoveOtherIndividualTransform.key) orElse
    readsForTransform[AddOtherIndividualTransform](AddOtherIndividualTransform.key)
  }

  def trustDetailsReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[SetTrustDetailTransform](SetTrustDetailTransform.key)
  }

  def assetReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddAssetTransform](AddAssetTransform.key) orElse
    readsForTransform[AmendAssetTransform](AmendAssetTransform.key) orElse
    readsForTransform[RemoveAssetTransform](RemoveAssetTransform.key)
  }

  def taxLiabilityReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[SetTaxLiabilityTransform](SetTaxLiabilityTransform.key)
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

  def beneficiariesWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddBeneficiaryTransform =>
      Json.obj(AddBeneficiaryTransform.key -> Json.toJson(transform)(AddBeneficiaryTransform.format))
    case transform: AmendBeneficiaryTransform =>
      Json.obj(AmendBeneficiaryTransform.key -> Json.toJson(transform)(AmendBeneficiaryTransform.format))
    case transform: RemoveBeneficiaryTransform =>
      Json.obj(RemoveBeneficiaryTransform.key -> Json.toJson(transform)(RemoveBeneficiaryTransform.format))
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

  def addOtherIndividualsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: AddOtherIndividualTransform =>
      Json.obj(AddOtherIndividualTransform.key -> Json.toJson(transform)(AddOtherIndividualTransform.format))
  }

  def amendOtherIndividualsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AmendOtherIndividualTransform =>
      Json.obj(AmendOtherIndividualTransform.key -> Json.toJson(transform)(AmendOtherIndividualTransform.format))
  }

  def removeOtherIndividualsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: RemoveOtherIndividualTransform =>
      Json.obj(RemoveOtherIndividualTransform.key -> Json.toJson(transform)(RemoveOtherIndividualTransform.format))
  }

  def assetsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: AddAssetTransform =>
      Json.obj(AddAssetTransform.key -> Json.toJson(transform)(AddAssetTransform.format))
    case transform: AmendAssetTransform =>
      Json.obj(AmendAssetTransform.key -> Json.toJson(transform)(AmendAssetTransform.format))
    case transform: RemoveAssetTransform =>
      Json.obj(RemoveAssetTransform.key -> Json.toJson(transform)(RemoveAssetTransform.format))
  }

  def trustDetailsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: SetTrustDetailTransform =>
      Json.obj(SetTrustDetailTransform.key -> Json.toJson(transform)(SetTrustDetailTransform.format))
  }

  def taxLiabilityWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: SetTaxLiabilityTransform =>
      Json.obj(SetTaxLiabilityTransform.key -> Json.toJson(transform)(SetTaxLiabilityTransform.format))
  }

  def defaultWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform => throw new Exception(s"Don't know how to serialise transform - $transform")
  }

  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { deltaTransform =>
    (trusteeWrites orElse
      beneficiariesWrites orElse
      addSettlorsWrites orElse
      amendSettlorsWrites orElse
      removeSettlorsWrites orElse
      addProtectorsWrites orElse
      amendProtectorsWrites orElse
      removeProtectorsWrites orElse
      amendOtherIndividualsWrites orElse
      removeOtherIndividualsWrites orElse
      addOtherIndividualsWrites orElse
      trustDetailsWrites orElse
      assetsWrites orElse
      taxLiabilityWrites orElse
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
