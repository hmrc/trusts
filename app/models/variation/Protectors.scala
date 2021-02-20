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

case class ProtectorsType(protector: Option[List[Protector]],
                          protectorCompany: Option[List[ProtectorCompany]])

object ProtectorsType {
  implicit val protectorsTypeFormat: Format[ProtectorsType] = Json.format[ProtectorsType]
}

case class Protector(lineNo: Option[String],
                     bpMatchStatus: Option[String],
                     name: NameType,
                     dateOfBirth: Option[LocalDate],
                     identification: Option[IdentificationType],
                     countryOfResidence: Option[String],
                     legallyIncapable: Option[Boolean],
                     nationality: Option[String],
                     entityStart: LocalDate,
                     entityEnd: Option[LocalDate])

object Protector {
  implicit val protectorFormat: Format[Protector] = Json.format[Protector]

  val writeToMaintain: Writes[Protector] = (o: Protector) => Json.obj(
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
                            entityEnd: Option[LocalDate])

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
