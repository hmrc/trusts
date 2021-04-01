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

package models.get_trust

import java.time.LocalDate
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.Constants._
import utils.DeedOfVariation.DeedOfVariation
import utils.TypeOfTrust.TypeOfTrust

case class GetTrustDesResponse(getTrust: Option[GetTrust],
                               responseHeader: ResponseHeader)

object GetTrustDesResponse {
  implicit val writes: Writes[GetTrustDesResponse] = Json.writes[GetTrustDesResponse]
  implicit val reads: Reads[GetTrustDesResponse] = (
    (JsPath \ TRUST_OR_ESTATE_DISPLAY).readNullable[GetTrust] and
      (JsPath \ RESPONSE_HEADER).read[ResponseHeader]
    ) (GetTrustDesResponse.apply _)
}

case class ResponseHeader(status: String,
                          formBundleNo: String)

object ResponseHeader {
  implicit val apiWrites: Writes[ResponseHeader] = Json.writes[ResponseHeader]

  val mongoWrites: Writes[ResponseHeader] = (header: ResponseHeader) => Json.obj(
    DFMCA_RETURN_USER_STATUS -> header.status,
    FORM_BUNDLE_NUMBER -> header.formBundleNo
  )

  implicit val reads: Reads[ResponseHeader] = (
    (JsPath \ DFMCA_RETURN_USER_STATUS).read[String] and
      (JsPath \ FORM_BUNDLE_NUMBER).read[String]
    )(ResponseHeader.apply _)
}

// Both optional in display response
case class MatchData(utr: Option[String], urn: Option[String])

object MatchData {
  implicit val matchDataFormat: Format[MatchData] = Json.format[MatchData]
}

case class GetTrust(matchData: MatchData,
                    correspondence: Correspondence,
                    declaration: Declaration,
                    trust: DisplayTrust,
                    submissionDate: Option[LocalDate], // New to 5MLD response, mandatory in 5MLD
                    yearsReturns: Option[YearsReturns])

object GetTrust {
  implicit val writes: Writes[GetTrust] = Json.writes[GetTrust]

  implicit val reads: Reads[GetTrust] = (
    MATCH_DATA.read[MatchData] and
      CORRESPONDENCE.read[Correspondence] and
      DECLARATION.read[Declaration] and
      TRUST.read[DisplayTrust] and
      SUBMISSION_DATE.readNullable[LocalDate] and
      YEARS_RETURNS.readNullable[YearsReturns]
    ) (GetTrust.apply _)
}

case class Correspondence(abroadIndicator: Boolean,
                          name: String,
                          address: AddressType,
                          phoneNumber: String,
                          welsh: Option[Boolean],   // new 5MLD optional
                          braille: Option[Boolean]) // new 5MLD optional

object Correspondence {
  implicit val correspondenceFormat : Format[Correspondence] = Json.format[Correspondence]

}

case class Declaration(name: NameType,
                       address: AddressType)

object Declaration {
  implicit val declarationFormat: Format[Declaration] = Json.format[Declaration]
}

case class DisplayTrust(
                         details: TrustDetailsType,
                         entities: DisplayTrustEntitiesType,
                         assets: Option[DisplayTrustAssets])    // now optional with 5mld

object DisplayTrust {
  implicit val trustFormat: Format[DisplayTrust] = Json.format[DisplayTrust]
}

case class TrustDetailsType(startDate: LocalDate,
                            lawCountry: Option[String],
                            administrationCountry: Option[String],
                            residentialStatus: Option[ResidentialStatusType],
                            typeOfTrust: Option[TypeOfTrust],     // now optional with 5MLD
                            deedOfVariation: Option[DeedOfVariation],
                            interVivos: Option[Boolean],
                            efrbsStartDate: Option[LocalDate],
                            trustTaxable: Option[Boolean],        // new 5MLD required
                            expressTrust: Option[Boolean],        // new 5MLD required
                            trustUKResident: Option[Boolean],     // new 5MLD required
                            trustUKProperty: Option[Boolean],      // new 5MLD optional
                            trustRecorded: Option[Boolean],       // new 5MLD required
                            trustUKRelation: Option[Boolean]      // new 5MLD required
                           )

