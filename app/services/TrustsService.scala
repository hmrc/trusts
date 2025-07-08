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

package services

import cats.data.EitherT
import connector.{SubscriptionConnector, TrustsConnector}
import errors.{InternalServerErrorResponse, ServerError, VariationFailureForAudit}
import models._
import models.existing_trust.{ExistingCheckRequest, ExistingCheckResponse}
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse, TrustProcessedResponse}
import models.registration.RegistrationResponse
import models.tax_enrolments.SubscriptionIdSuccessResponse
import models.variation.VariationSuccessResponse
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import repositories.CacheRepository
import utils.TrustEnvelope.TrustEnvelope

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrustsService @Inject()(val trustsConnector: TrustsConnector,
                              val subscriptionConnector: SubscriptionConnector,
                              val repository: CacheRepository)(implicit ec: ExecutionContext) extends Logging {

  private val className = this.getClass.getSimpleName

  def getTrustInfoFormBundleNo(identifier: String): TrustEnvelope[String] = EitherT {
    trustsConnector.getTrustInfo(identifier).value.map {
      case Right(response: GetTrustSuccessResponse) => Right(response.responseHeader.formBundleNo)
      case Right(response) =>
        val msg = s"Failed to retrieve latest form bundle no from ETMP: $response"
        logger.error(s"[$className][getTrustInfoFormBundleNo][UTR/URN: $identifier] $msg")
        Left(VariationFailureForAudit(InternalServerErrorResponse, s"Submission could not proceed. $msg"))

      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][UTR/URN: $identifier] Failed to get trust info. $message")
        Left(ServerError())

      case Left(_) =>
        logger.warn(s"[$className][UTR/URN: $identifier] Failed to get trust info.")
        Left(ServerError())
    }
  }

  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest): TrustEnvelope[ExistingCheckResponse] = {
    trustsConnector.checkExistingTrust(existingTrustCheckRequest)
  }

  def registerTrust(registration: Registration): TrustEnvelope[RegistrationResponse] = {
    trustsConnector.registerTrust(registration)
  }

  def getSubscriptionId(trn: String): TrustEnvelope[SubscriptionIdSuccessResponse] = {
    subscriptionConnector.getSubscriptionId(trn)
  }

  def resetCache(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Unit] = EitherT {
    repository.resetCache(identifier, internalId, sessionId).value.map {
      case Left(trustErrors) => Left(trustErrors)
      case Right(_) => Right(())
    }
  }

  def refreshCacheAndGetTrustInfo(identifier: String, internalId: String, sessionId: String): TrustEnvelope[GetTrustResponse] = EitherT {
    logger.info(s"refreshCacheAndGetTrustInfo...start $identifier, $internalId, $sessionId")
    repository.resetCache(identifier, internalId, sessionId).value.flatMap {
      case Right(_) =>
        trustsConnector.getTrustInfo(identifier).value.flatMap {
          case Right(response: TrustProcessedResponse) =>
            repository.set(identifier, internalId, sessionId, Json.toJson(response)(TrustProcessedResponse.mongoWrites)).value
              .map {
                case Left(_) =>
                  logger.warn(s"[$className][refreshCacheAndGetTrustInfo][SessionId: $sessionId] problem while setting trust info.")
                  Left(ServerError())
                case Right(_) => Right(response)
              }
          case Right(getTrustResponse) => Future.successful(Right(getTrustResponse))
          case Left(ServerError(message)) if message.nonEmpty =>
            logger.warn(s"[$className][refreshCacheAndGetTrustInfo][SessionId: $sessionId] failed to get trust info. Message: $message")
            Future.successful(Left(ServerError()))
          case Left(_) =>
            Future.successful(Left(ServerError()))
        }
      case Left(_) =>
        logger.warn(s"[$className][refreshCacheAndGetTrustInfo][SessionId: $sessionId] reset cache failed.")
        Future.successful(Left(ServerError()))
    }
  }

  def getTrustInfo(identifier: String, internalId: String, sessionId: String): TrustEnvelope[GetTrustResponse] = EitherT {
    logger.info(s"getTrustInfo..start $identifier, $internalId, $sessionId")
    repository.get(identifier, internalId, sessionId).value.flatMap {
      case Right(Some(value)) =>
        value.validate[GetTrustSuccessResponse].fold(
          errs => {
            logger.error(s"[$className][getTrustInfo][SessionId: $sessionId] " +
              s"Unable to parse json from cache as GetTrustSuccessResponse - $errs")
            Future.successful(Left(ServerError(errs.toString)))
          },
          response => {
            Future.successful(Right(response))
          }
        )
      case Right(None) =>
        refreshCacheAndGetTrustInfo(identifier, internalId, sessionId).value
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][getTrustInfo][SessionId: $sessionId] Failed to get data from cache repository. Message: $message")
        Future.successful(Left(ServerError()))
      case Left(_) =>
        logger.warn(s"[$className][getTrustInfo][SessionId: $sessionId] Failed to get data from cache repository.")
        Future.successful(Left(ServerError()))
    }
  }

  def trustVariation(trustVariation: JsValue): TrustEnvelope[VariationSuccessResponse] = EitherT {
    trustsConnector.trustVariation(trustVariation: JsValue).value.map {
      case Left(ServerError(message)) if message.nonEmpty => Left(VariationFailureForAudit(InternalServerErrorResponse, message))
      case value => value
    }
  }
}
