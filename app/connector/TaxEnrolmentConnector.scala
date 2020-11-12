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

package connector

import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import config.AppConfig
import exceptions.InternalServerErrorException
import models.tax_enrolments.{TaxEnrolmentSubscription, TaxEnrolmentSuscriberResponse}
import services.TrustsStoreService
import play.api.Logging
import utils.Constants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentConnectorImpl @Inject()(http: HttpClient,
                                          config: AppConfig,
                                          trustsStoreService: TrustsStoreService
                                         ) extends TaxEnrolmentConnector with Logging {

  def headers =
    Seq(
      CONTENT_TYPE -> CONTENT_TYPE_JSON
    )

  def is5MLD()(implicit hc: HeaderCarrier): Future[Boolean] = trustsStoreService.is5mldEnabled().recoverWith {
    case e =>
      logger.error(s"TaxEnrolmentConnector is5MLD Exception ${e.getMessage} ${e.getCause}")
      Future.failed(InternalServerErrorException(s"TaxEnrolmentConnector is5MLD Exception ${e.getMessage}"))
  }

  override def getTaxEnrolmentSubscription(subscriptionId: String, is5MLD: Boolean, taxable: Boolean, trn: String): TaxEnrolmentSubscription = {
    val taxEnrolmentSubscriptionRequest = (is5MLD, taxable) match {
      case (true, false) => {
        TaxEnrolmentSubscription(
          serviceName = config.taxEnrolmentsPayloadBodyServiceNameNonTaxable,
          callback = config.taxEnrolmentsPayloadBodyCallbackNonTaxable(trn),
          etmpId = subscriptionId)
      }
      case (_, _) => {
        TaxEnrolmentSubscription(
          serviceName = config.taxEnrolmentsPayloadBodyServiceNameTaxable,
          callback = config.taxEnrolmentsPayloadBodyCallbackTaxable(trn),
          etmpId = subscriptionId)
      }
    }
    taxEnrolmentSubscriptionRequest
  }

  override def getResponse(subscriptionId: String,
                  is5MLD: Boolean,
                  taxable: Boolean,
                  trn: String)(implicit hc: HeaderCarrier) :  Future[TaxEnrolmentSuscriberResponse] = {
    val taxEnrolmentsEndpoint = s"${config.taxEnrolmentsUrl}/tax-enrolments/subscriptions/$subscriptionId/subscriber"
    val taxEnrolmentHeaders = hc.withExtraHeaders(headers: _*)

    val taxEnrolmentSubscriptionRequest: TaxEnrolmentSubscription = getTaxEnrolmentSubscription(subscriptionId, is5MLD, taxable, trn)

    val response = http.PUT[JsValue, TaxEnrolmentSuscriberResponse](taxEnrolmentsEndpoint, Json.toJson(taxEnrolmentSubscriptionRequest))
    (Writes.JsValueWrites ,TaxEnrolmentSuscriberResponse.httpReads,taxEnrolmentHeaders.headers, global)
    response
  }


  override  def enrolSubscriber(subscriptionId: String, taxable: Boolean, trn: String)(implicit hc: HeaderCarrier) :  Future[TaxEnrolmentSuscriberResponse] = {
    for {
      is5MLD <- is5MLD
      response <- getResponse(subscriptionId, is5MLD, taxable, trn)
    } yield
    response
  }

}

@ImplementedBy(classOf[TaxEnrolmentConnectorImpl])
trait TaxEnrolmentConnector {
  def getTaxEnrolmentSubscription(subscriptionId: String, is5MLD: Boolean, taxable: Boolean, trn: String): TaxEnrolmentSubscription
  def enrolSubscriber(subscriptionId: String, taxable: Boolean, trn: String)(implicit hc: HeaderCarrier):  Future[TaxEnrolmentSuscriberResponse]
  def getResponse(subscriptionId: String, is5MLD: Boolean, taxable: Boolean, trn: String)(implicit hc: HeaderCarrier) : Future[TaxEnrolmentSuscriberResponse]
}
