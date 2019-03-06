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
      val registrationWithFutureStartDate = registrationWithEfrbsStartDate(new DateTime().plusDays(1000),EMPLOYMENT_RELATED_TRUST)
      SUT(registrationRequest).trustEfrbsDateIsNotFutureDate mustBe None
    }

    "return None for today's date" in {
      val registrationWithFutureStartDate = registrationWithEfrbsStartDate(new DateTime(),EMPLOYMENT_RELATED_TRUST)
      SUT(registrationWithFutureStartDate).trustEfrbsDateIsNotFutureDate mustBe None
    }

    "return validation error for tomorrow date" in {
      val registrationWithFutureStartDate = registrationWithEfrbsStartDate(new DateTime().plusDays(1),EMPLOYMENT_RELATED_TRUST)
      SUT(registrationWithFutureStartDate).trustEfrbsDateIsNotFutureDate.get.message mustBe
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


}
