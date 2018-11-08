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
import play.api.libs.json.Reads._
import play.api.data.validation.ValidationError

case class ExistingTrustCheckRequest(name: String, postcode: Option[String] = None, utr: String){
}

object ExistingTrustCheckRequest {

  private val validationError = ValidationError("")
  private val utrValidation: Reads[String] =
    Reads.StringReads.filter(validationError)(
      utrValidationRegEx.findFirstIn(_).isDefined
    )
  private val utrValidationRegEx = """^[0-9]{10}$""".r

  private val postcodeValidation: Reads[String] =
    Reads.StringReads.filter(validationError)(
      postcodeRegEx.findFirstIn(_).isDefined
    )
  private val postcodeRegEx = """^[a-zA-Z0-9]{1,10}$""".r

  private val nameValidation: Reads[String] =
    Reads.StringReads.filter(validationError)(
      nameRegEx.findFirstIn(_).isDefined
    )

  private val nameRegEx = """[A-Za-z0-9 ,.()/&'-]{1,56}$""".r

  implicit val writes: Writes[ExistingTrustCheckRequest] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "postcode").writeNullable[String] and
      (JsPath \ "sa-utr").write[String]
    ) (c => (
    c.name,
    c.postcode,
    c.utr))

  implicit val reads: Reads[ExistingTrustCheckRequest] = (
    (JsPath \ "name").read[String](nameValidation) and
      (JsPath \ "postcode").readNullable[String](postcodeValidation) and
      (JsPath \ "utr").read[String](utrValidation)
  )(ExistingTrustCheckRequest.apply _)






}






case class ErrorResponse(message: String, code: String)

object ErrorResponse {
  implicit val formats = Json.format[ErrorResponse]
}
