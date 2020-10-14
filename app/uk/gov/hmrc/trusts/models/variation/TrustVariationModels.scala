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

package uk.gov.hmrc.trusts.models.variation

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.JsonWithoutNulls._
import uk.gov.hmrc.trusts.models._

case class TrustVariation(
                      matchData: MatchData,
                      correspondence: Correspondence,
                      declaration: Declaration,
                      details: Trust,
                      agentDetails: Option[AgentDetails] = None,
                      trustEndDate: Option[LocalDate],
                      reqHeader: ReqHeader,
                      submissionDate: Option[LocalDate]   // New to 5MLD variation, mandatory in 5MLD
                    )

object TrustVariation {

  val variationReads: Reads[TrustVariation] = {
    (
      (__ \ "matchData").read[MatchData] and
        (__ \ "correspondence").read[Correspondence] and
        (__ \ "declaration").read[Declaration] and
        (__ \ "details" \ "trust").read[Trust] and
        (__ \ "agentDetails").readNullable[AgentDetails] and
        (__ \ "trustEndDate").readNullable[LocalDate] and
        (__ \ "reqHeader").read[ReqHeader] and
        (__ \ "submissionDate").readNullable[LocalDate]
      ) (TrustVariation.apply _)
  }

  val writeToDes: Writes[TrustVariation] = (
    (JsPath \ "matchData").write[MatchData] and
      (JsPath \ "correspondence").write[Correspondence] and
      (JsPath \ "declaration").write[Declaration] and
      (JsPath \ "details" \ "trust").write[Trust] and
      (JsPath \ "agentDetails").writeNullable[AgentDetails] and
      (JsPath \ "trustEndDate").writeNullable[LocalDate] and
      (JsPath \ "reqHeader").write[ReqHeader] and
      (JsPath \ "submissionDate").writeNullable[LocalDate]
    ) (unlift(TrustVariation.unapply))

  implicit val variationFormat: Format[TrustVariation] = Format(variationReads, writeToDes)

}

// Both optional in display response
case class MatchData(utr: Option[String], urn: Option[String])

object MatchData {
  implicit val matchDataFormat: Format[MatchData] = Json.format[MatchData]
}

case class ReqHeader(formBundleNo: String)

object ReqHeader {
  implicit val reqHeaderFormat: Format[ReqHeader] = Json.format[ReqHeader]
}

case class Trust(
                  details: TrustDetailsType,
                  entities: TrustEntitiesType,
                  assets: Option[Assets]    // now optional with 5mld
                )

object Trust {
  implicit val trustFormat: Format[Trust] = Json.format[Trust]
}

case class TrustEntitiesType(
                              naturalPerson: Option[List[NaturalPersonType]],
                              beneficiary: BeneficiaryType,
                              deceased: Option[WillType],
                              leadTrustees: List[LeadTrusteeType],
                              trustees: Option[List[TrusteeType]],
                              protectors: Option[ProtectorsType],
                              settlors: Option[Settlors]
                            )

object TrustEntitiesType {
  implicit val trustEntitiesTypeFormat: Format[TrustEntitiesType] = Json.format[TrustEntitiesType]
}

case class NaturalPersonType(
                              lineNo: Option[String],
                              bpMatchStatus: Option[String],
                              name: NameType,
                              dateOfBirth: Option[LocalDate],
                              identification: Option[IdentificationType],
                              countryOfResidence: Option[String],    // new 5MLD optional
                              legallyIncapable: Option[Boolean],     // new 5MLD optional
                              nationality: Option[String],           // new 5MLD optional
                              entityStart: LocalDate,
                              entityEnd: Option[LocalDate]
                            )

object NaturalPersonType {
  implicit val naturalPersonTypeFormat: Format[NaturalPersonType] = Json.format[NaturalPersonType]

