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


class DomainValidatorSpec extends BaseSpec with DataExamples {

  def SUT(registration: Registration) = new DomainValidator(registration)

  "trustStartDateIsNotFutureDate" should {
    "return None for valid date" in {
      SUT(registrationRequest).trustStartDateIsNotFutureDate mustBe None
    }

    "return validation error for tomorrow date" in {
      val registrationWithFutureStartDate = registrationWithStartDate(new DateTime().plusDays(1))
      println(SUT(registrationWithFutureStartDate).trustStartDateIsNotFutureDate)
      SUT(registrationWithFutureStartDate).trustStartDateIsNotFutureDate.get.message mustBe
        "Trust' start Date must be today or in the past."
    }
  }


}
