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

import models.JsonWithoutNulls._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json._

class JsonWithoutNullsSpec extends AnyFreeSpec {

  "withoutNulls" - {

    "removes null fields from an object" in {
      Json.obj("a" -> 1, "b" -> JsNull).withoutNulls mustBe Json.obj("a" -> 1)
    }

    "leaves a value that is not an object unchanged" in {
      Json.arr(JsNull).withoutNulls mustBe Json.arr(JsNull)
    }
  }

}
