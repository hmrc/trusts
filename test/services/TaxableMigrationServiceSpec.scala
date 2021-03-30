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

import base.BaseSpec
import connector.{OrchestratorConnector, TaxEnrolmentConnector}
import exceptions.InternalServerErrorException
import models.tax_enrolments.{SubscriptionIdentifier, TaxEnrolmentSuccess, TaxEnrolmentsSubscriptionsResponse}
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.RecoverMethods.recoverToSucceededIf
import repositories.TaxableMigrationRepository
import uk.gov.hmrc.http.{BadRequestException, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxableMigrationServiceSpec extends BaseSpec {

  val subscriptionId = "sub123456789"
  val urn = "NTTRUST00000001"
  val utr = "123456789"

  "MigrationService" when {

    ".migrateSubscriberToTaxable" should {
      val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
      val mockOrchestratorConnector = mock[OrchestratorConnector]
      val taxableMigrationRepository = injector.instanceOf[TaxableMigrationRepository]
      val SUT = new TaxableMigrationService(mockTaxEnrolmentConnector, mockOrchestratorConnector, taxableMigrationRepository)

      "return TaxEnrolmentSuccess" in {
        when(mockTaxEnrolmentConnector.migrateSubscriberToTaxable(subscriptionId, urn)).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.migrateSubscriberToTaxable(subscriptionId, urn)
        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    ".completeMigration" should {
      val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
      val mockOrchestratorConnector = mock[OrchestratorConnector]
      val taxableMigrationRepository = injector.instanceOf[TaxableMigrationRepository]
      val SUT = new TaxableMigrationService(mockTaxEnrolmentConnector, mockOrchestratorConnector, taxableMigrationRepository)

      "return utr when we have a valid response" in {
        val subscriptionsResponse = TaxEnrolmentsSubscriptionsResponse(List(SubscriptionIdentifier("SAUTR", utr)), "")
        val orchestratorResponse = mock[HttpResponse]

        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).thenReturn(Future.successful(subscriptionsResponse))
        when(mockOrchestratorConnector.migrateToTaxable(urn, utr)).thenReturn(Future.successful(orchestratorResponse))

        val futureResult = SUT.completeMigration(subscriptionId, urn)
        whenReady(futureResult) {
          result => result mustBe utr
        }
      }

      "return BadRequestException if we don't supply a UTR" in {
        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).
          thenReturn(Future.successful(TaxEnrolmentsSubscriptionsResponse(Nil, "")))

        val futureResult = SUT.completeMigration(subscriptionId, urn)
        whenReady(futureResult.failed) {
          result => result mustBe a[BadRequestException]
        }
      }

      "return Exception when subscriptions returns an Exception" in {
        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).
          thenReturn(Future.failed(InternalServerErrorException("There was a problem")))

        val futureResult = SUT.completeMigration(subscriptionId, urn)
        whenReady(futureResult.failed) {
          result => result mustBe a[InternalServerErrorException]
        }
      }

      "return Exception when migrateToTaxable returns an Exception" in {
        val subscriptionsResponse = TaxEnrolmentsSubscriptionsResponse(List(SubscriptionIdentifier("SAUTR", utr)), "")

        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).
          thenReturn(Future.successful(subscriptionsResponse))
        when(mockOrchestratorConnector.migrateToTaxable(urn, utr)).
          thenReturn(Future.failed(InternalServerErrorException("There was a problem")))

        val futureResult = SUT.completeMigration(subscriptionId, urn)
        whenReady(futureResult.failed) {
          result => result mustBe a[InternalServerErrorException]
        }
      }
    }

    ".migratingFromNonTaxableToTaxable" should {

      "return true" when {
        "Some(true) returned from repository" in {
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(Some(true)))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id")

          whenReady(result) { r =>
            r mustBe true
          }
        }
      }

      "return false" when {

        "Some(false) returned from repository" in {
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(Some(false)))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id")

          whenReady(result) { r =>
            r mustBe false
          }
        }

        "None returned from repository" in {
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(None))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id")

          whenReady(result) { r =>
            r mustBe false
          }
        }
      }

      "return error" when {
        "failed to get the taxable migration flag from the repository" in {
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.failed(new Throwable("repository get failed")))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id")

          recoverToSucceededIf[Exception](result)
        }
      }
    }

    ".setTaxableMigrationFlag" should {
      "call repository set method" in {
        val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
        val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
        val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
        val SUT = new TaxableMigrationService(taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

        when(mockTaxableMigrationRepository.set(any(), any(), any())).thenReturn(Future.successful(true))

        val result = SUT.setTaxableMigrationFlag(urn, "id", migratingToTaxable = true)

        whenReady(result) { r =>
          r mustBe true
          verify(mockTaxableMigrationRepository).set(urn, "id", migratingToTaxable = true)
        }
      }
    }
  }
}
