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

package transformers.settlors

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsPath, JsValue, Json, __}

import java.time.LocalDate

case class TestAmendSettlorTransform(index: Int,
                                amended: JsValue,
                                original: JsValue,
                                endDate: LocalDate)  extends AmendSettlorTransform {
  override val path: JsPath = __ \ 'settlors \ 'settlorEntities
}

class AmendSettlorTransformSpec extends FreeSpec with MustMatchers {
  "AmendSettlorTransform should" - {

    "before declaration" - {

      "amend non-ETMP settlor details by replacing the settlor" in {
        val amended = Json.parse(
          """
            |{
            | "field1": "newValue1",
            | "field3": "newValue3"
            |}
            |""".stripMargin)

        val original = Json.parse(
          """
            |{
            | "field1": "value1",
            | "field2": "value2"
            |}
            |""".stripMargin)

        val input = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "field1": "value1",
            |       "field2": "value2"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        val transform = TestAmendSettlorTransform(1, amended, original, LocalDate.of(2012, 2, 20))
        val result = transform.applyTransform(input)
        val expected = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        result.get mustBe expected
      }
      "amend matched ETMP settlor details by replacing the settlor" in {
        val amended = Json.parse(
          """
            |{
            | "field1": "newValue1",
            | "field3": "newValue3"
            |}
            |""".stripMargin)

        val original = Json.parse(
          """
            |{
            | "lineNo": 3,
            | "bpMatchStatus": "01",
            | "field1": "value1",
            | "field2": "value2"
            |}
            |""".stripMargin)

        val input = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "lineNo": 3,
            |       "bpMatchStatus": "01",
            |       "field1": "value1",
            |       "field2": "value2"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        val transform = TestAmendSettlorTransform(1, amended, original, LocalDate.of(2012, 2, 20))
        val result = transform.applyTransform(input)
        val expected = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "lineNo": 3,
            |       "bpMatchStatus": "01",
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        result.get mustBe expected
      }
      "amend unmatched ETMP settlor details by replacing the settlor" in {
        val amended = Json.parse(
          """
            |{
            | "field1": "newValue1",
            | "field3": "newValue3"
            |}
            |""".stripMargin)

        val original = Json.parse(
          """
            |{
            | "lineNo": 3,
            | "field1": "value1",
            | "field2": "value2"
            |}
            |""".stripMargin)

        val input = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "lineNo": 3,
            |       "field1": "value1",
            |       "field2": "value2"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        val transform = TestAmendSettlorTransform(1, amended, original, LocalDate.of(2012, 2, 20))
        val result = transform.applyTransform(input)
        val expected = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "lineNo": 3,
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        result.get mustBe expected
      }
    }
    "at declaration time" - {

      "do nothing for non-ETMP settlor" in {
        val amended = Json.parse(
          """
            |{
            | "field1": "newValue1",
            | "field3": "newValue3"
            |}
            |""".stripMargin)

        val original = Json.parse(
          """
            |{
            | "field1": "value1",
            | "field2": "value2"
            |}
            |""".stripMargin)

        val input = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        val transform = TestAmendSettlorTransform(1, amended, original, LocalDate.of(2012, 2, 20))
        val result = transform.applyDeclarationTransform(input)
        val expected = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        result.get mustBe expected
      }
      "add ended entity and strip ETMP status for matched ETMP settlor" in {
        val amended = Json.parse(
          """
            |{
            | "field1": "newValue1",
            | "field3": "newValue3"
            |}
            |""".stripMargin)

        val original = Json.parse(
          """
            |{
            | "lineNo": 3,
            | "bpMatchStatus": "01",
            | "field1": "value1",
            | "field2": "value2"
            |}
            |""".stripMargin)

        val input = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "lineNo": 3,
            |       "bpMatchStatus": "01",
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        val transform = TestAmendSettlorTransform(1, amended, original, LocalDate.of(2012, 2, 20))
        val result = transform.applyDeclarationTransform(input)
        val expected = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     },
            |     {
            |       "lineNo": 3,
            |       "bpMatchStatus": "01",
            |       "field1": "value1",
            |       "field2": "value2",
            |       "entityEnd": "2012-02-20"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        result.get mustBe expected
      }
      "add ended entity and strip ETMP status for matched ETMP settlor in different position" in {
        val amended = Json.parse(
          """
            |{
            | "field1": "newValue1",
            | "field3": "newValue3"
            |}
            |""".stripMargin)

        val original = Json.parse(
          """
            |{
            | "lineNo": 3,
            | "bpMatchStatus": "01",
            | "field1": "value1",
            | "field2": "value2"
            |}
            |""".stripMargin)

        val input = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "lineNo": 3,
            |       "bpMatchStatus": "01",
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        val transform = TestAmendSettlorTransform(1, amended, original, LocalDate.of(2012, 2, 20))
        val result = transform.applyDeclarationTransform(input)
        val expected = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "newValue1",
            |       "field3": "newValue3"
            |     },
            |     {
            |       "lineNo": 3,
            |       "bpMatchStatus": "01",
            |       "field1": "value1",
            |       "field2": "value2",
            |       "entityEnd": "2012-02-20"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        result.get mustBe expected
      }
      "add ended entity for matched ETMP settlor that has been deleted" in {
        val amended = Json.parse(
          """
            |{
            | "field1": "newValue1",
            | "field3": "newValue3"
            |}
            |""".stripMargin)

        val original = Json.parse(
          """
            |{
            | "lineNo": 3,
            | "bpMatchStatus": "01",
            | "field1": "value1",
            | "field2": "value2"
            |}
            |""".stripMargin)

        val input = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        val transform = TestAmendSettlorTransform(1, amended, original, LocalDate.of(2012, 2, 20))
        val result = transform.applyDeclarationTransform(input)
        val expected = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "field1": "valueA",
            |       "field2": "valueB"
            |     },
            |     {
            |       "lineNo": 3,
            |       "bpMatchStatus": "01",
            |       "field1": "value1",
            |       "field2": "value2",
            |       "entityEnd": "2012-02-20"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        result.get mustBe expected
      }
      "add ended entity for matched ETMP settlor with all settlors deleted" in {
        val amended = Json.parse(
          """
            |{
            | "field1": "newValue1",
            | "field3": "newValue3"
            |}
            |""".stripMargin)

        val original = Json.parse(
          """
            |{
            | "lineNo": 3,
            | "bpMatchStatus": "01",
            | "field1": "value1",
            | "field2": "value2"
            |}
            |""".stripMargin)

        val input = Json.parse(
          """
            |{
            | "settlors": {
            | }
            |}
            |""".stripMargin)

        val transform = TestAmendSettlorTransform(1, amended, original, LocalDate.of(2012, 2, 20))
        val result = transform.applyDeclarationTransform(input)
        val expected = Json.parse(
          """
            |{
            | "settlors": {
            |   "settlorEntities": [
            |     {
            |       "lineNo": 3,
            |       "bpMatchStatus": "01",
            |       "field1": "value1",
            |       "field2": "value2",
            |       "entityEnd": "2012-02-20"
            |     }
            |   ]
            | }
            |}
            |""".stripMargin)

        result.get mustBe expected
      }
    }
  }
}
