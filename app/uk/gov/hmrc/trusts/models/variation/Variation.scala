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

package uk.gov.hmrc.trusts.models.variation

import org.joda.time.DateTime
import uk.gov.hmrc.trusts.models._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.utils.Constants.dateTimePattern

case class Variation(
                      matchData: MatchData,
                      correspondence: Correspondence,
                      declaration: Declaration,
                      details: Trust,
                      agentDetails: Option[AgentDetails] = None,
                      trustEndDate: Option[DateTime],
                      reqHeader: ReqHeader
                    )

object Variation {

  val variationReads: Reads[Variation] = {
    (
      (__ \ "matchData").read[MatchData] and
        (__ \ "correspondence").read[Correspondence] and
        (__ \ "declaration").read[Declaration] and
        (__ \ "details" \ "trust").read[Trust] and
        (__ \ "agentDetails").readNullable[AgentDetails] and
        (__ \ "trustEndDate").readNullable[DateTime] and
        (__ \ "reqHeader").read[ReqHeader]
      ) (Variation.apply _)
  }

  val writeToDes: Writes[Variation] = (
    (JsPath \ "matchData").write[MatchData] and
      (JsPath \ "correspondence").write[Correspondence] and
      (JsPath \ "declaration").write[Declaration] and
      (JsPath \ "details" \ "trust").write[Trust] and
      (JsPath \ "agentDetails").writeNullable[AgentDetails] and
      (JsPath \ "trustEndDate").writeNullable[DateTime] and
      (JsPath \ "reqHeader").write[ReqHeader]
    ) (r => (r.matchData, r.correspondence, r.declaration, r.details, r.agentDetails, r.trustEndDate, r.reqHeader))

  implicit val variationFormat: Format[Variation] = Format(variationReads, writeToDes)

}

case class MatchData(utr: String)

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
                  assets: Assets
                )

object Trust {
  implicit val trustFormat: Format[Trust] = Json.format[Trust]
}

case class TrustEntitiesType(
                              naturalPerson: Option[List[NaturalPersonType]],
                              beneficiary: BeneficiaryType,
                              deceased: Option[WillType],
                              leadTrustees: LeadTrusteeType,
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
                              dateOfBirth: Option[DateTime],
                              identification: Option[IdentificationType],
                              entityStart: DateTime,
                              entityEnd: Option[DateTime]
                            )

object NaturalPersonType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val naturalPersonTypeFormat: Format[NaturalPersonType] = Json.format[NaturalPersonType]
}

case class BeneficiaryType(
                            individualDetails: Option[List[IndividualDetailsType]],
                            company: Option[List[CompanyType]],
                            trust: Option[List[BeneficiaryTrustType]],
                            charity: Option[List[CharityType]],
                            unidentified: Option[List[UnidentifiedType]],
                            large: Option[List[LargeType]],
                            other: Option[List[OtherType]]
                          )

object BeneficiaryType {
  implicit val beneficiaryTypeFormat: Format[BeneficiaryType] = Json.format[BeneficiaryType]
}

case class IndividualDetailsType(
                                  lineNo: Option[String],
                                  bpMatchStatus: Option[String],
                                  name: NameType,
                                  dateOfBirth: Option[DateTime],
                                  vulnerableBeneficiary: Boolean,
                                  beneficiaryType: Option[String],
                                  beneficiaryDiscretion: Option[Boolean],
                                  beneficiaryShareOfIncome: Option[String],
                                  identification: Option[IdentificationType],
                                  entityStart: DateTime,
                                  entityEnd: Option[DateTime]
                                )

object IndividualDetailsType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val individualDetailsTypeFormat: Format[IndividualDetailsType] = Json.format[IndividualDetailsType]
}

case class WillType(
                     lineNo: Option[String],
                     bpMatchStatus: Option[String],
                     name: NameType,
                     dateOfBirth: Option[DateTime],
                     dateOfDeath: Option[DateTime],
                     identification: Option[Identification],
                     entityStart: DateTime,
                     entityEnd: Option[DateTime]
                   )

object WillType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val willTypeFormat: Format[WillType] = Json.format[WillType]
}

