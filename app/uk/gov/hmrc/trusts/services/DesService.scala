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

import com.google.inject.ImplementedBy
import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.GetEstateResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustResponse
import uk.gov.hmrc.trusts.models.variation.{Variation, VariationResponse}

import scala.concurrent.Future


class DesServiceImpl @Inject()(val desConnector: DesConnector) extends DesService {

  override def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest)
                                 (implicit hc: HeaderCarrier): Future[ExistingCheckResponse] = {
    desConnector.checkExistingTrust(existingTrustCheckRequest)
  }

  override def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest)
                                 (implicit hc: HeaderCarrier): Future[ExistingCheckResponse] = {
    desConnector.checkExistingEstate(existingEstateCheckRequest)
  }

  override def registerTrust(registration: Registration)
                            (implicit hc: HeaderCarrier): Future[RegistrationResponse] = {
    desConnector.registerTrust(registration)
  }

  override def registerEstate(estateRegistration: EstateRegistration)
                            (implicit hc: HeaderCarrier): Future[RegistrationResponse] = {
    desConnector.registerEstate(estateRegistration)
  }

  override def getSubscriptionId(trn: String)(implicit hc: HeaderCarrier): Future[SubscriptionIdResponse] = {
    desConnector.getSubscriptionId(trn)
  }

  def getTrustInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetTrustResponse] = {
    desConnector.getTrustInfo(utr)
  }

  def getEstateInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    desConnector.getEstateInfo(utr)
  }

  def variation(variation: Variation)(implicit hc: HeaderCarrier): Future[VariationResponse] =
    desConnector.variations(variation: Variation)
}


@ImplementedBy(classOf[DesServiceImpl])
trait DesService {

  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest)(implicit hc: HeaderCarrier): Future[ExistingCheckResponse]
  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest)(implicit hc: HeaderCarrier): Future[ExistingCheckResponse]

  def registerTrust(registration: Registration)(implicit hc: HeaderCarrier): Future[RegistrationResponse]
  def registerEstate(estateRegistration: EstateRegistration)(implicit hc: HeaderCarrier): Future[RegistrationResponse]

  def getSubscriptionId(trn: String)(implicit hc: HeaderCarrier): Future[SubscriptionIdResponse]

  def getTrustInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetTrustResponse]
  def getEstateInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse]

  def variation(variation: Variation)(implicit hc: HeaderCarrier): Future[VariationResponse]
}
