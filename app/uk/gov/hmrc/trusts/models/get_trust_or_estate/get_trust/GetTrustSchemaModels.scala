/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.{MatchData, ResponseHeader}
import uk.gov.hmrc.trusts.models._
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
                                    leadTrustees: DisplayTrustLeadTrusteeType,
                                    trustees: Option[List[DisplayTrustTrusteeType]],
                                    protectors: Option[DisplayTrustProtectorsType],
                                    settlors: Option[DisplayTrustSettlors])

object DisplayTrustEntitiesType {

  implicit val trustEntitiesTypeFormat: Format[DisplayTrustEntitiesType] = Json.format[DisplayTrustEntitiesType]
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
                                           lineNo: String,
                                           bpMatchStatus: Option[String],
                                           name: NameType,
                                           dateOfBirth: DateTime,
                                           phoneNumber: String,
                                           email: Option[String] = None,
                                           identification: DisplayTrustIdentificationType,
                                           entityStart: String
                                         )

object DisplayTrustLeadTrusteeIndType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val leadTrusteeIndTypeFormat: Format[DisplayTrustLeadTrusteeIndType] = Json.format[DisplayTrustLeadTrusteeIndType]

}

case class DisplayTrustLeadTrusteeOrgType(
                                           lineNo: String,
                                           bpMatchStatus: Option[String],
                                           name: String,
                                           phoneNumber: String,
                                           email: Option[String] = None,
                                           identification: DisplayTrustIdentificationOrgType,
                                           entityStart: String
                                         )

object DisplayTrustLeadTrusteeOrgType {
  implicit val leadTrusteeOrgTypeFormat: Format[DisplayTrustLeadTrusteeOrgType] = Json.format[DisplayTrustLeadTrusteeOrgType]
}

case class DisplayTrustLeadTrusteeType(
                                        leadTrusteeInd: Option[DisplayTrustLeadTrusteeIndType] = None,
                                        leadTrusteeOrg: Option[DisplayTrustLeadTrusteeOrgType] = None
                                      )

object DisplayTrustLeadTrusteeType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))

  implicit object LeadTrusteeFormats extends Format[DisplayTrustLeadTrusteeType] {

    override def writes(o: DisplayTrustLeadTrusteeType): JsValue = {
      o.leadTrusteeInd match {
        case Some(indLeadTrutee) => Json.toJson(indLeadTrutee)
        case None => Json.toJson(o.leadTrusteeOrg)
      }
    }

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

  implicit val leadTrusteeFormats : Format[DisplayTrustLeadTrusteeType] = LeadTrusteeFormats
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
                                   bpMatchStatus: Option[String],organisationName: String,
                                   beneficiaryDiscretion: Option[Boolean],
                                   beneficiaryShareOfIncome: Option[String],
                                   identification: DisplayTrustIdentificationOrgType,
                                   entityStart: String)

object DisplayTrustCompanyType {
  implicit val companyTypeFormat: Format[DisplayTrustCompanyType] = Json.format[DisplayTrustCompanyType]
}

case class DisplayTrustWillType(lineNo: String,
                                bpMatchStatus: Option[String],
                                name: NameType,
                                dateOfBirth: Option[DateTime],
                                dateOfDeath: Option[DateTime],
                                identification: Option[DisplayTrustIdentification],
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
                                            identification: DisplayTrustIdentificationOrgType,
                                            entityStart: String)

object DisplayTrustBeneficiaryTrustType {
  implicit val beneficiaryTrustTypeFormat: Format[DisplayTrustBeneficiaryTrustType] = Json.format[DisplayTrustBeneficiaryTrustType]
}

case class DisplayTrustCharityType(lineNo: String,
                                   bpMatchStatus: Option[String],
                                    organisationName: String,
                                   beneficiaryDiscretion: Option[Boolean],
                                   beneficiaryShareOfIncome: Option[String],
                                   identification: DisplayTrustIdentificationOrgType,
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

case class DisplayTrustTrusteeType(trusteeInd : Option[DisplayTrustTrusteeIndividualType], trusteeOrg : Option[DisplayTrustTrusteeOrgType] )
object DisplayTrustTrusteeType {
  implicit val trusteeTypeFormat : Format[DisplayTrustTrusteeType] = Json.format[DisplayTrustTrusteeType]
}

case class DisplayTrustTrusteeOrgType(lineNo: String,
                                      bpMatchStatus: Option[String],
                                      name: String,
                                      phoneNumber: Option[String] = None,
                                      email: Option[String] = None,
                                      identification: DisplayTrustIdentificationOrgType,
                                      entityStart: String)

object DisplayTrustTrusteeOrgType {
  implicit val trusteeOrgTypeFormat: Format[DisplayTrustTrusteeOrgType] = Json.format[DisplayTrustTrusteeOrgType]
}

case class DisplayTrustTrusteeIndividualType(lineNo: String,
                                             bpMatchStatus: Option[String],
                                             name: NameType,
                                             dateOfBirth: Option[DateTime],
                                             phoneNumber: Option[String],
                                             identification: Option[DisplayTrustIdentificationType],
                                             entityStart: String)

object DisplayTrustTrusteeIndividualType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val trusteeIndividualTypeFormat : Format[DisplayTrustTrusteeIndividualType] = Json.format[DisplayTrustTrusteeIndividualType]
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
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val protectorFormat: Format[DisplayTrustProtector] = Json.format[DisplayTrustProtector]
}

case class DisplayTrustProtectorCompany(lineNo: String,
                                        bpMatchStatus: Option[String],
                                        name: String,
                                        identification: DisplayTrustIdentificationOrgType,
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
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
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

case class DisplayTrustIdentification(safeId: Option[String],
                                      nino: Option[String],
                                      address: Option[AddressType])

object DisplayTrustIdentification {
  implicit val identificationFormat: Format[DisplayTrustIdentification] = Json.format[DisplayTrustIdentification]
}

case class DisplayTrustPartnershipType(utr: Option[String],
                                       description: String,
                                       partnershipStart: Option[DateTime])

object DisplayTrustPartnershipType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val partnershipTypeFormat: Format[DisplayTrustPartnershipType] = Json.format[DisplayTrustPartnershipType]
}

case class DisplayTrustAssets(monetary: Option[List[AssetMonetaryAmount]],
                              propertyOrLand: Option[List[PropertyLandType]],
                              shares: Option[List[SharesType]],
                              business: Option[List[BusinessAssetType]],
                              partnerShip: Option[List[DisplayTrustPartnershipType]],
                              other: Option[List[OtherAssetType]])

object DisplayTrustAssets {
  implicit val assetsFormat: Format[DisplayTrustAssets] = Json.format[DisplayTrustAssets]
}