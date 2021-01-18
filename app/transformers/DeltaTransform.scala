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

import models.variation._
import play.api.libs.json.{JsValue, _}
import transformers.assets.{AddAssetTransform, AmendAssetTransform, RemoveAssetTransform}
import transformers.beneficiaries._
import transformers.otherindividuals._
import transformers.protectors._
import transformers.settlors._
import transformers.taxliability.SetTaxLiabilityTransform
import transformers.trustdetails._
import transformers.trustees._
import utils.Constants._

trait DeltaTransform {
  def applyTransform(input: JsValue): JsResult[JsValue]

  def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = JsSuccess(input)

  val isTaxableMigrationTransform: Boolean = false
}

object DeltaTransform {

  private def readsForTransform[T](key: String)(implicit reads: Reads[T]): PartialFunction[JsObject, JsResult[T]] = {
    case json if json.keys.contains(key) && (json \ key).validate[T].isSuccess =>
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
          taxLiabilityReads orElse
          backwardsCompatibilityReads orElse
          defaultReads
      )(value.as[JsObject])
  )

  def defaultReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    case _ => throw new Exception("Don't know how to de-serialise transform")
  }

  def trusteeReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddTrusteeTransform](AddTrusteeTransform.key) orElse
    readsForTransform[AmendTrusteeTransform](AmendTrusteeTransform.key) orElse
    readsForTransform[PromoteTrusteeTransform](PromoteTrusteeTransform.key) orElse
    readsForTransform[RemoveTrusteeTransform](RemoveTrusteeTransform.key)
  }

  def beneficiaryReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddBeneficiaryTransform](AddBeneficiaryTransform.key) orElse
    readsForTransform[AmendBeneficiaryTransform](AmendBeneficiaryTransform.key) orElse
    readsForTransform[RemoveBeneficiaryTransform](RemoveBeneficiaryTransform.key)
  }

  def settlorReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddSettlorTransform](AddSettlorTransform.key) orElse
    readsForTransform[AmendSettlorTransform](AmendSettlorTransform.key) orElse
    readsForTransform[RemoveSettlorTransform](RemoveSettlorTransform.key)
  }

  def protectorReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddProtectorTransform](AddProtectorTransform.key) orElse
    readsForTransform[AmendProtectorTransform](AmendProtectorTransform.key) orElse
    readsForTransform[RemoveProtectorTransform](RemoveProtectorTransform.key)
  }

  def otherIndividualReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddOtherIndividualTransform](AddOtherIndividualTransform.key) orElse
    readsForTransform[AmendOtherIndividualTransform](AmendOtherIndividualTransform.key) orElse
    readsForTransform[RemoveOtherIndividualTransform](RemoveOtherIndividualTransform.key)
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

  // TODO - remove code once deployed and users no longer using old transforms
  def backwardsCompatibilityReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {

    lazy val addBeneficiaryReads = {
      readsForTransform[AddBeneficiaryTransform]("AddCharityBeneficiaryTransform")(AddBeneficiaryTransform.reads[BeneficiaryCharityType](CHARITY_BENEFICIARY)) orElse
        readsForTransform[AddBeneficiaryTransform]("AddCompanyBeneficiaryTransform")(AddBeneficiaryTransform.reads[BeneficiaryCompanyType](COMPANY_BENEFICIARY)) orElse
        readsForTransform[AddBeneficiaryTransform]("AddIndividualBeneficiaryTransform")(AddBeneficiaryTransform.reads[IndividualDetailsType](INDIVIDUAL_BENEFICIARY)) orElse
        readsForTransform[AddBeneficiaryTransform]("AddLargeBeneficiaryTransform")(AddBeneficiaryTransform.reads[LargeType](LARGE_BENEFICIARY)) orElse
        readsForTransform[AddBeneficiaryTransform]("AddOtherBeneficiaryTransform")(AddBeneficiaryTransform.reads[OtherType](OTHER_BENEFICIARY)) orElse
        readsForTransform[AddBeneficiaryTransform]("AddTrustBeneficiaryTransform")(AddBeneficiaryTransform.reads[BeneficiaryTrustType](TRUST_BENEFICIARY)) orElse
        readsForTransform[AddBeneficiaryTransform]("AddUnidentifiedBeneficiaryTransform")(AddBeneficiaryTransform.reads[UnidentifiedType](UNIDENTIFIED_BENEFICIARY))
    }

    lazy val amendBeneficiaryReads = {
      readsForTransform[AmendBeneficiaryTransform]("AmendCharityBeneficiaryTransform")(AmendBeneficiaryTransform.reads(CHARITY_BENEFICIARY)) orElse
        readsForTransform[AmendBeneficiaryTransform]("AmendCompanyBeneficiaryTransform")(AmendBeneficiaryTransform.reads(COMPANY_BENEFICIARY)) orElse
        readsForTransform[AmendBeneficiaryTransform]("AmendIndividualBeneficiaryTransform")(AmendBeneficiaryTransform.reads(INDIVIDUAL_BENEFICIARY)) orElse
        readsForTransform[AmendBeneficiaryTransform]("AmendLargeBeneficiaryTransform")(AmendBeneficiaryTransform.reads(LARGE_BENEFICIARY)) orElse
        readsForTransform[AmendBeneficiaryTransform]("AmendOtherBeneficiaryTransform")(AmendBeneficiaryTransform.reads(OTHER_BENEFICIARY)) orElse
        readsForTransform[AmendBeneficiaryTransform]("AmendTrustBeneficiaryTransform")(AmendBeneficiaryTransform.reads(TRUST_BENEFICIARY)) orElse
        readsForTransform[AmendBeneficiaryTransform]("AmendUnidentifiedBeneficiaryTransform")(AmendBeneficiaryTransform.reads(UNIDENTIFIED_BENEFICIARY, "description"))
    }

    lazy val removeBeneficiaryReads = {
      readsForTransform[RemoveBeneficiaryTransform]("RemoveBeneficiariesTransform")(RemoveBeneficiaryTransform.reads)
    }

    // NB - spelling mistake is intentional as this is how it was before
    lazy val addSettlorReads = {
      readsForTransform[AddSettlorTransform]("AddBuisnessSettlorTransform")(AddSettlorTransform.reads[SettlorCompany](BUSINESS_SETTLOR, "newCompanySettlor")) orElse
      readsForTransform[AddSettlorTransform]("AddIndividualSettlorTransform")(AddSettlorTransform.reads[Settlor](INDIVIDUAL_SETTLOR, "newSettlor"))
    }

    lazy val amendSettlorReads = {
      readsForTransform[AmendSettlorTransform]("AmendBusinessSettlorTransform")(AmendSettlorTransform.livingReads(BUSINESS_SETTLOR)) orElse
      readsForTransform[AmendSettlorTransform]("AmendDeceasedSettlorTransform")(AmendSettlorTransform.deceasedReads) orElse
      readsForTransform[AmendSettlorTransform]("AmendIndividualSettlorTransform")(AmendSettlorTransform.livingReads(INDIVIDUAL_SETTLOR))
    }

    lazy val removeSettlorReads = {
      readsForTransform[RemoveSettlorTransform]("RemoveSettlorsTransform")(RemoveSettlorTransform.reads)
    }

    lazy val addTrusteeReads = {
      readsForTransform[AddTrusteeTransform]("AddTrusteeIndTransform")(AddTrusteeTransform.reads[TrusteeIndividualType](INDIVIDUAL_TRUSTEE)) orElse
      readsForTransform[AddTrusteeTransform]("AddTrusteeOrgTransform")(AddTrusteeTransform.reads[TrusteeOrgType](BUSINESS_TRUSTEE))
    }

    lazy val amendTrusteeReads = {
      readsForTransform[AmendTrusteeTransform]("AmendLeadTrusteeIndTransform")(AmendTrusteeTransform.leadReads[AmendedLeadTrusteeIndType](INDIVIDUAL_LEAD_TRUSTEE)) orElse
      readsForTransform[AmendTrusteeTransform]("AmendLeadTrusteeOrgTransform")(AmendTrusteeTransform.leadReads[AmendedLeadTrusteeOrgType](BUSINESS_LEAD_TRUSTEE)) orElse
      readsForTransform[AmendTrusteeTransform]("AmendTrusteeIndTransform")(AmendTrusteeTransform.nonLeadReads[TrusteeIndividualType](INDIVIDUAL_TRUSTEE, "newTrustee")) orElse
      readsForTransform[AmendTrusteeTransform]("AmendTrusteeOrgTransform")(AmendTrusteeTransform.nonLeadReads[TrusteeOrgType](BUSINESS_TRUSTEE, "trustee"))
    }

    lazy val promoteTrusteeReads = {
      readsForTransform[PromoteTrusteeTransform]("PromoteTrusteeIndTransform")(PromoteTrusteeTransform.reads[AmendedLeadTrusteeIndType](INDIVIDUAL_TRUSTEE)) orElse
      readsForTransform[PromoteTrusteeTransform]("PromoteTrusteeOrgTransform")(PromoteTrusteeTransform.reads[AmendedLeadTrusteeOrgType](BUSINESS_TRUSTEE))
    }

    lazy val removeTrusteeReads = {
      readsForTransform[RemoveTrusteeTransform]("RemoveTrusteeTransform")(RemoveTrusteeTransform.reads)
    }

    addBeneficiaryReads orElse
      amendBeneficiaryReads orElse
      removeBeneficiaryReads orElse
      addSettlorReads orElse
      amendSettlorReads orElse
      removeSettlorReads orElse
      addTrusteeReads orElse
      amendTrusteeReads orElse
      promoteTrusteeReads orElse
      removeTrusteeReads
  }

  def trusteeWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddTrusteeTransform =>
      Json.obj(AddTrusteeTransform.key -> Json.toJson(transform)(AddTrusteeTransform.format))
    case transform: AmendTrusteeTransform =>
      Json.obj(AmendTrusteeTransform.key -> Json.toJson(transform)(AmendTrusteeTransform.format))
    case transform: PromoteTrusteeTransform =>
      Json.obj(PromoteTrusteeTransform.key -> Json.toJson(transform)(PromoteTrusteeTransform.format))
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

  def settlorsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddSettlorTransform =>
      Json.obj(AddSettlorTransform.key -> Json.toJson(transform)(AddSettlorTransform.format))
    case transform: AmendSettlorTransform =>
      Json.obj(AmendSettlorTransform.key -> Json.toJson(transform)(AmendSettlorTransform.format))
    case transform: RemoveSettlorTransform =>
      Json.obj(RemoveSettlorTransform.key -> Json.toJson(transform)(RemoveSettlorTransform.format))
  }

  def protectorsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddProtectorTransform =>
      Json.obj(AddProtectorTransform.key -> Json.toJson(transform)(AddProtectorTransform.format))
    case transform: AmendProtectorTransform =>
      Json.obj(AmendProtectorTransform.key -> Json.toJson(transform)(AmendProtectorTransform.format))
    case transform: RemoveProtectorTransform =>
      Json.obj(RemoveProtectorTransform.key -> Json.toJson(transform)(RemoveProtectorTransform.format))
  }

  def otherIndividualsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddOtherIndividualTransform =>
      Json.obj(AddOtherIndividualTransform.key -> Json.toJson(transform)(AddOtherIndividualTransform.format))
    case transform: AmendOtherIndividualTransform =>
      Json.obj(AmendOtherIndividualTransform.key -> Json.toJson(transform)(AmendOtherIndividualTransform.format))
    case transform: RemoveOtherIndividualTransform =>
      Json.obj(RemoveOtherIndividualTransform.key -> Json.toJson(transform)(RemoveOtherIndividualTransform.format))
  }

  def assetsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: AddAssetTransform =>
      Json.obj(AddAssetTransform.key -> Json.toJson(transform)(AddAssetTransform.format))
    case transform: AmendAssetTransform =>
      Json.obj(AmendAssetTransform.key -> Json.toJson(transform)(AmendAssetTransform.format))
    case transform: RemoveAssetTransform =>
      Json.obj(RemoveAssetTransform.key -> Json.toJson(transform)(RemoveAssetTransform.format))
  }

  def trustDetailsWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: SetTrustDetailTransform =>
      Json.obj(SetTrustDetailTransform.key -> Json.toJson(transform)(SetTrustDetailTransform.format))
  }

  def taxLiabilityWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform: SetTaxLiabilityTransform =>
      Json.obj(SetTaxLiabilityTransform.key -> Json.toJson(transform)(SetTaxLiabilityTransform.format))
  }

  def defaultWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform => throw new Exception(s"Don't know how to serialise transform - $transform")
  }

  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { deltaTransform =>
    (trusteeWrites orElse
      beneficiariesWrites orElse
      settlorsWrites orElse
      protectorsWrites orElse
      otherIndividualsWrites orElse
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
