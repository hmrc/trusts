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

package models.tax_enrolments

import base.BaseSpec

class TaxEnrolmentsSubscriptionsResponseSpec extends BaseSpec {

  val testSubscriptionId = "testSubscriptionId"

  ".utr" should {
    "be None if the identifiers are empty" in {
      TaxEnrolmentsSubscriptionsResponse(identifiers = Nil, "").utr mustBe None
    }

    "be None if the identifiers doesn't have a SAUTR key" in {
      TaxEnrolmentsSubscriptionsResponse(identifiers = List(SubscriptionIdentifier("testKey", "testValue")), "").utr mustBe None
    }

    "be the correct value if the identifiers does have a SAUTR key" in {
      TaxEnrolmentsSubscriptionsResponse(identifiers = List(SubscriptionIdentifier("SAUTR", "testValue")), "").utr mustBe Some("testValue")
    }
  }
}
