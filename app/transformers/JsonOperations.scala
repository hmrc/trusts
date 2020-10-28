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

package transformers

import java.time.LocalDate

import play.api.libs.json._

trait JsonOperations {

  def lineNoPick: Reads[JsValue] = (__ \ 'lineNo).json.pick

  def isKnownToEtmp(json: JsValue): Boolean = {
    json.transform(lineNoPick).isSuccess
  }

  def getTypeAtPosition[T](input: JsValue,
                           path: JsPath,
                           index: Int)
                       (implicit reads: Reads[T]): T = {

    input.transform(path.json.pick) match {

      case JsSuccess(json, _) =>

        val list = json.as[JsArray].value.toList

        list(index).validate[T] match {
          case JsSuccess(value, _) =>
            value

          case JsError(errors) =>
            throw JsResultException(errors)
        }
      case JsError(errors) =>
        throw JsResultException(errors)
    }
  }

   def endEntity(input: JsValue, path: JsPath, entityJson: JsValue, endDate: LocalDate): JsResult[JsValue] = {
    if (isKnownToEtmp(entityJson)) {
      addToList(input, path, objectPlusField(entityJson,"entityEnd", Json.toJson(endDate)))
    } else {
      JsSuccess(input)
    }
  }

  def addToList(input: JsValue,
                path: JsPath,
                jsonToAdd: JsValue) : JsResult[JsValue] = {

    import play.api.libs.json._

    input.transform(path.json.pick[JsArray]) match {
      case JsSuccess(_, _) =>

        val updatedItems: Reads[JsObject] = path.json.update(
          Reads.of[JsArray].map { array =>
              array :+ jsonToAdd
            }
          )

        input.transform(updatedItems)
      case JsError(_) =>
        input.transform(__.json.update {
          path.json.put(JsArray(
            Seq(jsonToAdd))
          )
        })
    }
  }

  def amendAtPosition(input : JsValue, path: JsPath, index: Int, newValue: JsValue) : JsResult[JsValue] = {
    input.transform(path.json.pick) match {

      case JsSuccess(json, _) =>

        val array = json.as[JsArray]

        val updated = (array.value.take(index) :+ newValue) ++ array.value.drop(index + 1)

        input.transform(
          path.json.prune andThen
            JsPath.json.update {
              path.json.put(Json.toJson(updated))
            }
        )

      case e: JsError => e
    }
  }

  def removeAtPosition(input : JsValue, path: JsPath, index: Int) : JsResult[JsValue] = {

    input.transform(path.json.pick) match {
      case JsSuccess(json, _) =>

        val array = json.as[JsArray]

        val filtered = array.value.take(index) ++ array.value.drop(index + 1)
        if (filtered.isEmpty) {
          input.transform(path.json.prune)
        } else {
          input.transform(
            path.json.prune andThen
              JsPath.json.update {
                path.json.put(Json.toJson(filtered))
              }
          )
        }

      case e: JsError => e
    }
  }

  def objectPlusField[A](json: JsValue, field: String, value: JsValue) : JsValue = json.as[JsObject] + (field -> value)

  def copyField(original: JsValue, field: String, amended: JsValue): JsValue = {
    val pickField = (__ \ field).json.pick

    original.transform(pickField).fold(
      _ => amended,
      value => objectPlusField(amended, field, value)
    )
  }
}
