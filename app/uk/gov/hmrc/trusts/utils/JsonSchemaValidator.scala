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

package uk.gov.hmrc.trusts.utils


import play.api.{Logger, Play}
import com.eclipsesource.schema.SchemaType
import play.api.data.validation.ValidationError
import play.api.libs.json._
import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Success, Try}
import com.eclipsesource.schema.SchemaValidator


trait JsonSchemaValidator {


  val registrationSchema : SchemaType



   def validateRequest(registration: String): Boolean = {

    Logger.info(s"Validating input request for registration against schema.")
    val validator = SchemaValidator()
    val validationResult: JsResult[JsValue] = validator.validate(registrationSchema, Json.parse(registration))

    validationResult.isSuccess match {
      case true => Logger.info(s"Validated registration payload against trusts schema successfully")
      case false => {
        val errors: Seq[(JsPath, Seq[ValidationError])] = validationResult.asEither.left.get

        errors.map {
          case (path, validationErrors) =>
            validationErrors.map(err => {
              for (e <- err.args) {

                val errorTransformer = (__ \ 'errors).json.pickBranch
                val errorsJsObj: JsObject = e.asInstanceOf[JsValue].transform(errorTransformer).get.as[JsObject]
                val errorsString = errorsJsObj.toString
                val regex = "\"value\":.*?,\"instancePath\"".r
                val errorsToReport: String = regex.replaceAllIn(errorsString, """"value": "PROTECTED", "instancePath"""")
                Logger.error(s"Validated against DES schema FAILED = $errorsToReport")

              }
            })
        }
      }
    }
    validationResult.isSuccess
  }
}




object TrustsRegistrationSchemaValidator extends JsonSchemaValidator {

  val jsonString = Source.fromFile(getClass.getResource("/resources/schemas/desSchemaRegistration_2.10.json").getPath).mkString
  val schemaType = Json.fromJson[SchemaType](Json.parse(jsonString))

  val registrationSchema : SchemaType = schemaType.get

}

