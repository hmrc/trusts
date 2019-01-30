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

package uk.gov.hmrc.trusts.services

import javax.inject.Inject

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import play.api.Logger
import play.api.libs.json.{JsPath, Json, Reads}
import com.github.fge.jsonschema.core.report.LogLevel.ERROR
import com.github.fge.jsonschema.core.report.ProcessingReport
import play.api.data.validation.ValidationError
import play.api.mvc.Result
import uk.gov.hmrc.trusts.models.Registration

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.io.Source


class ValidationService @Inject()() {


  private val factory = JsonSchemaFactory.byDefault()

  def get(schemaFile: String): Validator = {
    val schemaJsonFileString = Source.fromFile(getClass.getResource(schemaFile).getPath).mkString
    val schemaJson = JsonLoader.fromString(schemaJsonFileString)
    val schema = factory.getJsonSchema(schemaJson)
    new Validator(schema)
  }

}


class Validator(schema: JsonSchema) {

  private val JsonErrorMessageTag = "message"
  private val JsonErrorInstanceTag = "instance"
  private val JsonErrorPointerTag = "pointer"

  def validate[T](inputJson: String)(implicit reads: Reads[T]): Either[List[TrustsValidationError], T] = {
    val json = JsonLoader.fromString(inputJson)
    val result = schema.validate(json)
    Logger.error(s"[validate]result isSuccess :  ${result.isSuccess}")
    if (result.isSuccess) {
      Json.parse(inputJson).validate[T].fold(
        errors => Left(getValidationErrors(errors)),
        request => Right(request)
      )
    } else {
      Left(getValidationErrors(result))
    }

  }


  protected def getValidationErrors(errors: Seq[(JsPath, Seq[ValidationError])]): List[TrustsValidationError] = {
    val validationErrors = errors.flatMap(errors => errors._2.map(error => TrustsValidationError(error.message, errors._1.toString()))).toList
    Logger.debug(s"[Validator][getValidationErrors]  validationErrors in validate :  $validationErrors")
    validationErrors
  }


  private def getValidationErrors(validationOutput: ProcessingReport): List[TrustsValidationError] = {
    val validationErrors: List[TrustsValidationError] = validationOutput.iterator.asScala.toList.filter(m => m.getLogLevel == ERROR).map(m => {
      val error = m.asJson()
      val message = error.findValue(JsonErrorMessageTag).asText("")
      val location = error.findValue(JsonErrorInstanceTag).at(s"/$JsonErrorPointerTag").asText()
      val locations = error.findValues(JsonErrorPointerTag)
      Logger.error(s"validation failed at locations :  ${locations}")
      TrustsValidationError(message, location)
    })
    validationErrors
  }

}

case class TrustsValidationError(message: String, location: String)

object TrustsValidationError {
  implicit val formats = Json.format[TrustsValidationError]
}



