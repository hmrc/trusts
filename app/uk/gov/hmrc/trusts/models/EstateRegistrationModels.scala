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

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class EstateRegistration(matchData: Option[MatchData],
                              correspondence: Correspondence,
                              yearsReturns: Option[YearsReturns],
                              declaration: Declaration,
                              estate: Estate,
                              agentDetails: Option[AgentDetails] = None
                             )

object EstateRegistration {
  implicit val estateRegistrationReads :Reads[EstateRegistration] = Json.reads[EstateRegistration]
  implicit val estateWriteToDes :Writes[EstateRegistration] = (
    (JsPath \ "matchData").writeNullable[MatchData] and
      (JsPath \ "correspondence").write[Correspondence] and
      (JsPath \ "declaration").write[Declaration] and
      (JsPath \ "yearsReturns").writeNullable[YearsReturns] and
      (JsPath \ "details" \ "estate").write[Estate] and
      (JsPath \ "agentDetails" ).writeNullable[AgentDetails]
    )(r => (r.matchData, r.correspondence,r.declaration, r.yearsReturns, r.estate,r.agentDetails))
}


case class Estate(entities: EntitiesType,
                  administrationEndDate: Option[LocalDate],
                  periodTaxDues: String)

object Estate {
  implicit val estateFormat: Format[Estate] = Json.format[Estate]
}

case class EntitiesType(personalRepresentative: PersonalRepresentativeType,
                        deceased: EstateWillType)

object EntitiesType {
  implicit val entitiesTypeFormat: Format[EntitiesType] = Json.format[EntitiesType]
}


case class PersonalRepresentativeType (
                                        estatePerRepInd : Option[EstatePerRepIndType] = None,
                                        estatePerRepOrg : Option[EstatePerRepOrgType] = None
                                      )

object PersonalRepresentativeType {
  implicit val personalRepTypeReads:Reads[PersonalRepresentativeType] = Json.reads[PersonalRepresentativeType]

  implicit val personalRepTypeWritesToDes : Writes[PersonalRepresentativeType] = Writes {
    personalRepType => personalRepType.estatePerRepInd match {
      case Some(indPerRep) => Json.toJson(indPerRep)
      case None => Json.toJson(personalRepType.estatePerRepOrg)
    }
  }
}

case class EstatePerRepIndType(   name: NameType,
                                  dateOfBirth: LocalDate,
                                  identification: IdentificationType,
                                  phoneNumber: String,
                                  email: Option[String])

object EstatePerRepIndType {
  implicit val estatePerRepIndTypeFormat: Format[EstatePerRepIndType] = Json.format[EstatePerRepIndType]
}

case class EstatePerRepOrgType(orgName: String,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationOrgType)

object EstatePerRepOrgType {
  implicit val estatePerRepOrgTypeFormat: Format[EstatePerRepOrgType] = Json.format[EstatePerRepOrgType]
}


case class EstateWillType(name: NameType,
                          dateOfBirth: Option[LocalDate],
                          dateOfDeath: LocalDate,
                          identification: Option[IdentificationType])

object EstateWillType {
  implicit val estateWillTypeFormat: Format[EstateWillType] = Json.format[EstateWillType]
}
