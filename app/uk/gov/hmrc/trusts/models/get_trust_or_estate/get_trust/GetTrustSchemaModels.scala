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

package uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.{MatchData, ResponseHeader}
import uk.gov.hmrc.trusts.models.JsonWithoutNulls._

case class GetTrust(matchData: MatchData,
                    correspondence: Correspondence,
                    declaration: Declaration,
                    trust: DisplayTrust)

object GetTrust {
  implicit val writes: Writes[GetTrust] = Json.writes[GetTrust]
  implicit val reads: Reads[GetTrust] = (
    (JsPath \ "matchData").read[MatchData] and
      (JsPath \ "correspondence").read[Correspondence] and
      (JsPath \ "declaration").read[Declaration] and
      (JsPath \ "details" \ "trust").read[DisplayTrust]
    ) (GetTrust.apply _)
}

case class GetTrustDesResponse(getTrust: Option[GetTrust],
                               responseHeader: ResponseHeader)

object GetTrustDesResponse {
  implicit val writes: Writes[GetTrustDesResponse] = Json.writes[GetTrustDesResponse]
  implicit val reads: Reads[GetTrustDesResponse] = (
    (JsPath \ "trustOrEstateDisplay").readNullable[GetTrust] and
      (JsPath \ "responseHeader").read[ResponseHeader]
    ) (GetTrustDesResponse.apply _)
}

case class DisplayTrust(
                         details: TrustDetailsType,
                         entities: DisplayTrustEntitiesType,
                         assets: DisplayTrustAssets)

object DisplayTrust {
  implicit val trustFormat: Format[DisplayTrust] = Json.format[DisplayTrust]
}

case class DisplayTrustEntitiesType(naturalPerson: Option[List[DisplayTrustNaturalPersonType]],
                                    beneficiary: DisplayTrustBeneficiaryType,
                                    deceased: Option[DisplayTrustWillType],
                                    leadTrustee: DisplayTrustLeadTrusteeType,
                                    trustees: Option[List[DisplayTrustTrusteeType]],
                                    protectors: Option[DisplayTrustProtectorsType],
                                    settlors: Option[DisplayTrustSettlors])

object DisplayTrustEntitiesType {

  implicit val displayTrustEntitiesTypeReads : Reads[DisplayTrustEntitiesType] = (
    (__ \ "naturalPerson").readNullable[List[DisplayTrustNaturalPersonType]] and
    (__ \ "beneficiary").read[DisplayTrustBeneficiaryType] and
    (__ \ "deceased").readNullable[DisplayTrustWillType] and
    (__ \ "leadTrustees").read[DisplayTrustLeadTrusteeType] and
    (__ \ "trustees").readNullable[List[DisplayTrustTrusteeType]] and
    (__ \ "protectors").readNullable[DisplayTrustProtectorsType] and
    (__ \ "settlors").readNullable[DisplayTrustSettlors]
  )(
    (natural, beneficiary, deceased, leadTrustee, trustees, protectors, settlors) =>
      DisplayTrustEntitiesType(
        natural,
        beneficiary,
        deceased,
        leadTrustee,
        trustees,
        protectors,
        settlors
      )
  )

  implicit val trustEntitiesTypeWrites: Writes[DisplayTrustEntitiesType] = Json.writes[DisplayTrustEntitiesType]
}

case class DisplayTrustNaturalPersonType(lineNo: Option[String],
                                         bpMatchStatus: Option[String],
                                         name: NameType,
                                         dateOfBirth: Option[LocalDate],
                                         identification: Option[DisplayTrustIdentificationType],
                                         entityStart: LocalDate)

object DisplayTrustNaturalPersonType {
  implicit val naturalPersonTypeFormat: Format[DisplayTrustNaturalPersonType] = Json.format[DisplayTrustNaturalPersonType]

}

case class DisplayTrustLeadTrusteeIndType(
                                           lineNo: Option[String],
                                           bpMatchStatus: Option[String],
                                           name: NameType,
                                           dateOfBirth: LocalDate,
                                           phoneNumber: String,
                                           email: Option[String] = None,
                                           identification: DisplayTrustIdentificationType,
                                           entityStart: Option[LocalDate]
                                         )

object DisplayTrustLeadTrusteeIndType {

