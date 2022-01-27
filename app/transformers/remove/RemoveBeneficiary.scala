/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json._
import utils.Constants._

import java.time.LocalDate

case class RemoveBeneficiary(endDate: LocalDate,
                             index: Int,
                             override val `type`: String) extends Remove

object RemoveBeneficiary {
  val validBeneficiaryTypes: Seq[String] = Seq(
    INDIVIDUAL_BENEFICIARY,
    UNIDENTIFIED_BENEFICIARY,
    COMPANY_BENEFICIARY,
    LARGE_BENEFICIARY,
    TRUST_BENEFICIARY,
    CHARITY_BENEFICIARY,
    OTHER_BENEFICIARY
  )

  val reads: Reads[RemoveBeneficiary] = Json.reads[RemoveBeneficiary].filter(rb => validBeneficiaryTypes.contains(rb.`type`))
  val writes: OWrites[RemoveBeneficiary] = Json.writes[RemoveBeneficiary]

  implicit val formats: Format[RemoveBeneficiary] = Format(reads, writes)
}