case class LeadTrusteeIndType(
                               lineNo: Option[String],
                               bpMatchStatus: Option[String],
                               name: NameType,
                               dateOfBirth: DateTime,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationType,
                               entityStart: DateTime,
                               entityEnd: Option[DateTime]
                             )

object LeadTrusteeIndType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val leadTrusteeIndTypeFormat: Format[LeadTrusteeIndType] = Json.format[LeadTrusteeIndType]

}

case class LeadTrusteeOrgType(
                               lineNo: Option[String],
                               bpMatchStatus: Option[String],
                               name: String,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationOrgType,
                               entityStart: DateTime,
                               entityEnd: Option[DateTime]
                             )

object LeadTrusteeOrgType {
  implicit val leadTrusteeOrgTypeFormat: Format[LeadTrusteeOrgType] = Json.format[LeadTrusteeOrgType]
}

case class LeadTrusteeType(
                            leadTrusteeInd: Option[LeadTrusteeIndType] = None,
                            leadTrusteeOrg: Option[LeadTrusteeOrgType] = None
                          )

object LeadTrusteeType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val leadTrusteeTypeReads: Reads[LeadTrusteeType] = Json.reads[LeadTrusteeType]

  implicit val leadTrusteeWritesToDes: Writes[LeadTrusteeType] = Writes {
    leadTrustee =>
      leadTrustee.leadTrusteeInd match {
        case Some(indLeadTrustee) => Json.toJson(indLeadTrustee)
        case None => Json.toJson(leadTrustee.leadTrusteeOrg)
      }
  }
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
                           entityStart: DateTime,
                           entityEnd: Option[DateTime]
                         )

object TrusteeOrgType {
  implicit val trusteeOrgTypeFormat: Format[TrusteeOrgType] = Json.format[TrusteeOrgType]
}

case class TrusteeIndividualType(
                                  lineNo: Option[String],
                                  bpMatchStatus: Option[String],
                                  name: NameType,
                                  dateOfBirth: Option[DateTime],
                                  phoneNumber: Option[String],
                                  identification: Option[IdentificationType],
                                  entityStart: DateTime,
                                  entityEnd: Option[DateTime]
                                )

object TrusteeIndividualType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val trusteeIndividualTypeFormat: Format[TrusteeIndividualType] = Json.format[TrusteeIndividualType]
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
                      dateOfBirth: Option[DateTime],
                      identification: Option[IdentificationType],
                      entityStart: DateTime,
                      entityEnd: Option[DateTime]
                    )

object Protector {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val protectorFormat: Format[Protector] = Json.format[Protector]
}

case class ProtectorCompany(
                             lineNo: Option[String],
                             bpMatchStatus: Option[String],
                             name: String,
                             identification: Option[IdentificationOrgType],
                             entityStart: DateTime,
                             entityEnd: Option[DateTime]
                           )

object ProtectorCompany {
  implicit val protectorCompanyFormat: Format[ProtectorCompany] = Json.format[ProtectorCompany]
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
                    dateOfBirth: Option[DateTime],
                    identification: Option[IdentificationType],
                    entityStart: DateTime,
                    entityEnd: Option[DateTime]
                  )

object Settlor {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val settlorFormat: Format[Settlor] = Json.format[Settlor]
}

case class SettlorCompany(
                           lineNo: Option[String],
                           bpMatchStatus: Option[String],
                           name: String,
                           companyType: Option[String],
                           companyTime: Option[Boolean],
                           identification: Option[IdentificationOrgType],
                           entityStart: DateTime,
                           entityEnd: Option[DateTime]
                         )

object SettlorCompany {
  implicit val settlorCompanyFormat: Format[SettlorCompany] = Json.format[SettlorCompany]
}

case class Assets(
                   monetary: Option[List[AssetMonetaryAmount]],
                   propertyOrLand: Option[List[PropertyLandType]],
                   shares: Option[List[SharesType]],
                   business: Option[List[BusinessAssetType]],
                   partnerShip: Option[List[PartnershipType]],
                   other: Option[List[OtherAssetType]]
                 )

object Assets {
  implicit val assetsFormat: Format[Assets] = Json.format[Assets]
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
                            partnershipStart: Option[DateTime]
                          )

object PartnershipType {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))
  implicit val partnershipTypeFormat: Format[PartnershipType] = Json.format[PartnershipType]
}