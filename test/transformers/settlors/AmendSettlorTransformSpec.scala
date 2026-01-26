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

package transformers.settlors

import models.NameType
import models.variation.{AmendDeceasedSettlor, SettlorCompany, SettlorIndividual}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsValue, Json}
import utils.JsonUtils

import java.time.LocalDate

class AmendSettlorTransformSpec extends AnyFreeSpec {

  "the amend settlor transformer" - {

    "when individual" - {

      val `type` = "settlor"

      "before declaration" - {

        "amend settlor details by replacing the settlor" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-settlor-transform-before.json")
          val afterJson  = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-settlor-transform-after.json")

          val amended = SettlorIndividual(
            lineNo = None,
            bpMatchStatus = None,
            NameType("First updated", None, "Last updated"),
            None,
            identification = None,
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            LocalDate.parse("2018-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo": "1",
              |  "bpMatchStatus": "01",
              |  "name": {
              |    "firstName": "First",
              |    "lastName": "Last"
              |  },
              |  "dateOfBirth": "2010-05-03",
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer =
            AmendSettlorTransform(Some(0), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), `type`)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original settlor, adding in the amendment as a new settlor for a settlor known by etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-settlor-transform-before.json")
          val afterJson  =
            JsonUtils.getJsonValueFromFile("transforms/trusts-individual-settlor-transform-after-declaration.json")

          val amended = SettlorIndividual(
            lineNo = None,
            bpMatchStatus = None,
            NameType("First updated", None, "Last updated"),
            None,
            identification = None,
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            LocalDate.parse("2018-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo": "1",
              |  "bpMatchStatus": "01",
              |  "name": {
              |    "firstName": "First",
              |    "lastName": "Last"
              |  },
              |  "dateOfBirth": "2010-05-03",
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendSettlorTransform(
            Some(0),
            Json.toJson(amended),
            original,
            endDate = LocalDate.parse("2020-03-25"),
            `type`
          )

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new settlor that is not known to etmp" in {
          val beforeJson =
            JsonUtils.getJsonValueFromFile("transforms/trusts-new-individual-settlor-transform-before.json")
          val afterJson  =
            JsonUtils.getJsonValueFromFile("transforms/trusts-new-individual-settlor-transform-after-declaration.json")

          val amended = SettlorIndividual(
            lineNo = None,
            bpMatchStatus = None,
            NameType("Second updated", None, "Second updated"),
            None,
            identification = None,
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            LocalDate.parse("2020-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "name": {
              |    "firstName": "Second",
              |    "lastName": "Last"
              |  },
              |  "dateOfBirth": "2010-05-03",
              |  "entityStart": "2020-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendSettlorTransform(
            Some(1),
            Json.toJson(amended),
            original,
            endDate = LocalDate.parse("2020-03-25"),
            `type`
          )

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }

    "when business" - {

      val `type` = "settlorCompany"

      "before declaration" - {

        "amend settlor details by replacing the settlor" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-business-settlor-transform-before.json")
          val afterJson  = JsonUtils.getJsonValueFromFile("transforms/trusts-business-settlor-transform-after.json")

          val amended = SettlorCompany(
            lineNo = None,
            bpMatchStatus = None,
            name = "New Company",
            companyType = None,
            companyTime = None,
            identification = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2018-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo": "1",
              |  "bpMatchStatus": "01",
              |  "name": "Company",
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer =
            AmendSettlorTransform(Some(0), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), `type`)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original settlor, adding in the amendment as a new settlor for a settlor known by etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-business-settlor-transform-before.json")
          val afterJson  =
            JsonUtils.getJsonValueFromFile("transforms/trusts-business-settlor-transform-after-declaration.json")

          val amended = SettlorCompany(
            lineNo = None,
            bpMatchStatus = None,
            name = "New Company",
            companyType = None,
            companyTime = None,
            identification = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2018-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo": "1",
              |  "bpMatchStatus": "01",
              |  "name": "Company",
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendSettlorTransform(
            Some(0),
            Json.toJson(amended),
            original,
            endDate = LocalDate.parse("2020-03-25"),
            `type`
          )

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new settlor that is not known to etmp" in {

          val beforeJson =
            JsonUtils.getJsonValueFromFile("transforms/trusts-new-business-settlor-transform-before.json")
          val afterJson  =
            JsonUtils.getJsonValueFromFile("transforms/trusts-new-business-settlor-transform-after-declaration.json")

          val amended = SettlorCompany(
            lineNo = None,
            bpMatchStatus = None,
            name = "Second company updated",
            companyType = None,
            companyTime = None,
            identification = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2020-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "name": "Second",
              |  "entityStart": "2020-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendSettlorTransform(
            Some(1),
            Json.toJson(amended),
            original,
            endDate = LocalDate.parse("2020-03-25"),
            `type`
          )

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }

    "when deceased" - {

      val `type` = "deceased"

      "before declaration" - {

        "amend settlor details by replacing it, but retaining their start date, bpMatchStatus and lineNo" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-deceased-settlor-transform-before.json")
          val afterJson  = JsonUtils.getJsonValueFromFile("transforms/trusts-deceased-settlor-transform-after.json")

          val amended = AmendDeceasedSettlor(
            name = NameType("updated first", None, "updated last"),
            dateOfBirth = None,
            dateOfDeath = None,
            countryOfResidence = None,
            nationality = None,
            identification = None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo":"1",
              |  "bpMatchStatus": "01",
              |  "name":{
              |    "firstName":"John",
              |    "middleName":"William",
              |    "lastName":"O'Connor"
              |  },
              |  "dateOfBirth":"1956-02-12",
              |  "dateOfDeath":"2016-01-01",
              |  "identification":{
              |    "nino":"KC456736"
              |  },
              |  "entityStart":"1998-02-12"
              |}
              |""".stripMargin)

          val transformer = AmendSettlorTransform(None, Json.toJson(amended), original, LocalDate.now(), `type`)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "amend settlor details by replacing it, but retaining their start date, bpMatchStatus and lineNo" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-deceased-settlor-transform-before.json")
          val afterJson  = JsonUtils.getJsonValueFromFile("transforms/trusts-deceased-settlor-transform-after.json")

          val amended = AmendDeceasedSettlor(
            name = NameType("updated first", None, "updated last"),
            dateOfBirth = None,
            dateOfDeath = None,
            countryOfResidence = None,
            nationality = None,
            identification = None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo":"1",
              |  "bpMatchStatus": "01",
              |  "name":{
              |    "firstName":"John",
              |    "middleName":"William",
              |    "lastName":"O'Connor"
              |  },
              |  "dateOfBirth":"1956-02-12",
              |  "dateOfDeath":"2016-01-01",
              |  "identification":{
              |    "nino":"KC456736"
              |  },
              |  "entityStart":"1998-02-12"
              |}
              |""".stripMargin)

          val transformer = AmendSettlorTransform(None, Json.toJson(amended), original, LocalDate.now(), `type`)

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }
  }

}
