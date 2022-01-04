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

import base.BaseSpec
import connector.{OrchestratorConnector, TaxEnrolmentConnector}
import exceptions.{InternalServerErrorException, InvalidDataException}
import models.tax_enrolments._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers._
import org.scalatest.RecoverMethods.recoverToSucceededIf
import repositories.TaxableMigrationRepository
import services.auditing.MigrationAuditService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxableMigrationServiceSpec extends BaseSpec {

  val subscriptionId = "sub123456789"
  val urn = "NTTRUST00000001"
  val utr = "123456789"

  "TaxableMigrationService" when {

    ".migrateSubscriberToTaxable" should {
      val mockAuditService = mock[MigrationAuditService]
      val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
      val mockOrchestratorConnector = mock[OrchestratorConnector]
      val taxableMigrationRepository = injector.instanceOf[TaxableMigrationRepository]
      val SUT = new TaxableMigrationService(mockAuditService, mockTaxEnrolmentConnector, mockOrchestratorConnector, taxableMigrationRepository)

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
      val mockAuditService = mock[MigrationAuditService]
      val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
      val mockOrchestratorConnector = mock[OrchestratorConnector]
      val taxableMigrationRepository = injector.instanceOf[TaxableMigrationRepository]
      val SUT = new TaxableMigrationService(mockAuditService, mockTaxEnrolmentConnector, mockOrchestratorConnector, taxableMigrationRepository)

      "return utr when we have a valid response" in {
        val subscriptionsResponse = TaxEnrolmentsSubscriptionsResponse(subscriptionId, utr, "PROCESSED")

        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).thenReturn(Future.successful(subscriptionsResponse))
        when(mockOrchestratorConnector.migrateToTaxable(urn, utr)).thenReturn(Future.successful(OrchestratorToTaxableSuccess))

        val futureResult = SUT.completeMigration(subscriptionId, urn)
        whenReady(futureResult) {
          result => result mustBe OrchestratorToTaxableSuccess
        }
      }

      "return InvalidDataException if we don't supply a UTR" in {
        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).
          thenReturn(Future.failed(InvalidDataException(s"No UTR supplied")))

        val futureResult = SUT.completeMigration(subscriptionId, urn)
        whenReady(futureResult.failed) {
          result => result mustBe a[InvalidDataException]
        }

        verify(mockAuditService).auditTaxEnrolmentFailure(eqTo(subscriptionId), eqTo(urn), any[String])(any[HeaderCarrier])
      }

      "return Exception when subscriptions returns an Exception" in {
        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).
          thenReturn(Future.failed(InternalServerErrorException("There was a problem")))

        val futureResult = SUT.completeMigration(subscriptionId, urn)
        whenReady(futureResult.failed) {
          result => result mustBe a[InternalServerErrorException]
        }
      }

      "return OrchestratorToTaxableFailure when migrateToTaxable returns an Exception" in {
        val subscriptionsResponse = TaxEnrolmentsSubscriptionsResponse(subscriptionId, utr, "PROCESSED")

        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).
          thenReturn(Future.successful(subscriptionsResponse))
        when(mockOrchestratorConnector.migrateToTaxable(urn, utr)).
          thenReturn(Future.failed(InternalServerErrorException("There was a problem")))

        val futureResult = SUT.completeMigration(subscriptionId, urn)
        whenReady(futureResult) {
          result => result mustBe OrchestratorToTaxableFailure
        }

        verify(mockAuditService).auditOrchestratorFailure(eqTo(urn), eqTo(utr), eqTo("There was a problem"))(any[HeaderCarrier])
      }
    }

    ".migratingFromNonTaxableToTaxable" should {

      "return true" when {
        "taxable migration flag is set to true" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]

          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(Some(true)))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id")

          whenReady(result) { r =>
            r mustBe true
          }
        }
      }

      "return false" when {

        "taxable migration flag is set to false" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(Some(false)))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id")

          whenReady(result) { r =>
            r mustBe false
          }
        }

        "taxable migration flag is undefined" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(None))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id")

          whenReady(result) { r =>
            r mustBe false
          }
        }
      }

      "return error" when {
        "fails to get the taxable migration flag from the repository" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.failed(new Throwable("repository get failed")))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id")

          recoverToSucceededIf[Exception](result)
        }
      }
    }

    ".getTaxableMigrationFlag" should {

      "return Some(true)" when {
        "the taxable migration flag is set to true" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(Some(true)))

          val result = SUT.getTaxableMigrationFlag(urn, "id")

          whenReady(result) { r =>
            r mustBe Some(true)
            verify(mockTaxableMigrationRepository).get(urn, "id")
          }
        }
      }

      "return Some(false)" when {
        "the taxable migration flag is set to false" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(Some(false)))

          val result = SUT.getTaxableMigrationFlag(urn, "id")

          whenReady(result) { r =>
            r mustBe Some(false)
            verify(mockTaxableMigrationRepository).get(urn, "id")
          }
        }
      }

      "return None" when {
        "the taxable migration flag is undefined" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.successful(None))

          val result = SUT.getTaxableMigrationFlag(urn, "id")

          whenReady(result) { r =>
            r mustBe None
            verify(mockTaxableMigrationRepository).get(urn, "id")
          }
        }
      }

      "return error" when {
        "fails to get the taxable migration flag from the repository" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.get(any(), any())).thenReturn(Future.failed(new Throwable("repository get failed")))

          val result = SUT.getTaxableMigrationFlag(urn, "id")

          recoverToSucceededIf[Exception](result)
        }
      }
    }

    ".setTaxableMigrationFlag" should {
      "call repository set method" in {
        val auditService = injector.instanceOf[MigrationAuditService]
        val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
        val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
        val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
        val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

        when(mockTaxableMigrationRepository.set(any(), any(), any())).thenReturn(Future.successful(true))

        val result = SUT.setTaxableMigrationFlag(urn, "id", migratingToTaxable = true)

        whenReady(result) { r =>
          r mustBe true
          verify(mockTaxableMigrationRepository).set(urn, "id", migratingToTaxable = true)
        }
      }

      "return error" when {
        "fails to set the taxable migration flag in the repository" in {
          val auditService = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT = new TaxableMigrationService(auditService, taxEnrolmentConnector, orchestratorConnector, mockTaxableMigrationRepository)

          when(mockTaxableMigrationRepository.set(any(), any(), any())).thenReturn(Future.failed(new Throwable("repository set failed")))

          val result = SUT.setTaxableMigrationFlag(urn, "id", migratingToTaxable = true)

          recoverToSucceededIf[Exception](result)
        }
      }
    }
  }
}
