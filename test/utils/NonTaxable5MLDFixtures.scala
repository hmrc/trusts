/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.JsValue

object NonTaxable5MLDFixtures extends JsonFixtures {

  object DES {
    /**
     * These fixtures are used to mock a response from the DES connector.
     * These fixtures must conform with the schema for API#1488
     */

    lazy val get5MLDTrustNonTaxableResponse: String = getJsonFromFile("5MLD/NonTaxable/des/valid-get-trust-5mld-non-taxable-des-response.json")
    lazy val newGet5MLDTrustNonTaxableResponse: String = getJsonFromFile("5MLD/NonTaxable/des/new-valid-get-trust-5mld-non-taxable-des-response.json")
    lazy val get5MLDTrustNonTaxableResponseWithAllAssetTypes: String = getJsonFromFile("5MLD/NonTaxable/des/valid-get-trust-5mld-non-taxable-des-response-with-all-asset-types.json")
  }

  object Cache {
    /**
     * These fixtures are used to mock the behaviour when the CacheRepository finds a record for Display or variation in the mongo cache.
     * The shape of the data is such that the HTTP reads has already been completed in GetTrustResponse.httpReads
     *
     * Ensure the data conforms to the structure of case class GetTrust()
     *
     * For instance, __ \ trust \ details \ trusts moves to -> __ \ trust
     *
     * The data is returned as an instance of a TrustsProcessed response
     */

    lazy val getTransformedNonTaxableTrustResponse: JsValue = getJsonValueFromFile("5MLD/NonTaxable/cached/transformed-get-non-taxable-trust-response.json")

  }

  object Trusts {
    /**
     * These fixtures are used to test the response from the trusts microservice at the:
     * /trusts/$identifier and /trusts/$identifier/transformed endpoints
     */
    object Assets {
      lazy val nonTaxable5mldAssets: JsValue = getJsonValueFromFile("5MLD/NonTaxable/trusts/assets/non-taxable-assets.json")
    }

    lazy val getTransformedNonTaxableTrustResponse: JsValue = getJsonValueFromFile("5MLD/NonTaxable/trusts/get-trust-transformed-non-taxable-response.json")
    lazy val newGetTransformedNonTaxableTrustResponse: JsValue = getJsonValueFromFile("5MLD/NonTaxable/trusts/new-get-trust-transformed-non-taxable-response.json")
    lazy val getTransformedNonTaxableTrustResponseWithAllAssetTypes: JsValue = getJsonValueFromFile("5MLD/NonTaxable/trusts/get-trust-transformed-non-taxable-response-with-all-asset-types.json")

  }

}
