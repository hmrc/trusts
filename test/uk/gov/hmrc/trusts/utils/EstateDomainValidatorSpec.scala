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

package uk.gov.hmrc.trusts.utils

import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.models.{EstateRegistration, Registration}


class EstateDomainValidatorSpec extends BaseSpec with EstateDataExamples {

  def SUT(estateRegistration: EstateRegistration) = new EstateDomainValidator(estateRegistration)


  "return validation error when individual personal representative has future date of birth" in {
    val estateRegistration = estatePerRepIndWithValues(estatePerRepIndDob = "2040-01-01")
    val response = SUT(estateRegistration).perRepDobIsNotFutureDate
    response mustNot be(None)
    response.map {
      error =>
        error.message mustBe "Date of birth must be today or in the past."
        error.location mustBe "/estate/entities/personalRepresentative/estatePerRepInd/dateOfBirth"
    }
    EstateBusinessValidation.check(estateRegistration).size mustBe 1

  }

  "return None when individual personal representative has valid date of birth" in {
    val estateRegistration = estatePerRepIndWithValues(estatePerRepIndDob = "1500-01-01")
    val response = SUT(estateRegistration).perRepDobIsNotFutureDate
    response mustBe None
    EstateBusinessValidation.check(estateRegistration) mustBe empty
  }

  "return validation error when personal representative organisation  utr is same as estate utr " in {
    val estateRegistration = estatePerRepOrgWithValues("5454541615")
    val response = SUT(estateRegistration).personalRepOrgUtrIsNotSameEstateUtr
    response mustNot be(None)
    response.map {
       error=>
        error.message mustBe "Personal representative organisation utr is same as estate utr."
        error.location mustBe "/estate/entities/personalRepresentative/estatePerRepOrg/identification/utr"
    }
    EstateBusinessValidation.check(estateRegistration).size mustBe 1
  }

  "return None when personal representative organisation  utr is different from estate utr " in {
    val estateRegistration = estatePerRepOrgWithValues("5454541616")
    val response = SUT(estateRegistration).personalRepOrgUtrIsNotSameEstateUtr
    response mustBe None
    EstateBusinessValidation.check(estateRegistration) mustBe empty
  }








}
