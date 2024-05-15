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

package utils

import base.BaseSpec
import models.taxable_migration.MigrationStatus._
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json._
import utils.Constants._
import utils.JsonOps.{prunePath, prunePathAndPutNewValue, putNewValue}

class RequiredEntityDetailsForMigrationSpec extends BaseSpec {

  val util: RequiredEntityDetailsForMigration = injector.instanceOf[RequiredEntityDetailsForMigration]

  def runTest(entities: JsPath, `type`: Option[String], newValue: JsValue, expectedResult: MigrationStatus, typeOfTrust: Option[String] = None)
             (f: JsValue => JsResult[MigrationStatus]): Assertion = {

    def removeAndAdd(): Reads[JsObject] = `type` match {
      case None => prunePathAndPutNewValue(entities, newValue)
      case Some(t) => prunePathAndPutNewValue(entities \ INDIVIDUAL_BENEFICIARY, JsArray()) andThen
        prunePathAndPutNewValue(entities \ COMPANY_BENEFICIARY, JsArray()) andThen
        prunePathAndPutNewValue(entities \ TRUST_BENEFICIARY, JsArray()) andThen
        prunePathAndPutNewValue(entities \ CHARITY_BENEFICIARY, JsArray()) andThen
        prunePathAndPutNewValue(entities \ OTHER_BENEFICIARY, JsArray()) andThen
        putNewValue(entities \ t, newValue)
    }

    val trust = getTransformedTrustResponse
      .transform(
        removeAndAdd() andThen
          (typeOfTrust match {
            case Some(value) => prunePathAndPutNewValue(TRUST \ DETAILS \ TYPE_OF_TRUST, JsString(value))
            case None => prunePath(TRUST \ DETAILS \ TYPE_OF_TRUST)
          })
      ).get

    val result = f(trust)

    result.get mustEqual expectedResult
  }

