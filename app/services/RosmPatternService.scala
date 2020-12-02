/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Logging
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import models.tax_enrolments.{TaxEnrolmentFailure, TaxEnrolmentNotProcessed, TaxEnrolmentSuccess, TaxEnrolmentSuscriberResponse}
import utils.Session

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal


class RosmPatternServiceImpl @Inject()(desService :TrustService, taxEnrolmentService : TaxEnrolmentsService) extends RosmPatternService with Logging {

  override def setSubscriptionId(trn : String, taxable: Boolean)(implicit hc : HeaderCarrier): Future[TaxEnrolmentSuscriberResponse] ={

    for {
      subscriptionIdResponse <- desService.getSubscriptionId(trn = trn)
      taxEnrolmentSuscriberResponse <- taxEnrolmentService.setSubscriptionId(subscriptionIdResponse.subscriptionId, taxable, trn)
    } yield {
      taxEnrolmentSuscriberResponse
    }
  }

  override def enrolAndLogResult(trn: String, affinityGroup: AffinityGroup, taxable: Boolean)
                                                    (implicit hc: HeaderCarrier) : Future[TaxEnrolmentSuscriberResponse] = {
    affinityGroup match {
      case AffinityGroup.Organisation =>
        setSubscriptionId(trn, taxable) map {
          case TaxEnrolmentSuccess =>
            logger.info(s"[enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
              s" Rosm completed successfully for provided trn : $trn.")
            TaxEnrolmentSuccess
          case TaxEnrolmentFailure =>
            logger.error(s"[enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
              s"Rosm pattern is not completed for trn:  $trn.")
            TaxEnrolmentFailure
        } recover {
          case NonFatal(exception) =>
            logger.error(s"[enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
              s"Rosm pattern is not completed for trn:  $trn.")
            logger.error(s"[enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
              s"Rosm Exception received : $exception.")
            TaxEnrolmentFailure
          }
      case _ =>
        logger.info(s"[enrolAndLogResult][Session ID: ${Session.id(hc)}]" +
          "Tax enrolments is not required for Agent.")
        Future.successful(TaxEnrolmentNotProcessed)
    }
  }

}
@ImplementedBy(classOf[RosmPatternServiceImpl])
trait RosmPatternService {
  def setSubscriptionId(trn : String, taxable: Boolean)(implicit hc : HeaderCarrier): Future[TaxEnrolmentSuscriberResponse]
  def enrolAndLogResult(trn: String, affinityGroup: AffinityGroup, taxable: Boolean)(implicit hc: HeaderCarrier) : Future[TaxEnrolmentSuscriberResponse]
}
