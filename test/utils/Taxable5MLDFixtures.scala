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

package utils

import play.api.libs.json.JsValue

object Taxable5MLDFixtures extends JsonFixtures {

  object DES {
    /**
     * These fixtures are used to mock a response from the DES connector.
     * These fixtures must conform with the schema for API#1488
     */


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
     * The data is returned as part of a TrustProcessedResponse
     */

    lazy val taxable5mld2134514321: JsValue = getJsonValueFromFile("5MLD/Taxable/cached/2134514321-taxable.json")

  }

  object Trusts {
    /**
     * These fixtures are used to test the response from the trusts microservice at the:
     * /trusts/:identifier
     * /trusts/:identifier/transformed endpoints
     * /trusts/:identifier/transformed/lead-trustee
     */

    object LeadTrustee {
      lazy val taxable5mld2134514321LeadTrustee: JsValue =
        getJsonValueFromFile("5MLD/Taxable/trusts/lead-trustee/2134514321-taxable-lead-trustee.json")
    }

    object Trustees {
      lazy val taxable5mld2134514321Trustees: JsValue =
        getJsonValueFromFile("5MLD/Taxable/trusts/trustees/2134514321-taxable-trustees.json")
    }

    object Beneficiaries {
      lazy val taxable5mld2134514321Beneficiaries: JsValue =
        getJsonValueFromFile("5MLD/Taxable/trusts/beneficiaries/2134514321-taxable-beneficiaries.json")
    }

    object Settlors {
      lazy val taxable5mld2134514321Settlors: JsValue =
        getJsonValueFromFile("5MLD/Taxable/trusts/settlors/2134514321-taxable-settlors.json")
    }

  }

}
