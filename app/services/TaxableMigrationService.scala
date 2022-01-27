/*
 * Copyright 2022 HM Revenue & Customs
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

import connector.{OrchestratorConnector, TaxEnrolmentConnector}
import models.tax_enrolments._
import play.api.Logging
import repositories.TaxableMigrationRepository
import services.auditing.MigrationAuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxableMigrationService @Inject()(
                                         auditService: MigrationAuditService,
                                         taxEnrolmentConnector: TaxEnrolmentConnector,
                                         orchestratorConnector: OrchestratorConnector,
                                         taxableMigrationRepository: TaxableMigrationRepository
                                       ) extends Logging {

  def migrateSubscriberToTaxable(subscriptionId: String, urn: String)
                                (implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    taxEnrolmentConnector.migrateSubscriberToTaxable(subscriptionId, urn) recover {
      case e: Exception =>
        logger.error(s"[Session ID: ${Session.id(hc)}]" +
          s"[SubscriptionId: $subscriptionId, URN: $urn]" +
          s" failed to prepare tax-enrolments for UTR"
        )

        auditService.auditTaxEnrolmentFailure(subscriptionId, urn, e.getMessage)

        TaxEnrolmentFailure
    }
  }

  def completeMigration(subscriptionId: String, urn: String)
                       (implicit hc: HeaderCarrier): Future[OrchestratorToTaxableResponse] = {
    logger.info(
      s"[Session ID: ${Session.id(hc)}]" +
      s"[SubscriptionId: $subscriptionId, URN: $urn]" +
      s" tax-enrolment received UTR, completing migration to taxable process with orchestrator"
    )

    for {
      subscriptionResponse <- getUtrFromSubscriptions(subscriptionId, urn)
      orchestratorResponse <- updateOrchestratorToTaxable(urn, subscriptionResponse.utr)
    } yield {
      orchestratorResponse
    }
  }

  private def getUtrFromSubscriptions(subscriptionId: String, urn: String)
                                     (implicit hc: HeaderCarrier): Future[TaxEnrolmentsSubscriptionsResponse] = {
    taxEnrolmentConnector.subscriptions(subscriptionId) recover {
      case e: Exception =>
        logger.error(
          s"[Session ID: ${Session.id(hc)}]" +
            s"[SubscriptionId: $subscriptionId, URN: $urn]" +
            s" unable to get UTR from subscription to complete migration to taxable"
        )

        auditService.auditTaxEnrolmentFailure(subscriptionId, urn, e.getMessage)

        throw e
    }
  }

  private def updateOrchestratorToTaxable(urn: String, utr: String)
                                         (implicit hc: HeaderCarrier): Future[OrchestratorToTaxableResponse] = {
    orchestratorConnector.migrateToTaxable(urn, utr) recover {
      case e: Exception =>
        logger.error(
          s"[Session ID: ${Session.id(hc)}]" +
            s"[UTR: $utr, URN: $urn]" +
            s" unable to trigger orchestration to clean up credentials"
        )

        auditService.auditOrchestratorFailure(urn, utr, e.getMessage)

        OrchestratorToTaxableFailure
    }
  }

  def migratingFromNonTaxableToTaxable(identifier: String, internalId: String, sessionId: String): Future[Boolean] = {
    getTaxableMigrationFlag(identifier, internalId, sessionId).map(_.contains(true))
  }

  def getTaxableMigrationFlag(identifier: String, internalId: String, sessionId: String): Future[Option[Boolean]] = {
    taxableMigrationRepository.get(identifier, internalId, sessionId).recoverWith {
      case e =>
        logger.error(s"Error getting taxable migration flag from repository: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def setTaxableMigrationFlag(identifier: String, internalId: String, sessionId: String, migratingToTaxable: Boolean): Future[Boolean] = {
    taxableMigrationRepository.set(identifier, internalId, sessionId, migratingToTaxable).recoverWith {
      case e =>
        logger.error(s"Error setting taxable migration flag in repository: ${e.getMessage}")
        Future.failed(e)
    }
  }

}
