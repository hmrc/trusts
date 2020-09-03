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

package uk.gov.hmrc.trusts.models

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.utils.DeedOfVariation.DeedOfVariation
import uk.gov.hmrc.trusts.utils.TypeOfTrust.{Employment, TypeOfTrust}
/**
  * DES API Schema - definitions models below
  */

case class Registration(matchData: Option[MatchData],
                        correspondence: Correspondence,
                        yearsReturns: Option[YearsReturns],
                        declaration: Declaration,
                        trust: Trust,
                        submissionDate: Option[LocalDate],      // new 5MLD required
                        agentDetails: Option[AgentDetails] = None
                       )

object Registration {
 implicit val registrationReads :Reads[Registration] = Json.reads[Registration]
 implicit val writeToDes :Writes[Registration] = (
    (JsPath \ "matchData").writeNullable[MatchData] and
    (JsPath \ "correspondence").write[Correspondence] and
    (JsPath \ "declaration").write[Declaration] and
    (JsPath \ "yearsReturns").writeNullable[YearsReturns] and
    (JsPath \ "details" \ "trust").write[Trust] and
    (JsPath \ "agentDetails" ).writeNullable[AgentDetails]
  )(r => (r.matchData, r.correspondence,r.declaration, r.yearsReturns, r.trust,r.agentDetails))
}

case class Details(trust: Trust)

object Details {
  implicit val detailsFormat: Format[Details] = Json.format[Details]
}

case class MatchData(utr: String,
                     name: String,
                     postCode: Option[String]
                    )

