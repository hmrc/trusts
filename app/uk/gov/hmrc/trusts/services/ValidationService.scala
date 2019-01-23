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

import scala.collection.JavaConverters._
import scala.io.Source


class ValidationService @Inject()() {


  private val factory = JsonSchemaFactory.byDefault()

  def get(schemaFile : String ) : Validator = {
    val schemaJsonFileString = Source.fromFile(getClass.getResource(schemaFile).getPath).mkString
    val schemaJson = JsonLoader.fromString(schemaJsonFileString)
    val schema = factory.getJsonSchema(schemaJson)
    new Validator(schema)
  }

}


class Validator(schema : JsonSchema) {

  def validate (inputJson : String) : List[String] = {
    val json = JsonLoader.fromString(inputJson)
    val result = schema.validate(json)

    if(result.isSuccess){
      List.empty
    } else {
      result.iterator.asScala.toList.map {
        _.getMessage
      }
    }

  }

}



