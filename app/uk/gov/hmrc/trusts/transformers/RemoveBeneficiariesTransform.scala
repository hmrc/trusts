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

import play.api.libs.json.{JsArray, JsResult, JsValue, Reads, __}

sealed trait RemoveBeneficiariesTransform extends DeltaTransform {
  def index : Int

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    val removeFromArray = __.json.pick[JsArray].map { arr =>
        JsArray(
          arr.value.zipWithIndex.filterNot(_._2 == index).map(_._1)
        )
    }

    val xform = (__ \ "trustOrEstateDisplay" \ "details" \ "trust" \
      "entities" \ "beneficiary" \ "unidentified" ).json.update(removeFromArray)

    input.transform(xform)
  }
}

object RemoveBeneficiariesTransform {
  case class Unidentified(endDate: LocalDate, index: Int) extends RemoveBeneficiariesTransform
  case class Individual(endDate: LocalDate, index: Int) extends RemoveBeneficiariesTransform
}