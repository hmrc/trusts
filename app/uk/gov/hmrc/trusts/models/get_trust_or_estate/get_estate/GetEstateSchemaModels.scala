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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.{MatchData, ResponseHeader}
import uk.gov.hmrc.trusts.models.{Correspondence, Declaration, Estate}

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

case class GetEstateDesResponse(getEstate: Option[GetEstate],
                               responseHeader: ResponseHeader)

object GetEstateDesResponse {
  implicit val writes: Writes[GetEstateDesResponse] = Json.writes[GetEstateDesResponse]
  implicit val reads: Reads[GetEstateDesResponse] = (
    (JsPath \ "trustOrEstateDisplay").readNullable[GetEstate] and
    (JsPath \ "responseHeader").read[ResponseHeader]
  )(GetEstateDesResponse.apply _)
}