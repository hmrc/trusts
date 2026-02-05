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

import base.BaseSpec
import cats.data.EitherT
import connector.{OrchestratorConnector, TaxEnrolmentConnector}
import errors._
import models.tax_enrolments._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers._
import repositories.TaxableMigrationRepository
import services.auditing.MigrationAuditService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxableMigrationServiceSpec extends BaseSpec {

  private val subscriptionId    = "sub123456789"
  private val urn               = "NTTRUST00000001"
  private val utr               = "123456789"
  private val sessionId: String = "sessionId"

  "TaxableMigrationService" when {

    ".migrateSubscriberToTaxable" should {
      val mockAuditService           = mock[MigrationAuditService]
      val mockTaxEnrolmentConnector  = mock[TaxEnrolmentConnector]
      val mockOrchestratorConnector  = mock[OrchestratorConnector]
      val taxableMigrationRepository = injector.instanceOf[TaxableMigrationRepository]
      val SUT                        = new TaxableMigrationService(
        mockAuditService,
        mockTaxEnrolmentConnector,
        mockOrchestratorConnector,
        taxableMigrationRepository
      )

      "return TaxEnrolmentSuccess" in {
        when(mockTaxEnrolmentConnector.migrateSubscriberToTaxable(subscriptionId, urn))
          .thenReturn(
            EitherT[Future, TrustErrors, TaxEnrolmentSubscriberResponse](Future.successful(Right(TaxEnrolmentSuccess)))
          )

        val futureResult = SUT.migrateSubscriberToTaxable(subscriptionId, urn).value
        whenReady(futureResult) { result =>
          result mustBe Right(TaxEnrolmentSuccess)
        }
      }

      "send audit for TaxEnrolmentFailure when failed to prepare tax-enrolments for utr" in {
        when(mockTaxEnrolmentConnector.migrateSubscriberToTaxable(subscriptionId, urn))
          .thenReturn(
            EitherT[Future, TrustErrors, TaxEnrolmentSubscriberResponse](
              Future.successful(Left(ServerError("exception message")))
            )
          )

        val futureResult = SUT.migrateSubscriberToTaxable(subscriptionId, urn).value
        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }

        verify(mockAuditService).auditTaxEnrolmentFailure(eqTo(subscriptionId), eqTo(urn), any[String])(
          any[HeaderCarrier]
        )
      }

      "return a ServeError() when migrateSubscriberToTaxable returns an error" in {
        when(mockTaxEnrolmentConnector.migrateSubscriberToTaxable(subscriptionId, urn))
          .thenReturn(
            EitherT[Future, TrustErrors, TaxEnrolmentSubscriberResponse](Future.successful(Left(ServerError())))
          )

        val futureResult = SUT.migrateSubscriberToTaxable(subscriptionId, urn).value
        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }
      }
    }

    ".completeMigration" should {
      val mockAuditService           = mock[MigrationAuditService]
      val mockTaxEnrolmentConnector  = mock[TaxEnrolmentConnector]
      val mockOrchestratorConnector  = mock[OrchestratorConnector]
      val taxableMigrationRepository = injector.instanceOf[TaxableMigrationRepository]
      val SUT                        = new TaxableMigrationService(
        mockAuditService,
        mockTaxEnrolmentConnector,
        mockOrchestratorConnector,
        taxableMigrationRepository
      )

      "return utr when we have a valid response" in {
        val subscriptionsResponse = TaxEnrolmentsSubscriptionsSuccessResponse(subscriptionId, utr, "PROCESSED")

        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId))
          .thenReturn(
            EitherT[Future, TrustErrors, TaxEnrolmentsSubscriptionsSuccessResponse](
              Future.successful(Right(subscriptionsResponse))
            )
          )
        when(mockOrchestratorConnector.migrateToTaxable(urn, utr))
          .thenReturn(
            EitherT[Future, TrustErrors, OrchestratorToTaxableSuccessResponse](
              Future.successful(Right(OrchestratorToTaxableSuccessResponse()))
            )
          )

        val futureResult = SUT.completeMigration(subscriptionId, urn).value
        whenReady(futureResult) { result =>
          result mustBe Right(OrchestratorToTaxableSuccessResponse())
        }
      }

      "return ServerError(\"TaxEnrolmentFailure\") if we don't supply a UTR" in {
        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId))
          .thenReturn(
            EitherT[Future, TrustErrors, TaxEnrolmentsSubscriptionsSuccessResponse](
              Future.successful(
                Left(ServerError(s"No UTR supplied"))
              )
            )
          )

        val futureResult = SUT.completeMigration(subscriptionId, urn).value
        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("TaxEnrolmentFailure"))
        }

        verify(mockAuditService).auditTaxEnrolmentFailure(eqTo(subscriptionId), eqTo(urn), any[String])(
          any[HeaderCarrier]
        )
      }

      "return ServerError(\"TaxEnrolmentFailure\") when subscriptions returns an Exception" in {
        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId)).thenReturn(
          EitherT[Future, TrustErrors, TaxEnrolmentsSubscriptionsSuccessResponse](
            Future.successful(Left(ServerError("There was a problem")))
          )
        )

        val futureResult = SUT.completeMigration(subscriptionId, urn).value
        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("TaxEnrolmentFailure"))
        }
      }

      "return ServerError(\"OrchestratorToTaxableFailure\") when migrateToTaxable returns an Exception" in {
        val subscriptionsResponse = TaxEnrolmentsSubscriptionsSuccessResponse(subscriptionId, utr, "PROCESSED")

        when(mockTaxEnrolmentConnector.subscriptions(subscriptionId))
          .thenReturn(
            EitherT[Future, TrustErrors, TaxEnrolmentsSubscriptionsSuccessResponse](
              Future.successful(Right(subscriptionsResponse))
            )
          )
        when(mockOrchestratorConnector.migrateToTaxable(urn, utr))
          .thenReturn(
            EitherT[Future, TrustErrors, OrchestratorToTaxableSuccessResponse](
              Future.successful(
                Left(ServerError("There was a problem"))
              )
            )
          )

        val futureResult = SUT.completeMigration(subscriptionId, urn).value
        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("OrchestratorToTaxableFailure"))
        }

        verify(mockAuditService).auditOrchestratorFailure(eqTo(urn), eqTo(utr), eqTo("There was a problem"))(
          any[HeaderCarrier]
        )
      }

      "return ServerError()" when {
        "tax enrolment connector returns an error for subscriptions, where message is an empty string" in {
          when(mockTaxEnrolmentConnector.subscriptions(subscriptionId))
            .thenReturn(
              EitherT[Future, TrustErrors, TaxEnrolmentsSubscriptionsSuccessResponse](
                Future.successful(Left(ServerError()))
              )
            )
          when(mockOrchestratorConnector.migrateToTaxable(urn, utr))
            .thenReturn(
              EitherT[Future, TrustErrors, OrchestratorToTaxableSuccessResponse](
                Future.successful(Right(OrchestratorToTaxableSuccessResponse()))
              )
            )

          val futureResult = SUT.completeMigration(subscriptionId, urn).value
          whenReady(futureResult) { result =>
            result mustBe Left(ServerError())
          }
        }

        "orchestrator connector returns an error for migrateToTaxable, where message is an empty string" in {
          val subscriptionsResponse = TaxEnrolmentsSubscriptionsSuccessResponse(subscriptionId, utr, "PROCESSED")

          when(mockTaxEnrolmentConnector.subscriptions(subscriptionId))
            .thenReturn(
              EitherT[Future, TrustErrors, TaxEnrolmentsSubscriptionsSuccessResponse](
                Future.successful(Right(subscriptionsResponse))
              )
            )
          when(mockOrchestratorConnector.migrateToTaxable(urn, utr))
            .thenReturn(
              EitherT[Future, TrustErrors, OrchestratorToTaxableSuccessResponse](Future.successful(Left(ServerError())))
            )

          val futureResult = SUT.completeMigration(subscriptionId, urn).value
          whenReady(futureResult) { result =>
            result mustBe Left(ServerError())
          }
        }
      }
    }

    ".migratingFromNonTaxableToTaxable" should {

      "return Some(true)" when {
        "taxable migration flag is set to true" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]

          val SUT = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, Option[Boolean]](Future.successful(Right(Some(true)))))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id", sessionId).value

          whenReady(result) { r =>
            r mustBe Right(true)
          }
        }
      }

      "return Some(false)" when {

        "taxable migration flag is set to false" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, Option[Boolean]](Future.successful(Right(Some(false)))))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id", sessionId).value

          whenReady(result) { r =>
            r mustBe Right(false)
          }
        }

        "taxable migration flag is undefined" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, Option[Boolean]](Future.successful(Right(None))))

          val result = SUT.migratingFromNonTaxableToTaxable(urn, "id", sessionId).value

          whenReady(result) { r =>
            r mustBe Right(false)
          }
        }
      }

      "return Left(trustErrors)" when {
        "fails to get the taxable migration flag from the repository" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, Option[Boolean]](Future.successful(Left(ServerError()))))

          val futureResult = SUT.migratingFromNonTaxableToTaxable(urn, "id", sessionId).value

          whenReady(futureResult) { result =>
            result mustBe Left(ServerError())
          }
        }
      }
    }

    ".getTaxableMigrationFlag" should {

      "return Right(Some(true))" when {
        "the taxable migration flag is set to true" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, Option[Boolean]](Future.successful(Right(Some(true)))))

          val result = SUT.getTaxableMigrationFlag(urn, "id", sessionId).value

          whenReady(result) { r =>
            r mustBe Right(Some(true))
            verify(mockTaxableMigrationRepository).get(urn, "id", sessionId)
          }
        }
      }

      "return Right(Some(false))" when {
        "the taxable migration flag is set to false" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, Option[Boolean]](Future.successful(Right(Some(false)))))

          val result = SUT.getTaxableMigrationFlag(urn, "id", sessionId).value

          whenReady(result) { r =>
            r mustBe Right(Some(false))
            verify(mockTaxableMigrationRepository).get(urn, "id", sessionId)
          }
        }
      }

      "return Right(None)" when {
        "the taxable migration flag is undefined" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, Option[Boolean]](Future.successful(Right(None))))

          val result = SUT.getTaxableMigrationFlag(urn, "id", sessionId).value

          whenReady(result) { r =>
            r mustBe Right(None)
            verify(mockTaxableMigrationRepository).get(urn, "id", sessionId)
          }
        }
      }

      "return Left(ServerError())" when {
        "fails to get the taxable migration flag from the repository - message is nonEmpty" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(
              EitherT[Future, TrustErrors, Option[Boolean]](
                Future.successful(Left(ServerError("exception from Mongo")))
              )
            )

          val futureResult = SUT.migratingFromNonTaxableToTaxable(urn, "id", sessionId).value

          whenReady(futureResult) { result =>
            result mustBe Left(ServerError())
          }
        }

        "fails to get the taxable migration flag from the repository - message is empty" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.get(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, Option[Boolean]](Future.successful(Left(ServerError()))))

          val futureResult = SUT.migratingFromNonTaxableToTaxable(urn, "id", sessionId).value

          whenReady(futureResult) { result =>
            result mustBe Left(ServerError())
          }
        }
      }
    }

    ".setTaxableMigrationFlag" should {
      "call repository set method" in {
        val auditService                   = injector.instanceOf[MigrationAuditService]
        val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
        val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
        val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
        val SUT                            = new TaxableMigrationService(
          auditService,
          taxEnrolmentConnector,
          orchestratorConnector,
          mockTaxableMigrationRepository
        )

        when(mockTaxableMigrationRepository.set(any(), any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val result = SUT.setTaxableMigrationFlag(urn, "id", sessionId, migratingToTaxable = true).value

        whenReady(result) { r =>
          r mustBe Right(true)
          verify(mockTaxableMigrationRepository).set(urn, "id", sessionId, migratingToTaxable = true)
        }
      }

      "return error" when {
        "fails to set the taxable migration flag in the repository - message is nonEmpty" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.set(any(), any(), any(), any()))
            .thenReturn(
              EitherT[Future, TrustErrors, Boolean](
                Future.successful(
                  Left(ServerError("operation failed due to exception from Mongo"))
                )
              )
            )

          val result = SUT.setTaxableMigrationFlag(urn, "id", sessionId, migratingToTaxable = true).value

          whenReady(result) { r =>
            r mustBe Left(ServerError())
          }
        }

        "fails to set the taxable migration flag in the repository - message is empty" in {
          val auditService                   = injector.instanceOf[MigrationAuditService]
          val taxEnrolmentConnector          = injector.instanceOf[TaxEnrolmentConnector]
          val orchestratorConnector          = injector.instanceOf[OrchestratorConnector]
          val mockTaxableMigrationRepository = mock[TaxableMigrationRepository]
          val SUT                            = new TaxableMigrationService(
            auditService,
            taxEnrolmentConnector,
            orchestratorConnector,
            mockTaxableMigrationRepository
          )

          when(mockTaxableMigrationRepository.set(any(), any(), any(), any()))
            .thenReturn(
              EitherT[Future, TrustErrors, Boolean](
                Future.successful(
                  Left(ServerError())
                )
              )
            )

          val result = SUT.setTaxableMigrationFlag(urn, "id", sessionId, migratingToTaxable = true).value

          whenReady(result) { r =>
            r mustBe Left(ServerError())
          }
        }
      }
    }
  }

}