object MatchData {
  implicit val matchDataFormat: Format[MatchData] = Json.format[MatchData]
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

case class YearsReturns(var returns: Option[List[YearReturnType]])

object YearsReturns {
  implicit val yearsReturnsFormat: Format[YearsReturns] = Json.format[YearsReturns]
}

case class Assets(monetary: Option[List[AssetMonetaryAmount]],
                  propertyOrLand: Option[List[PropertyLandType]],
                  shares: Option[List[SharesType]],
                  business: Option[List[BusinessAssetType]],
                  partnerShip: Option[List[PartnershipType]],
                  other: Option[List[OtherAssetType]],
                  nonEEABusiness: Option[List[NonEEABusinessType]])   // new 5MLD optional

object Assets {
  implicit val assetsFormat: Format[Assets] = Json.format[Assets]
}

case class AssetMonetaryAmount(assetMonetaryAmount: Long)

object AssetMonetaryAmount {
  implicit val assetMonetaryAmountFormat: Format[AssetMonetaryAmount] = Json.format[AssetMonetaryAmount]
}

case class Declaration(name: NameType,
                       address: AddressType)

object Declaration {
  implicit val declarationFormat: Format[Declaration] = Json.format[Declaration]
}

case class DeclarationName(name: NameType)

object DeclarationName {
  implicit val declarationFormat: Format[DeclarationName] = Json.format[DeclarationName]
}

case class DeclarationForApi(declaration: DeclarationName,
                             agentDetails: Option[AgentDetails],
                             endDate: Option[LocalDate])

object DeclarationForApi {
  implicit val declarationForApiFormat: Format[DeclarationForApi] = Json.format[DeclarationForApi]
}

case class Trust(
                  details: TrustDetailsType,
                  entities: TrustEntitiesType,
                  assets: Option[Assets])   // Now optional with 5MLD

object Trust {
  implicit val trustFormat: Format[Trust] = Json.format[Trust]
}

case class TrustEntitiesType(naturalPerson: Option[List[NaturalPersonType]],
                             beneficiary: BeneficiaryType,
                             deceased: Option[WillType],
                             leadTrustees: LeadTrusteeType,
                             trustees: Option[List[TrusteeType]],
                             protectors: Option[ProtectorsType],
                             settlors: Option[Settlors])
object TrustEntitiesType {
  implicit val trustEntitiesTypeFormat: Format[TrustEntitiesType] = Json.format[TrustEntitiesType]
}

case class ProtectorsType(protector: Option[List[Protector]],
                          protectorCompany: Option[List[ProtectorCompany]])

object ProtectorsType {
  implicit val protectorsTypeFormat: Format[ProtectorsType] = Json.format[ProtectorsType]
}

case class Protector(name: NameType,
                     dateOfBirth: Option[LocalDate],
                     identification: Option[IdentificationType],
                     countryOfResidence: Option[String],  // new 5MLD optional
                     nationality: Option[String],         // new 5MLD optional
                     legallyIncapable: Option[Boolean])   // new 5MLD optional

object Protector {
  implicit val protectorFormat: Format[Protector] = Json.format[Protector]
}

case class ProtectorCompany(name: String,
                            identification: Option[IdentificationOrgType],
                            countryOfResidence: Option[String])  // new 5MLD optional

object ProtectorCompany {
  implicit val protectorCompanyFormat: Format[ProtectorCompany] = Json.format[ProtectorCompany]
}



case class TrusteeType(trusteeInd : Option[TrusteeIndividualType], trusteeOrg : Option[TrusteeOrgType] )
object TrusteeType {
  implicit val trusteeTypeFormat : Format[TrusteeType] = Json.format[TrusteeType]
}

case class TrusteeOrgType(name: String,
                          phoneNumber: Option[String] = None,
                          email: Option[String] = None,
                          identification: Option[IdentificationOrgType],
                          countryOfResidence: Option[String])    // new 5MLD optional

object TrusteeOrgType {
  implicit val trusteeOrgTypeFormat: Format[TrusteeOrgType] = Json.format[TrusteeOrgType]
}

case class TrusteeIndividualType(name: NameType,
                       dateOfBirth: Option[LocalDate],
                       phoneNumber: Option[String],
                       identification: Option[IdentificationType],
                       countryOfResidence: Option[String],  // new 5MLD optional
                       nationality: Option[String],         // new 5MLD optional
                       legallyIncapable: Option[Boolean])   // new 5MLD optional


object TrusteeIndividualType {
  implicit val trusteeIndividualTypeFormat : Format[TrusteeIndividualType] = Json.format[TrusteeIndividualType]
}

case class Settlors(settlor: Option[List[Settlor]],
                    settlorCompany: Option[List[SettlorCompany]])

object Settlors {
  implicit val settlorsFormat: Format[Settlors] = Json.format[Settlors]
}

case class Settlor(name: NameType,
                   dateOfBirth: Option[LocalDate],
                   identification: Option[IdentificationType],
                   countryOfResidence: Option[String],  // new 5MLD optional
                   nationality: Option[String],         // new 5MLD optional
                   legallyIncapable: Option[Boolean])   // new 5MLD optional


object Settlor {
  implicit val settlorFormat: Format[Settlor] = Json.format[Settlor]
}

case class SettlorCompany(name: String,
                          companyType: Option[String],
                          companyTime: Option[Boolean],
                          identification: Option[IdentificationOrgType],
                          countryOfResidence: Option[String])  // new 5MLD optional


object SettlorCompany {
  implicit val settlorCompanyFormat: Format[SettlorCompany] = Json.format[SettlorCompany]
}

case class LeadTrusteeIndType (
                            name: NameType,
                            dateOfBirth: LocalDate,
                            phoneNumber: String,
                            email: Option[String] = None,
                            identification: IdentificationType,
                            countryOfResidence: Option[String],  // new 5MLD required
                            nationality: Option[String],         // new 5MLD required
                            legallyIncapable: Option[Boolean]    // new 5MLD optional
)

object LeadTrusteeIndType {
  implicit val leadTrusteeIndTypeFormat: Format[LeadTrusteeIndType] = Json.format[LeadTrusteeIndType]
}

case class LeadTrusteeOrgType(
                               name: String,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationOrgType,
                               countryOfResidence: Option[String])    // new 5MLD required

object LeadTrusteeOrgType {
  implicit val leadTrusteeOrgTypeFormat: Format[LeadTrusteeOrgType] = Json.format[LeadTrusteeOrgType]
}

case class LeadTrusteeType(
                            leadTrusteeInd : Option[LeadTrusteeIndType] = None,
                            leadTrusteeOrg : Option[LeadTrusteeOrgType] = None
                          )

object LeadTrusteeType {
  implicit val leadTrusteeTypeReads:Reads[LeadTrusteeType] = Json.reads[LeadTrusteeType]

  // For when you need normal writes.
  val writes:Writes[LeadTrusteeType] = Json.writes[LeadTrusteeType]