  implicit val leadTrusteeIndTypeFormat: Format[DisplayTrustLeadTrusteeIndType] = Json.format[DisplayTrustLeadTrusteeIndType]

}

case class DisplayTrustLeadTrusteeOrgType(
                                           lineNo: Option[String],
                                           bpMatchStatus: Option[String],
                                           name: String,
                                           phoneNumber: String,
                                           email: Option[String] = None,
                                           identification: DisplayTrustIdentificationOrgType,
                                           entityStart: Option[LocalDate]
                                         )

object DisplayTrustLeadTrusteeOrgType {
  implicit val leadTrusteeOrgTypeFormat: Format[DisplayTrustLeadTrusteeOrgType] = Json.format[DisplayTrustLeadTrusteeOrgType]
}

case class DisplayTrustLeadTrusteeType(
                                        leadTrusteeInd: Option[DisplayTrustLeadTrusteeIndType] = None,
                                        leadTrusteeOrg: Option[DisplayTrustLeadTrusteeOrgType] = None
                                      )

object DisplayTrustLeadTrusteeType {

  implicit val writes: Writes[DisplayTrustLeadTrusteeType] = Json.writes[DisplayTrustLeadTrusteeType]

  object LeadTrusteeReads extends Reads[DisplayTrustLeadTrusteeType] {

    override def reads(json: JsValue): JsResult[DisplayTrustLeadTrusteeType] = {

      json.validate[DisplayTrustLeadTrusteeIndType].map {
        leadTrusteeInd =>
          DisplayTrustLeadTrusteeType(leadTrusteeInd = Some(leadTrusteeInd))
      }.orElse {
        json.validate[DisplayTrustLeadTrusteeOrgType].map {
          org =>
            DisplayTrustLeadTrusteeType(leadTrusteeOrg = Some(org))
        }
      }
    }
  }

  implicit val reads : Reads[DisplayTrustLeadTrusteeType] = LeadTrusteeReads
}

case class DisplayTrustBeneficiaryType(individualDetails: Option[List[DisplayTrustIndividualDetailsType]],
                                       company: Option[List[DisplayTrustBeneficiaryCompanyType]],
                                       trust: Option[List[DisplayTrustBeneficiaryTrustType]],
                                       charity: Option[List[DisplayTrustCharityType]],
                                       unidentified: Option[List[DisplayTrustUnidentifiedType]],
                                       large: Option[List[DisplayTrustLargeType]],
                                       other: Option[List[DisplayTrustOtherType]])

object DisplayTrustBeneficiaryType {
  implicit val beneficiaryTypeFormat: Format[DisplayTrustBeneficiaryType] = Json.format[DisplayTrustBeneficiaryType]
}


case class DisplayTrustIndividualDetailsType(lineNo: Option[String],
                                             bpMatchStatus: Option[String],
                                             name: NameType,
                                             dateOfBirth: Option[LocalDate],
                                             vulnerableBeneficiary: Boolean,
                                             beneficiaryType: Option[String],
                                             beneficiaryDiscretion: Option[Boolean],
                                             beneficiaryShareOfIncome: Option[String],
                                             identification: Option[DisplayTrustIdentificationType],
                                             entityStart: String)

object DisplayTrustIndividualDetailsType {
  implicit val individualDetailsTypeFormat: Format[DisplayTrustIndividualDetailsType] = Json.format[DisplayTrustIndividualDetailsType]

