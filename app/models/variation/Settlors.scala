/*
 * Copyright 2024 HM Revenue & Customs
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
import utils.TypeOfTrust.{Employment, TypeOfTrust}

import java.time.LocalDate

trait Settlor[T] extends MigrationEntity[T]

case class Settlors(settlor: Option[List[SettlorIndividual]], settlorCompany: Option[List[SettlorCompany]])

object Settlors {
  implicit val settlorsFormat: Format[Settlors] = Json.format[Settlors]
}

case class SettlorIndividual(
  lineNo: Option[String],
  bpMatchStatus: Option[String],
  name: NameType,
  dateOfBirth: Option[LocalDate],
  identification: Option[IdentificationType],
  countryOfResidence: Option[String],
  legallyIncapable: Option[Boolean],
  nationality: Option[String],
  entityStart: LocalDate,
  entityEnd: Option[LocalDate]
) extends Settlor[SettlorIndividual] {

  override val writeToMaintain: Writes[SettlorIndividual] = SettlorIndividual.writeToMaintain
}

object SettlorIndividual {
  implicit val settlorFormat: Format[SettlorIndividual] = Json.format[SettlorIndividual]

  val writeToMaintain: Writes[SettlorIndividual] = (o: SettlorIndividual) =>
    Json
      .obj(
        "lineNo"             -> o.lineNo,
        "bpMatchStatus"      -> o.bpMatchStatus,
        "name"               -> o.name,
        "dateOfBirth"        -> o.dateOfBirth,
        "identification"     -> o.identification,
        "countryOfResidence" -> o.countryOfResidence,
        "legallyIncapable"   -> o.legallyIncapable,
        "nationality"        -> o.nationality,
        "entityStart"        -> o.entityStart,
        "entityEnd"          -> o.entityEnd,
        "provisional"        -> o.lineNo.isEmpty
      )
      .withoutNulls

}

case class SettlorCompany(
  lineNo: Option[String],
  bpMatchStatus: Option[String],
  name: String,
  companyType: Option[String],
  companyTime: Option[Boolean],
  identification: Option[IdentificationOrgType],
  countryOfResidence: Option[String],
  entityStart: LocalDate,
  entityEnd: Option[LocalDate]
) extends Settlor[SettlorCompany] {

  override val writeToMaintain: Writes[SettlorCompany] = SettlorCompany.writeToMaintain

  override def hasRequiredDataForMigration(trustType: Option[TypeOfTrust]): Boolean =
    trustType match {
      case Some(Employment) => companyType.isDefined && companyTime.isDefined
      case _                => true
    }

}

object SettlorCompany {
  implicit val settlorCompanyFormat: Format[SettlorCompany] = Json.format[SettlorCompany]

  val writeToMaintain: Writes[SettlorCompany] = (o: SettlorCompany) =>
    Json
      .obj(
        "lineNo"             -> o.lineNo,
        "bpMatchStatus"      -> o.bpMatchStatus,
        "name"               -> o.name,
        "companyType"        -> o.companyType,
        "companyTime"        -> o.companyTime,
        "identification"     -> o.identification,
        "countryOfResidence" -> o.countryOfResidence,
        "entityStart"        -> o.entityStart,
        "entityEnd"          -> o.entityEnd,
        "provisional"        -> o.lineNo.isEmpty
      )
      .withoutNulls

}

case class WillType(
  lineNo: Option[String],
  bpMatchStatus: Option[String],
  name: NameType,
  dateOfBirth: Option[LocalDate],
  dateOfDeath: Option[LocalDate],
  identification: Option[IdentificationType],
  countryOfResidence: Option[String],
  nationality: Option[String],
  entityStart: LocalDate,
  entityEnd: Option[LocalDate]
)

object WillType {
  implicit val willTypeFormat: Format[WillType] = Json.format[WillType]
}

case class AmendDeceasedSettlor(
  name: NameType,
  dateOfBirth: Option[LocalDate],
  dateOfDeath: Option[LocalDate],
  countryOfResidence: Option[String],
  nationality: Option[String],
  identification: Option[IdentificationType]
)

object AmendDeceasedSettlor {
  implicit val formats: Format[AmendDeceasedSettlor] = Json.format[AmendDeceasedSettlor]
}
