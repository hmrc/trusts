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

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.{MatchData, ResponseHeader}
import uk.gov.hmrc.trusts.utils.Constants.dateTimePattern

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

case class DisplayTrustNaturalPersonType(lineNo: String,
                                         bpMatchStatus: Option[String],
                                         name: NameType,
                                         dateOfBirth: Option[DateTime],
                                         identification: Option[DisplayTrustIdentificationType],
                                         entityStart: String)

object DisplayTrustNaturalPersonType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val naturalPersonTypeFormat: Format[DisplayTrustNaturalPersonType] = Json.format[DisplayTrustNaturalPersonType]
}

case class DisplayTrustLeadTrusteeIndType(
                                           lineNo: Option[String],
                                           bpMatchStatus: Option[String],
                                           name: NameType,
                                           dateOfBirth: DateTime,
                                           phoneNumber: String,
                                           email: Option[String] = None,
                                           identification: DisplayTrustIdentificationType,
                                           entityStart: Option[DateTime]
                                         )

object DisplayTrustLeadTrusteeIndType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val leadTrusteeIndTypeFormat: Format[DisplayTrustLeadTrusteeIndType] = Json.format[DisplayTrustLeadTrusteeIndType]

}

case class DisplayTrustLeadTrusteeOrgType(
                                           lineNo: Option[String],
                                           bpMatchStatus: Option[String],
                                           name: String,
                                           phoneNumber: String,
                                           email: Option[String] = None,
                                           identification: DisplayTrustIdentificationOrgType,
                                           entityStart: Option[DateTime]
                                         )

object DisplayTrustLeadTrusteeOrgType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val leadTrusteeOrgTypeFormat: Format[DisplayTrustLeadTrusteeOrgType] = Json.format[DisplayTrustLeadTrusteeOrgType]
}

case class DisplayTrustLeadTrusteeType(
                                        leadTrusteeInd: Option[DisplayTrustLeadTrusteeIndType] = None,
                                        leadTrusteeOrg: Option[DisplayTrustLeadTrusteeOrgType] = None
                                      )

object DisplayTrustLeadTrusteeType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))

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
                                       company: Option[List[DisplayTrustCompanyType]],
                                       trust: Option[List[DisplayTrustBeneficiaryTrustType]],
                                       charity: Option[List[DisplayTrustCharityType]],
                                       unidentified: Option[List[DisplayTrustUnidentifiedType]],
                                       large: Option[List[DisplayTrustLargeType]],
                                       other: Option[List[DisplayTrustOtherType]])

object DisplayTrustBeneficiaryType {
  implicit val beneficiaryTypeFormat: Format[DisplayTrustBeneficiaryType] = Json.format[DisplayTrustBeneficiaryType]
}


case class DisplayTrustIndividualDetailsType(lineNo: String,
                                             bpMatchStatus: Option[String],
                                             name: NameType,
                                             dateOfBirth: Option[DateTime],
                                             vulnerableBeneficiary: Boolean,
                                             beneficiaryType: Option[String],
                                             beneficiaryDiscretion: Option[Boolean],
                                             beneficiaryShareOfIncome: Option[String],
                                             identification: Option[DisplayTrustIdentificationType],
                                             entityStart: String)

object DisplayTrustIndividualDetailsType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val individualDetailsTypeFormat: Format[DisplayTrustIndividualDetailsType] = Json.format[DisplayTrustIndividualDetailsType]
}

case class DisplayTrustCompanyType(lineNo: String,
                                   bpMatchStatus: Option[String], organisationName: String,
                                   beneficiaryDiscretion: Option[Boolean],
                                   beneficiaryShareOfIncome: Option[String],
                                   identification: Option[DisplayTrustIdentificationOrgType],
                                   entityStart: String)

object DisplayTrustCompanyType {
  implicit val companyTypeFormat: Format[DisplayTrustCompanyType] = Json.format[DisplayTrustCompanyType]
}

case class DisplayTrustWillType(lineNo: String,
                                bpMatchStatus: Option[String],
                                name: NameType,
                                dateOfBirth: Option[DateTime],
                                dateOfDeath: Option[DateTime],
                                identification: Option[DisplayTrustIdentificationType],
                                entityStart: String)

object DisplayTrustWillType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val willTypeFormat: Format[DisplayTrustWillType] = Json.format[DisplayTrustWillType]
}

