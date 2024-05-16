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
import connector.{OrchestratorConnector, TaxEnrolmentConnector}
import errors.{OrchestratorToTaxableFailure, ServerError}
import models.tax_enrolments._
import play.api.Logging
import repositories.TaxableMigrationRepository
import services.auditing.MigrationAuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session
import utils.TrustEnvelope.TrustEnvelope

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TaxableMigrationService @Inject()(
                                         auditService: MigrationAuditService,
                                         taxEnrolmentConnector: TaxEnrolmentConnector,
                                         orchestratorConnector: OrchestratorConnector,
                                         taxableMigrationRepository: TaxableMigrationRepository
                                       )(implicit ec: ExecutionContext) extends Logging {

  private val className = this.getClass.getSimpleName

  def migrateSubscriberToTaxable(subscriptionId: String, urn: String)
                                (implicit hc: HeaderCarrier): TrustEnvelope[TaxEnrolmentSubscriberResponse] = EitherT {
    taxEnrolmentConnector.migrateSubscriberToTaxable(subscriptionId, urn).value.map {
      case Left(error: ServerError) if error.message.nonEmpty =>
        logger.warn(s"[$className][migrateSubscriberToTaxable][Session ID: ${Session.id(hc)}]" +
          s"[SubscriptionId: $subscriptionId, URN: $urn] failed to prepare tax-enrolments for UTR"
        )
        auditService.auditTaxEnrolmentFailure(subscriptionId, urn, error.message)
        Left(ServerError())

      case Left(_) => Left(ServerError())
      case Right(response) => Right(response)
    }
  }

  def completeMigration(subscriptionId: String, urn: String)
                       (implicit hc: HeaderCarrier): TrustEnvelope[OrchestratorToTaxableSuccessResponse] = EitherT {
    logger.info(
      s"[$className][completeMigration][Session ID: ${Session.id(hc)}]" +
        s"[SubscriptionId: $subscriptionId, URN: $urn]" +
        s" tax-enrolment received UTR, completing migration to taxable process with orchestrator"
    )

    val result = for {
      subscriptionResponse <- getUtrFromSubscriptions(subscriptionId, urn)
      orchestratorResponse <- updateOrchestratorToTaxable(urn, subscriptionResponse.utr)
    } yield {
      orchestratorResponse
    }

    result.value.map {
      case Right(orchestratorResponse) => Right(orchestratorResponse)
      case Left(errors.TaxEnrolmentFailure) =>
        logger.warn(s"[$className][completeMigration][Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId, URN: $urn] " +
        s"failed to complete migration due to TaxEnrolmentFailure")
        Left(ServerError("TaxEnrolmentFailure"))
      case Left(OrchestratorToTaxableFailure) =>
        logger.warn(s"[$className][completeMigration][Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId, URN: $urn] " +
        s"failed to complete migration due to OrchestratorToTaxableFailure")
        Left(ServerError("OrchestratorToTaxableFailure"))
      case Left(_) =>
        logger.warn(s"[$className][completeMigration][Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId, URN: $urn] " +
          s"there was a problem, failed to complete migration")
        Left(ServerError())
    }
  }

  private def getUtrFromSubscriptions(subscriptionId: String, urn: String)
                                     (implicit hc: HeaderCarrier): TrustEnvelope[TaxEnrolmentsSubscriptionsSuccessResponse] = EitherT {
    taxEnrolmentConnector.subscriptions(subscriptionId).value.map {
      case Right(taxEnrolmentsSubscriptionsSuccessResponse) => Right(taxEnrolmentsSubscriptionsSuccessResponse)
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][getUtrFromSubscriptions][Session ID: ${Session.id(hc)}]" +
          s"[SubscriptionId: $subscriptionId, URN: $urn] unable to get UTR from subscription to complete migration to taxable"
      )
        auditService.auditTaxEnrolmentFailure(subscriptionId, urn, message)
        Left(errors.TaxEnrolmentFailure)

      case Left(_) => Left(ServerError())
    }
  }

  private def updateOrchestratorToTaxable(urn: String, utr: String)
                                         (implicit hc: HeaderCarrier): TrustEnvelope[OrchestratorToTaxableSuccessResponse] = EitherT {
    orchestratorConnector.migrateToTaxable(urn, utr).value.map {
      case Right(successResponse) => Right(successResponse)
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][updateOrchestratorToTaxable][Session ID: ${Session.id(hc)}]" +
          s"[UTR: $utr, URN: $urn] unable to trigger orchestration to clean up credentials"
      )
        auditService.auditOrchestratorFailure(urn, utr, message)
        Left(OrchestratorToTaxableFailure)

      case Left(_) => Left(ServerError())
    }
  }

  def migratingFromNonTaxableToTaxable(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Boolean] = EitherT {
    getTaxableMigrationFlag(identifier, internalId, sessionId).value.map {
      case Right(Some(value)) => Right(value)
      case Right(_) => Right(false)
      case Left(trustErrors) => Left(trustErrors)
    }

  }

  def getTaxableMigrationFlag(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Option[Boolean]] = EitherT {
    taxableMigrationRepository.get(identifier, internalId, sessionId).value.map {
      case Right(value) => Right(value)
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.error(s"[$className][getTaxableMigrationFlag] failed to get taxable migration flag from repository. Message: $message")
        Left(ServerError())
      case Left(_) =>
        logger.error(s"[$className][getTaxableMigrationFlag] failed to get taxable migration flag from repository.")
        Left(ServerError())
    }
  }

  def setTaxableMigrationFlag(identifier: String, internalId: String, sessionId: String, migratingToTaxable: Boolean): TrustEnvelope[Boolean] = EitherT {
    taxableMigrationRepository.set(identifier, internalId, sessionId, migratingToTaxable).value.map {
      case Right(value) => Right(value)
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.error(s"[$className][setTaxableMigrationFlag] failed to set taxable migration flag in repository. Message: $message")
        Left(ServerError())
      case Left(_) =>
        logger.error(s"[$className][setTaxableMigrationFlag] failed to set taxable migration flag in repository.")
        Left(ServerError())
    }
  }

}
