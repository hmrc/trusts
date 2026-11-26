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

class EnumerableSpec extends AnyFreeSpec with Enumerable.Implicits {

  sealed private trait Colour
  private case object Red extends Colour
  private case object Blue extends Colour

  implicit private val colours: Enumerable[Colour] = Enumerable("Red" -> Red, "Blue" -> Blue)

  "Enumerable" - {

    "reads a known name" in {
      JsString("Blue").validate[Colour] mustBe JsSuccess(Blue)
    }

    "fails for an unknown name" in {
      JsString("Green").validate[Colour] mustBe JsError("error.invalid")
    }

    "fails for a value that is not a string" in {
      JsNumber(1).validate[Colour] mustBe JsError("error.invalid")
    }

    "writes the name" in {
      Json.toJson[Colour](Red) mustBe JsString("Red")
    }
  }

}