  val writeToMaintain : Writes[NaturalPersonType] = (o: NaturalPersonType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "name" -> o.name,
    "dateOfBirth" -> o.dateOfBirth,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "legallyIncapable" -> o.legallyIncapable,
    "nationality" -> o.nationality,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class BeneficiaryType(
                            individualDetails: Option[List[IndividualDetailsType]],
                            company: Option[List[BeneficiaryCompanyType]],
                            trust: Option[List[BeneficiaryTrustType]],
                            charity: Option[List[BeneficiaryCharityType]],
                            unidentified: Option[List[UnidentifiedType]],
                            large: Option[List[LargeType]],
                            other: Option[List[OtherType]]
                          )

object BeneficiaryType {
  implicit val beneficiaryTypeFormat: Format[BeneficiaryType] = Json.format[BeneficiaryType]
}

case class UnidentifiedType(lineNo: Option[String],
                            bpMatchStatus: Option[String],
                            description: String,
                            beneficiaryDiscretion: Option[Boolean],
                            beneficiaryShareOfIncome: Option[String],
                            entityStart: LocalDate,
                            entityEnd: Option[LocalDate])

object UnidentifiedType {
  implicit val unidentifiedTypeFormat: Format[UnidentifiedType] = Json.format[UnidentifiedType]

  val writeToMaintain : Writes[UnidentifiedType] = (o: UnidentifiedType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "description" -> o.description,
    "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
    "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class LargeType(lineNo: Option[String],
                     bpMatchStatus: Option[String],
                     organisationName: String,
                     description: String,
                     description1: Option[String],
                     description2: Option[String],
                     description3: Option[String],
                     description4: Option[String],
                     numberOfBeneficiary: String,
                     identification: Option[IdentificationOrgType],
                     beneficiaryDiscretion: Option[Boolean],
                     beneficiaryShareOfIncome: Option[String],
                     countryOfResidence: Option[String],    // new 5MLD optional
                     entityStart: LocalDate,
                     entityEnd: Option[LocalDate])

object LargeType {
  implicit val largeTypeFormat: Format[LargeType] = Json.format[LargeType]

  val writeToMaintain : Writes[LargeType] = (o: LargeType) => Json.obj(
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
    "countryOfResidence" -> o.countryOfResidence,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class OtherType(lineNo: Option[String],
                     bpMatchStatus: Option[String],
                     description: String,
                     address: Option[AddressType],
                     beneficiaryDiscretion: Option[Boolean],
                     beneficiaryShareOfIncome: Option[String],
                     countryOfResidence: Option[String],    // new 5MLD optional
                     entityStart: LocalDate,
                     entityEnd: Option[LocalDate])

object OtherType {
  implicit val otherTypeFormat: Format[OtherType] = Json.format[OtherType]

  val writeToMaintain : Writes[OtherType] = (o: OtherType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "description" -> o.description,
    "address" -> o.address,
    "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
    "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
    "countryOfResidence" -> o.countryOfResidence,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class IndividualDetailsType(
                                  lineNo: Option[String],
                                  bpMatchStatus: Option[String],
                                  name: NameType,
                                  dateOfBirth: Option[LocalDate],
                                  vulnerableBeneficiary: Boolean,       // actually optional in 5MLD schema
                                  beneficiaryType: Option[String],
                                  beneficiaryDiscretion: Option[Boolean],
                                  beneficiaryShareOfIncome: Option[String],
                                  identification: Option[IdentificationType],
                                  countryOfResidence: Option[String],    // new 5MLD optional
                                  legallyIncapable: Option[Boolean],     // new 5MLD optional
                                  nationality: Option[String],           // new 5MLD optional
                                  entityStart: LocalDate,
                                  entityEnd: Option[LocalDate]
                                )

object IndividualDetailsType {
  implicit val individualDetailsTypeFormat: Format[IndividualDetailsType] = Json.format[IndividualDetailsType]
  val writeToMaintain : Writes[IndividualDetailsType] = (o: IndividualDetailsType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "name" -> o.name,
    "dateOfBirth" -> o.dateOfBirth,
    "vulnerableBeneficiary" -> o.vulnerableBeneficiary,
    "beneficiaryType" -> o.beneficiaryType,
    "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
    "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "legallyIncapable" -> o.legallyIncapable,
    "nationality" -> o.nationality,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class BeneficiaryCompanyType(lineNo: Option[String],
                                  bpMatchStatus: Option[String],
                                  organisationName: String,
                                  beneficiaryDiscretion: Option[Boolean],
                                  beneficiaryShareOfIncome: Option[String],
                                  identification: Option[IdentificationOrgType],
                                  countryOfResidence: Option[String],   // new 5MLD optional
                                  entityStart: LocalDate,
                                  entityEnd: Option[LocalDate])

object BeneficiaryCompanyType {
  implicit val companyTypeFormat: Format[BeneficiaryCompanyType] = Json.format[BeneficiaryCompanyType]

  val writeToMaintain : Writes[BeneficiaryCompanyType] = (o: BeneficiaryCompanyType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "organisationName" -> o.organisationName,
    "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
    "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class BeneficiaryTrustType(lineNo: Option[String],
                                bpMatchStatus: Option[String],
                                organisationName: String,
                                beneficiaryDiscretion: Option[Boolean],
                                beneficiaryShareOfIncome: Option[String],
                                identification: Option[IdentificationOrgType],
                                countryOfResidence: Option[String],   // new 5MLD optional
                                entityStart: LocalDate,
                                entityEnd: Option[LocalDate])

object BeneficiaryTrustType {
  implicit val beneficiaryTrustTypeFormat: Format[BeneficiaryTrustType] = Json.format[BeneficiaryTrustType]

  val writeToMaintain : Writes[BeneficiaryTrustType] = (o: BeneficiaryTrustType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "organisationName" -> o.organisationName,
    "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
    "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class BeneficiaryCharityType(lineNo: Option[String],
                                  bpMatchStatus: Option[String],
                                  organisationName: String,
                                  beneficiaryDiscretion: Option[Boolean],
                                  beneficiaryShareOfIncome: Option[String],
                                  identification: Option[IdentificationOrgType],
                                  countryOfResidence: Option[String],   // new 5MLD optional
                                  entityStart: LocalDate,
                                  entityEnd: Option[LocalDate])

object BeneficiaryCharityType {
  implicit val charityTypeFormat: Format[BeneficiaryCharityType] = Json.format[BeneficiaryCharityType]

  val writeToMaintain : Writes[BeneficiaryCharityType] = (o: BeneficiaryCharityType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "organisationName" -> o.organisationName,
    "beneficiaryDiscretion" -> o.beneficiaryDiscretion,
    "beneficiaryShareOfIncome" -> o.beneficiaryShareOfIncome,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class WillType(
                     lineNo: Option[String],
                     bpMatchStatus: Option[String],
                     name: NameType,
                     dateOfBirth: Option[LocalDate],
                     dateOfDeath: Option[LocalDate],
                     identification: Option[IdentificationType],
                     countryOfResidence: Option[String],    // new 5MLD optional
                     nationality: Option[String],           // new 5MLD optional
                     entityStart: LocalDate,
                     entityEnd: Option[LocalDate]
                   )

object WillType {
  implicit val willTypeFormat: Format[WillType] = Json.format[WillType]
}

case class LeadTrusteeIndType(
                               lineNo: Option[String],
                               bpMatchStatus: Option[String],
                               name: NameType,
                               dateOfBirth: LocalDate,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationType,
                               countryOfResidence: Option[String],    // new 5MLD optional
                               legallyIncapable: Option[Boolean],     // new 5MLD optional
                               nationality: Option[String],           // new 5MLD optional
                               entityStart: LocalDate,
                               entityEnd: Option[LocalDate]
                             )

object LeadTrusteeIndType {
  implicit val leadTrusteeIndTypeFormat: Format[LeadTrusteeIndType] = Json.format[LeadTrusteeIndType]
}

case class LeadTrusteeOrgType(
                               lineNo: Option[String],
                               bpMatchStatus: Option[String],
                               name: String,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationOrgType,
                               countryOfResidence: Option[String],    // new 5MLD optional
                               entityStart: LocalDate,
                               entityEnd: Option[LocalDate]
                             )

object LeadTrusteeOrgType {
  implicit val leadTrusteeOrgTypeFormat: Format[LeadTrusteeOrgType] = Json.format[LeadTrusteeOrgType]
}

case class LeadTrusteeType(
                            leadTrusteeInd: Option[LeadTrusteeIndType] = None,
                            leadTrusteeOrg: Option[LeadTrusteeOrgType] = None
                          )

object LeadTrusteeType {
  implicit val leadTrusteeFormats: Format[LeadTrusteeType] = Json.format[LeadTrusteeType]
}

case class TrusteeType(
                        trusteeInd: Option[TrusteeIndividualType],
                        trusteeOrg: Option[TrusteeOrgType]
                      )

object TrusteeType {
  implicit val trusteeTypeFormat: Format[TrusteeType] = Json.format[TrusteeType]
}

case class TrusteeOrgType(
                           lineNo: Option[String],
                           bpMatchStatus: Option[String],
                           name: String,
                           phoneNumber: Option[String] = None,
                           email: Option[String] = None,
                           identification: Option[IdentificationOrgType],
                           countryOfResidence: Option[String],    // new 5MLD optional
                           entityStart: LocalDate,
                           entityEnd: Option[LocalDate]
                         )

object TrusteeOrgType {

  implicit val trusteeOrgTypeFormat: Format[TrusteeOrgType] = Json.format[TrusteeOrgType]

  val writeToMaintain : Writes[TrusteeOrgType] = (o: TrusteeOrgType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "name" -> o.name,
    "phoneNumber" -> o.phoneNumber,
    "email" -> o.email,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class TrusteeIndividualType(
                                  lineNo: Option[String],
                                  bpMatchStatus: Option[String],
                                  name: NameType,
                                  dateOfBirth: Option[LocalDate],
                                  phoneNumber: Option[String],
                                  identification: Option[IdentificationType],
                                  countryOfResidence: Option[String],    // new 5MLD optional
                                  legallyIncapable: Option[Boolean],     // new 5MLD optional
                                  nationality: Option[String],           // new 5MLD optional
                                  entityStart: LocalDate,
                                  entityEnd: Option[LocalDate]
                                )

object TrusteeIndividualType {

  implicit val trusteeIndividualTypeFormat: Format[TrusteeIndividualType] = Json.format[TrusteeIndividualType]

  val writeToMaintain : Writes[TrusteeIndividualType] = (o: TrusteeIndividualType) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "name" -> o.name,
    "dateOfBirth" -> o.dateOfBirth,
    "phoneNumber" -> o.phoneNumber,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "legallyIncapable" -> o.legallyIncapable,
    "nationality" -> o.nationality,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class ProtectorsType(protector: Option[List[Protector]],
                          protectorCompany: Option[List[ProtectorCompany]])

object ProtectorsType {
  implicit val protectorsTypeFormat: Format[ProtectorsType] = Json.format[ProtectorsType]
}

case class Protector(
                      lineNo: Option[String],
                      bpMatchStatus: Option[String],
                      name: NameType,
                      dateOfBirth: Option[LocalDate],
                      identification: Option[IdentificationType],
                      countryOfResidence: Option[String],    // new 5MLD optional
                      legallyIncapable: Option[Boolean],     // new 5MLD optional
                      nationality: Option[String],           // new 5MLD optional
                      entityStart: LocalDate,
                      entityEnd: Option[LocalDate]
                    )

object Protector {
  implicit val protectorFormat: Format[Protector] = Json.format[Protector]

  val writeToMaintain : Writes[Protector] = (o: Protector) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "name" -> o.name,
    "dateOfBirth" -> o.dateOfBirth,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "legallyIncapable" -> o.legallyIncapable,
    "nationality" -> o.nationality,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class ProtectorCompany(
                             lineNo: Option[String],
                             bpMatchStatus: Option[String],
                             name: String,
                             identification: Option[IdentificationOrgType],
                             countryOfResidence: Option[String],    // new 5MLD optional
                             entityStart: LocalDate,
                             entityEnd: Option[LocalDate]
                           )

object ProtectorCompany {

  implicit val protectorCompanyFormat: Format[ProtectorCompany] = Json.format[ProtectorCompany]

  val writeToMaintain : Writes[ProtectorCompany] = (o: ProtectorCompany) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "name" -> o.name,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class Settlors(
                     settlor: Option[List[Settlor]],
                     settlorCompany: Option[List[SettlorCompany]]
                   )

object Settlors {
  implicit val settlorsFormat: Format[Settlors] = Json.format[Settlors]
}

case class Settlor(
                    lineNo: Option[String],
                    bpMatchStatus: Option[String],
                    name: NameType,
                    dateOfBirth: Option[LocalDate],
                    identification: Option[IdentificationType],
                    countryOfResidence: Option[String],    // new 5MLD optional
                    legallyIncapable: Option[Boolean],     // new 5MLD optional
                    nationality: Option[String],           // new 5MLD optional
                    entityStart: LocalDate,
                    entityEnd: Option[LocalDate]
                  )

object Settlor {
  implicit val settlorFormat: Format[Settlor] = Json.format[Settlor]

  val writeToMaintain : Writes[Settlor] = (o: Settlor) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "name" -> o.name,
    "dateOfBirth" -> o.dateOfBirth,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "legallyIncapable" -> o.legallyIncapable,
    "nationality" -> o.nationality,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class SettlorCompany(
                           lineNo: Option[String],
                           bpMatchStatus: Option[String],
                           name: String,
                           companyType: Option[String],
                           companyTime: Option[Boolean],
                           identification: Option[IdentificationOrgType],
                           countryOfResidence: Option[String],    // new 5MLD optional
                           entityStart: LocalDate,
                           entityEnd: Option[LocalDate]
                         )

object SettlorCompany {
  implicit val settlorCompanyFormat: Format[SettlorCompany] = Json.format[SettlorCompany]

  val writeToMaintain : Writes[SettlorCompany] = (o: SettlorCompany) => Json.obj(
    "lineNo" -> o.lineNo,
    "bpMatchStatus" -> o.bpMatchStatus,
    "name" -> o.name,
    "companyType" -> o.companyType,
    "companyTime" -> o.companyTime,
    "identification" -> o.identification,
    "countryOfResidence" -> o.countryOfResidence,
    "entityStart" -> o.entityStart,
    "entityEnd" -> o.entityEnd,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

case class Assets(
                   monetary: Option[List[AssetMonetaryAmount]],
                   propertyOrLand: Option[List[PropertyLandType]],
                   shares: Option[List[SharesType]],
                   business: Option[List[BusinessAssetType]],
                   partnerShip: Option[List[PartnershipType]],
                   other: Option[List[OtherAssetType]],
                   nonEEABusiness: Option[List[NonEEABusinessType]]
                 )

object Assets {
  implicit val assetsFormat: Format[Assets] = Json.format[Assets]
}

case class PropertyLandType(buildingLandName: Option[String],
                            address: Option[AddressType],
                            valueFull: Long,
                            valuePrevious: Option[Long])

object PropertyLandType {
  implicit val propertyLandTypeFormat: Format[PropertyLandType] = Json.format[PropertyLandType]
}

case class SharesType(
                       numberOfShares: Option[String],
                       orgName: String,
                       utr: Option[String],
                       shareClass: Option[String],
                       typeOfShare: Option[String],
                       value: Option[Long]
                     )

object SharesType {
  implicit val sharesTypeFormat: Format[SharesType] = Json.format[SharesType]
}

case class BusinessAssetType(
                              utr: Option[String],
                              orgName: String,
                              businessDescription: String,
                              address: Option[AddressType],
                              businessValue: Option[Long]
                            )

object BusinessAssetType {
  implicit val businessAssetTypeFormat: Format[BusinessAssetType] = Json.format[BusinessAssetType]
}

case class PartnershipType(
                            utr: Option[String],
                            description: String,
                            partnershipStart: Option[LocalDate]
                          )

object PartnershipType {

  implicit val partnershipTypeFormat: Format[PartnershipType] = Json.format[PartnershipType]
}

case class OtherAssetType(description: String,
                          value: Option[Long])

object OtherAssetType {
  implicit val otherAssetTypeFormat: Format[OtherAssetType] = Json.format[OtherAssetType]
}


case class IdentificationType(nino: Option[String],
                              passport: Option[PassportType],
                              address: Option[AddressType],
                              safeId: Option[String])

object IdentificationType {
  implicit val identificationTypeFormat: Format[IdentificationType] = Json.format[IdentificationType]
}


case class IdentificationOrgType(utr: Option[String],
                                 address: Option[AddressType],
                                 safeId: Option[String])

object IdentificationOrgType {
  implicit val trustBeneficiaryIdentificationFormat: Format[IdentificationOrgType] = Json.format[IdentificationOrgType]
}

// new 5MLD type
case class NonEEABusinessType(lineNo: String,
                               orgName: String,
                               address: AddressType,
                               govLawCountry: String,
                               startDate: LocalDate,
                               endDate: Option[LocalDate])

object NonEEABusinessType {
  implicit val format: Format[NonEEABusinessType] = Json.format[NonEEABusinessType]
}