object TrustDetailsType {
  implicit val trustDetailsTypeFormat: Format[TrustDetailsType] = Json.format[TrustDetailsType]
}

case class ResidentialStatusType(uk: Option[UkType],
                                 nonUK: Option[NonUKType])

object ResidentialStatusType {
  implicit val residentialStatusTypeFormat: Format[ResidentialStatusType] = Json.format[ResidentialStatusType]
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
    (__ \ OTHER_INDIVIDUALS).readNullable[List[DisplayTrustNaturalPersonType]] and
    (__ \ BENEFICIARIES).read[DisplayTrustBeneficiaryType] and
    (__ \ DECEASED_SETTLOR).readNullable[DisplayTrustWillType] and
    (__ \ LEAD_TRUSTEE).read[DisplayTrustLeadTrusteeType] and
    (__ \ TRUSTEES).readNullable[List[DisplayTrustTrusteeType]] and
    (__ \ PROTECTORS).readNullable[DisplayTrustProtectorsType] and
    (__ \ SETTLORS).readNullable[DisplayTrustSettlors]
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
                                         countryOfResidence: Option[String],    // new 5MLD optional
                                         legallyIncapable: Option[Boolean],     // new 5MLD optional
                                         nationality: Option[String],           // new 5MLD optional
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
                                           countryOfResidence: Option[String],    // new 5MLD optional
                                           legallyIncapable: Option[Boolean],     // new 5MLD optional
                                           nationality: Option[String],           // new 5MLD optional
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
                                           countryOfResidence: Option[String],    // new 5MLD optional
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
                                             vulnerableBeneficiary: Option[Boolean], // Optional in 5MLD for non-tax trusts
                                             beneficiaryType: Option[String],
                                             beneficiaryDiscretion: Option[Boolean],
                                             beneficiaryShareOfIncome: Option[String],
                                             identification: Option[DisplayTrustIdentificationType],
                                             countryOfResidence: Option[String],    // new 5MLD optional
                                             legallyIncapable: Option[Boolean],     // new 5MLD optional
                                             nationality: Option[String],           // new 5MLD optional
                                             entityStart: String)

object DisplayTrustIndividualDetailsType {
  implicit val individualDetailsTypeFormat: Format[DisplayTrustIndividualDetailsType] = Json.format[DisplayTrustIndividualDetailsType]
}

case class DisplayTrustBeneficiaryCompanyType(lineNo: Option[String],
                                              bpMatchStatus: Option[String],
                                              organisationName: String,
                                              beneficiaryDiscretion: Option[Boolean],
                                              beneficiaryShareOfIncome: Option[String],
                                              identification: Option[DisplayTrustIdentificationOrgType],
                                              countryOfResidence: Option[String],   // new 5MLD optional
                                              entityStart: String)

object DisplayTrustBeneficiaryCompanyType {
  implicit val companyTypeFormat: Format[DisplayTrustBeneficiaryCompanyType] = Json.format[DisplayTrustBeneficiaryCompanyType]
}

case class DisplayTrustWillType(lineNo: String,
                                bpMatchStatus: Option[String],
                                name: NameType,
                                dateOfBirth: Option[LocalDate],
                                dateOfDeath: Option[LocalDate],
                                identification: Option[DisplayTrustIdentificationType],
                                countryOfResidence: Option[String],    // new 5MLD optional
                                nationality: Option[String],           // new 5MLD optional
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
                                            countryOfResidence: Option[String],   // new 5MLD optional
                                            entityStart: String)

object DisplayTrustBeneficiaryTrustType {
  implicit val beneficiaryTrustTypeFormat: Format[DisplayTrustBeneficiaryTrustType] = Json.format[DisplayTrustBeneficiaryTrustType]
}

