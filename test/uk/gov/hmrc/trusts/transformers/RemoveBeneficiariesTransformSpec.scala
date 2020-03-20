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

package uk.gov.hmrc.trusts.transformers

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json._
import uk.gov.hmrc.trusts.utils.JsonUtils

class RemoveBeneficiariesTransformSpec extends FreeSpec with MustMatchers with OptionValues  {

  private def originalBeneficiaryJson(value1 : String) = Json.obj("field1" -> value1, "field2" -> "value2")

  def buildInputJson(beneficiaryType: String, beneficiaryData: Seq[JsValue]) = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "trustOrEstateDisplay" \ "details" \ "trust" \ "entities" \ "beneficiary" \ beneficiaryType).json
      .put(JsArray(beneficiaryData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "the remove unidentified benficiary transform must" - {
    "remove an unidentified beneficiary from the list that is returned to the frontend" in {
      val inputJson = buildInputJson("unidentified", Seq(
        originalBeneficiaryJson("One"),
        originalBeneficiaryJson("Two"),
        originalBeneficiaryJson("Three")
      ))

      val expectedOutput = buildInputJson("unidentified", Seq(
        originalBeneficiaryJson("One"),
        originalBeneficiaryJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiariesTransform.Unidentified(LocalDate.of(2018, 4, 21), 1)))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too high" in {
      val inputJson = buildInputJson("unidentified", Seq(
        originalBeneficiaryJson("One"),
        originalBeneficiaryJson("Two"),
        originalBeneficiaryJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiariesTransform.Unidentified(LocalDate.of(2018, 4, 21), 10)))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {
      val inputJson = buildInputJson("unidentified", Seq(
        originalBeneficiaryJson("One"),
        originalBeneficiaryJson("Two"),
        originalBeneficiaryJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiariesTransform.Unidentified(LocalDate.of(2018, 4, 21), -1)))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }
  }
}
