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

import java.time.LocalDate

import models.NameType
import play.api.libs.json.{Format, Json}

case class AmendedLeadTrusteeIndType(
                               name: NameType,
                               dateOfBirth: LocalDate,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationType,
                               countryOfResidence: Option[String],    // new 5MLD optional
                               legallyIncapable: Option[Boolean],     // new 5MLD optional
                               nationality: Option[String]            // new 5MLD optional
                             )

object AmendedLeadTrusteeIndType {
  implicit val format: Format[AmendedLeadTrusteeIndType] = Json.format[AmendedLeadTrusteeIndType]
}

case class AmendedLeadTrusteeOrgType(
                               name: String,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationOrgType,
                               countryOfResidence: Option[String]     // new 5MLD optional
                             )

object AmendedLeadTrusteeOrgType {
  implicit val format: Format[AmendedLeadTrusteeOrgType] = Json.format[AmendedLeadTrusteeOrgType]
}
