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
import javax.inject.Inject
import models.tax_enrolments._
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigrationService @Inject()(auditService: AuditService,
                                 taxEnrolmentConnector: TaxEnrolmentConnector,
                                 orchestratorConnector: OrchestratorConnector) extends Logging {

  def migrateSubscriberToTaxable(subscriptionId: String, urn: String)
                                (implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    taxEnrolmentConnector.migrateSubscriberToTaxable(subscriptionId, urn) recover {
      case e: Exception =>
        auditService.auditTaxEnrolmentTransformationToTaxableError(subscriptionId, urn, e.getMessage)
        TaxEnrolmentFailure
    }
  }

  def completeMigration(subscriptionId: String, urn: String)
                       (implicit hc: HeaderCarrier): Future[OrchestratorToTaxableResponse] = {
    logger.info(s"[MigrationService][SubscriptionId: $subscriptionId, URN: $urn].completeMigration")
    for {
      subscriptionResponse <- getUtrFromSubscriptions(subscriptionId, urn)
      orchestratorResponse <- updateOrchestratorToTaxable(subscriptionId, urn, subscriptionResponse.utr)
    } yield {
      orchestratorResponse
    }
  }

  private def getUtrFromSubscriptions(subscriptionId: String, urn: String)
                                     (implicit hc: HeaderCarrier): Future[TaxEnrolmentsSubscriptionsResponse] = {
    taxEnrolmentConnector.subscriptions(subscriptionId) recover {
      case e: Exception =>
        auditService.auditTaxEnrolmentTransformationToTaxableError(subscriptionId, urn, e.getMessage)
        throw e
    }
  }

  private def updateOrchestratorToTaxable(subscriptionId: String, urn: String, utr: String)
                                         (implicit hc: HeaderCarrier): Future[OrchestratorToTaxableResponse] = {
    orchestratorConnector.migrateToTaxable(urn, utr) recover {
      case e: Exception =>
        auditService.auditOrchestratorTransformationToTaxableError(urn, utr, e.getMessage)
        OrchestratorToTaxableFailure
    }
  }
}
