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

package connector

import com.google.inject.ImplementedBy
import config.AppConfig
import javax.inject.Inject
import models.tax_enrolments.{TaxEnrolmentSubscriberResponse, TaxEnrolmentSubscription, TaxEnrolmentsSubscriptionsResponse}
import play.api.Logging
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Constants._
import utils.Session

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentConnectorImpl @Inject()(http: HttpClient,
                                          config: AppConfig
                                         ) extends TaxEnrolmentConnector with Logging {

  private def headers = Seq(CONTENT_TYPE -> CONTENT_TYPE_JSON)

  override def getTaxEnrolmentSubscription(subscriptionId: String, taxable: Boolean, trn: String): TaxEnrolmentSubscription = {
    if (taxable) {
      TaxEnrolmentSubscription(
        serviceName = config.taxEnrolmentsPayloadBodyServiceNameTaxable,
        callback = config.taxEnrolmentsPayloadBodyCallbackTaxable(trn),
        etmpId = subscriptionId)
    } else {
      TaxEnrolmentSubscription(
        serviceName = config.taxEnrolmentsPayloadBodyServiceNameNonTaxable,
        callback = config.taxEnrolmentsPayloadBodyCallbackNonTaxable(trn),
        etmpId = subscriptionId)
    }
  }

  override def getTaxEnrolmentMigration(subscriptionId: String, urn: String): TaxEnrolmentSubscription = {
    TaxEnrolmentSubscription(
      serviceName = config.taxEnrolmentsMigrationPayloadServiceName,
      callback = config.taxEnrolmentsMigrationPayloadBodyCallback(subscriptionId, urn),
      etmpId = subscriptionId)

  }

  def getResponse(taxEnrolmentsEndpoint: String,
                  taxEnrolmentSubscriptionRequest: TaxEnrolmentSubscription)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {

    val taxEnrolmentHeaders = hc.withExtraHeaders(headers: _*)

    http.PUT[JsValue, TaxEnrolmentSubscriberResponse](
      taxEnrolmentsEndpoint,
      Json.toJson(taxEnrolmentSubscriptionRequest)
    )(Writes.JsValueWrites, TaxEnrolmentSubscriberResponse.httpReads, taxEnrolmentHeaders, global)
  }


  override def enrolSubscriber(subscriptionId: String, taxable: Boolean, trn: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    val taxEnrolmentsEndpoint = s"${config.taxEnrolmentsUrl}/tax-enrolments/subscriptions/$subscriptionId/subscriber"
    val taxEnrolmentSubscriptionRequest: TaxEnrolmentSubscription = getTaxEnrolmentSubscription(subscriptionId, taxable, trn)
    getResponse(taxEnrolmentsEndpoint, taxEnrolmentSubscriptionRequest)
  }

  override def migrateSubscriberToTaxable(subscriptionId: String, urn: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    val taxEnrolmentsEndpoint = s"${config.taxEnrolmentsMigrationUrl}/tax-enrolments/subscriptions/$subscriptionId/subscriber"
    val taxEnrolmentSubscriptionRequest: TaxEnrolmentSubscription = getTaxEnrolmentMigration(subscriptionId, urn)
    getResponse(taxEnrolmentsEndpoint, taxEnrolmentSubscriptionRequest)
  }

  override def subscriptions(subscriptionId: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentsSubscriptionsResponse] = {
    logger.info(s"[TaxEnrolmentConnectorImpl][Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId] subscriptions")
    val getSubscriptionsEndpoint = s"${config.taxEnrolmentsMigrationUrl}/tax-enrolments/subscriptions/$subscriptionId"
    http.GET[TaxEnrolmentsSubscriptionsResponse](
      getSubscriptionsEndpoint
    )(TaxEnrolmentsSubscriptionsResponse.httpReads(subscriptionId), implicitly[HeaderCarrier](hc), global)
  }
}

@ImplementedBy(classOf[TaxEnrolmentConnectorImpl])
trait TaxEnrolmentConnector {

  def getTaxEnrolmentSubscription(subscriptionId: String, taxable: Boolean, trn: String): TaxEnrolmentSubscription
  def getTaxEnrolmentMigration(subscriptionId: String,  urn: String): TaxEnrolmentSubscription
  def enrolSubscriber(subscriptionId: String, taxable: Boolean, trn: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse]
  def migrateSubscriberToTaxable(subscriptionId: String, urn: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse]
  def subscriptions(subscriptionId: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentsSubscriptionsResponse]

}
