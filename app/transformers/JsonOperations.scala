/*
 * Copyright 2022 HM Revenue & Customs
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

package transformers

import play.api.libs.json._
import utils.Constants._
import utils.JsonOps.{prunePathAndPutNewValue, putNewValue}

import java.time.LocalDate

trait JsonOperations {

  def lineNoPick: Reads[JsValue] = (__ \ LINE_NUMBER).json.pick

  private def isKnownToEtmp(json: JsValue): Boolean = {
    json.transform(lineNoPick).isSuccess
  }

  def endEntity(input: JsValue, path: JsPath, entityJson: JsValue, endDate: LocalDate, endDateField: String = ENTITY_END): JsResult[JsValue] = {
    if (isKnownToEtmp(entityJson)) {
      addToList(input, path, objectPlusField(entityJson, endDateField, Json.toJson(endDate)))
    } else {
      JsSuccess(input)
    }
  }

  private def addTo(input: JsValue, path: JsPath, jsonToAdd: JsValue): JsResult[JsValue] = {
    input.transform(putNewValue(path, jsonToAdd))
  }

  def pruneThenAddTo(input: JsValue, path: JsPath, jsonToAdd: JsValue): JsResult[JsValue] = {
    input.transform(prunePathAndPutNewValue(path, jsonToAdd))
  }

  def addToList(input: JsValue, path: JsPath, jsonToAdd: JsValue): JsResult[JsValue] = {
    input.transform(path.json.pick[JsArray]) match {
      case JsSuccess(_, _) =>
        val updatedItems: Reads[JsObject] = path.json.update(
          Reads.of[JsArray].map { array =>
            array :+ jsonToAdd
          }
        )

        input.transform(updatedItems)
      case JsError(_) =>
        addTo(input, path, JsArray(Seq(jsonToAdd)))
    }
  }

  def amendAtPosition(input: JsValue, path: JsPath, index: Option[Int], newValue: JsValue): JsResult[JsValue] = {
    index match {
      case Some(i) =>
        input.transform(path.json.pick[JsArray]) match {
          case JsSuccess(array, _) =>
            val updated = (array.value.take(i) :+ newValue) ++ array.value.drop(i + 1)
            pruneThenAddTo(input, path, JsArray(updated))
          case e: JsError => e
        }
      case _ => JsError("Cannot amend at position if index is None")
    }
  }

  def removeAtPosition(input: JsValue, path: JsPath, index: Option[Int]): JsResult[JsValue] = {
    index match {
      case Some(i) =>
        input.transform(path.json.pick[JsArray]) match {
          case JsSuccess(array, _) =>
            val filtered = array.value.take(i) ++ array.value.drop(i + 1)
            if (filtered.isEmpty) {
              input.transform(path.json.prune)
            } else {
              pruneThenAddTo(input, path, JsArray(filtered))
            }
          case e: JsError => e
        }
      case _ => JsError("Cannot remove at position if index is None")
    }
  }

  def removeJsObjectFields(value: JsObject, fields: Seq[String]): JsObject = {
    fields.foldLeft[JsObject](value)((updated, field) => {
      updated - field
    })
  }

  def removeJsValueFields(value: JsValue, fields: Seq[String]): JsValue = {
    fields.foldLeft(value)((acc, field) => {
      acc.transform((__ \ field).json.prune) match {
        case JsSuccess(value, _) => value
        case _ => acc
      }
    })
  }

  def objectPlusField[A](json: JsValue, field: String, value: JsValue): JsValue = {
    json.as[JsObject] + (field -> value)
  }

  def copyField(original: JsValue, field: String, amended: JsValue): JsValue = {
    val pickField = (__ \ field).json.pick

    original.transform(pickField).fold(
      _ => amended,
      value => objectPlusField(amended, field, value)
    )
  }

  def merge(value1: JsValue, value2: JsValue): JsObject = {
    value1.as[JsObject].deepMerge(value2.as[JsObject])
  }

}