case class DisplayTrustCharityType(lineNo: Option[String],
                                   bpMatchStatus: Option[String],
                                   organisationName: String,
                                   beneficiaryDiscretion: Option[Boolean],
                                   beneficiaryShareOfIncome: Option[String],
                                   identification: Option[DisplayTrustIdentificationOrgType],
                                   countryOfResidence: Option[String],   // new 5MLD optional
                                   entityStart: String)

object DisplayTrustCharityType {
  implicit val charityTypeFormat: Format[DisplayTrustCharityType] = Json.format[DisplayTrustCharityType]
}


case class DisplayTrustUnidentifiedType(lineNo: Option[String],
                                        bpMatchStatus: Option[String],
                                        description: String,
                                        beneficiaryDiscretion: Option[Boolean],
                                        beneficiaryShareOfIncome: Option[String],
                                        entityStart: String)

object DisplayTrustUnidentifiedType {
  implicit val unidentifiedTypeFormat: Format[DisplayTrustUnidentifiedType] = Json.format[DisplayTrustUnidentifiedType]
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
                                 countryOfResidence: Option[String],    // new 5MLD optional
                                 entityStart: String)

object DisplayTrustLargeType {
  implicit val largeTypeFormat: Format[DisplayTrustLargeType] = Json.format[DisplayTrustLargeType]
}

case class DisplayTrustOtherType(lineNo: Option[String],
                                 bpMatchStatus: Option[String],
                                 description: String,
                                 address: Option[AddressType],
                                 beneficiaryDiscretion: Option[Boolean],
                                 beneficiaryShareOfIncome: Option[String],
                                 countryOfResidence: Option[String],    // new 5MLD optional
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
                                      countryOfResidence: Option[String],    // new 5MLD optional
                                      entityStart: LocalDate)

object DisplayTrustTrusteeOrgType {
  implicit val trusteeOrgTypeFormat: Format[DisplayTrustTrusteeOrgType] = Json.format[DisplayTrustTrusteeOrgType]
}

case class DisplayTrustTrusteeIndividualType(lineNo: Option[String],
                                             bpMatchStatus: Option[String],
                                             name: NameType,
                                             dateOfBirth: Option[LocalDate],
                                             phoneNumber: Option[String],
                                             identification: Option[DisplayTrustIdentificationType],
                                             countryOfResidence: Option[String],    // new 5MLD optional
                                             legallyIncapable: Option[Boolean],     // new 5MLD optional
                                             nationality: Option[String],           // new 5MLD optional
                                             entityStart: LocalDate
                                            )

object DisplayTrustTrusteeIndividualType {
  implicit val trusteeIndividualTypeFormat: Format[DisplayTrustTrusteeIndividualType] = Json.format[DisplayTrustTrusteeIndividualType]
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
                                 countryOfResidence: Option[String],    // new 5MLD optional
                                 legallyIncapable: Option[Boolean],     // new 5MLD optional
                                 nationality: Option[String],           // new 5MLD optional
                                 entityStart: LocalDate)

object DisplayTrustProtector {
  implicit val protectorFormat: Format[DisplayTrustProtector] = Json.format[DisplayTrustProtector]
}

case class DisplayTrustProtectorCompany(lineNo: Option[String],
                                        bpMatchStatus: Option[String],
                                        name: String,
                                        identification: Option[DisplayTrustIdentificationOrgType],
                                        countryOfResidence: Option[String],    // new 5MLD optional
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
                               countryOfResidence: Option[String],    // new 5MLD optional
                               legallyIncapable: Option[Boolean],     // new 5MLD optional
                               nationality: Option[String],           // new 5MLD optional
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
                                      countryOfResidence: Option[String],    // new 5MLD optional
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
                              other: Option[List[DisplayOtherAssetType]],
                              nonEEABusiness: Option[List[DisplayNonEEABusinessType]])

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

// new 5MLD type
case class DisplayNonEEABusinessType(lineNo: Option[String],
                                     orgName: String,
                                     address: AddressType,
                                     govLawCountry: String,
                                     startDate: LocalDate,
                                     endDate: Option[LocalDate])

object DisplayNonEEABusinessType {
  implicit val format: Format[DisplayNonEEABusinessType] = Json.format[DisplayNonEEABusinessType]
}

