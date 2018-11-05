/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ExistingTrustCheckRequest(name: String, postCode: Option[String] = None, utr: String)

object ExistingTrustCheckRequest {
  implicit val writes: Writes[ExistingTrustCheckRequest] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "postcode").writeNullable[String] and
      (JsPath \ "sa-utr").write[String]
    )(c => (
    c.name,
    c.postCode,
    c.utr))

  implicit val reads: Reads[ExistingTrustCheckRequest] = Json.reads[ExistingTrustCheckRequest]}