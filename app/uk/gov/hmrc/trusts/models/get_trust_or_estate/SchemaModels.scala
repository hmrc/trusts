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

package uk.gov.hmrc.trusts.models.get_trust_or_estate

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.trusts.models._

case class MatchData(utr: String)

object MatchData {
  implicit val matchDataFormat: Format[MatchData] = Json.format[MatchData]
}

case class ResponseHeader(status: String,
                          formBundleNo: String)

object ResponseHeader {
  implicit val writes: Writes[ResponseHeader] = Json.writes[ResponseHeader]
  implicit val reads: Reads[ResponseHeader] = (
    (JsPath \ "dfmcaReturnUserStatus").read[String] and
      (JsPath \ "formBundleNo").read[String]
    )(ResponseHeader.apply _)
}

case class IdentificationType(nino: Option[String],
                              passport: Option[PassportType],
                              address: Option[AddressType],
                              safeId: Option[String])

object IdentificationType {
  implicit val identificationTypeFormat: Format[IdentificationType] = Json.format[IdentificationType]
}

case class Identification(nino: Option[String],
                          address: Option[AddressType],
                          safeId: Option[String])

object Identification {
  implicit val identificationFormat: Format[Identification] = Json.format[Identification]
}

case class IdentificationOrgType(utr: Option[String],
                                 address: Option[AddressType],
                                 safeId: Option[String])

object IdentificationOrgType {
  implicit val trustBeneficiaryIdentificationFormat: Format[IdentificationOrgType] = Json.format[IdentificationOrgType]
}