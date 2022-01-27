/*
 * Copyright 2022 HM Revenue & Customs
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

trait Protector[T] extends Entity[T]

case class ProtectorsType(protector: Option[List[ProtectorIndividual]],
                          protectorCompany: Option[List[ProtectorCompany]])

object ProtectorsType {
  implicit val protectorsTypeFormat: Format[ProtectorsType] = Json.format[ProtectorsType]
}

case class ProtectorIndividual(lineNo: Option[String],
                               bpMatchStatus: Option[String],
                               name: NameType,
                               dateOfBirth: Option[LocalDate],
                               identification: Option[IdentificationType],
                               countryOfResidence: Option[String],
                               legallyIncapable: Option[Boolean],
                               nationality: Option[String],
                               entityStart: LocalDate,
                               entityEnd: Option[LocalDate]) extends Protector[ProtectorIndividual] {

  override val writeToMaintain: Writes[ProtectorIndividual] = ProtectorIndividual.writeToMaintain
}

object ProtectorIndividual {
  implicit val protectorFormat: Format[ProtectorIndividual] = Json.format[ProtectorIndividual]

  val writeToMaintain: Writes[ProtectorIndividual] = (o: ProtectorIndividual) => Json.obj(
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

case class ProtectorCompany(lineNo: Option[String],
                            bpMatchStatus: Option[String],
                            name: String,
                            identification: Option[IdentificationOrgType],
                            countryOfResidence: Option[String],
                            entityStart: LocalDate,
                            entityEnd: Option[LocalDate]) extends Protector[ProtectorCompany] {

  override val writeToMaintain: Writes[ProtectorCompany] = ProtectorCompany.writeToMaintain
}

object ProtectorCompany {
  implicit val protectorCompanyFormat: Format[ProtectorCompany] = Json.format[ProtectorCompany]

  val writeToMaintain: Writes[ProtectorCompany] = (o: ProtectorCompany) => Json.obj(
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
