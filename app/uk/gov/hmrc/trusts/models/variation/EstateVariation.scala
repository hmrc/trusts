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
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.{AgentDetails, Correspondence, Declaration, NameType}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.{Identification, IdentificationOrgType, IdentificationType}
import uk.gov.hmrc.trusts.utils.Constants.dateTimePattern

case class EstateVariation(
                      matchData: MatchData,
                      correspondence: Correspondence,
                      declaration: Declaration,
                      details: Estate,
                      agentDetails: Option[AgentDetails] = None,
                      estateEndDate: Option[DateTime],
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
        (__ \ "estateEndDate").readNullable[DateTime] and
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
        (JsPath \ "estateEndDate").writeNullable[DateTime] and
        (JsPath \ "reqHeader").write[ReqHeader]
      ) (unlift(EstateVariation.unapply))
  }

  implicit val variationFormat: Format[EstateVariation] = Format(variationReads, writeToDes)

}

case class Estate(entities: EstateEntitiesType,
                  administrationEndDate: Option[DateTime],
                  periodTaxDues: String)

object Estate {

  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
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

  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val personalRepFormats :Format[PersonalRepresentativeType] = Json.format[PersonalRepresentativeType]

}

case class EstatePerRepIndType(   lineNo: Option[String],
                                  bpMatchStatus: Option[String],
                                  name: NameType,
                                  dateOfBirth: DateTime,
                                  identification: IdentificationType,
                                  phoneNumber: String,
                                  email: Option[String],
                                  entityStart: DateTime,
                                  entityEnd: Option[DateTime]
                              )

object EstatePerRepIndType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val estatePerRepIndTypeFormat: Format[EstatePerRepIndType] = Json.format[EstatePerRepIndType]
}

case class EstatePerRepOrgType( lineNo: Option[String],
                                bpMatchStatus: Option[String],
                                orgName: String,
                                phoneNumber: String,
                                email: Option[String] = None,
                                identification: IdentificationOrgType,
                                entityStart: DateTime,
                                entityEnd: Option[DateTime])

object EstatePerRepOrgType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val estatePerRepOrgTypeFormat: Format[EstatePerRepOrgType] = Json.format[EstatePerRepOrgType]
}


case class EstateWillType(  lineNo: Option[String],
                            bpMatchStatus: Option[String],
                            name: NameType,
                            dateOfBirth: Option[DateTime],
                            dateOfDeath: DateTime,
                            identification: Option[Identification],
                            entityStart: DateTime,
                            entityEnd: Option[DateTime]
                         )

object EstateWillType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val estateWillTypeFormat: Format[EstateWillType] = Json.format[EstateWillType]
}