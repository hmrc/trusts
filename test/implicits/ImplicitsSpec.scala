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

package implicits

import base.BaseSpec
import implicits.Implicits._
import org.scalatest.matchers.must.Matchers._

import java.time.{LocalDate, MonthDay}

class ImplicitsSpec extends BaseSpec {

  "Implicits" when {

    "LocalDateImplicits" when {

      val date = LocalDate.of(1996, 2, 3)

      "next" must {

        "return following year" when {
          "month/day is before date" in {
            val monthDay = MonthDay.of(2, 2)
            val result   = date.next(monthDay)
            result mustBe LocalDate.of(1997, 2, 2)
          }
        }

        "return same year" when {

          "month/day is same as date" in {
            val monthDay = MonthDay.of(2, 3)
            val result   = date.next(monthDay)
            result mustBe LocalDate.of(1996, 2, 3)
          }

          "month/day is after date" in {

            val monthDay = MonthDay.of(2, 4)
            val result   = date.next(monthDay)
            result mustBe LocalDate.of(1996, 2, 4)
          }
        }
      }

      "getMonthDay" must {
        "return month/day representation of given date" in {
          val result = date.getMonthDay
          result mustBe MonthDay.of(2, 3)
        }
      }
    }
  }

}
