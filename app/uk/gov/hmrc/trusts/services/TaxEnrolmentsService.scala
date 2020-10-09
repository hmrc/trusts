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

package uk.gov.hmrc.trusts.services

import akka.actor.ActorSystem
import akka.pattern.after
import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.connector.TaxEnrolmentConnector
import uk.gov.hmrc.trusts.models.tax_enrolments.{TaxEnrolmentFailure, TaxEnrolmentSuscriberResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal


class TaxEnrolmentsServiceImpl @Inject()(taxEnrolmentConnector :TaxEnrolmentConnector, config: AppConfig) extends TaxEnrolmentsService with Logging {

  private val DELAY_SECONDS_BETWEEN_REQUEST = config.delayToConnectTaxEnrolment
  private val MAX_TRIES = config.maxRetry

  override def setSubscriptionId(subscriptionId: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSuscriberResponse] = {
    implicit val as: ActorSystem = ActorSystem()
    enrolSubscriberWithRetry(subscriptionId, 1)
  }

  private def enrolSubscriberWithRetry(subscriptionId: String, acc: Int)
                                      (implicit as: ActorSystem, hc: HeaderCarrier): Future[TaxEnrolmentSuscriberResponse] = {
    makeRequest(subscriptionId) recoverWith {
      case NonFatal(_) =>
        if (isMaxRetryReached(acc)) {
          logger.error(s"[enrolSubscriberWithRetry] Maximum retry completed. Tax enrolment failed for subscription id $subscriptionId")
          Future.successful(TaxEnrolmentFailure)
        } else {
          after(DELAY_SECONDS_BETWEEN_REQUEST.seconds, as.scheduler){
            logger.error(s"[enrolSubscriberWithRetry]  Retrying to enrol subscription id $subscriptionId,  $acc")
            enrolSubscriberWithRetry(subscriptionId, acc + 1)
          }
      }
    }
  }

  private def isMaxRetryReached(currentCounter: Int): Boolean =
    currentCounter == MAX_TRIES

  private def makeRequest(subscriptionId: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSuscriberResponse] = {
    taxEnrolmentConnector.enrolSubscriber(subscriptionId)
  }

}

@ImplementedBy(classOf[TaxEnrolmentsServiceImpl])
trait TaxEnrolmentsService{
   def setSubscriptionId(subscriptionId: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSuscriberResponse]
}