case class DisplayTrustBeneficiaryTrustType(lineNo: String,
                                            bpMatchStatus: Option[String],
                                            organisationName: String,
                                            beneficiaryDiscretion: Option[Boolean],
                                            beneficiaryShareOfIncome: Option[String],
                                            identification: Option[DisplayTrustIdentificationOrgType],
                                            entityStart: String)

object DisplayTrustBeneficiaryTrustType {
  implicit val beneficiaryTrustTypeFormat: Format[DisplayTrustBeneficiaryTrustType] = Json.format[DisplayTrustBeneficiaryTrustType]
}

case class DisplayTrustCharityType(lineNo: String,
                                   bpMatchStatus: Option[String],
                                   organisationName: String,
                                   beneficiaryDiscretion: Option[Boolean],
                                   beneficiaryShareOfIncome: Option[String],
                                   identification: Option[DisplayTrustIdentificationOrgType],
                                   entityStart: String)

object DisplayTrustCharityType {
  implicit val charityTypeFormat: Format[DisplayTrustCharityType] = Json.format[DisplayTrustCharityType]
}


case class DisplayTrustUnidentifiedType(lineNo: String,
                                        bpMatchStatus: Option[String],
                                        description: String,
                                        beneficiaryDiscretion: Option[Boolean],
                                        beneficiaryShareOfIncome: Option[String],
                                        entityStart: String)

object DisplayTrustUnidentifiedType {
  implicit val unidentifiedTypeFormat: Format[DisplayTrustUnidentifiedType] = Json.format[DisplayTrustUnidentifiedType]
}


case class DisplayTrustLargeType(lineNo: String,
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
}

case class DisplayTrustOtherType(lineNo: String,
                                 bpMatchStatus: Option[String],
                                 description: String,
                                 address: Option[AddressType],
                                 beneficiaryDiscretion: Option[Boolean],
                                 beneficiaryShareOfIncome: Option[String],
                                 entityStart: String)

object DisplayTrustOtherType {
  implicit val otherTypeFormat: Format[DisplayTrustOtherType] = Json.format[DisplayTrustOtherType]
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
                                      entityStart: DateTime)

object DisplayTrustTrusteeOrgType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val trusteeOrgTypeFormat: Format[DisplayTrustTrusteeOrgType] = Json.format[DisplayTrustTrusteeOrgType]

  import uk.gov.hmrc.trusts.models.JsonWithoutNulls._

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
                                             dateOfBirth: Option[DateTime],
                                             phoneNumber: Option[String],
                                             identification: Option[DisplayTrustIdentificationType],
                                             entityStart: DateTime
                                            )

object DisplayTrustTrusteeIndividualType {

  import uk.gov.hmrc.trusts.models.JsonWithoutNulls._

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
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

case class DisplayTrustProtector(lineNo: String,
                                 bpMatchStatus: Option[String],
                                 name: NameType,
                                 dateOfBirth: Option[DateTime],
                                 identification: Option[DisplayTrustIdentificationType],
                                 entityStart: String)

object DisplayTrustProtector {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val protectorFormat: Format[DisplayTrustProtector] = Json.format[DisplayTrustProtector]
}

case class DisplayTrustProtectorCompany(lineNo: String,
                                        bpMatchStatus: Option[String],
                                        name: String,
                                        identification: Option[DisplayTrustIdentificationOrgType],
                                        entityStart: String)

object DisplayTrustProtectorCompany {
  implicit val protectorCompanyFormat: Format[DisplayTrustProtectorCompany] = Json.format[DisplayTrustProtectorCompany]
}


case class DisplayTrustSettlors(settlor: Option[List[DisplayTrustSettlor]],
                                settlorCompany: Option[List[DisplayTrustSettlorCompany]])

object DisplayTrustSettlors {
  implicit val settlorsFormat: Format[DisplayTrustSettlors] = Json.format[DisplayTrustSettlors]
}

case class DisplayTrustSettlor(lineNo: String,
                               bpMatchStatus: Option[String],
                               name: NameType,
                               dateOfBirth: Option[DateTime],
                               identification: Option[DisplayTrustIdentificationType],
                               entityStart: String)

object DisplayTrustSettlor {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val settlorFormat: Format[DisplayTrustSettlor] = Json.format[DisplayTrustSettlor]
}

case class DisplayTrustSettlorCompany(lineNo: String,
                                      bpMatchStatus: Option[String],
                                      name: String,
                                      companyType: Option[String],
                                      companyTime: Option[Boolean],
                                      identification: Option[DisplayTrustIdentificationOrgType],
                                      entityStart: String)

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
                                       partnershipStart: Option[DateTime])

object DisplayTrustPartnershipType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
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