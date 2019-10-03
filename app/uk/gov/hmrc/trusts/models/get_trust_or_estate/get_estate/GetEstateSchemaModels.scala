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

package uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.{Correspondence, Declaration, NameType}
import uk.gov.hmrc.trusts.utils.Constants.dateTimePattern

case class PersonalRepresentativeType (
                                        estatePerRepInd : Option[EstatePerRepIndType] = None,
                                        estatePerRepOrg : Option[EstatePerRepOrgType] = None
                                      )

object PersonalRepresentativeType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))

  implicit object PersonalRepFormats extends Format[PersonalRepresentativeType] {

    override def writes(o: PersonalRepresentativeType): JsValue = {
      o.estatePerRepInd match {
        case Some(ind) => Json.toJson(ind)
        case None => Json.toJson(o.estatePerRepOrg)
      }
    }

    override def reads(json: JsValue): JsResult[PersonalRepresentativeType] = {
      json.validate[EstatePerRepIndType].map {
        ind =>
          PersonalRepresentativeType(estatePerRepInd = Some(ind))
      }.orElse {
        json.validate[EstatePerRepOrgType].map {
          org =>
            PersonalRepresentativeType(estatePerRepOrg = Some(org))
        }
      }
    }
  }

  implicit val personalRepFormats : Format[PersonalRepresentativeType] = PersonalRepFormats
}

case class EstatePerRepIndType(name: NameType,
                               dateOfBirth: DateTime,
                               identification: IdentificationType,
                               phoneNumber: String,
                               email: Option[String],
                               lineNo: String,
                               bpMatchStatus: Option[String],
                               entityStart: DateTime)

object EstatePerRepIndType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val estatePerRepIndTypeFormat: Format[EstatePerRepIndType] = Json.format[EstatePerRepIndType]
}

case class EstatePerRepOrgType(orgName: String,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationOrgType,
                               lineNo: String,
                               bpMatchStatus: Option[String],
                               entityStart: DateTime)

object EstatePerRepOrgType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val estatePerRepOrgTypeFormat: Format[EstatePerRepOrgType] = Json.format[EstatePerRepOrgType]
}

case class EstateWillType(name: NameType,
                          dateOfBirth: Option[DateTime],
                          dateOfDeath: DateTime,
                          identification: Option[IdentificationType],
                          lineNo: String,
                          bpMatchStatus: Option[String],
                          entityStart: DateTime)

object EstateWillType {
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val estateWillTypeFormat: Format[EstateWillType] = Json.format[EstateWillType]
}

case class EntitiesType(personalRepresentative: PersonalRepresentativeType,
                        deceased: EstateWillType)

object EntitiesType {
  implicit val entitiesTypeFormat: Format[EntitiesType] = Json.format[EntitiesType]
}

case class Estate(entities: EntitiesType,
                  administrationEndDate: Option[DateTime],
                  periodTaxDues: String)

object Estate {
  implicit val dateFormat: Format[DateTime] = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )
  implicit val estateFormat: Format[Estate] = Json.format[Estate]
}

case class GetEstate(matchData: MatchData,
                     correspondence: Correspondence,
                     declaration: Declaration,
                     estate: Estate)

object GetEstate {
  implicit val writes: Writes[GetEstate] = Json.writes[GetEstate]
  implicit val reads: Reads[GetEstate] = (
    (JsPath \ "matchData").read[MatchData] and
      (JsPath \ "correspondence").read[Correspondence] and
      (JsPath \ "declaration").read[Declaration] and
      (JsPath \ "details" \ "estate").read[Estate]
    )(GetEstate.apply _)
}