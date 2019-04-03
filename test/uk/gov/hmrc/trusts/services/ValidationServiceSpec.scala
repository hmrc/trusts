/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.services

import play.api.libs.json.Json
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.models.{EstateRegistration, ExistingTrustCheckRequest, Registration}
import uk.gov.hmrc.trusts.utils.{DataExamples, EstateDataExamples, JsonUtils}


class ValidationServiceSpec extends BaseSpec with DataExamples with EstateDataExamples {

  private lazy val validatationService: ValidationService = new ValidationService()
  private lazy val validator : Validator = validatationService.get("/resources/schemas/trustsApiRegistrationSchema_3.2.0.json")
  private lazy val estateValidator : Validator = validatationService.get("/resources/schemas/estatesRegistrationSchema_3.2.0.json")

  "a validator " should {
    "return an empty list of errors when " when {
      "Json having all required fields" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-trusts-registration-api.json")
        validator.validate[Registration](jsonString).isRight mustBe true
      }

      "Json having trust with orgnisation  trustees" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-trusts-org-trustees.json")
        validator.validate[Registration](jsonString).isRight mustBe true
      }

      "estate payload json having all required fields" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-estate-registration-01.json")
        estateValidator.validate[EstateRegistration](jsonString).isRight mustBe true
      }

      "estate payload json having required fields for estate type 02" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-estate-registration-02.json")
        estateValidator.validate[EstateRegistration](jsonString).isRight mustBe true
      }

      "estate payload json having required fields for estate type 04" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-estate-registration-04.json")
        val estateRegistration = estateValidator.validate[EstateRegistration](jsonString).right.get
        estateRegistration.estate.entities.personalRepresentative.estatePerRepOrg.isDefined mustBe true
        estateRegistration.estate.entities.deceased.identification.isDefined mustBe false
      }
    }

    "return registration domain" when {
      "valid json having large type beneficiary with 5 description" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-trusts-registration-api.json")
       val registration =  validator.validate[Registration](jsonString).right.get
        registration.trust.entities.beneficiary.large.get.map{
          largeBeneficiary =>
            largeBeneficiary.description mustBe "Description"
            largeBeneficiary.description1.get mustBe "Description1"
            largeBeneficiary.description2.get mustBe "Description2"
            largeBeneficiary.description3.get mustBe "Description3"
            largeBeneficiary.description4.get mustBe "Description4"
        }
      }

      "valid json having large type beneficiary with 1 required description " in {
        val jsonString = JsonUtils.getJsonFromFile("valid-trusts-org-trustees.json")
        val registration =  validator.validate[Registration](jsonString).right.get
        registration.trust.entities.beneficiary.large.get.map{
          largeBeneficiary =>
            largeBeneficiary.description mustBe "Description"
            largeBeneficiary.description1 mustBe None
            largeBeneficiary.description2 mustBe None
            largeBeneficiary.description3 mustBe None
            largeBeneficiary.description4 mustBe None

        }
      }

      "valid json having asset value of 12 digits. " in {
        val jsonString = JsonUtils.getJsonFromFile("valid-trusts-registration-api.json")
        val registration =  validator.validate[Registration](jsonString).right.get
        registration.trust.assets.monetary.get.map{x=>x.assetMonetaryAmount.toString.length mustBe 12}
        registration.trust.assets.propertyOrLand.get.map{x=>x.valueFull.get.toString.length mustBe 12}
        registration.trust.assets.shares.get.map{x=>x.value.get.toString.length mustBe 12}
        registration.trust.assets.other.get.map{x=>x.value.get.toString.length mustBe 12}
      }

    }

    "return a list of validaton errors " when {
      "json document is invalid" in {
        val jsonString = JsonUtils.getJsonFromFile("invalid-payload-trusts-registration.json")
        validator.validate[Registration](jsonString).isLeft mustBe true
      }

      "json document is valid but failed to match with Domain" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-trusts-registration-api.json")
        validator.validate[ExistingTrustCheckRequest](jsonString).isLeft mustBe true
      }

      "date of birth of trustee is before 1500/01/01" in {
        val jsonString = JsonUtils.getJsonFromFile("trustees-invalid-dob.json")
        val errorList =validator.validate[Registration](jsonString).left.get.
          filter(_.location=="/trust/entities/trustees/0/trusteeInd/dateOfBirth")
        errorList.size mustBe 1
      }

      "no beneficiary is provided" in {
        val errorList = validator.validate[Registration](trustWithoutBeneficiary).left.get.
          filter(_.message=="object has missing required properties ([\"beneficiary\"])")
        errorList.size mustBe 1
      }

      "date of birth of individual beneficiary is before 1500/01/01" in {
        val jsonString = trustWithValues(indBenficiaryDob = "1499-12-31")
        val errorList = validator.validate[Registration](jsonString).left.get.
          filter(_.location=="/trust/entities/beneficiary/individualDetails/0/dateOfBirth")
        errorList.size mustBe 1
      }

      "no description provided for large type beneficiary" in {
        val jsonString =JsonUtils.getJsonFromFile("trust-without-large-ben-description.json")
        val errorList = validator.validate[Registration](jsonString).left.get.
          filter(_.location=="/trust/entities/beneficiary/large/0")
        errorList.size mustBe 1
      }

      "no asset type is provided" in {
        val errorList = validator.validate[Registration](trustWithoutAssets).left.get.
          filter(_.location=="/trust/assets")
        errorList.size mustBe 1
      }

      "no personal representative provided" in {
        val jsonString =JsonUtils.getJsonFromFile("invalid-estate-registration-01.json")
        val errorList = estateValidator.validate[EstateRegistration](jsonString).left.get.
          filter(_.message =="object has missing required properties ([\"personalRepresentative\"])")
        errorList.size mustBe 1
      }

      "no correspodence address provided for estate" in {
        val errorList = estateValidator.validate[EstateRegistration](estateWithoutCorrespondenceAddress).left.get.
          filter(_.message =="object has missing required properties ([\"address\"])")
        errorList.size mustBe 1
      }
    }

    "return a list of validaton errors for trusts " when {
      "json request is valid but failed in business rules for trust start date, efrbs start date " in {
        val jsonString = JsonUtils.getJsonFromFile("trust-business-validation-fail.json")
        validator.validate[Registration](jsonString).left.get.size mustBe 8
      }

      "individual trustees has same NINO " in {
        val jsonString = JsonUtils.getJsonFromFile("trust-business-validation-fail.json")
        val errorList =validator.validate[Registration](jsonString).left.get.
          filter(_.message=="NINO is already used for another individual trustee.")
        errorList.size mustBe 2
      }

      "business trustees has same utr " in {
        val jsonString = JsonUtils.getJsonFromFile("trust-business-validation-fail.json")
        val errorList =validator.validate[Registration](jsonString).left.get.
          filter(_.message=="Utr is already used for another business trustee.")
        errorList.size mustBe 1
      }

      "business trustees has same utr as trust utr" in {
        val jsonString = JsonUtils.getJsonFromFile("trust-business-validation-fail.json")
        val errorList =validator.validate[Registration](jsonString).left.get.
          filter(_.message=="Business trustee utr is same as trust utr.")
        errorList.size mustBe 2
      }

    }

    "return a list of validaton errors for estates " when {
      "individual personal representative has future date of birth" in {
        val jsonString = getJsonFromFile("estate-registration-dynamic-01.json").
          replace("{estatePerRepIndDob}", "2030-01-01")
        val errorList =estateValidator.validate[EstateRegistration](jsonString).left.get.
          filter(_.message=="Date of birth must be today or in the past.")
        errorList.size mustBe 1
      }

    }
  }//validator

}
