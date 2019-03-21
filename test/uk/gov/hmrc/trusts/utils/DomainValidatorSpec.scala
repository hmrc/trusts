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

package uk.gov.hmrc.trusts.utils

import org.joda.time.DateTime
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.models.{Registration, TrusteeType}
import uk.gov.hmrc.trusts.models.Registration
import uk.gov.hmrc.trusts.utils.TypeOfTrust._


class DomainValidatorSpec extends BaseSpec with DataExamples {

  def SUT(registration: Registration) = new DomainValidator(registration)

  "trustStartDateIsNotFutureDate" should {
    "return None for valid date" in {
      SUT(registrationRequest).trustStartDateIsNotFutureDate mustBe None
    }

    "return None for today's date" in {
      val registrationWithFutureStartDate = registrationWithStartDate(new DateTime())
      SUT(registrationWithFutureStartDate).trustStartDateIsNotFutureDate mustBe None
    }

    "return validation error for tomorrow date" in {
      val registrationWithFutureStartDate = registrationWithStartDate(new DateTime().plusDays(1))
      SUT(registrationWithFutureStartDate).trustStartDateIsNotFutureDate.get.message mustBe
        "Trusts start date must be today or in the past."
    }
  }

  "trustEfrbsDateIsNotFutureDate for employment related trust" should {
    "return None for valid date" in {
      val request = registrationWithEfrbsStartDate(new DateTime().plusDays(-1000),EMPLOYMENT_RELATED_TRUST)
      SUT(request).trustEfrbsDateIsNotFutureDate mustBe None
    }

    "return None for today's date" in {
      val request = registrationWithEfrbsStartDate(new DateTime(),EMPLOYMENT_RELATED_TRUST)
      SUT(request).trustEfrbsDateIsNotFutureDate mustBe None
    }

    "return validation error for tomorrow date" in {
      val request = registrationWithEfrbsStartDate(new DateTime().plusDays(1),EMPLOYMENT_RELATED_TRUST)
      SUT(request).trustEfrbsDateIsNotFutureDate.get.message mustBe
        "Trusts efrbs start date must be today or in the past."
    }
  }

  "validateEfrbsDate" should {
    "return None when efrbs date is provided for employment trusts" in {
      val employmentRelatedTrust = registrationWithEfrbsStartDate(new DateTime().plusDays(1000),EMPLOYMENT_RELATED_TRUST)
      SUT(employmentRelatedTrust).validateEfrbsDate mustBe None
    }

    "return validation error when efrbs date is provided with any other trust except employment trusts" in {
      val willTrust = registrationWithEfrbsStartDate(new DateTime().plusDays(1000),WILL_TRUST)
      SUT(willTrust).validateEfrbsDate.get.message mustBe
        "Trusts efrbs start date can be provided for Employment Related trust only."
    }
  }

  "trusteesDobIsNotFutureDate" should {
    "return None when there is no trustees" in {
      val request = registrationWithTrustess(None)
      SUT(request).indTrusteesDobIsNotFutureDate.flatten mustBe empty
    }

    "return validation error when trustee has future date of birth" in {
      val request = registrationWithTrustess(Some(listOfIndividualTrustees))
      val response =  SUT(request).indTrusteesDobIsNotFutureDate
      response.flatten.size mustBe 1
      response.flatten.map {
        error =>
          error.message mustBe "Date of birth must be today or in the past."
          error.location mustBe "/trust/entities/trustees/1/trusteeInd/dateOfBirth"
      }
    }

    "return validation error " when {
      "trustees are mix of individual and organisation" when {
        "individual trustee has future date of birth" in {

          val request = registrationWithTrustess(Some(listOfIndAndOrgTrustees))
          val response =  SUT(request).indTrusteesDobIsNotFutureDate
          response.flatten.size mustBe 1
          response.flatten.map {
            error =>
              error.message mustBe "Date of birth must be today or in the past."
              error.location mustBe "/trust/entities/trustees/0/trusteeInd/dateOfBirth"
          }
        }
      }
    }

    "return None when there is trustees is of business/organisation type" in {
      val request = registrationWithTrustess(Some(listOfOrgTrustees))
      SUT(request).indTrusteesDobIsNotFutureDate.flatten mustBe empty
    }
  }

