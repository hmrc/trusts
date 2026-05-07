/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import com.networknt.schema.{Error, InputFormat, Schema, SchemaRegistry, SpecificationVersion}
import models.Registration
import play.api.Logging
import play.api.libs.json._
import utils.BusinessValidation

import java.io.InputStream
import javax.inject.{Inject, Singleton}
import scala.io.Source
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.util.Using

@Singleton
class ValidationService @Inject() () {

  def get(schemaFile: String): Validator = {
    val resource = resourceAsString(schemaFile)
      .getOrElse(throw new RuntimeException("Missing schema: " + schemaFile))

    val schema = SchemaRegistry
      .withDefaultDialect(SpecificationVersion.DRAFT_4)
      .getSchema(resource)

    new Validator(schema)
  }

  private def resourceAsString(resourcePath: String): Option[String] =
    resourceAsInputStream(resourcePath).flatMap { is =>
      Using(Source.fromInputStream(is))(_.getLines().mkString("\n")).toOption
    }

  private def resourceAsInputStream(resourcePath: String): Option[InputStream] =
    Option(getClass.getResourceAsStream(resourcePath))

}

class Validator(schema: Schema) extends Logging {

  def validate[T](inputJson: String)(implicit reads: Reads[T]): Either[List[TrustsValidationError], T] =
    try {
      val validationOutput: List[Error] = schema.validate(inputJson, InputFormat.JSON).asScala.toList
      if (validationOutput.isEmpty) {
        Json
          .parse(inputJson)
          .validate[T]
          .fold(
            errors => Left(getValidationErrors(errors)),
            request => validateBusinessRules(request)
          )
      } else {
        val validationErrors = getValidationErrors(validationOutput)
        logger.error(s"[Validator][validate] unable to validate to schema ${validationErrors.mkString}")

        Left(validationErrors)
      }
    } catch {
      case e: Exception =>
        logger.error(s"[Validator][validate] IOException $e")
        Left(List(TrustsValidationError(s"[Validator][validate] IOException $e", "")))
    }

  private def validateBusinessRules[T](request: T): Either[List[TrustsValidationError], T] =
    request match {
      case registration: Registration =>
        BusinessValidation.check(registration) match {
          case Nil             => Right(request)
          case errors @ _ :: _ =>
            logger.error(s"[Validator][validateBusinessRules] Validation fails : $errors")
            Left(errors)
        }
      case _                          => Right(request)
    }

  private def getValidationErrors(
    validationOutput: List[Error]
  ): List[TrustsValidationError] = {
    val validationErrors = validationOutput
      .map { errors =>
        TrustsValidationError(errors.getMessage, errors.getInstanceLocation.toString)
      }
    logger.debug(s"[Validator][getValidationErrors] validationErrors in validate :  $validationErrors")
    validationErrors
  }

  private def getValidationErrors(
    errors: Iterable[(JsPath, Iterable[JsonValidationError])]
  ): List[TrustsValidationError] = {
    val validationErrors = errors
      .flatMap(errors => errors._2.map(error => TrustsValidationError(error.message, errors._1.toString())))
      .toList
    logger.debug(s"[Validator][getValidationErrors] validationErrors in validate :  $validationErrors")
    validationErrors
  }

}

case class TrustsValidationError(message: String, location: String)

object TrustsValidationError {
  implicit val formats: Format[TrustsValidationError] = Json.format[TrustsValidationError]
}
