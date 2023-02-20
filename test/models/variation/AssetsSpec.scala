/*
 * Copyright 2023 HM Revenue & Customs
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

package models.variation

import base.BaseSpec
import models.AddressType
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json

import java.time.LocalDate

class AssetsSpec extends BaseSpec {

  "NonEEABusinessType" when {
    "create the correct json when serialised and has a LineNo" in {
      val asset = NonEEABusinessType(
        lineNo = Some("1"),
        orgName = "Panda care Ltd",
        address = AddressType("1010 EASY ST", "OTTAWA", Some("ONTARIO"), Some("ONTARIO"), None, "CA"),
        govLawCountry = "CA",
        startDate = LocalDate.of(2020, 1, 5),
        endDate = Some(LocalDate.of(2021, 1, 5))
      )

      val result = Json.parse(
        """
          | {
          |   "lineNo": "1",
          |   "orgName": "Panda care Ltd",
          |   "address": {
          |     "country": "CA",
          |     "line1": "1010 EASY ST",
          |     "line2": "OTTAWA",
          |     "line3": "ONTARIO",
          |     "line4": "ONTARIO"
          |   },
          |   "govLawCountry": "CA",
          |   "startDate": "2020-01-05",
          |   "endDate": "2021-01-05",
          |   "provisional": false
          | }
          |""".stripMargin)

      Json.toJson(asset)(NonEEABusinessType.writeToMaintain) mustBe result

    }

    "create the correct json when serialised and has no LineNo" in {
      val asset = NonEEABusinessType(
        lineNo = None,
        orgName = "Panda care Ltd",
        address = AddressType("1010 EASY ST", "OTTAWA", Some("ONTARIO"), Some("ONTARIO"), None, "CA"),
        govLawCountry = "CA",
        startDate = LocalDate.of(2020, 1, 5),
        endDate = Some(LocalDate.of(2021, 1, 5))
      )

      val result = Json.parse(
        """
          | {
          |   "orgName": "Panda care Ltd",
          |   "address": {
          |     "country": "CA",
          |     "line1": "1010 EASY ST",
          |     "line2": "OTTAWA",
          |     "line3": "ONTARIO",
          |     "line4": "ONTARIO"
          |   },
          |   "govLawCountry": "CA",
          |   "startDate": "2020-01-05",
          |   "endDate": "2021-01-05",
          |   "provisional": true
          | }
          |""".stripMargin)

      Json.toJson(asset)(NonEEABusinessType.writeToMaintain) mustBe result

    }
  }
}
