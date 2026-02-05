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

package services.rosm

import base.BaseSpec
import cats.data.EitherT
import errors.{ServerError, TrustErrors}
import models.tax_enrolments.{
  SubscriptionIdSuccessResponse, TaxEnrolmentFailure, TaxEnrolmentNotProcessed, TaxEnrolmentSuccess
}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers._
import services.TrustsService
import uk.gov.hmrc.auth.core.AffinityGroup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RosmPatternServiceSpec extends BaseSpec {

  private val taxable: Boolean         = false
  private val trn                      = "XTRN1234567"
  private val mockTrustsService        = mock[TrustsService]
  private val mockTaxEnrolmentsService = mock[TaxEnrolmentsService]

  private val SUT = new RosmPatternServiceImpl(mockTrustsService, mockTaxEnrolmentsService)

  ".setSubscriptionId" should {

    "return TaxEnrolmentSuccess" when {
      "successfully sets subscriptionId id in tax enrolments for provided trn." in {
        when(mockTrustsService.getSubscriptionId(trn))
          .thenReturn(
            EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
              Future.successful(Right(SubscriptionIdSuccessResponse("123456789")))
            )
          )
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
          .thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.setSubscriptionId(trn, taxable).value

        whenReady(futureResult) { result =>
          result mustBe Right(TaxEnrolmentSuccess)
        }
      }
    }
    "return TaxEnrolmentFailure " when {
      "tax enrolment service does not find provided subscription id." in {
        when(mockTrustsService.getSubscriptionId(trn))
          .thenReturn(
            EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
              Future.successful(Right(SubscriptionIdSuccessResponse("123456789")))
            )
          )
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
          .thenReturn(Future.successful(TaxEnrolmentFailure))

        val futureResult = SUT.setSubscriptionId(trn, taxable).value

        whenReady(futureResult) { result =>
          result mustBe Right(TaxEnrolmentFailure)
        }
      }
    }

    "return Left(ServerError(message))" when {
      "message is nonEmpty - des is down and not able to return subscription id." in {
        when(mockTrustsService.getSubscriptionId(trn))
          .thenReturn(
            EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
              Future.successful(Left(ServerError("des is down")))
            )
          )
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
          .thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.setSubscriptionId(trn, taxable).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("des is down"))
        }
      }

      "message is an empty string" in {
        when(mockTrustsService.getSubscriptionId(trn))
          .thenReturn(
            EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](Future.successful(Left(ServerError())))
          )
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
          .thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.setSubscriptionId(trn, taxable).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }
      }
    }

  }

  ".enrolAndLogResult" should {

    val affinityGroup = AffinityGroup.Organisation

    "return TaxEnrolmentSuccess when Rosm is completed successfully" in {
      when(mockTrustsService.getSubscriptionId(trn))
        .thenReturn(
          EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
            Future.successful(Right(SubscriptionIdSuccessResponse("123456789")))
          )
        )
      when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
        .thenReturn(Future.successful(TaxEnrolmentSuccess))

      val futureResult = SUT.enrolAndLogResult(trn, affinityGroup, taxable).value

      whenReady(futureResult) { result =>
        result mustBe Right(TaxEnrolmentSuccess)
      }
    }

    "return TaxEnrolmentNotProcessed when user is an Agent" in {
      val affinityGroup = AffinityGroup.Agent

      when(mockTrustsService.getSubscriptionId(trn))
        .thenReturn(
          EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
            Future.successful(Right(SubscriptionIdSuccessResponse("123456789")))
          )
        )
      when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
        .thenReturn(Future.successful(TaxEnrolmentSuccess))

      val futureResult = SUT.enrolAndLogResult(trn, affinityGroup, taxable).value

      whenReady(futureResult) { result =>
        result mustBe Right(TaxEnrolmentNotProcessed)
      }
    }

    "return TaxEnrolmentFailure" when {
      "setSubscriptionId returns a SubscriptionIdSuccessResponse that isn't 'TaxEnrolmentSuccess'" in {
        when(mockTrustsService.getSubscriptionId(trn))
          .thenReturn(
            EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
              Future.successful(Right(SubscriptionIdSuccessResponse("123456789")))
            )
          )
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
          .thenReturn(Future.successful(TaxEnrolmentFailure))

        val futureResult = SUT.enrolAndLogResult(trn, affinityGroup, taxable).value

        whenReady(futureResult) { result =>
          result mustBe Right(TaxEnrolmentFailure)
        }
      }

      "setSubscriptionId returns an exception is returned (recover a non fatal exception)" in {
        when(mockTrustsService.getSubscriptionId(trn))
          .thenReturn(
            EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
              Future.successful(Right(SubscriptionIdSuccessResponse("123456789")))
            )
          )
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
          .thenReturn(Future.failed(new Exception("non fatal exception with message")))

        val futureResult = SUT.enrolAndLogResult(trn, affinityGroup, taxable).value

        whenReady(futureResult) { result =>
          result mustBe Right(TaxEnrolmentFailure)
        }
      }
    }

    "return a TrustErrors when setSubscriptionId returns a TrustErrors" in {
      when(mockTrustsService.getSubscriptionId(trn))
        .thenReturn(EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](Future.successful(Left(ServerError()))))
      when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn))
        .thenReturn(Future.successful(TaxEnrolmentSuccess))

      val futureResult = SUT.enrolAndLogResult(trn, affinityGroup, taxable).value

      whenReady(futureResult) { result =>
        result mustBe Left(ServerError())
      }
    }
  }

}
