/*
 * Copyright 2021 HM Revenue & Customs
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

package transformers.otherindividuals

import play.api.libs.functional.syntax._
import play.api.libs.json._
import transformers.AmendEntityTransform

import java.time.LocalDate

case class AmendOtherIndividualTransform(index: Option[Int],
                                         amended: JsValue,
                                         original: JsValue,
                                         endDate: LocalDate) extends OtherIndividualTransform with AmendEntityTransform

object AmendOtherIndividualTransform {

  val key = "AmendOtherIndividualTransform"

  implicit val format: Format[AmendOtherIndividualTransform] = Json.format[AmendOtherIndividualTransform]

  // TODO - remove code once deployed and users no longer using old transforms
  def reads: Reads[AmendOtherIndividualTransform] =
    ((__ \ "index").read[Int] and
      (__ \ "amended").read[JsValue] and
      (__ \ "original").read[JsValue] and
      (__ \ "endDate").read[LocalDate]).tupled.map {
      case (index, amended, original, endDate) =>
        AmendOtherIndividualTransform(Some(index), amended, original, endDate)
    }
}
