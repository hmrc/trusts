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
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.exceptions.InternalServerErrorException
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.GetEstateResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustResponse, GetTrustSuccessResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.variation.{EstateVariation, VariationResponse}
import uk.gov.hmrc.trusts.repositories.Repository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DesService @Inject()(val desConnector: DesConnector, val repository: Repository)  {

  def getTrustInfoFormBundleNo(utr: String)(implicit hc:HeaderCarrier): Future[String] =
    desConnector.getTrustInfo(utr).map {
      case response: GetTrustSuccessResponse => response.responseHeader.formBundleNo
      case response =>
        val msg = s"Failed to retrieve latest form bundle no from ETMP : $response"
        Logger.warn(msg)
        throw InternalServerErrorException(s"Submission could not proceed, $msg")
    }

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
      Logger.debug("Retrieving Trust Info from DES")
      Logger.info(s"[DesService][refreshCacheAndGetTrustInfo] refreshing cache")
      desConnector.getTrustInfo(utr).map {
        case response:TrustProcessedResponse =>
          repository.set(utr, internalId, Json.toJson(response)(TrustProcessedResponse.mongoWrites))
          response
        case x => x
      }
  }

  def getTrustInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetTrustResponse] = {
    Logger.debug("Getting trust Info")
    repository.get(utr, internalId).flatMap {
      case Some(x) => x.validate[GetTrustSuccessResponse].fold(
        errs => {
          Logger.error(s"[DesService] unable to parse json from cache as GetTrustSuccessResponse $errs")
          Future.failed[GetTrustResponse](new Exception(errs.toString))
        },
        response => {
          Future.successful(response)
        }
      )
      case None => refreshCacheAndGetTrustInfo(utr, internalId)
    }
  }

  def getEstateInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    desConnector.getEstateInfo(utr)
  }

  def trustVariation(trustVariation: JsValue)(implicit hc: HeaderCarrier): Future[VariationResponse] =
    desConnector.trustVariation(trustVariation: JsValue)

  def estateVariation(estateVariation: EstateVariation)(implicit hc: HeaderCarrier): Future[VariationResponse] =
    desConnector.estateVariation(estateVariation: EstateVariation)
}


