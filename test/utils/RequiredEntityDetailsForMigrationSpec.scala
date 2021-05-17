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

package utils

import base.BaseSpec
import org.scalatest.Assertion
import play.api.libs.json._
import utils.Constants._
import utils.JsonOps.prunePathAndPutNewValue

class RequiredEntityDetailsForMigrationSpec extends BaseSpec {

  val util: RequiredEntityDetailsForMigration = injector.instanceOf[RequiredEntityDetailsForMigration]

  def runTest(path: JsPath, newValue: JsValue, expectedResult: Boolean, typeOfTrust: String = "Will Trust or Intestacy Trust")
             (f: JsValue => JsResult[Boolean]): Assertion = {
    val trust = getTransformedTrustResponse
      .transform(
        prunePathAndPutNewValue(path, newValue) andThen
          prunePathAndPutNewValue(TRUST \ DETAILS \ TYPE_OF_TRUST, JsString(typeOfTrust))
      ).get

    val result = f(trust)

    result.get mustEqual expectedResult
  }

  "RequiredEntityDetailsForMigration" when {

    "areBeneficiariesCompleteForMigration" must {
      
      lazy val f: JsValue => JsResult[Boolean] = util.areBeneficiariesCompleteForMigration

      "return false" when {

        "individual beneficiary doesn't have required data" when {

          val path = ENTITIES \ BENEFICIARIES \ INDIVIDUAL_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = false)(f)
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

            runTest(path, beneficiary, expectedResult = false)(f)
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

            runTest(path, beneficiary, expectedResult = false)(f)
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

            runTest(path, beneficiary, expectedResult = false, typeOfTrust = "Employment Related")(f)
          }
        }

        "company beneficiary doesn't have required data" when {

          val path = ENTITIES \ BENEFICIARIES \ COMPANY_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = false)(f)
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

            runTest(path, beneficiary, expectedResult = false)(f)
          }
        }

        "large beneficiary doesn't have required data" when {

          val path = ENTITIES \ BENEFICIARIES \ LARGE_BENEFICIARY

          "missing beneficiaryDiscretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "description": "Org Description",
                |    "numberOfBeneficiary": "201",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(path, beneficiary, expectedResult = false)(f)
          }

          "missing beneficiaryShareOfIncome when beneficiaryDiscretion is false" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": false,
                |    "description": "Org Description",
                |    "numberOfBeneficiary": "201",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(path, beneficiary, expectedResult = false)(f)
          }
        }

        "trust beneficiary doesn't have required data" when {

          val path = ENTITIES \ BENEFICIARIES \ TRUST_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = false)(f)
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

            runTest(path, beneficiary, expectedResult = false)(f)
          }
        }

        "charity beneficiary doesn't have required data" when {

          val path = ENTITIES \ BENEFICIARIES \ CHARITY_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = false)(f)
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

            runTest(path, beneficiary, expectedResult = false)(f)
          }
        }

        "other beneficiary doesn't have required data" when {

          val path = ENTITIES \ BENEFICIARIES \ OTHER_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = false)(f)
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

            runTest(path, beneficiary, expectedResult = false)(f)
          }
        }
      }

      "return true" when {

        "individual beneficiary has required data" when {

          val path = ENTITIES \ BENEFICIARIES \ INDIVIDUAL_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true, typeOfTrust = "Employment Related")(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
          }
        }

        "company beneficiary has required data" when {

          val path = ENTITIES \ BENEFICIARIES \ COMPANY_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
          }
        }

        "large beneficiary has required data" when {

          val path = ENTITIES \ BENEFICIARIES \ LARGE_BENEFICIARY

          "has discretion" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": true,
                |    "description": "Org Description",
                |    "numberOfBeneficiary": "201",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(path, beneficiary, expectedResult = true)(f)
          }

          "has no discretion and has share of income" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "beneficiaryDiscretion": false,
                |    "beneficiaryShareOfIncome": "50",
                |    "description": "Org Description",
                |    "numberOfBeneficiary": "201",
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(path, beneficiary, expectedResult = true)(f)
          }

          "has an end date" in {

            val beneficiary = Json.parse(
              """
                |[
                |  {
                |    "organisationName": "Org Name",
                |    "description": "Org Description",
                |    "numberOfBeneficiary": "201",
                |    "entityStart": "2020-01-01",
                |    "entityEnd": "2021-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(path, beneficiary, expectedResult = true)(f)
          }
        }

        "trust beneficiary has required data" when {

          val path = ENTITIES \ BENEFICIARIES \ TRUST_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
          }
        }

        "charity beneficiary has required data" when {

          val path = ENTITIES \ BENEFICIARIES \ CHARITY_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
          }
        }

        "other beneficiary has required data" when {

          val path = ENTITIES \ BENEFICIARIES \ OTHER_BENEFICIARY

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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
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

            runTest(path, beneficiary, expectedResult = true)(f)
          }
        }
      }
    }

    "areSettlorsCompleteForMigration" must {

      lazy val f: JsValue => JsResult[Boolean] = util.areSettlorsCompleteForMigration

      val path = ENTITIES \ SETTLORS \ BUSINESS_SETTLOR
      
      "return false" when {
        "business settlor doesn't have required data" when {
          "typeOfTrust is Employment Related" when {

            "missing companyType" in {

              val settlor = Json.parse(
                """
                  |[
                  |  {
                  |    "name": {
                  |      "firstName": "Joe",
                  |      "lastName": "Bloggs"
                  |    },
                  |    "companyTime": true,
                  |    "entityStart": "2020-01-01"
                  |  }
                  |]
                  |""".stripMargin
              )

              runTest(path, settlor, expectedResult = false, typeOfTrust = "Employment Related")(f)
            }

            "missing companyTime" in {

              val settlor = Json.parse(
                """
                  |[
                  |  {
                  |    "name": {
                  |      "firstName": "Joe",
                  |      "lastName": "Bloggs"
                  |    },
                  |    "companyType": "Trading",
                  |    "entityStart": "2020-01-01"
                  |  }
                  |]
                  |""".stripMargin
              )

              runTest(path, settlor, expectedResult = false, typeOfTrust = "Employment Related")(f)
            }
          }
        }
      }

      "return true" when {
        "business settlor has required data" when {

          "not an employment related trust" in {

            val settlor = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "entityStart": "2020-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(path, settlor, expectedResult = true)(f)
          }

          "an employment related trust" when {
            "has companyType and companyTime" in {

              val settlor = Json.parse(
                """
                  |[
                  |  {
                  |    "name": {
                  |      "firstName": "Joe",
                  |      "lastName": "Bloggs"
                  |    },
                  |    "companyType": "Trading",
                  |    "companyTime": true,
                  |    "entityStart": "2020-01-01"
                  |  }
                  |]
                  |""".stripMargin
              )

              runTest(path, settlor, expectedResult = true, typeOfTrust = "Employment Related")(f)
            }
          }

          "has an end date" in {

            val settlor = Json.parse(
              """
                |[
                |  {
                |    "name": {
                |      "firstName": "Joe",
                |      "lastName": "Bloggs"
                |    },
                |    "entityStart": "2020-01-01",
                |    "entityEnd": "2021-01-01"
                |  }
                |]
                |""".stripMargin
            )

            runTest(path, settlor, expectedResult = true)(f)
          }
        }
      }
    }
  }
}
