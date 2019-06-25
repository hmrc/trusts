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

package uk.gov.hmrc.trusts.models.GetTrust

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.{Correspondence, Trust, Declaration}

case class MatchData(utr: String)

object MatchData {
  implicit val matchDataFormat: Format[MatchData] = Json.format[MatchData]
}

case class ResponseHeader(dfmcaReturnUserStatus: String,
                          formBundleNo: Int)

object ResponseHeader {
  implicit val writes: Writes[ResponseHeader] = Json.writes[ResponseHeader]
  implicit val reads: Reads[ResponseHeader] = (
    (JsPath \ "dfmcaReturnUserStatus").read[String] and
      (JsPath \ "formBundleNo").read[Int]
    )(ResponseHeader.apply _)
}

case class GetTrust(matchData: MatchData,
                    correspondence: Correspondence,
                    declaration: Declaration,
                    trust: Trust)

object GetTrust {
  implicit val writes: Writes[GetTrust] = Json.writes[GetTrust]
  implicit val reads: Reads[GetTrust] = (
    (JsPath \ "matchData").read[MatchData] and
    (JsPath \ "correspondence").read[Correspondence] and
    (JsPath \ "declaration").read[Declaration] and
    (JsPath \ "details" \ "trust").read[Trust]
  )(GetTrust.apply _)
}

case class GetTrustDesResponse(getTrust: Option[GetTrust],
                               responseHeader: ResponseHeader)

object GetTrustDesResponse {
  implicit val writes: Writes[GetTrustDesResponse] = Json.writes[GetTrustDesResponse]
  implicit val reads: Reads[GetTrustDesResponse] = (
    (JsPath \ "trustOrEstateDisplay").readNullable[GetTrust] and
    (JsPath \ "responseHeader").read[ResponseHeader]
  )(GetTrustDesResponse.apply _)
}