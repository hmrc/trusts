/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.Reads.of
import play.api.libs.json._
import utils.Constants._

trait AddOrAmendTransform extends JsonOperations {

  val trustTypeDependentFields: Seq[String] = Nil

  def removeTrustTypeDependentFields(value: JsValue): JsValue = removeJsValueFields(value, trustTypeDependentFields)

  def prepareInputForDeclaration(input: JsValue, entity: JsValue, path: JsPath): JsResult[JsValue] = {
    removeIsPassportField(input, entity, path)
  }

  private def removeIsPassportField(input: JsValue, entity: JsValue, path: JsPath): JsResult[JsValue] = {
    input.transform(path.json.pick) match {
      case JsSuccess(_: JsArray, _) =>
        val entityTransform = (__ \ IDENTIFICATION \ PASSPORT \ IS_PASSPORT).json.prune

        val arrayTransform = path.json.update(of[JsArray].map {
          case JsArray(xs) => JsArray(xs map { x =>
            if (x == entity) x.transform(entityTransform).getOrElse(x) else x
          })
        })

        input.transform(arrayTransform)
      case _ =>
        JsSuccess(input)
    }
  }

}