  val writeToMaintain : Writes[DisplayTrustIndividualDetailsType] = new Writes[DisplayTrustIndividualDetailsType] {
    override def writes(o: DisplayTrustIndividualDetailsType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "name" -> o.name,
      "dateOfBirth" -> o.dateOfBirth,
      "vulnerableBeneficiary" -> o.vulnerableBeneficiary,
      "beneficiaryType" -> o.beneficiaryType,
      "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
      "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
      "identification" -> o.identification,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}

case class DisplayTrustBeneficiaryCompanyType(lineNo: Option[String],
                                              bpMatchStatus: Option[String],
                                              organisationName: String,
                                              beneficiaryDiscretion: Option[Boolean],
                                              beneficiaryShareOfIncome: Option[String],
                                              identification: Option[DisplayTrustIdentificationOrgType],
                                              entityStart: String)

object DisplayTrustBeneficiaryCompanyType {
  implicit val companyTypeFormat: Format[DisplayTrustBeneficiaryCompanyType] = Json.format[DisplayTrustBeneficiaryCompanyType]

  val writeToMaintain : Writes[DisplayTrustBeneficiaryCompanyType] = new Writes[DisplayTrustBeneficiaryCompanyType] {
    override def writes(o: DisplayTrustBeneficiaryCompanyType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "organisationName" -> o.organisationName,
      "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
      "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
      "identification" -> o.identification,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}

case class DisplayTrustWillType(lineNo: String,
                                bpMatchStatus: Option[String],
                                name: NameType,
                                dateOfBirth: Option[LocalDate],
                                dateOfDeath: Option[LocalDate],
                                identification: Option[DisplayTrustIdentificationType],
                                entityStart: String)

object DisplayTrustWillType {
  implicit val willTypeFormat: Format[DisplayTrustWillType] = Json.format[DisplayTrustWillType]
}

case class DisplayTrustBeneficiaryTrustType(lineNo: Option[String],
                                            bpMatchStatus: Option[String],
                                            organisationName: String,
                                            beneficiaryDiscretion: Option[Boolean],
                                            beneficiaryShareOfIncome: Option[String],
                                            identification: Option[DisplayTrustIdentificationOrgType],
                                            entityStart: String)

object DisplayTrustBeneficiaryTrustType {
  implicit val beneficiaryTrustTypeFormat: Format[DisplayTrustBeneficiaryTrustType] = Json.format[DisplayTrustBeneficiaryTrustType]

  val writeToMaintain : Writes[DisplayTrustBeneficiaryTrustType] = new Writes[DisplayTrustBeneficiaryTrustType] {
    override def writes(o: DisplayTrustBeneficiaryTrustType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "organisationName" -> o.organisationName,
      "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
      "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
      "identification" -> o.identification,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}

case class DisplayTrustCharityType(lineNo: Option[String],
                                   bpMatchStatus: Option[String],
                                   organisationName: String,
                                   beneficiaryDiscretion: Option[Boolean],
                                   beneficiaryShareOfIncome: Option[String],
                                   identification: Option[DisplayTrustIdentificationOrgType],
                                   entityStart: String)

object DisplayTrustCharityType {
  implicit val charityTypeFormat: Format[DisplayTrustCharityType] = Json.format[DisplayTrustCharityType]

  val writeToMaintain : Writes[DisplayTrustCharityType] = new Writes[DisplayTrustCharityType] {
    override def writes(o: DisplayTrustCharityType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "organisationName" -> o.organisationName,
      "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
      "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
      "identification" -> o.identification,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}


case class DisplayTrustUnidentifiedType(lineNo: Option[String],
                                        bpMatchStatus: Option[String],
                                        description: String,
                                        beneficiaryDiscretion: Option[Boolean],
                                        beneficiaryShareOfIncome: Option[String],
                                        entityStart: String)

object DisplayTrustUnidentifiedType {
  implicit val unidentifiedTypeFormat: Format[DisplayTrustUnidentifiedType] = Json.format[DisplayTrustUnidentifiedType]

  val writeToMaintain : Writes[DisplayTrustUnidentifiedType] = new Writes[DisplayTrustUnidentifiedType] {
    override def writes(o: DisplayTrustUnidentifiedType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "description" -> o.description,
      "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
      "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}


case class DisplayTrustLargeType(lineNo: Option[String],
                                 bpMatchStatus: Option[String],
                                 organisationName: String,
                                 description: String,
                                 description1: Option[String],
                                 description2: Option[String],
                                 description3: Option[String],
                                 description4: Option[String],
                                 numberOfBeneficiary: String,
                                 identification: Option[DisplayTrustIdentificationOrgType],
                                 beneficiaryDiscretion: Option[Boolean],
                                 beneficiaryShareOfIncome: Option[String],
                                 entityStart: String)

object DisplayTrustLargeType {
  implicit val largeTypeFormat: Format[DisplayTrustLargeType] = Json.format[DisplayTrustLargeType]

  val writeToMaintain : Writes[DisplayTrustLargeType] = new Writes[DisplayTrustLargeType] {
    override def writes(o: DisplayTrustLargeType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "organisationName" -> o.organisationName,
      "description" -> o.description,
      "description1" -> o.description1,
      "description2" -> o.description2,
      "description3" -> o.description3,
      "description4" -> o.description4,
      "numberOfBeneficiary" -> o.numberOfBeneficiary,
      "identification" -> o.identification,
      "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
      "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}

case class DisplayTrustOtherType(lineNo: Option[String],
                                 bpMatchStatus: Option[String],
                                 description: String,
                                 address: Option[AddressType],
                                 beneficiaryDiscretion: Option[Boolean],
                                 beneficiaryShareOfIncome: Option[String],
                                 entityStart: String)

object DisplayTrustOtherType {
  implicit val otherTypeFormat: Format[DisplayTrustOtherType] = Json.format[DisplayTrustOtherType]

  val writeToMaintain : Writes[DisplayTrustOtherType] = new Writes[DisplayTrustOtherType] {
    override def writes(o: DisplayTrustOtherType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "description" -> o.description,
      "address" -> o.address,
      "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
      "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}

case class DisplayTrustTrusteeType(trusteeInd: Option[DisplayTrustTrusteeIndividualType],
                                   trusteeOrg: Option[DisplayTrustTrusteeOrgType])

object DisplayTrustTrusteeType {
  implicit val trusteeTypeFormat: Format[DisplayTrustTrusteeType] = Json.format[DisplayTrustTrusteeType]
}

case class DisplayTrustTrusteeOrgType(lineNo: Option[String],
                                      bpMatchStatus: Option[String],
                                      name: String,
                                      phoneNumber: Option[String] = None,
                                      email: Option[String] = None,
                                      identification: Option[DisplayTrustIdentificationOrgType],
                                      entityStart: LocalDate)

object DisplayTrustTrusteeOrgType {

  implicit val trusteeOrgTypeFormat: Format[DisplayTrustTrusteeOrgType] = Json.format[DisplayTrustTrusteeOrgType]

  val writeToMaintain : Writes[DisplayTrustTrusteeOrgType] = new Writes[DisplayTrustTrusteeOrgType] {
    override def writes(o: DisplayTrustTrusteeOrgType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "name" -> o.name,
      "phoneNumber" -> o.phoneNumber,
      "email" -> o.email,
      "identification" -> o.identification,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}

case class DisplayTrustTrusteeIndividualType(lineNo: Option[String],
                                             bpMatchStatus: Option[String],
                                             name: NameType,
                                             dateOfBirth: Option[LocalDate],
                                             phoneNumber: Option[String],
                                             identification: Option[DisplayTrustIdentificationType],
                                             entityStart: LocalDate
                                            )

object DisplayTrustTrusteeIndividualType {

  implicit val trusteeIndividualTypeFormat: Format[DisplayTrustTrusteeIndividualType] = Json.format[DisplayTrustTrusteeIndividualType]

  val writeToMaintain : Writes[DisplayTrustTrusteeIndividualType] = new Writes[DisplayTrustTrusteeIndividualType] {
    override def writes(o: DisplayTrustTrusteeIndividualType): JsValue = Json.obj(
      "lineNo" -> o.lineNo,
      "bpMatchStatus" -> o.bpMatchStatus,
      "name" -> o.name,
      "dateOfBirth" -> o.dateOfBirth,
      "phoneNumber" -> o.phoneNumber,
      "identification" -> o.identification,
      "entityStart" -> o.entityStart,
      "provisional" -> o.lineNo.isEmpty
    ).withoutNulls
  }
}


case class DisplayTrustProtectorsType(protector: Option[List[DisplayTrustProtector]],
                                      protectorCompany: Option[List[DisplayTrustProtectorCompany]])

object DisplayTrustProtectorsType {
  implicit val protectorsTypeFormat: Format[DisplayTrustProtectorsType] = Json.format[DisplayTrustProtectorsType]
}

case class DisplayTrustProtector(lineNo: Option[String],
                                 bpMatchStatus: Option[String],
                                 name: NameType,
                                 dateOfBirth: Option[LocalDate],
                                 identification: Option[DisplayTrustIdentificationType],
                                 entityStart: LocalDate)

object DisplayTrustProtector {
  implicit val protectorFormat: Format[DisplayTrustProtector] = Json.format[DisplayTrustProtector]
}

case class DisplayTrustProtectorCompany(lineNo: Option[String],
                                        bpMatchStatus: Option[String],
                                        name: String,
                                        identification: Option[DisplayTrustIdentificationOrgType],
                                        entityStart: LocalDate)

object DisplayTrustProtectorCompany {
  implicit val protectorCompanyFormat: Format[DisplayTrustProtectorCompany] = Json.format[DisplayTrustProtectorCompany]
}


case class DisplayTrustSettlors(settlor: Option[List[DisplayTrustSettlor]],
                                settlorCompany: Option[List[DisplayTrustSettlorCompany]])

object DisplayTrustSettlors {
  implicit val settlorsFormat: Format[DisplayTrustSettlors] = Json.format[DisplayTrustSettlors]
}

case class DisplayTrustSettlor(lineNo: Option[String],
                               bpMatchStatus: Option[String],
                               name: NameType,
                               dateOfBirth: Option[LocalDate],
                               identification: Option[DisplayTrustIdentificationType],
                               entityStart: LocalDate)

object DisplayTrustSettlor {
  implicit val settlorFormat: Format[DisplayTrustSettlor] = Json.format[DisplayTrustSettlor]
}

case class DisplayTrustSettlorCompany(lineNo: Option[String],
                                      bpMatchStatus: Option[String],
                                      name: String,
                                      companyType: Option[String],
                                      companyTime: Option[Boolean],
                                      identification: Option[DisplayTrustIdentificationOrgType],
                                      entityStart: LocalDate)

object DisplayTrustSettlorCompany {
  implicit val settlorCompanyFormat: Format[DisplayTrustSettlorCompany] = Json.format[DisplayTrustSettlorCompany]
}

case class DisplayTrustIdentificationType(safeId: Option[String],
                                          nino: Option[String],
                                          passport: Option[PassportType],
                                          address: Option[AddressType])

object DisplayTrustIdentificationType {
  implicit val identificationTypeFormat: Format[DisplayTrustIdentificationType] = Json.format[DisplayTrustIdentificationType]
}

case class DisplayTrustIdentificationOrgType(safeId: Option[String],
                                             utr: Option[String],
                                             address: Option[AddressType])

object DisplayTrustIdentificationOrgType {
  implicit val trustBeneficiaryIdentificationFormat: Format[DisplayTrustIdentificationOrgType] = Json.format[DisplayTrustIdentificationOrgType]
}

case class DisplayTrustPartnershipType(utr: Option[String],
                                       description: String,
                                       partnershipStart: Option[LocalDate])

object DisplayTrustPartnershipType {

  implicit val partnershipTypeFormat: Format[DisplayTrustPartnershipType] = Json.format[DisplayTrustPartnershipType]
}

case class DisplayTrustAssets(monetary: Option[List[AssetMonetaryAmount]],
                              propertyOrLand: Option[List[PropertyLandType]],
                              shares: Option[List[DisplaySharesType]],
                              business: Option[List[DisplayBusinessAssetType]],
                              partnerShip: Option[List[DisplayTrustPartnershipType]],
                              other: Option[List[DisplayOtherAssetType]])

object DisplayTrustAssets {
  implicit val assetsFormat: Format[DisplayTrustAssets] = Json.format[DisplayTrustAssets]
}

case class PropertyLandType(buildingLandName: Option[String],
                            address: Option[AddressType],
                            valueFull: Long,
                            valuePrevious: Option[Long])

object PropertyLandType {
  implicit val propertyLandTypeFormat: Format[PropertyLandType] = Json.format[PropertyLandType]
}

case class DisplaySharesType(numberOfShares: Option[String],
                             orgName: String,
                             utr: Option[String],
                             shareClass: Option[String],
                             typeOfShare: Option[String],
                             value: Option[Long])

object DisplaySharesType {
  implicit val sharesTypeFormat: Format[DisplaySharesType] = Json.format[DisplaySharesType]
}

case class DisplayBusinessAssetType(orgName: String,
                                    utr: Option[String],
                                    businessDescription: String,
                                    address: Option[AddressType],
                                    businessValue: Option[Long])

object DisplayBusinessAssetType {
  implicit val businessAssetTypeFormat: Format[DisplayBusinessAssetType] = Json.format[DisplayBusinessAssetType]
}

case class DisplayOtherAssetType(description: String,
                                 value: Option[Long])

object DisplayOtherAssetType {
  implicit val otherAssetTypeFormat: Format[DisplayOtherAssetType] = Json.format[DisplayOtherAssetType]
}