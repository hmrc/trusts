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

import play.api.libs.json.JsValue
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.variation.{EstateVariation, TrustVariation}

trait JsonRequests extends JsonUtils {

  lazy val validRegistrationRequestJson: String =  getJsonFromFile("valid-trusts-registration-api.json")
  lazy val invalidRegistrationRequestJson: String =  getJsonFromFile("invalid-payload-trusts-registration.json")
  lazy val invalidTrustBusinessValidation: String =  getJsonFromFile("trust-business-validation-fail.json")

  lazy val estateRegRequest: EstateRegistration = getJsonValueFromFile("valid-estate-registration-01.json").validate[EstateRegistration].get

  lazy val estateRegistration01: String =  getJsonFromFile("valid-estate-registration-01.json")
  lazy val estateRegistration03: String =  getJsonFromFile("valid-estate-registration-03.json")

  lazy val validTrustVariationsRequestJson: String =  getJsonFromFile("valid-trusts-variations-api.json")
  lazy val invalidTrustVariationsRequestJson: String = getJsonFromFile("invalid-payload-trusts-variations.json")

  lazy val validEstateVariationsRequestJson: String =  getJsonFromFile("valid-estate-variation-api.json")
  lazy val invalidEstateVariationsRequestJson: String = getJsonFromFile("invalid-estate-variation-api.json")

  lazy val trustVariationsRequest: JsValue = getJsonValueFromFile("valid-trusts-variations-api.json").validate[JsValue].get
  lazy val invalidTrustVariationsRequest: JsValue = getJsonValueFromFile("invalid-payload-trusts-variations.json")

  lazy val estateVariationsRequest: EstateVariation = getJsonValueFromFile("valid-estate-variation-api.json").validate[EstateVariation].get
  lazy val invalidEstateVariationsRequest: JsValue = getJsonValueFromFile("invalid-estate-variation-api.json")

  lazy val registrationRequest: Registration = getJsonValueFromFile("valid-trusts-registration-api.json").validate[Registration].get
  lazy val invalidRegistrationRequest: Registration = getJsonValueFromFile("invalid-payload-trusts-registration.json").validate[Registration].get

  lazy val getTrustResponseJson: String = getJsonFromFile("valid-get-trust-response.json")
  lazy val getTrustResponse: JsValue = getJsonValueFromFile("valid-get-trust-response.json")
  lazy val getTransformedTrustResponse: JsValue = getJsonValueFromFile("transformed-get-trust-response.json")
  lazy val getEmptyTransformedTrustResponse: JsValue = getJsonValueFromFile("empty-transformed-get-trust-response.json")
  lazy val getTransformedApiResponse: JsValue = getJsonValueFromFile("trust-transformed-get-api-result.json")
  lazy val getTransformedLeadTrusteeResponse: JsValue = getJsonValueFromFile("trust-transformed-get-lead-trustee-result.json")
  lazy val getTransformedTrusteesResponse: JsValue = getJsonValueFromFile("trust-transformed-get-trustees-result.json")
  lazy val getTransformedBeneficiariesResponse: JsValue = getJsonValueFromFile("trust-transformed-get-beneficiary-result.json")
  lazy val getTransformedSettlorsResponse: JsValue = getJsonValueFromFile("trust-transformed-get-settlor-result.json")

  lazy val getEstateResponseJson: String = getJsonFromFile("valid-get-estate-response.json")
  lazy val getEstateExpectedResponse: JsValue = getJsonValueFromFile("valid-get-estate-expected-response.json")

  lazy val getTrustOrEstateProcessingResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-in-processing-response.json")

  lazy val getTrustMalformedJsonResponse: String = getJsonFromFile("get-trust-malformed-json-response.json")

  lazy val getTrustOrEstateProcessingResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-in-processing-response.json")

  lazy val getTrustOrEstatePendingClosureResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-pending-closure-response.json")
  lazy val getTrustOrEstatePendingClosureResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-pending-closure-response.json")

  lazy val getTrustOrEstateClosedResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-closed-response.json")
  lazy val getTrustOrEstateClosedResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-closed-response.json")

  lazy val getTrustOrEstateSuspendedResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-suspended-response.json")
  lazy val getTrustOrEstateSuspendedResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-suspended-response.json")

  lazy val getTrustOrEstateParkedResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-parked-response.json")
  lazy val getTrustOrEstateParkedResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-parked-response.json")

  lazy val getTrustOrEstateObsoletedResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-obsoleted-response.json")
  lazy val getTrustOrEstateObsoletedResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-obsoleted-response.json")

  lazy val expectedParsedJson: JsValue = getJsonValueFromFile("expected-parsed-trust-response.json")
}
