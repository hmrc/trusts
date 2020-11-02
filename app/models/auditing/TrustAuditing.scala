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

package models.auditing

object TrustAuditing {

  val TRUST_REGISTRATION_SUBMITTED = "TrustRegistrationSubmitted"

  val GET_TRUST = "GetTrust"

  val TRUST_VARIATION = "TrustVariation"
  val TRUST_VARIATION_ATTEMPT = "TrustVariationAttempt"
  val TRUST_TRANSFORMATIONS = "TrustTransformations"

  val VARIATION_SUBMITTED_BY_ORGANISATION = "VariationSubmittedByOrganisation"
  val VARIATION_SUBMITTED_BY_AGENT = "VariationSubmittedByAgent"
  val CLOSURE_SUBMITTED_BY_ORGANISATION = "ClosureSubmittedByOrganisation"
  val CLOSURE_SUBMITTED_BY_AGENT = "ClosureSubmittedByAgent"

}