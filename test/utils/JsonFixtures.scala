/*
 * Copyright 2024 HM Revenue & Customs
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

import models._
import play.api.libs.json.JsValue

object JsonFixtures extends JsonFixtures

trait JsonFixtures extends JsonUtils {

  lazy val validRegistrationRequestJson: String = getJsonFromFile("valid-trusts-registration-api.json")

  lazy val validRegistration5MldRequestJson: String = getJsonFromFile("valid-trusts-registration-api-5mld.json")

  lazy val validRegistration5MldNontaxableRequestJson: String = getJsonFromFile(
    "valid-trusts-registration-api-5mld-nontaxable.json"
  )

  lazy val invalidRegistrationRequestJson: String = getJsonFromFile("invalid-payload-trusts-registration.json")
  lazy val invalidTrustBusinessValidation: String = getJsonFromFile("trust-business-validation-fail.json")

  lazy val validTrustVariations5mldRequestJson: String = getJsonFromFile(
    "valid-trusts-variations-api-5mld-taxable.json"
  )

  lazy val validTrustVariationsTaxYears5mldRequestJson: String =
    getJsonFromFile("valid-trusts-variations-api-5mld-taxable-tax-years.json")

  lazy val invalidTrustVariationsRequestJson: String = getJsonFromFile("invalid-payload-trusts-variations.json")

  lazy val trustVariationsRequest: JsValue =
    getJsonValueFromFile("valid-trusts-variations-api.json").validate[JsValue].get

  lazy val trustVariationsNoPreviousPropertyValueRequest: JsValue = getJsonValueFromFile(
    "valid-trusts-variations-no-previous-value-property-api.json"
  ).validate[JsValue].get

  lazy val invalidTrustVariationsRequest: JsValue = getJsonValueFromFile("invalid-payload-trusts-variations.json")

  lazy val registrationRequest: Registration =
    getJsonValueFromFile("valid-trusts-registration-api.json").validate[Registration].get

  lazy val invalidRegistrationRequest: Registration =
    getJsonValueFromFile("invalid-payload-trusts-registration.json").validate[Registration].get

  lazy val get5MLDTrustResponseJson: String = getJsonFromFile("valid-get-trust-response.json")

  lazy val get5MLDTrustResponse: JsValue = getJsonValueFromFile("valid-get-trust-response.json")

  lazy val getTrustPropertyLandNoPreviousValue: String = getJsonFromFile(
    "valid-get-trust-response-property-or-land-no-previous-value.json"
  )

  lazy val getTrustPropertyLandNoPreviousValueJson: JsValue = getJsonValueFromFile(
    "valid-get-trust-response-property-or-land-no-previous-value.json"
  )

  lazy val getTransformedTrustResponse: JsValue = getJsonValueFromFile("transformed-get-trust-response.json")

  lazy val getTransformedTrustAllAssetsResponse: JsValue = getJsonValueFromFile(
    "transformed-get-trust-all-assets-response.json"
  )

  lazy val getTransformedTrustResponse5mld: JsValue = getJsonValueFromFile(
    "5MLD/Taxable/cached/2134514321-taxable.json"
  )

  lazy val getTransformedTrustResponseWithYearsReturns: JsValue = getJsonValueFromFile(
    "transformed-get-trust-response-with-years-returns.json"
  )

  lazy val getEmptyTransformedTrustResponse: JsValue = getJsonValueFromFile("empty-transformed-get-trust-response.json")

  lazy val getTransformedApiResponse: JsValue = getJsonValueFromFile("trust-transformed-get-api-result.json")

  lazy val getTransformedLeadTrusteeResponse: JsValue = getJsonValueFromFile(
    "trust-transformed-get-lead-trustee-result.json"
  )

  lazy val getTransformedTrusteesResponse: JsValue = getJsonValueFromFile("trust-transformed-get-trustees-result.json")

  lazy val getTransformedBeneficiariesResponse: JsValue = getJsonValueFromFile(
    "trust-transformed-get-beneficiary-result.json"
  )

  lazy val getTransformedSettlorsResponse: JsValue = getJsonValueFromFile("trust-transformed-get-settlor-result.json")

  lazy val getTransformedAllAssetsResponse: JsValue = getJsonValueFromFile(
    "trust-transformed-get-all-assets-result.json"
  )

  lazy val getTransformedTrustDeceasedSettlorWithoutDeathResponse: JsValue =
    getJsonValueFromFile("transformed-get-trust-response-deceased-settlor-without-date-of-death.json")

  lazy val getTransformedProtectorsResponse: JsValue = getJsonValueFromFile(
    "trust-transformed-get-protector-result.json"
  )

  lazy val getTransformedOtherIndividualsResponse: JsValue = getJsonValueFromFile(
    "trust-transformed-get-other-individual-result.json"
  )

  lazy val getTrustOrEstateProcessingResponseJson: String = getJsonFromFile(
    "valid-get-trust-or-estate-in-processing-response.json"
  )

  lazy val getTrustMalformedJsonResponse: String = getJsonFromFile("get-trust-malformed-json-response.json")

  lazy val expectedParsedJson: JsValue = getJsonValueFromFile("expected-parsed-trust-response.json")
}
