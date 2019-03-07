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
import uk.gov.hmrc.trusts.models.{ExistingTrustCheckRequest, Registration}
import uk.gov.hmrc.trusts.utils.JsonUtils


class ValidationServiceSpec extends BaseSpec {

  private lazy val validatationService: ValidationService = new ValidationService()
  private lazy val validator : Validator = validatationService.get("/resources/schemas/trustsApiRegistrationSchema_3.1.0.json")

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
    }
    "return a list of validaton errors " when {
      "json request is valid but failed in business rules for trust start date, efrbs start date " in {
        val jsonString = JsonUtils.getJsonFromFile("trust-business-validation-fail.json")
        validator.validate[Registration](jsonString).left.get.size mustBe 3
      }
    }
  }//validator

}
