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

package uk.gov.hmrc.trusts.transformers
import java.time.LocalDate

import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, JsArray, JsObject, JsResult, JsValue, Json, Reads, Writes, __}
import play.api.libs.functional.syntax._

sealed trait RemoveBeneficiariesTransform extends DeltaTransform {
  def index : Int
  def beneficiaryData : JsObject
  def endDate : LocalDate
  def beneficiaryType : String

  private lazy val path = (__ \ "details" \ "trust" \
    "entities" \ "beneficiary" \ beneficiaryType )

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    val removeFromArray = __.json.pick[JsArray].map { arr =>
        JsArray(
          arr.value.zipWithIndex.filterNot(_._2 == index).map(_._1)
        )
    }

    val xform = path.json.update(removeFromArray)

    input.transform(xform)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    super.applyDeclarationTransform(input)

    if (isKnownToEtmp(beneficiaryData)) {
      val newBeneficiary = beneficiaryData.deepMerge(Json.obj("endDate" -> endDate))
      val appendToArray = __.json.pick[JsArray].map (_.append(newBeneficiary))
      val xform = path.json.update(appendToArray)
      input.transform(xform)
    } else {
      ???
    }
  }

  private def isKnownToEtmp(json: JsValue): Boolean = {
    json.transform((__ \ 'lineNo).json.pick).isSuccess
  }
}

object RemoveBeneficiariesTransform {
  val key = "RemoveBeneficiariesTransform"

  private val builders: Map[String, (LocalDate, Int, JsObject) => RemoveBeneficiariesTransform] = Map(
    "unidentified" -> Unidentified.apply,
    "individual" -> Individual.apply
  )

  private val validateBeneficiaryType = Reads.filter[String](ValidationError("Unexpected Beneficiary Type in transform reads"))(builders.contains)

  implicit val reads: Reads[RemoveBeneficiariesTransform] = Reads (js => {
    val x =    ((__ \ "type").read(validateBeneficiaryType) and
      (__ \ "endDate").read[LocalDate] and
      (__ \ "index").read[Int] and
      (__ \ "beneficiaryData").read[JsObject]).apply((typ, endDate, index, data) => builders(typ)(endDate, index, data))

    js.validate[RemoveBeneficiariesTransform](x)
  })

  implicit val writes: Writes[RemoveBeneficiariesTransform] =
    ((__ \ "type").write[String] and
      (__ \ "endDate").write[LocalDate] and
      (__ \ "index").write[Int] and
      (__ \ "beneficiaryData").write[JsObject]).apply { (rb:RemoveBeneficiariesTransform) => rb match {
        case RemoveBeneficiariesTransform.Unidentified(endDate, index, data) => ("unidentified", endDate, index, data)
        case RemoveBeneficiariesTransform.Individual(endDate, index, data) => ("individual", endDate, index, data)
    }}

  implicit val format: Format[RemoveBeneficiariesTransform] = Format(reads, writes)

  case class Unidentified(endDate: LocalDate, index: Int, beneficiaryData: JsObject) extends RemoveBeneficiariesTransform {
    val beneficiaryType = "unidentified"
  }

  case class Individual(endDate: LocalDate, index: Int, beneficiaryData: JsObject) extends RemoveBeneficiariesTransform {
    val beneficiaryType = "individual"
  }
}