  implicit val leadTrusteeWritesToDes : Writes[LeadTrusteeType] = Writes {
    leadTrustee=> leadTrustee.leadTrusteeInd match {
      case Some(indLeadTrutee) => Json.toJson(indLeadTrutee)
      case None => Json.toJson(leadTrustee.leadTrusteeOrg)
    }
  }
}


case class BeneficiaryType(individualDetails: Option[List[IndividualDetailsType]],
                           company: Option[List[BeneficiaryCompanyType]],
                           trust: Option[List[BeneficiaryTrustType]],
                           charity: Option[List[BeneficiaryCharityType]],
                           unidentified: Option[List[UnidentifiedType]],
                           large: Option[List[LargeType]],
                           other: Option[List[OtherType]])

object BeneficiaryType {
  implicit val beneficiaryTypeFormat: Format[BeneficiaryType] = Json.format[BeneficiaryType]
}

case class IndividualDetailsType(name: NameType,
                                 dateOfBirth: Option[LocalDate],
                                 vulnerableBeneficiary: Option[Boolean],  // Now optional in 5MLD
                                 beneficiaryType: Option[String],
                                 beneficiaryDiscretion: Option[Boolean],
                                 beneficiaryShareOfIncome: Option[String],
                                 identification: Option[IdentificationType],
                                 countryOfResidence: Option[String],  // new 5MLD optional
                                 nationality: Option[String],         // new 5MLD optional
                                 legallyIncapable: Option[Boolean])   // new 5MLD optional


object IndividualDetailsType {
  implicit val individualDetailsTypeFormat: Format[IndividualDetailsType] = Json.format[IndividualDetailsType]
}

case class BeneficiaryTrustType(organisationName: String,
                                beneficiaryDiscretion: Option[Boolean],
                                beneficiaryShareOfIncome: Option[String],
                                identification: Option[IdentificationOrgType],
                                countryOfResidence: Option[String])    // new 5MLD optional

object BeneficiaryTrustType {
  implicit val beneficiaryTrustTypeFormat: Format[BeneficiaryTrustType] = Json.format[BeneficiaryTrustType]
}

case class IdentificationOrgType(utr: Option[String],
                                          address: Option[AddressType])

object IdentificationOrgType {
  implicit val trustBeneficiaryIdentificationFormat: Format[IdentificationOrgType] = Json.format[IdentificationOrgType]
}

case class BeneficiaryCharityType(organisationName: String,
                                  beneficiaryDiscretion: Option[Boolean],
                                  beneficiaryShareOfIncome: Option[String],
                                  identification: Option[IdentificationOrgType],
                                  countryOfResidence: Option[String])    // new 5MLD optional

object BeneficiaryCharityType {
  implicit val charityTypeFormat: Format[BeneficiaryCharityType] = Json.format[BeneficiaryCharityType]
}

case class UnidentifiedType(description: String,
                            beneficiaryDiscretion: Option[Boolean],
                            beneficiaryShareOfIncome: Option[String])

object UnidentifiedType {
  implicit val unidentifiedTypeFormat: Format[UnidentifiedType] = Json.format[UnidentifiedType]
}

case class LargeType(organisationName: String,
                     description: String,
                     description1: Option[String],
                     description2: Option[String],
                     description3: Option[String],
                     description4: Option[String],
                     numberOfBeneficiary: String,
                     identification: Option[IdentificationOrgType],
                     beneficiaryDiscretion: Option[Boolean],
                     beneficiaryShareOfIncome: Option[String],
                     countryOfResidence: Option[String])    // new 5MLD optional

object LargeType {
  implicit val largeTypeFormat: Format[LargeType] = Json.format[LargeType]
}

case class OtherType(description: String,
                     address: Option[AddressType],
                     beneficiaryDiscretion: Option[Boolean],
                     beneficiaryShareOfIncome: Option[String],
                     countryOfResidence: Option[String])    // new 5MLD optional

object OtherType {
  implicit val otherTypeFormat: Format[OtherType] = Json.format[OtherType]
}

case class BeneficiaryCompanyType(organisationName: String,
                                  beneficiaryDiscretion: Option[Boolean],
                                  beneficiaryShareOfIncome: Option[String],
                                  identification: Option[IdentificationOrgType],
                                  countryOfResidence: Option[String])    // new 5MLD optional

object BeneficiaryCompanyType {
  implicit val companyTypeFormat: Format[BeneficiaryCompanyType] = Json.format[BeneficiaryCompanyType]
}

case class NaturalPersonType(name: NameType,
                             dateOfBirth: Option[LocalDate],
                             identification: Option[IdentificationType],
                             countryOfResidence: Option[String],  // new 5MLD optional
                             nationality: Option[String],         // new 5MLD optional
                             legallyIncapable: Option[Boolean])   // new 5MLD optional

object NaturalPersonType {
  implicit val naturalPersonTypeFormat: Format[NaturalPersonType] = Json.format[NaturalPersonType]
}

case class IdentificationType(nino: Option[String],
                              passport: Option[PassportType],
                              address: Option[AddressType])

object IdentificationType {
  implicit val identificationTypeFormat: Format[IdentificationType] = Json.format[IdentificationType]
}

case class TrustDetailsType(startDate: LocalDate,
                            lawCountry: Option[String],
                            administrationCountry: Option[String],
                            residentialStatus: Option[ResidentialStatusType],
                            typeOfTrust: TypeOfTrust,
                            deedOfVariation: Option[DeedOfVariation],
                            interVivos: Option[Boolean],
                            efrbsStartDate: Option[LocalDate],
                            trustTaxable: Option[Boolean],        // new 5MLD required
                            expressTrust: Option[Boolean],        // new 5MLD required
                            trustUKResident: Option[Boolean],     // new 5MLD required
                            trustUKProperty: Option[Boolean]      // new 5MLD optional
                           ) {

  def isEmploymentRelatedTrust : Boolean = typeOfTrust == Employment

}

object TrustDetailsType {
  implicit val trustDetailsTypeFormat: Format[TrustDetailsType] = Json.format[TrustDetailsType]
}

case class ResidentialStatusType(uk: Option[UkType],
                                 nonUK: Option[NonUKType])

object ResidentialStatusType {
  implicit val residentialStatusTypeFormat: Format[ResidentialStatusType] = Json.format[ResidentialStatusType]
}

case class UkType(scottishLaw: Boolean,
                  preOffShore: Option[String])

object UkType {
  implicit val ukTypeFormat: Format[UkType] = Json.format[UkType]
}

case class NonUKType(sch5atcgga92: Boolean,
                     s218ihta84: Option[Boolean],
                     agentS218IHTA84: Option[Boolean],
                     trusteeStatus: Option[String])

object NonUKType {
  implicit val nonUKTypeFormat: Format[NonUKType] = Json.format[NonUKType]
}

case class PropertyLandType(buildingLandName: Option[String],
                            address: Option[AddressType],
                            valueFull: Long,
                            valuePrevious: Long)

object PropertyLandType {
  implicit val propertyLandTypeFormat: Format[PropertyLandType] = Json.format[PropertyLandType]
}

case class BusinessAssetType(orgName: String,
                             businessDescription: Option[String],
                             address: Option[AddressType],  // Now optional with 5MLD
                             businessValue: Option[Long])   // Now optional with 5MLD

object BusinessAssetType {
  implicit val businessAssetTypeFormat: Format[BusinessAssetType] = Json.format[BusinessAssetType]
}

case class OtherAssetType(description: String,
                          value: Option[Long])    // Now optional with 5MLD

object OtherAssetType {
  implicit val otherAssetTypeFormat: Format[OtherAssetType] = Json.format[OtherAssetType]
}

// new 5MLD type
case class NonEEABusinessType(orgName: String,
                              address: AddressType,
                              govLawCountry: String,
                              startDate: LocalDate,
                              endDate: Option[LocalDate])

object NonEEABusinessType {
  implicit val format: Format[NonEEABusinessType] = Json.format[NonEEABusinessType]
}

case class PartnershipType(description: String,
                           partnershipStart: Option[LocalDate])   // Now optional with 5MLD

object PartnershipType {
  implicit val partnershipTypeFormat: Format[PartnershipType] = Json.format[PartnershipType]
}

case class SharesType(numberOfShares: Option[String],   // now optional with 5MLD
                      orgName: String,
                      shareClass: Option[String],       // now optional with 5MLD
                      typeOfShare: Option[String],      // now optional with 5MLD
                      value: Option[Long],              // now optional with 5MLD
                      utr: Option[String])              // new 5MLD optional

object SharesType {
  implicit val sharesTypeFormat: Format[SharesType] = Json.format[SharesType]
}

case class YearReturnType(taxReturnYear: String, taxConsequence: Boolean)

object YearReturnType {
  implicit val yearReturnTypeFormat: Format[YearReturnType] = Json.format[YearReturnType]
}

case class YearsReturnType(returns: Option[Array[YearReturnType]])

object YearsReturnType {
  implicit val yearsReturnTypeFormat: Format[YearsReturnType] = Json.format[YearsReturnType]
}


case class PassportType(number: String,
                        expirationDate: LocalDate,
                        countryOfIssue: String)

object PassportType {
  implicit val passportTypeFormat: Format[PassportType] = Json.format[PassportType]
}

case class NameType(firstName: String,
                    middleName: Option[String],
                    lastName: String)

object NameType {
  implicit val nameTypeFormat: Format[NameType] = Json.format[NameType]
}

case class AddressType(line1: String,
                       line2: String,
                       line3: Option[String],
                       line4: Option[String],
                       postCode: Option[String],
                       country: String)

object AddressType {
  implicit val addressTypeFormat: Format[AddressType] = Json.format[AddressType]
}

case class WillType(name: NameType,
                    dateOfBirth: Option[LocalDate],
                    dateOfDeath: Option[LocalDate],
                    identification: Option[IdentificationType],
                    countryOfResidence: Option[String],     // new 5MLD optional
                    nationality: Option[String])            // new 5MLD optional

object WillType {
  implicit val willTypeFormat: Format[WillType] = Json.format[WillType]
}

case class AgentDetails(arn: String,
                        agentName: String,
                        agentAddress: AddressType,
                        agentTelephoneNumber: String,
                        clientReference: String)
object AgentDetails {
  implicit val agentDetailsFormat: Format[AgentDetails] = Json.format[AgentDetails]
}
