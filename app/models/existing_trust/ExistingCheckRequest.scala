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

package models.existing_trust

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class ExistingCheckRequest(name: String, postcode: Option[String] = None, utr: String)

object ExistingCheckRequest {

  private def validationError(msg: String) = JsonValidationError(msg)
  private val utrValidationRegEx = """^[0-9]{10}$""".r
  private val postcodeRegEx ="""^[a-zA-Z]{1,2}[0-9][0-9a-zA-Z]?\s?[0-9][a-zA-Z]{2}$""".r
  private val nameRegEx = """^[A-Za-z0-9 ,.()/&'-]{1,56}$""".r

  private val utrValidation: Reads[String] =
    Reads.StringReads.filter(validationError("Invalid UTR"))(
      utrValidationRegEx.findFirstIn(_).isDefined
    )

  private val postcodeValidation: Reads[String] =
    Reads.StringReads.filter(validationError("Invalid postcode"))(
      postcodeRegEx.findFirstIn(_).isDefined
    )

  private val nameValidation: Reads[String] =
    Reads.StringReads.filter(validationError("Invalid name"))(
      nameRegEx.findFirstIn(_).isDefined
    )


  implicit val writes: Writes[ExistingCheckRequest] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "postcode").writeNullable[String] and
      (JsPath \ "sa-utr").write[String]
    ) (c => (
    c.name,
    c.postcode,
    c.utr))

  implicit val reads: Reads[ExistingCheckRequest] = (
    (JsPath \ "name").read[String](nameValidation) and
      (JsPath \ "postcode").readNullable[String](postcodeValidation) and
      (JsPath \ "utr").read[String](utrValidation)
  )(ExistingCheckRequest.apply _)

}



