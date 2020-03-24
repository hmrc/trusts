/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads

sealed trait RemoveBeneficiary {
  def endDate: LocalDate
  def index: Int
  def beneficiaryType: String
}

object RemoveBeneficiary {
  case class Unidentified(endDate: LocalDate, index: Int) extends RemoveBeneficiary {val beneficiaryType = "unidentified"}
  case class Individual(endDate: LocalDate, index: Int) extends RemoveBeneficiary {val beneficiaryType = "individual"}

  private val builders: Map[String, (LocalDate, Int) => RemoveBeneficiary] = Map(
    "unidentified" -> Unidentified.apply,
    "individual" -> Individual.apply
  )

  private val validateBeneficiaryType = Reads.filter[String](ValidationError("Unexpected Beneficiary Type"))(builders.contains)

  implicit val reads: Reads[RemoveBeneficiary] =
    ((__ \ "type").read(validateBeneficiaryType) and
    (__ \ "endDate").read[LocalDate] and
    (__ \ "index").read[Int]).apply((typ, endDate, index) => builders(typ)(endDate, index))

  implicit val writes: Writes[RemoveBeneficiary] =
    ((__ \ "type").write[String] and
    (__ \ "endDate").write[LocalDate] and
    (__ \ "index").write[Int]).apply { (rb:RemoveBeneficiary) => rb match {
    case RemoveBeneficiary.Unidentified(endDate, index) => ("unidentified", endDate, index)
    case RemoveBeneficiary.Individual(endDate, index) => ("individual", endDate, index)
  }}

 }
