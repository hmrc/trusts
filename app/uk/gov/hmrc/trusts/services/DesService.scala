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

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.GetEstateResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustResponse, GetTrustSuccessResponse, TrustFoundResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.variation.{EstateVariation, TrustVariation, VariationResponse}
import uk.gov.hmrc.trusts.repositories.Repository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DesService @Inject()(val desConnector: DesConnector, val repository: Repository)  {

  private val logger = LoggerFactory.getLogger("application." + classOf[DesService].getCanonicalName)

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

  private def refreshCacheAndGetTrustInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier) = {
      logger.debug("Retrieving Trust Info from DES")
      desConnector.getTrustInfo(utr).map {
        case response:TrustProcessedResponse =>
          repository.set(utr, internalId, Json.toJson(response)(TrustProcessedResponse.mongoWrites))
          response
        case x => x
      }
  }

  def getTrustInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetTrustResponse] = {
    logger.debug("Getting trust Info")
    repository.get(utr, internalId).flatMap {
      case Some(x) => x.validate[GetTrustSuccessResponse].fold(
        errs => Future.failed[GetTrustResponse](new Exception(errs.toString)),
        response => Future.successful(response)
      )
      case None => refreshCacheAndGetTrustInfo(utr, internalId)
    }
  }

  def getEstateInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    desConnector.getEstateInfo(utr)
  }

  def trustVariation(trustVariation: TrustVariation)(implicit hc: HeaderCarrier): Future[VariationResponse] =
    desConnector.trustVariation(trustVariation: TrustVariation)

  def estateVariation(estateVariation: EstateVariation)(implicit hc: HeaderCarrier): Future[VariationResponse] =
    desConnector.estateVariation(estateVariation: EstateVariation)
}