  "RequiredEntityDetailsForMigration" when {

    "areBeneficiariesCompleteForMigration" must {

      lazy val f: JsValue => JsResult[MigrationStatus] = util.areBeneficiariesCompleteForMigration
      val entities = ENTITIES \ BENEFICIARIES

      "return NeedsUpdating" when {

        "no beneficiaries" in {

          runTest(entities, None, Json.obj(), expectedResult = NeedsUpdating)(f)
        }

        "individual beneficiary doesn't have required data" when {

          val `type` = INDIVIDUAL_BENEFICIARY

          "missing vulnerableBeneficiary" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "beneficiaryDiscretion": true,
                |    "legallyIncapable": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }

          "missing beneficiaryDiscretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "vulnerableBeneficiary": true,
                |    "legallyIncapable": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }

          "missing beneficiaryShareOfIncome when beneficiaryDiscretion is false" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "beneficiaryDiscretion": false,
                |    "vulnerableBeneficiary": true,
                |    "legallyIncapable": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }

          "missing beneficiaryType when typeOfTrust is Employment Related" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "beneficiaryDiscretion": true,
                |    "vulnerableBeneficiary": true,
                |    "legallyIncapable": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating, typeOfTrust = Some("Employment Related"))(f)
          }
        }

        "company beneficiary doesn't have required data" when {

          val `type` = COMPANY_BENEFICIARY

          "missing beneficiaryDiscretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }

          "missing beneficiaryShareOfIncome when beneficiaryDiscretion is false" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }
        }

        "trust beneficiary doesn't have required data" when {

          val `type` = TRUST_BENEFICIARY

          "missing beneficiaryDiscretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }

          "missing beneficiaryShareOfIncome when beneficiaryDiscretion is false" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }
        }

        "charity beneficiary doesn't have required data" when {

          val `type` = CHARITY_BENEFICIARY

          "missing beneficiaryDiscretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }

          "missing beneficiaryShareOfIncome when beneficiaryDiscretion is false" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }
        }

        "other beneficiary doesn't have required data" when {

          val `type` = OTHER_BENEFICIARY

          "missing beneficiaryDiscretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "description": "Org Description",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }

          "missing beneficiaryShareOfIncome when beneficiaryDiscretion is false" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "description": "Org Description",
                |    "beneficiaryDiscretion": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NeedsUpdating)(f)
          }
        }
      }

      "return Updated" when {

        "individual beneficiary has required data" when {

          val `type` = INDIVIDUAL_BENEFICIARY

          "has discretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "beneficiaryDiscretion": true,
                |    "vulnerableBeneficiary": true,
                |    "legallyIncapable": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }

          "has no discretion and has share of income" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "beneficiaryDiscretion": false,
                |    "beneficiaryShareOfIncome": "50",
                |    "vulnerableBeneficiary": true,
                |    "legallyIncapable": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }

          "has beneficiaryType for employment related trust" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "beneficiaryDiscretion": true,
                |    "beneficiaryType": "Director",
                |    "vulnerableBeneficiary": true,
                |    "legallyIncapable": false,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated, typeOfTrust = Some("Employment Related"))(f)
          }
        }

        "company beneficiary has required data" when {

          val `type` = COMPANY_BENEFICIARY

          "has discretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": true,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }

          "has no discretion and has share of income" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": false,
                |    "beneficiaryShareOfIncome": "50",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }
        }

        "trust beneficiary has required data" when {

          val `type` = TRUST_BENEFICIARY

          "has discretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": true,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }

          "has no discretion and has share of income" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": false,
                |    "beneficiaryShareOfIncome": "50",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }
        }

        "charity beneficiary has required data" when {

          val `type` = CHARITY_BENEFICIARY

          "has discretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": true,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }

          "has no discretion and has share of income" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": false,
                |    "beneficiaryShareOfIncome": "50",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }
        }

        "other beneficiary has required data" when {

          val `type` = OTHER_BENEFICIARY

          "has discretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "description": "Org Description",
                |    "beneficiaryDiscretion": true,
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }

          "has no discretion and has share of income" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "description": "Org Description",
                |    "beneficiaryDiscretion": false,
                |    "beneficiaryShareOfIncome": "50",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = Updated)(f)
          }
        }
      }

      "return NothingToUpdate" when {

        "individual" when {

          val `type` = INDIVIDUAL_BENEFICIARY

          "no individuals" in {

            runTest(entities, Some(`type`), JsArray(), expectedResult = NothingToUpdate)(f)
          }

          "has an end date" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "legallyIncapable": false,
                |    "entityStart": "2020-01-01",
                |    "entityEnd": "2021-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NothingToUpdate)(f)
          }
        }

        "company" when {

          val `type` = COMPANY_BENEFICIARY

          "no companies" in {

            runTest(entities, Some(`type`), JsArray(), expectedResult = NothingToUpdate)(f)
          }

          "has an end date" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01",
                |    "entityEnd": "2021-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NothingToUpdate)(f)
          }

          "has a UTR" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01",
                |    "identification": {
                |      "utr": "1234567890"
                |    }
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NothingToUpdate)(f)
          }
        }

        "trust" when {

          val `type` = TRUST_BENEFICIARY

          "no trusts" in {

            runTest(entities, Some(`type`), JsArray(), expectedResult = NothingToUpdate)(f)
          }

          "has an end date" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01",
                |    "entityEnd": "2021-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NothingToUpdate)(f)
          }

          "has a UTR" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01",
                |    "identification": {
                |      "utr": "1234567890"
                |    }
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NothingToUpdate)(f)
          }
        }

        "charity" when {

          val `type` = CHARITY_BENEFICIARY

          "no charities" in {

            runTest(entities, Some(`type`), JsArray(), expectedResult = NothingToUpdate)(f)
          }

          "has an end date" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01",
                |    "entityEnd": "2021-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NothingToUpdate)(f)
          }

          "has a UTR" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "entityStart": "2020-01-01",
                |    "identification": {
                |      "utr": "1234567890"
                |    }
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NothingToUpdate)(f)
          }
        }

        "other" when {

          val `type` = OTHER_BENEFICIARY

          "no others" in {

            runTest(entities, Some(`type`), JsArray(), expectedResult = NothingToUpdate)(f)
          }

          "has an end date" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "description": "Org Description",
                |    "entityStart": "2020-01-01",
                |    "entityEnd": "2021-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), beneficiary, expectedResult = NothingToUpdate)(f)
          }
        }
      }
    }

    "areSettlorsCompleteForMigration" must {

      lazy val f: JsValue => JsResult[MigrationStatus] = util.areSettlorsCompleteForMigration

      val entities = ENTITIES \ SETTLORS
      val `type` = BUSINESS_SETTLOR

      "return NeedsUpdating" when {
        "business settlor doesn't have required data" when {
          "typeOfTrust is Employment Related" when {

            "missing companyType" in {

              val settlor = Json.parse(
                """
                  |[
                  |  {
                  |    "name": "Org Name",
                  |    "companyTime": true,
                  |    "entityStart": "2020-01-01"
                  |  }
                  |]
                  |""".stripMargin
              )

              runTest(entities, Some(`type`), settlor, expectedResult = NeedsUpdating, typeOfTrust = Some("Employment Related"))(f)
            }

            "missing companyTime" in {

              val settlor = Json.parse(
                """
                  |[
                  |  {
                  |    "name": "Org Name",
                  |    "companyType": "Trading",
                  |    "entityStart": "2020-01-01"
                  |  }
                  |]
                  |""".stripMargin
              )

              runTest(entities, Some(`type`), settlor, expectedResult = NeedsUpdating, typeOfTrust = Some("Employment Related"))(f)
            }
          }
        }
      }

      "return Updated" when {
        "business settlor has required data" when {

          "not an employment related trust" in {

            val settlor = Json.parse(
              """
                |[
                |  {
                |    "name": "Org Name",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(entities, Some(`type`), settlor, expectedResult = Updated)(f)
          }

          "an employment related trust" when {
            "has companyType and companyTime" in {

              val settlor = Json.parse(
                """
                  |[
                  |  {
                  |    "name": "Org Name",
                  |    "companyType": "Trading",
                  |    "companyTime": true,
                  |    "entityStart": "2020-01-01"
                  |  }
                  |]
                  |""".stripMargin
              )

              runTest(entities, Some(`type`), settlor, expectedResult = Updated, typeOfTrust = Some("Employment Related"))(f)
            }
          }
        }
      }

      "return NothingToUpdate" when {

        "no business settlors" in {

          runTest(entities, Some(`type`), JsArray(), expectedResult = NothingToUpdate)(f)
        }

        "has an end date" in {

          val settlor = Json.parse(
            """
              |[
              |  {
              |    "name": "Org Name",
              |    "entityStart": "2020-01-01",
              |    "entityEnd": "2021-01-01"
              |  }
              |]
              |""".stripMargin
          )

          runTest(entities, Some(`type`), settlor, expectedResult = NothingToUpdate)(f)
        }
      }
    }
  }
}
