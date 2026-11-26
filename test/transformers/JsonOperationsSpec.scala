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

package transformers

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json._

class JsonOperationsSpec extends AnyFreeSpec {

  private val operations = new JsonOperations {}

  private val path  = __ \ "items"
  private val input = Json.obj("items" -> Json.arr("a", "b", "c"))

  "amendAtPosition" - {

    "replaces the element at the index" in {
      operations.amendAtPosition(input, path, Some(1), JsString("x")).asOpt mustBe
        Some(Json.obj("items" -> Json.arr("a", "x", "c")))
    }

    "fails when there is no array at the path" in {
      operations.amendAtPosition(Json.obj(), path, Some(0), JsString("x")) mustBe a[JsError]
    }

    "fails when the index is None" in {
      operations.amendAtPosition(input, path, None, JsString("x")) mustBe
        JsError("Cannot amend at position if index is None")
    }
  }

  "removeAtPosition" - {

    "removes the element at the index" in {
      operations.removeAtPosition(input, path, Some(0)).asOpt mustBe Some(Json.obj("items" -> Json.arr("b", "c")))
    }

    "removes the array when its last element is removed" in {
      operations.removeAtPosition(Json.obj("items" -> Json.arr("a"), "other" -> 1), path, Some(0)).asOpt mustBe
        Some(Json.obj("other" -> 1))
    }

    "fails when there is no array at the path" in {
      operations.removeAtPosition(Json.obj(), path, Some(0)) mustBe a[JsError]
    }

    "fails when the index is None" in {
      operations.removeAtPosition(input, path, None) mustBe JsError("Cannot remove at position if index is None")
    }
  }

  "removeJsValueFields" - {

    "removes the fields from an object" in {
      operations.removeJsValueFields(Json.obj("a" -> 1, "b" -> 2), Seq("a")) mustBe Json.obj("b" -> 2)
    }

    "returns the value unchanged when it is not an object" in {
      operations.removeJsValueFields(JsString("value"), Seq("a")) mustBe JsString("value")
    }
  }

}
