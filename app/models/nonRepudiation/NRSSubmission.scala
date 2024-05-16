/*
 * Copyright 2024 HM Revenue & Customs
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

package models.nonRepudiation

import models.requests.CredentialData

import java.time.LocalDateTime
import play.api.libs.json._
import uk.gov.hmrc.auth.core.AffinityGroup

case class NRSSubmission(payload: String,
                         metadata: MetaData)

object NRSSubmission {
  implicit val formats: OFormat[NRSSubmission] = Json.format[NRSSubmission]
}

case class IdentityData(
                         internalId: String,
                         affinityGroup: AffinityGroup,
                         deviceId: String,
                         clientIP: String,
                         clientPort: String,
                         sessionId: String,
                         requestId: String,
                         credential: CredentialData,
                         declaration: JsValue,
                         agentDetails: Option[JsValue]
                       )

object IdentityData {
  implicit val formats: OFormat[IdentityData] = Json.format[IdentityData]

  val txmWrites : Writes[IdentityData] = Writes { identity =>
    Json.toJsObject(identity).
      -("deviceId").
      -("clientIP").
      -("clientPort")
  }
}

case class MetaData(businessId: String,
                    notableEvent: NotableEvent,
                    payloadContentType: String,
                    payloadSha256Checksum: String,
                    userSubmissionTimestamp: LocalDateTime,
                    identityData: IdentityData,
                    userAuthToken: String,
                    headerData: JsValue,
                    searchKeys: SearchKeys
                   )

object MetaData {

  import utils.LocalDateTimeFormatter._

  implicit val formats: OFormat[MetaData] = Json.format[MetaData]

  val txmWrites: Writes[MetaData] = Writes { metaData =>
    Json.obj(
      "businessId" -> metaData.businessId,
      "notableEvent" -> metaData.notableEvent,
      "payloadSha256Checksum" -> metaData.payloadSha256Checksum,
      "userSubmissionTimestamp" -> Json.toJson(metaData.userSubmissionTimestamp),
      "identityData" -> Json.toJson(metaData.identityData)(IdentityData.txmWrites),
      "searchKeys" -> Json.toJson(metaData.searchKeys)
    )
  }
}


case class SearchKeys(searchKey: SearchKey, value: String)

object SearchKeys {

  implicit val reads: Reads[SearchKeys] = Json.reads[SearchKeys]

  implicit val writes: OWrites[SearchKeys] = OWrites {
    terms: SearchKeys =>
      terms.searchKey match {
        case SearchKey.TRN => Json.obj("trn" -> terms.value)
        case SearchKey.UTR => Json.obj("utr" -> terms.value)
        case SearchKey.URN => Json.obj("urn" -> terms.value)
      }
  }
}
