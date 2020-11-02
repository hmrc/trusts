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

package transformers.trustDetails

import play.api.libs.json._
import transformers.{DeltaTransform, JsonOperations}

case class SetPropertyTransform(express: Boolean) extends DeltaTransform with JsonOperations {

  private lazy val path = __ \ 'details \ 'trustUKProperty

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    addToList(input, path, Json.toJson(express))
  }

}

object SetPropertyTransform {

  val key = "SetPropertyTransform"

  implicit val format: Format[SetPropertyTransform] = Json.format[SetPropertyTransform]
}