/*
 * Copyright 2024 HM Revenue & Customs
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

package repositories

import play.api.libs.json._

object MongoFormats {
  val booleanFormat: Format[Boolean] = new Format[Boolean] {
    override def reads(json: JsValue): JsResult[Boolean] = json match {
      case boolean: JsBoolean => JsSuccess(boolean.value)
      case JsNumber(value) => if (value == 1) JsSuccess(true) else if (value == 0) JsSuccess(false) else JsError("cannot parse boolean")
      case JsString(value) => JsSuccess(value.toBoolean)
      case _ => JsError("cannot parse boolean")
    }

    override def writes(o: Boolean): JsValue = if (o) JsTrue else JsFalse
  }

}
