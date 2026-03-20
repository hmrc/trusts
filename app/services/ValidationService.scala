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

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.Schema

import com.networknt.schema.{SchemaRegistry, SpecificationVersion}
import models.Registration
import play.api.Logging
import play.api.libs.json._
import utils.BusinessValidation

import javax.inject.{Inject, Singleton}
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@Singleton
class ValidationService @Inject() () {

  private val schemaMapper: ObjectMapper = new ObjectMapper()

  private val schemaRegistry: SchemaRegistry =
    SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4)

  def get(schemaFile: String): Validator = {
    val source               = Source.fromInputStream(getClass.getResourceAsStream(schemaFile))
    val schemaJsonFileString = source.mkString
    source.close()
    val schemaNode           = schemaMapper.readTree(schemaJsonFileString)
    val schema               = schemaRegistry.getSchema(schemaNode)
    println("schema ---" + schema)
    new Validator(schema, schemaMapper)
  }

}

class Validator(schema: Schema, mapper: ObjectMapper) extends Logging {

  def validate[T](inputJson: String)(implicit reads: Reads[T]): Either[List[TrustsValidationError], T] =
    Try(mapper.readTree(inputJson)) match {
      case Success(jsonNode) =>
        val result = schema.validate(jsonNode)
        println("result----------" + result)
        if (result.isEmpty) {
          println("inside if")
          Json
            .parse(inputJson)
            .validate[T]
            .fold(
              _ => Left(getValidationErrors(result)),
              request => validateBusinessRules(request)
            )
        } else {
          println("inside else")
          logger.error(s"[Validator][validate] unable to validate to schema")
          Left(getValidationErrors(result))
        }
      case Failure(e)        =>
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
    errors: java.util.List[com.networknt.schema.Error]
  ): List[TrustsValidationError] = {
    val validationErrors = errors.asScala.toList
      .map { err =>
        val msg = err.getMessage
        val loc = err.getInstanceLocation.toString
        TrustsValidationError(msg, loc)
      }
    logger.debug(s"[Validator][getValidationErrors] validationErrors in validate :  $validationErrors")
    println("validationErrors" + validationErrors)
    validationErrors
  }

}

case class TrustsValidationError(message: String, location: String)

object TrustsValidationError {
  implicit val formats: Format[TrustsValidationError] = Json.format[TrustsValidationError]
}
