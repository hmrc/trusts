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

package uk.gov.hmrc.trusts.models.variation

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.IdentificationOrgType
import uk.gov.hmrc.trusts.models.{AgentDetails, Correspondence, Declaration, NameType}

case class EstateVariation(
                      matchData: MatchData,
                      correspondence: Correspondence,
                      declaration: Declaration,
                      details: Estate,
                      agentDetails: Option[AgentDetails] = None,
                      trustEndDate: Option[LocalDate],
                      reqHeader: ReqHeader
                    )

object EstateVariation {

  val variationReads: Reads[EstateVariation] = {
      (
      (__ \ "matchData").read[MatchData] and
        (__ \ "correspondence").read[Correspondence] and
        (__ \ "declaration").read[Declaration] and
        (__ \ "details" \ "estate").read[Estate] and
        (__ \ "agentDetails").readNullable[AgentDetails] and
        (__ \ "trustEndDate").readNullable[LocalDate] and
        (__ \ "reqHeader").read[ReqHeader]
      ) (EstateVariation.apply _)
  }

  val writeToDes: Writes[EstateVariation] = {
    (
      (JsPath \ "matchData").write[MatchData] and
        (JsPath \ "correspondence").write[Correspondence] and
        (JsPath \ "declaration").write[Declaration] and
        (JsPath \ "details" \ "estate").write[Estate] and
        (JsPath \ "agentDetails").writeNullable[AgentDetails] and
        (JsPath \ "trustEndDate").writeNullable[LocalDate] and
        (JsPath \ "reqHeader").write[ReqHeader]
      ) (unlift(EstateVariation.unapply))
  }

  implicit val variationFormat: Format[EstateVariation] = Format(variationReads, writeToDes)
}

case class Estate(entities: EstateEntitiesType,
                  administrationEndDate: Option[LocalDate],
                  periodTaxDues: String)

object Estate {
  implicit val estateFormat: Format[Estate] = Json.format[Estate]
}

case class EstateEntitiesType(
                         personalRepresentative: List[PersonalRepresentativeType],
                         deceased: EstateWillType
                       )

object EstateEntitiesType {

  implicit val entitiesTypeFormat: Format[EstateEntitiesType] = Json.format[EstateEntitiesType]
}


case class PersonalRepresentativeType(
                                        estatePerRepInd : Option[EstatePerRepIndType] = None,
                                        estatePerRepOrg : Option[EstatePerRepOrgType] = None
                                      )

object PersonalRepresentativeType {
  implicit val personalRepFormats :Format[PersonalRepresentativeType] = Json.format[PersonalRepresentativeType]
}

case class EstatePerRepIndType(   lineNo: Option[String],
                                  bpMatchStatus: Option[String],
                                  name: NameType,
                                  dateOfBirth: LocalDate,
                                  identification: IdentificationType,
                                  phoneNumber: String,
                                  email: Option[String],
                                  entityStart: LocalDate,
                                  entityEnd: Option[LocalDate]
                              )

object EstatePerRepIndType {
  implicit val estatePerRepIndTypeFormat: Format[EstatePerRepIndType] = Json.format[EstatePerRepIndType]
}

case class EstatePerRepOrgType( lineNo: Option[String],
                                bpMatchStatus: Option[String],
                                orgName: String,
                                phoneNumber: String,
                                email: Option[String] = None,
                                identification: IdentificationOrgType,
                                entityStart: LocalDate,
                                entityEnd: Option[LocalDate])

object EstatePerRepOrgType {
  implicit val estatePerRepOrgTypeFormat: Format[EstatePerRepOrgType] = Json.format[EstatePerRepOrgType]
}


case class EstateWillType(  lineNo: Option[String],
                            bpMatchStatus: Option[String],
                            name: NameType,
                            dateOfBirth: Option[LocalDate],
                            dateOfDeath: LocalDate,
                            identification: Option[IdentificationType],
                            entityStart: LocalDate,
                            entityEnd: Option[LocalDate]
                         )

object EstateWillType {
  implicit val estateWillTypeFormat: Format[EstateWillType] = Json.format[EstateWillType]
}