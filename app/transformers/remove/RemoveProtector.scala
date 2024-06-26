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

package transformers.remove

import java.time.LocalDate
import play.api.libs.json.{Format, Json, OWrites, Reads}
import utils.Constants._

case class RemoveProtector(endDate: LocalDate,
                           index: Int,
                           override val `type`: String) extends Remove

object RemoveProtector {
  val validProtectorTypes: Seq[String] = Seq(
    INDIVIDUAL_PROTECTOR,
    BUSINESS_PROTECTOR
  )

  val reads: Reads[RemoveProtector] = Json.reads[RemoveProtector].filter(rb => validProtectorTypes.contains(rb.`type`))
  val writes: OWrites[RemoveProtector] = Json.writes[RemoveProtector]

  implicit val formats: Format[RemoveProtector] = Format(reads, writes)
}
