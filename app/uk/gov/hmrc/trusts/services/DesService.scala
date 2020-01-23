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

package uk.gov.hmrc.trusts.services

import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.GetEstateResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustResponse
import uk.gov.hmrc.trusts.models.variation.{EstateVariation, TrustVariation, VariationResponse}
import uk.gov.hmrc.trusts.repositories.Repository

import scala.concurrent.Future


class DesService @Inject()(val desConnector: DesConnector, val repository: Repository)  {

  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest)
                                 (implicit hc: HeaderCarrier): Future[ExistingCheckResponse] = {
    desConnector.checkExistingTrust(existingTrustCheckRequest)
  }

  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest)
                                 (implicit hc: HeaderCarrier): Future[ExistingCheckResponse] = {
    desConnector.checkExistingEstate(existingEstateCheckRequest)
  }

  def registerTrust(registration: Registration)
                            (implicit hc: HeaderCarrier): Future[RegistrationResponse] = {
    desConnector.registerTrust(registration)
  }

  def registerEstate(estateRegistration: EstateRegistration)
                            (implicit hc: HeaderCarrier): Future[RegistrationResponse] = {
    desConnector.registerEstate(estateRegistration)
  }

  def getSubscriptionId(trn: String)(implicit hc: HeaderCarrier): Future[SubscriptionIdResponse] = {
    desConnector.getSubscriptionId(trn)
  }

  def getTrustInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[JsValue] = {
    repository.get(utr, internalId)
    desConnector.getTrustInfo(utr)
  }

  def getEstateInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    desConnector.getEstateInfo(utr)
  }

  def trustVariation(trustVariation: TrustVariation)(implicit hc: HeaderCarrier): Future[VariationResponse] =
    desConnector.trustVariation(trustVariation: TrustVariation)

  def estateVariation(estateVariation: EstateVariation)(implicit hc: HeaderCarrier): Future[VariationResponse] =
    desConnector.estateVariation(estateVariation: EstateVariation)
}


