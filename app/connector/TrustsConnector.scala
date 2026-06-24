/*
 * Copyright 2026 HM Revenue & Customs
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

package connector

import models.Registration
import models.existing_trust.{ExistingCheckRequest, ExistingCheckResponse}
import models.get_trust.GetTrustResponse
import models.registration.RegistrationResponse
import models.variation.VariationSuccessResponse
import play.api.libs.json.JsValue
import utils.TrustEnvelope.TrustEnvelope

trait TrustsConnector {

  val trustsServiceUrl: String
  val matchTrustsEndpoint: String
  val trustRegistrationEndpoint: String
  val trustVariationsEndpoint: String
  val getTrustOrEstateUrl: String

  def get5MLDTrustOrEstateEndpoint(identifier: String): String

  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest): TrustEnvelope[ExistingCheckResponse]

  def registerTrust(registration: Registration): TrustEnvelope[RegistrationResponse]

  def getTrustInfo(identifier: String): TrustEnvelope[GetTrustResponse]

  def trustVariation(trustVariations: JsValue): TrustEnvelope[VariationSuccessResponse]

}
