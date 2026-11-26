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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json._

class RichJsValueSpec extends AnyFreeSpec {

  private val value: JsValue = JsString("value")

  "set" - {

    "fails for an empty path" in {
      Json.obj().set(__, value) mustBe JsError("path cannot be empty")
    }

    "fails for a recursive search" in {
      Json.obj().set(__ \\ "a", value) mustBe JsError("recursive search not supported")
    }

    "sets a key on an object" in {
      Json.obj("a" -> 1).set(__ \ "b", value) mustBe JsSuccess(Json.obj("a" -> 1, "b" -> value))
    }

    "fails to set a key on something that is not an object" in {
      Json.arr().set(__ \ "a", value) mustBe a[JsError]
    }

    "replaces an existing array element" in {
      Json.arr(1, 2).set(__(1), value) mustBe JsSuccess(Json.arr(1, value))
    }

    "appends when the index is the array length" in {
      Json.arr(1).set(__(1), value) mustBe JsSuccess(Json.arr(1, value))
    }

    "fails when the index is out of bounds" in {
      Json.arr(1).set(__(5), value)  mustBe a[JsError]
      Json.arr(1).set(__(-1), value) mustBe a[JsError]
    }

    "fails to set an index on something that is not an array" in {
      Json.obj().set(__(0), value) mustBe a[JsError]
    }

    "creates a missing parent object" in {
      Json.obj().set(__ \ "a" \ "b", value) mustBe JsSuccess(Json.obj("a" -> Json.obj("b" -> value)))
    }

    "creates a missing parent array" in {
      Json.obj().set((__ \ "a")(0), value) mustBe JsSuccess(Json.obj("a" -> Json.arr(value)))
    }

    "updates inside an existing parent" in {
      Json.obj("a" -> Json.obj("b" -> 1)).set(__ \ "a" \ "c", value) mustBe
        JsSuccess(Json.obj("a" -> Json.obj("b" -> 1, "c" -> value)))
    }

    "fails for a recursive search below a missing parent" in {
      Json.obj().set(__ \ "a" \\ "b", value) mustBe JsError("recursive search is not supported")
    }
  }

  "remove" - {

    "fails for an empty path" in {
      Json.obj().remove(__) mustBe JsError("path cannot be empty")
    }

    "removes an existing key" in {
      Json.obj("a" -> 1, "b" -> 2).remove(__ \ "a") mustBe JsSuccess(Json.obj("b" -> 2))
    }

    "fails when the key does not exist" in {
      Json.obj("a" -> 1).remove(__ \ "b") mustBe JsError("cannot find value at path")
    }

    "fails to remove a key from something that is not an object" in {
      Json.arr().remove(__ \ "a") mustBe a[JsError]
    }

    "removes an array element" in {
      Json.arr(1, 2, 3).remove(__(1)) mustBe JsSuccess(Json.arr(1, 3))
    }

    "fails when the index is out of bounds" in {
      Json.arr(1).remove(__(1))  mustBe a[JsError]
      Json.arr(1).remove(__(-1)) mustBe a[JsError]
    }

    "removes a nested key" in {
      Json.obj("a" -> Json.obj("b" -> 1, "c" -> 2)).remove(__ \ "a" \ "b") mustBe
        JsSuccess(Json.obj("a" -> Json.obj("c" -> 2)))
    }

    "fails for a nested key below a missing parent object" in {
      Json.obj().remove(__ \ "a" \ "b") mustBe JsError("cannot find value at path")
    }

    "fails for an index below a missing parent array" in {
      Json.obj().remove((__ \ "a")(0)) mustBe a[JsError]
    }

    "fails for a recursive search below a missing parent" in {
      Json.obj().remove(__ \ "a" \\ "b") mustBe JsError("recursive search is not supported")
    }
  }

}
