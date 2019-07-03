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

import uk.gov.hmrc.trusts.models._

trait JsonRequests extends JsonUtils {

  lazy val validRegistrationRequestJson  =  getJsonFromFile("valid-trusts-registration-api.json")
  lazy val invalidRegistrationRequestJson  =  getJsonFromFile("invalid-payload-trusts-registration.json")
  lazy val invalidTrustBusinessValidation  =  getJsonFromFile("trust-business-validation-fail.json")

  lazy val estateRegistration01  =  getJsonFromFile("valid-estate-registration-01.json")
  lazy val estateRegistration03  =  getJsonFromFile("valid-estate-registration-03.json")

  lazy val registrationRequest = getJsonValueFromFile("valid-trusts-registration-api.json").validate[Registration].get

  lazy val invalidRegistrationRequest = getJsonValueFromFile("invalid-payload-trusts-registration.json").validate[Registration].get

  lazy val estateRegRequest = getJsonValueFromFile("valid-estate-registration-01.json").validate[EstateRegistration].get

  lazy val getTrustResponseJson = getJsonFromFile("valid-get-trust-response.json")
  lazy val getTrustResponse = getJsonValueFromFile("valid-get-trust-response.json")

  lazy val getEstateResponseJson = getJsonFromFile("valid-get-estate-response.json")
  lazy val getEstateExpectedResponse = getJsonValueFromFile("valid-get-estate-expected-response.json")

  lazy val getTrustOrEstateProcessingResponseJson = getJsonFromFile("valid-get-trust-or-estate-in-processing-response.json")
  lazy val getTrustOrEstateProcessingResponse = getJsonValueFromFile("valid-get-trust-or-estate-in-processing-response.json")

  lazy val getTrustOrEstatePendingClosureResponseJson = getJsonFromFile("valid-get-trust-or-estate-pending-closure-response.json")
  lazy val getTrustOrEstatePendingClosureResponse = getJsonValueFromFile("valid-get-trust-or-estate-pending-closure-response.json")

  lazy val getTrustOrEstateClosedResponseJson = getJsonFromFile("valid-get-trust-or-estate-closed-response.json")
  lazy val getTrustOrEstateClosedResponse = getJsonValueFromFile("valid-get-trust-or-estate-closed-response.json")

  lazy val getTrustOrEstateSuspendedResponseJson = getJsonFromFile("valid-get-trust-or-estate-suspended-response.json")
  lazy val getTrustOrEstateSuspendedResponse = getJsonValueFromFile("valid-get-trust-or-estate-suspended-response.json")

  lazy val getTrustOrEstateParkedResponseJson = getJsonFromFile("valid-get-trust-or-estate-parked-response.json")
  lazy val getTrustOrEstateParkedResponse = getJsonValueFromFile("valid-get-trust-or-estate-parked-response.json")

  lazy val getTrustOrEstateObsoletedResponseJson = getJsonFromFile("valid-get-trust-or-estate-obsoleted-response.json")
  lazy val getTrustOrEstateObsoletedResponse = getJsonValueFromFile("valid-get-trust-or-estate-obsoleted-response.json")

  lazy val expectedParsedJson = getJsonValueFromFile("expected-parsed-trust-response.json")
}
