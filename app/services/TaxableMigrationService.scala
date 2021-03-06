/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import javax.inject.Inject
import utils.Session

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxableMigrationService @Inject()(
                                         auditService: AuditService,
                                         taxEnrolmentConnector: TaxEnrolmentConnector,
                                         orchestratorConnector: OrchestratorConnector,
                                         taxableMigrationRepository: TaxableMigrationRepository
                                       ) extends Logging {

  def migrateSubscriberToTaxable(subscriptionId: String, urn: String)
                                (implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    taxEnrolmentConnector.migrateSubscriberToTaxable(subscriptionId, urn) recover {
      case e: Exception =>
        logger.error(s"[Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId, URN: $urn] failed to prepare tax-enrolments for UTR")
        auditService.auditTaxEnrolmentTransformationToTaxableError(subscriptionId, urn, e.getMessage)
        TaxEnrolmentFailure
    }
  }

  def completeMigration(subscriptionId: String, urn: String)
                       (implicit hc: HeaderCarrier): Future[OrchestratorToTaxableResponse] = {
    logger.info(s"[Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId, URN: $urn] tax-enrolment received UTR, completing migration to taxable process with orchestrator")
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
        logger.error(s"[Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId, URN: $urn] unable to get UTR from subscription to complete migration to taxable")
        auditService.auditTaxEnrolmentTransformationToTaxableError(subscriptionId, urn, e.getMessage)
        throw e
    }
  }

  private def updateOrchestratorToTaxable(urn: String, utr: String)
                                         (implicit hc: HeaderCarrier): Future[OrchestratorToTaxableResponse] = {
    orchestratorConnector.migrateToTaxable(urn, utr) recover {
      case e: Exception =>
        logger.error(s"[Session ID: ${Session.id(hc)}][UTR: $utr, URN: $urn] unable to trigger orchestration to clean up credentials")
        auditService.auditOrchestratorTransformationToTaxableError(urn, utr, e.getMessage)
        OrchestratorToTaxableFailure
    }
  }

  def migratingFromNonTaxableToTaxable(identifier: String, internalId: String): Future[Boolean] = {
    getTaxableMigrationFlag(identifier, internalId).map(_.contains(true))
  }

  def getTaxableMigrationFlag(identifier: String, internalId: String): Future[Option[Boolean]] = {
    taxableMigrationRepository.get(identifier, internalId).recoverWith {
      case e =>
        logger.error(s"Error getting taxable migration flag from repository: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def setTaxableMigrationFlag(identifier: String, internalId: String, migratingToTaxable: Boolean): Future[Boolean] = {
    taxableMigrationRepository.set(identifier, internalId, migratingToTaxable).recoverWith {
      case e =>
        logger.error(s"Error setting taxable migration flag in repository: ${e.getMessage}")
        Future.failed(e)
    }
  }

}
