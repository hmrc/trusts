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

package uk.gov.hmrc.trusts.models

import java.time.LocalDate

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsError, JsSuccess, Json}

class RemoveBeneficiarySpecs extends WordSpec with MustMatchers {
  "Can round trip throuh Json" in {
    val OUT = RemoveBeneficiary.Unidentified(LocalDate.of(1478, 12, 31), 67)

    Json.toJson(OUT).validate[RemoveBeneficiary] mustBe JsSuccess(OUT)
  }

  "Can deserialise" when {
    "beneficiary is unidentified" in {
      val json = Json.obj(
        "type" -> "unidentified",
        "endDate" -> LocalDate.of(2019, 12, 31),
        "index" -> 6
      )

      json.as[RemoveBeneficiary] mustBe (RemoveBeneficiary.Unidentified(LocalDate.of(2019, 12, 31), 6))
    }

    "beneficiary is IndividualType" in {
      val json = Json.obj(
        "type" -> "individual",
        "endDate" -> LocalDate.of(2019, 12, 31),
        "index" -> 6
      )

      json.as[RemoveBeneficiary] mustBe (RemoveBeneficiary.Individual(LocalDate.of(2019, 12, 31), 6))
    }
  }

  "Fail to deserialise" when {
    "beneficiray type is not understood" in  {
      val json = Json.obj(
        "type" -> "INVALID TYPE",
        "endDate" -> LocalDate.of(2019, 12, 31),
        "index" -> 6
      )

      json.validate[RemoveBeneficiary] mustBe a[JsError]
    }
  }
}
