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
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json._
import utils.Constants._
import utils.JsonOps.{prunePathAndPutNewValue, putNewValue}

class RequiredEntityDetailsForMigrationSpec extends BaseSpec {

  val util: RequiredEntityDetailsForMigration = injector.instanceOf[RequiredEntityDetailsForMigration]

  def runTest(entities: JsPath, `type`: String, newValue: JsValue, expectedResult: Option[Boolean], typeOfTrust: String = "Will Trust or Intestacy Trust")
             (f: JsValue => JsResult[Option[Boolean]]): Assertion = {
    val trust = getTransformedTrustResponse
      .transform(
        prunePathAndPutNewValue(entities, Json.obj()) andThen
          putNewValue(entities \ `type`, newValue) andThen
          prunePathAndPutNewValue(TRUST \ DETAILS \ TYPE_OF_TRUST, JsString(typeOfTrust))
      ).get

    val result = f(trust)

    result.get mustEqual expectedResult
  }

  "RequiredEntityDetailsForMigration" when {

    "areBeneficiariesCompleteForMigration" must {
      
      lazy val f: JsValue => JsResult[Option[Boolean]] = util.areBeneficiariesCompleteForMigration
      val entities = ENTITIES \ BENEFICIARIES

      "return Some(false)" when {

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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false), typeOfTrust = "Employment Related")(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(false))(f)
          }
        }
      }

      "return Some(true)" when {

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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true), typeOfTrust = "Employment Related")(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = Some(true))(f)
          }
        }
      }

      "return None" when {

        "individual" when {

          val `type` = INDIVIDUAL_BENEFICIARY

          "no individuals" in {

            runTest(entities, `type`, JsArray(), expectedResult = None)(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = None)(f)
          }
        }

        "company" when {

          val `type` = COMPANY_BENEFICIARY

          "no companies" in {

            runTest(entities, `type`, JsArray(), expectedResult = None)(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = None)(f)
          }
        }

        "trust" when {

          val `type` = TRUST_BENEFICIARY

          "no trusts" in {

            runTest(entities, `type`, JsArray(), expectedResult = None)(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = None)(f)
          }
        }

        "charity" when {

          val `type` = CHARITY_BENEFICIARY

          "no charities" in {

            runTest(entities, `type`, JsArray(), expectedResult = None)(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = None)(f)
          }
        }

        "other" when {

          val `type` = OTHER_BENEFICIARY

          "no others" in {

            runTest(entities, `type`, JsArray(), expectedResult = None)(f)
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

            runTest(entities, `type`, beneficiary, expectedResult = None)(f)
          }
        }
      }
    }

    "areSettlorsCompleteForMigration" must {

      lazy val f: JsValue => JsResult[Option[Boolean]] = util.areSettlorsCompleteForMigration

      val entities = ENTITIES \ SETTLORS
      val `type` = BUSINESS_SETTLOR

      "return Some(false)" when {
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

              runTest(entities, `type`, settlor, expectedResult = Some(false), typeOfTrust = "Employment Related")(f)
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

              runTest(entities, `type`, settlor, expectedResult = Some(false), typeOfTrust = "Employment Related")(f)
            }
          }
        }
      }

      "return Some(true)" when {
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

            runTest(entities, `type`, settlor, expectedResult = Some(true))(f)
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

              runTest(entities, `type`, settlor, expectedResult = Some(true), typeOfTrust = "Employment Related")(f)
            }
          }
        }
      }

      "return None" when {

        "no business settlors" in {

          runTest(entities, `type`, JsArray(), expectedResult = None)(f)
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

          runTest(entities, `type`, settlor, expectedResult = None)(f)
        }
      }
    }
  }
}
