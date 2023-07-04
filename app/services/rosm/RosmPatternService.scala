/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.EitherT
import errors.ServerError
import models.tax_enrolments.{TaxEnrolmentFailure, TaxEnrolmentNotProcessed, TaxEnrolmentSubscriberResponse, TaxEnrolmentSuccess}
import play.api.Logging
import services.TrustsService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.TrustEnvelope.TrustEnvelope
import utils.{Session, TrustEnvelope}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RosmPatternServiceImpl @Inject()(trustsService: TrustsService, taxEnrolmentService: TaxEnrolmentsService)
                                      (implicit ec: ExecutionContext) extends RosmPatternService with Logging {

  private val className = this.getClass.getSimpleName

  override def setSubscriptionId(trn: String, taxable: Boolean)
                                (implicit hc: HeaderCarrier): TrustEnvelope[TaxEnrolmentSubscriberResponse] = EitherT {

    val result = for {
      subscriptionIdSuccessResponse <- trustsService.getSubscriptionId(trn = trn)
      taxEnrolmentSubscriberResponse <- TrustEnvelope.fromFuture(
        taxEnrolmentService.setSubscriptionId(
          subscriptionIdSuccessResponse.subscriptionId, taxable, trn
        )
      )
    } yield {
      taxEnrolmentSubscriberResponse
    }

    result.value.map {
      case Right(response) => Right(response)
      case Left(error: ServerError) if error.message.nonEmpty =>
        logger.warn(s"[$className][setSubscriptionId][Session ID: ${Session.id(hc)}] failed to set subscriptionId - ${error.message}")
        Left(error)
      case Left(_) =>
        logger.warn(s"[$className][setSubscriptionId][Session ID: ${Session.id(hc)}] failed to set subscriptionId.")
        Left(ServerError())
    }
  }

  override def enrolAndLogResult(trn: String, affinityGroup: AffinityGroup, taxable: Boolean)
                                (implicit hc: HeaderCarrier): TrustEnvelope[TaxEnrolmentSubscriberResponse] = EitherT {
    affinityGroup match {
      case AffinityGroup.Organisation =>
        setSubscriptionId(trn, taxable).value.map {
          case Right(TaxEnrolmentSuccess) =>
            logger.info(s"[$className][enrolAndLogResult][Session ID: ${Session.id(hc)}] " +
              s" Rosm completed successfully for provided trn: $trn.")
            Right(TaxEnrolmentSuccess)
          case Right(_) =>
            logger.error(s"[$className][enrolAndLogResult][Session ID: ${Session.id(hc)}] " +
              s"Rosm pattern is not completed for trn: $trn.")
            Right(TaxEnrolmentFailure)
          case Left(trustErrors) => Left(trustErrors)
        } recover {
          case NonFatal(exception) =>
            logger.error(s"[$className][enrolAndLogResult][Session ID: ${Session.id(hc)}] " +
              s"Rosm pattern is not completed for trn: $trn. Exception received: $exception")
            Right(TaxEnrolmentFailure)
        }
      case _ =>
        logger.info(s"[$className][enrolAndLogResult][Session ID: ${Session.id(hc)}] " +
          "Tax enrolments is not required for Agent.")
        Future.successful(Right(TaxEnrolmentNotProcessed))
    }
  }

}

trait RosmPatternService {
  def setSubscriptionId(trn: String, taxable: Boolean)(implicit hc: HeaderCarrier): TrustEnvelope[TaxEnrolmentSubscriberResponse]

  def enrolAndLogResult(trn: String, affinityGroup: AffinityGroup, taxable: Boolean)(implicit hc: HeaderCarrier): TrustEnvelope[TaxEnrolmentSubscriberResponse]
}