  "indTrusteesDuplicateNino" should {
    "return None when there is no trustees" in {
      val request = registrationWithTrustess(None)
      SUT(request).indTrusteesDobIsNotFutureDate.flatten mustBe empty
    }

    "return validation error when individual trustees has duplicate nino " in {
      val request = registrationWithTrustess(Some(listOfDuplicateIndAndOrgTrustees))
      val response = SUT(request).indTrusteesDuplicateNino
      response.flatten.size mustBe 4
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "NINO is already used for another individual trustee."
          error.location mustBe s"/trust/entities/trustees/${index+1}/trusteeInd/identification/nino"
      }
    }
  }

  "businessTrusteesDuplicateUtr" should {
    "return None when there is no trustees" in {
      val request = registrationWithTrustess(None)
      SUT(request).businessTrusteesDuplicateUtr.flatten mustBe empty
    }

    "return validation error when business trustees has duplicate utr " in {
      val request = registrationWithTrustess(Some(listOfDuplicateIndAndOrgTrustees))
      val response = SUT(request).businessTrusteesDuplicateUtr
      response.flatten.size mustBe 1
      response.flatten.map {
        error =>
          error.message mustBe "Utr is already used for another business trustee."
          error.location mustBe s"/trust/entities/trustees/0/trusteeOrg/identification/utr"
      }
    }
  }


  "bussinessTrusteeUtrIsSameTrustUtr" should {
    "return None when there is no trustees" in {
      val request = registrationWithTrustess(None)
      SUT(request).businessTrusteeUtrIsNotTrustUtr.flatten mustBe empty
    }

    "return validation error when business trustees utr is same as trust utr " in {
      val request = registrationWithTrustess(Some(listOfDuplicateIndAndOrgTrustees))
      val response = SUT(request).businessTrusteeUtrIsNotTrustUtr
      response.flatten.size mustBe 2
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "Business trustee utr is same as trust utr."
          error.location mustBe s"/trust/entities/trustees/$index/trusteeOrg/identification/utr"
      }
    }
  }


  "indBeneficiariesDuplicateNino" should {
    "return None when individual beneficiary has different nino" in {
      val request = registrationWithBeneficiary()
      SUT(request).indBeneficiariesDuplicateNino.flatten mustBe empty
    }

    "return validation error when individual beneficiaries has same nino " in {
      val request = registrationWithBeneficiary(beneficiaryType = beneficiaryTypeEntity(Some(List(indBenficiary(),indBenficiary()))))
      val response = SUT(request).indBeneficiariesDuplicateNino
      response.flatten.size mustBe 1
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "NINO is already used for another individual beneficiary."
          error.location mustBe s"/trust/entities/beneficiary/individualDetails/${index}/identification/nino"
      }
    }
  }

  "indBeneficiariesDobIsNotFutureDate" should {

    "return validation error when individual beneficiaries has future date of birth" in {
      val request = registrationWithBeneficiary(beneficiaryType = beneficiaryTypeEntity(Some(List(indBenficiary(nino, "2030-12-31")))))
      val response =  SUT(request).indBeneficiariesDobIsNotFutureDate
      response.flatten.size mustBe 1
      response.flatten.map {
        error =>
          error.message mustBe "Date of birth must be today or in the past."
          error.location mustBe "/trust/entities/beneficiary/individualDetails/0/dateOfBirth"
      }
    }
  }

  "indBeneficiariesDuplicatePassportNumber" should {
    "return None when individual beneficiary has different passport Number" in {
      val request = registrationWithBeneficiary(beneficiaryType
        =   beneficiaryTypeEntity(Some(List(indBenficiary(passportIdentification()),indBenficiary(passportIdentification("AB123456789D"))))))
      SUT(request).indBeneficiariesDuplicatePassportNumber.flatten mustBe empty
    }

    "return validation error when individual beneficiaries has same passport number " in {
      val request = registrationWithBeneficiary(beneficiaryType
        =   beneficiaryTypeEntity(Some(List(indBenficiary(passportIdentification()),indBenficiary(passportIdentification())))))
      val response = SUT(request).indBeneficiariesDuplicatePassportNumber
      response.flatten.size mustBe 1
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "Passport number is already used for another individual beneficiary."
          error.location mustBe s"/trust/entities/beneficiary/individualDetails/${index}/identification/passport/number"
      }
    }
  }

  //settlor
  "deceasedSettlorDobIsNotFutureDate" should {
    "return validation error when deceased settlor's date of birth is future date" in {
      val willTrust = willTrustWithValues("2030-01-01","2031-01-01")
      SUT(willTrust).deceasedSettlorDobIsNotFutureDate.get.message mustBe
        "Date of birth must be today or in the past."
      BusinessValidation.check(willTrust).size mustBe 2
    }

    "return None when deceased settlor's date of birth is in past" in {
      val willTrust = willTrustWithValues("2019-01-01","2019-02-01")
      SUT(willTrust).deceasedSettlorDobIsNotFutureDate mustBe None
      BusinessValidation.check(willTrust).size mustBe 0

    }

    "return None when there is no deceased settlor" in {
      val employmentTrust = registrationRequest
      SUT(employmentTrust).deceasedSettlorDobIsNotFutureDate mustBe None
      println(BusinessValidation.check(employmentTrust))
      BusinessValidation.check(employmentTrust).size mustBe 0
    }

    "return validation error when deceased date of death is after date of birth." in {
      val willTrust = willTrustWithValues(deceasedDateOfBirth  ="2016-01-01",deceasedDateOfDeath  ="2015-01-01")
      SUT(willTrust).deceasedSettlorDoDIsNotAfterDob.get.message mustBe "Date of death is after date of birth"

      BusinessValidation.check(willTrust).size mustBe 1
    }

    "return validation error when deceased settlor's date of death is future date" in {
      val willTrust = willTrustWithValues("2019-01-01","2031-01-01")
      SUT(willTrust).deceasedSettlorDoDIsNotFutureDate.get.message mustBe
        "Date of death must be today or in the past."
      BusinessValidation.check(willTrust).size mustBe 1
    }


    "return validation error when deceased settlor nino is same as trustee nino" in {
      val willTrust = willTrustWithValues(deceasedNino="ST123456")
      SUT(willTrust).deceasedSettlorIsNotTrustee.get.message mustBe
        "Deceased NINO is same as trustee NINO."
      BusinessValidation.check(willTrust).size mustBe 1
    }

    "return validation error when settlor is not provided for employment related trust" in {
      val interVivos = willTrustWithValues(typeOfTrust = TypeOfTrust.INTER_VIVOS_SETTLEMENT.toString)
      SUT(interVivos).validateSettlor.get.message mustBe
        "Settlor is mandatory for provided type of trust."
      BusinessValidation.check(interVivos).size mustBe 1
    }


    "return validation error when individual settlor has same NINO" in {
      val employmentTrust = getJsonValueFromString(trustWithValues(settlorNino = "ST019091")).validate[Registration].get
      val response = SUT(employmentTrust).livingSettlorDuplicateNino
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "NINO is already used for another individual settlor."
          error.location mustBe s"/trust/entities/settlors/settlor/$index/identification/nino"
      }
      BusinessValidation.check(employmentTrust).size mustBe 1
    }
  }


}
