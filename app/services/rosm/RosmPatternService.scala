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

import models.tax_enrolments.{TaxEnrolmentFailure, TaxEnrolmentNotProcessed, TaxEnrolmentSubscriberResponse, TaxEnrolmentSuccess}
import play.api.Logging
import services.TrustsService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RosmPatternServiceImpl @Inject()(trustsService: TrustsService, taxEnrolmentService: TaxEnrolmentsService)(implicit ec: ExecutionContext) extends RosmPatternService with Logging {

  override def setSubscriptionId(trn: String, taxable: Boolean)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {

    for {
      subscriptionIdResponse <- trustsService.getSubscriptionId(trn = trn)
      taxEnrolmentSubscriberResponse <- taxEnrolmentService.setSubscriptionId(subscriptionIdResponse.subscriptionId, taxable, trn)
    } yield {
      taxEnrolmentSubscriberResponse
    }
  }

  override def enrolAndLogResult(trn: String, affinityGroup: AffinityGroup, taxable: Boolean)
                                (implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    affinityGroup match {
      case AffinityGroup.Organisation =>
        setSubscriptionId(trn, taxable) map {
          case TaxEnrolmentSuccess =>
            logger.info(s"[RosmPatternServiceImpl][enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
              s" Rosm completed successfully for provided trn: $trn.")
            TaxEnrolmentSuccess
          case _ =>
            logger.error(s"[RosmPatternServiceImpl][enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
              s"Rosm pattern is not completed for trn: $trn.")
            TaxEnrolmentFailure
        } recover {
          case NonFatal(exception) =>
            logger.error(s"[RosmPatternServiceImpl][enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
              s"Rosm pattern is not completed for trn: $trn. Exception recieved: exception")
            TaxEnrolmentFailure
        }
      case _ =>
        logger.info(s"[RosmPatternServiceImpl][enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
          "Tax enrolments is not required for Agent.")
        Future.successful(TaxEnrolmentNotProcessed)
    }
  }

}

trait RosmPatternService {
  def setSubscriptionId(trn: String, taxable: Boolean)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse]
  def enrolAndLogResult(trn: String, affinityGroup: AffinityGroup, taxable: Boolean)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse]
}
