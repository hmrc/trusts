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

package transformers.protectors

import play.api.libs.json._
import transformers.AddEntityTransform

case class AddProtectorTransform(entity: JsValue,
                                 `type`: String) extends ProtectorTransform with AddEntityTransform

object AddProtectorTransform {

  val key = "AddProtectorTransform"

  implicit val format: Format[AddProtectorTransform] = Json.format[AddProtectorTransform]

  // TODO - remove code once deployed and users no longer using old transforms
  def reads[T](`type`: String, field: String)(implicit rds: Reads[T], wts: Writes[T]): Reads[AddProtectorTransform] =
    (__ \ field).read[T].map {
      entity => AddProtectorTransform(Json.toJson(entity), `type`)
    }
}
