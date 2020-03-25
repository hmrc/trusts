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

package uk.gov.hmrc.trusts.transformers

import play.api.libs.json.{JsArray, JsError, JsPath, JsResult, JsSuccess, JsValue, Json}

trait JsonOperations {

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

  def removeAtPosition(input : JsValue, path: JsPath, index: Int, newValue: JsValue) : JsResult[JsValue] = {

    input.transform(path.json.pick) match {
      case JsSuccess(json, _) =>

        val array = json.as[JsArray]

        val filtered = array.value.take(index) ++ array.value.drop(index + 1)

        input.transform(
          path.json.prune andThen
            JsPath.json.update {
              path.json.put(Json.toJson(filtered))
            }
        )

      case e: JsError => e
    }
  }

}
