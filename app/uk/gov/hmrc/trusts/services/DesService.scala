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
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.exceptions.InternalServerErrorException
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.existing_trust.{ExistingCheckRequest, ExistingCheckResponse}
import uk.gov.hmrc.trusts.models.get_trust.get_trust.{GetTrustResponse, GetTrustSuccessResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.registration.RegistrationResponse
import uk.gov.hmrc.trusts.models.tax_enrolments.SubscriptionIdResponse
import uk.gov.hmrc.trusts.models.variation.VariationResponse
import uk.gov.hmrc.trusts.repositories.CacheRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DesService @Inject()(val desConnector: DesConnector, val repository: CacheRepository) extends Logging {

  def getTrustInfoFormBundleNo(utr: String): Future[String] =
    desConnector.getTrustInfo(utr).map {
      case response: GetTrustSuccessResponse => response.responseHeader.formBundleNo
      case response =>
        val msg = s"Failed to retrieve latest form bundle no from ETMP : $response"
        logger.warn(msg)
        throw InternalServerErrorException(s"Submission could not proceed, $msg")
    }

  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse] = {
    desConnector.checkExistingTrust(existingTrustCheckRequest)
  }

  def registerTrust(registration: Registration): Future[RegistrationResponse] = {
    desConnector.registerTrust(registration)
  }

  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {
    desConnector.getSubscriptionId(trn)
  }

  def resetCache(utr: String, internalId: String) : Future[Unit] = {
    repository.resetCache(utr, internalId).map { _ =>
      Future.successful(())
    }
  }

  def refreshCacheAndGetTrustInfo(utr: String, internalId: String): Future[GetTrustResponse] = {
    logger.debug("Retrieving Trust Info from DES")
    logger.info(s"[DesService][refreshCacheAndGetTrustInfo] refreshing cache")

    repository.resetCache(utr, internalId).flatMap { _ =>
      desConnector.getTrustInfo(utr).map {
        case response: TrustProcessedResponse =>
          repository.set(utr, internalId, Json.toJson(response)(TrustProcessedResponse.mongoWrites))
          response
        case x => x
      }
    }
  }

  def getTrustInfo(identifier: String, internalId: String): Future[GetTrustResponse] = {
    logger.debug("Getting trust Info")
    repository.get(identifier, internalId).flatMap {
      case Some(x) => x.validate[GetTrustSuccessResponse].fold(
        errs => {
          logger.error(s"[DesService] unable to parse json from cache as GetTrustSuccessResponse $errs")
          Future.failed[GetTrustResponse](new Exception(errs.toString))
        },
        response => {
          Future.successful(response)
        }
      )
      case None => refreshCacheAndGetTrustInfo(identifier, internalId)
    }
  }

  def trustVariation(trustVariation: JsValue): Future[VariationResponse] =
    desConnector.trustVariation(trustVariation: JsValue)
}


