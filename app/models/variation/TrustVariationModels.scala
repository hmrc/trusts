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

import models._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class TrustVariation(
                           matchData: MatchData,
                           correspondence: Correspondence,
                           yearsReturn: Option[YearsReturns],
                           declaration: Declaration,
                           details: Trust,
                           agentDetails: Option[AgentDetails] = None,
                           trustEndDate: Option[LocalDate],
                           reqHeader: ReqHeader,
                           submissionDate: Option[LocalDate]   // New to 5MLD variation, mandatory in 5MLD
                         )

object TrustVariation {

  val variationReads: Reads[TrustVariation] = {
    (
      (__ \ "matchData").read[MatchData] and
        (__ \ "correspondence").read[Correspondence] and
        (__ \ "yearsReturns").readNullable[YearsReturns] and
        (__ \ "declaration").read[Declaration] and
        (__ \ "details" \ "trust").read[Trust] and
        (__ \ "agentDetails").readNullable[AgentDetails] and
        (__ \ "trustEndDate").readNullable[LocalDate] and
        (__ \ "reqHeader").read[ReqHeader] and
        (__ \ "submissionDate").readNullable[LocalDate]
      ) (TrustVariation.apply _)
  }

  val writeToDes: Writes[TrustVariation] = (
    (JsPath \ "matchData").write[MatchData] and
      (JsPath \ "correspondence").write[Correspondence] and
      (JsPath \ "yearsReturns").writeNullable[YearsReturns] and
      (JsPath \ "declaration").write[Declaration] and
      (JsPath \ "details" \ "trust").write[Trust] and
      (JsPath \ "agentDetails").writeNullable[AgentDetails] and
      (JsPath \ "trustEndDate").writeNullable[LocalDate] and
      (JsPath \ "reqHeader").write[ReqHeader] and
      (JsPath \ "submissionDate").writeNullable[LocalDate]
    ) (unlift(TrustVariation.unapply))

  implicit val variationFormat: Format[TrustVariation] = Format(variationReads, writeToDes)

}

// Both optional in display response
case class MatchData(utr: Option[String], urn: Option[String])

object MatchData {
  implicit val matchDataFormat: Format[MatchData] = Json.format[MatchData]
}

case class ReqHeader(formBundleNo: String)

object ReqHeader {
  implicit val reqHeaderFormat: Format[ReqHeader] = Json.format[ReqHeader]
}

case class Trust(details: TrustDetailsType,
                 entities: TrustEntitiesType,
                 assets: Option[Assets])

object Trust {
  implicit val trustFormat: Format[Trust] = Json.format[Trust]
}

case class TrustEntitiesType(naturalPerson: Option[List[NaturalPersonType]],
                             beneficiary: BeneficiaryType,
                             deceased: Option[WillType],
                             leadTrustees: List[LeadTrusteeType],
                             trustees: Option[List[TrusteeType]],
                             protectors: Option[ProtectorsType],
                             settlors: Option[Settlors])

object TrustEntitiesType {
  implicit val trustEntitiesTypeFormat: Format[TrustEntitiesType] = Json.format[TrustEntitiesType]
}

case class IdentificationType(nino: Option[String],
                              passport: Option[PassportType],
                              address: Option[AddressType],
                              safeId: Option[String])

object IdentificationType {
  implicit val identificationTypeFormat: Format[IdentificationType] = Json.format[IdentificationType]
}

case class IdentificationOrgType(utr: Option[String],
                                 address: Option[AddressType],
                                 safeId: Option[String])

object IdentificationOrgType {
  implicit val trustBeneficiaryIdentificationFormat: Format[IdentificationOrgType] = Json.format[IdentificationOrgType]
}

case class DeclarationForApi(declaration: DeclarationName,
                             agentDetails: Option[AgentDetails],
                             endDate: Option[LocalDate])

object DeclarationForApi {
  implicit val declarationForApiFormat: Format[DeclarationForApi] = Json.format[DeclarationForApi]
}