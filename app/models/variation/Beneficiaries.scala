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

package models.variation

import models.JsonWithoutNulls._
import models.{AddressType, NameType}
import play.api.libs.json.{Format, Json, Writes}

import java.time.LocalDate

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

case class IndividualDetailsType(lineNo: Option[String],
                                 bpMatchStatus: Option[String],
                                 name: NameType,
                                 dateOfBirth: Option[LocalDate],
                                 vulnerableBeneficiary: Option[Boolean],
                                 beneficiaryType: Option[String],
                                 beneficiaryDiscretion: Option[Boolean],
                                 beneficiaryShareOfIncome: Option[String],
                                 identification: Option[IdentificationType],
                                 countryOfResidence: Option[String],
                                 legallyIncapable: Option[Boolean],
                                 nationality: Option[String],
                                 entityStart: LocalDate,
                                 entityEnd: Option[LocalDate])

object IndividualDetailsType {
  implicit val individualDetailsTypeFormat: Format[IndividualDetailsType] = Json.format[IndividualDetailsType]

  val writeToMaintain: Writes[IndividualDetailsType] = (o: IndividualDetailsType) => Json.obj(
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
                                  countryOfResidence: Option[String],
                                  entityStart: LocalDate,
                                  entityEnd: Option[LocalDate])

object BeneficiaryCompanyType {
  implicit val companyTypeFormat: Format[BeneficiaryCompanyType] = Json.format[BeneficiaryCompanyType]

  val writeToMaintain: Writes[BeneficiaryCompanyType] = (o: BeneficiaryCompanyType) => Json.obj(
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
                                countryOfResidence: Option[String],
                                entityStart: LocalDate,
                                entityEnd: Option[LocalDate])

object BeneficiaryTrustType {
  implicit val beneficiaryTrustTypeFormat: Format[BeneficiaryTrustType] = Json.format[BeneficiaryTrustType]

  val writeToMaintain: Writes[BeneficiaryTrustType] = (o: BeneficiaryTrustType) => Json.obj(
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
                                  countryOfResidence: Option[String],
                                  entityStart: LocalDate,
                                  entityEnd: Option[LocalDate])

object BeneficiaryCharityType {
  implicit val charityTypeFormat: Format[BeneficiaryCharityType] = Json.format[BeneficiaryCharityType]

  val writeToMaintain: Writes[BeneficiaryCharityType] = (o: BeneficiaryCharityType) => Json.obj(
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

case class UnidentifiedType(lineNo: Option[String],
                            bpMatchStatus: Option[String],
                            description: String,
                            beneficiaryDiscretion: Option[Boolean],
                            beneficiaryShareOfIncome: Option[String],
                            entityStart: LocalDate,
                            entityEnd: Option[LocalDate])

object UnidentifiedType {
  implicit val unidentifiedTypeFormat: Format[UnidentifiedType] = Json.format[UnidentifiedType]

  val writeToMaintain: Writes[UnidentifiedType] = (o: UnidentifiedType) => Json.obj(
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
                     countryOfResidence: Option[String],
                     entityStart: LocalDate,
                     entityEnd: Option[LocalDate])

object LargeType {
  implicit val largeTypeFormat: Format[LargeType] = Json.format[LargeType]

  val writeToMaintain: Writes[LargeType] = (o: LargeType) => Json.obj(
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
                     countryOfResidence: Option[String],
                     entityStart: LocalDate,
                     entityEnd: Option[LocalDate])

object OtherType {
  implicit val otherTypeFormat: Format[OtherType] = Json.format[OtherType]

  val writeToMaintain: Writes[OtherType] = (o: OtherType) => Json.obj(
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
