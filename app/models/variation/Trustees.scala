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
import models.NameType
import play.api.libs.json.{Format, Json, Writes}

import java.time.LocalDate

case class LeadTrusteeType(leadTrusteeInd: Option[LeadTrusteeIndType] = None,
                           leadTrusteeOrg: Option[LeadTrusteeOrgType] = None)

object LeadTrusteeType {
  implicit val leadTrusteeFormats: Format[LeadTrusteeType] = Json.format[LeadTrusteeType]
}

case class LeadTrusteeIndType(lineNo: Option[String],
                              bpMatchStatus: Option[String],
                              name: NameType,
                              dateOfBirth: LocalDate,
                              phoneNumber: String,
                              email: Option[String] = None,
                              identification: IdentificationType,
                              countryOfResidence: Option[String],
                              legallyIncapable: Option[Boolean],
                              nationality: Option[String],
                              entityStart: LocalDate,
                              entityEnd: Option[LocalDate])

object LeadTrusteeIndType {
  implicit val leadTrusteeIndTypeFormat: Format[LeadTrusteeIndType] = Json.format[LeadTrusteeIndType]
}

case class LeadTrusteeOrgType(lineNo: Option[String],
                              bpMatchStatus: Option[String],
                              name: String,
                              phoneNumber: String,
                              email: Option[String] = None,
                              identification: IdentificationOrgType,
                              countryOfResidence: Option[String],
                              entityStart: LocalDate,
                              entityEnd: Option[LocalDate])

object LeadTrusteeOrgType {
  implicit val leadTrusteeOrgTypeFormat: Format[LeadTrusteeOrgType] = Json.format[LeadTrusteeOrgType]
}

case class AmendedLeadTrusteeIndType(bpMatchStatus: Option[String],
                                     name: NameType,
                                     dateOfBirth: LocalDate,
                                     phoneNumber: String,
                                     email: Option[String] = None,
                                     identification: IdentificationType,
                                     countryOfResidence: Option[String],
                                     legallyIncapable: Option[Boolean],
                                     nationality: Option[String])

object AmendedLeadTrusteeIndType {
  implicit val format: Format[AmendedLeadTrusteeIndType] = Json.format[AmendedLeadTrusteeIndType]
}

case class AmendedLeadTrusteeOrgType(name: String,
                                     phoneNumber: String,
                                     email: Option[String] = None,
                                     identification: IdentificationOrgType,
                                     countryOfResidence: Option[String])

object AmendedLeadTrusteeOrgType {
  implicit val format: Format[AmendedLeadTrusteeOrgType] = Json.format[AmendedLeadTrusteeOrgType]
}

case class TrusteeType(trusteeInd: Option[TrusteeIndividualType],
                       trusteeOrg: Option[TrusteeOrgType]) extends Entity[TrusteeType] {

  override val writeToMaintain: Writes[TrusteeType] = TrusteeType.trusteeTypeFormat
}

object TrusteeType {
  implicit val trusteeTypeFormat: Format[TrusteeType] = Json.format[TrusteeType]
}

case class TrusteeIndividualType(lineNo: Option[String],
                                 bpMatchStatus: Option[String],
                                 name: NameType,
                                 dateOfBirth: Option[LocalDate],
                                 phoneNumber: Option[String],
                                 identification: Option[IdentificationType],
                                 countryOfResidence: Option[String],
                                 legallyIncapable: Option[Boolean],
                                 nationality: Option[String],
                                 entityStart: LocalDate,
                                 entityEnd: Option[LocalDate])

object TrusteeIndividualType {

  implicit val trusteeIndividualTypeFormat: Format[TrusteeIndividualType] = Json.format[TrusteeIndividualType]

  val writeToMaintain: Writes[TrusteeIndividualType] = (o: TrusteeIndividualType) => Json.obj(
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

case class TrusteeOrgType(lineNo: Option[String],
                          bpMatchStatus: Option[String],
                          name: String,
                          phoneNumber: Option[String] = None,
                          email: Option[String] = None,
                          identification: Option[IdentificationOrgType],
                          countryOfResidence: Option[String],
                          entityStart: LocalDate,
                          entityEnd: Option[LocalDate])

object TrusteeOrgType {

  implicit val trusteeOrgTypeFormat: Format[TrusteeOrgType] = Json.format[TrusteeOrgType]

  val writeToMaintain: Writes[TrusteeOrgType] = (o: TrusteeOrgType) => Json.obj(
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
