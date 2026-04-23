/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.data.EitherT
import config.AppConfig
import errors.ServerError
import models.tax_enrolments._
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.Constants._
import utils.TrustEnvelope.TrustEnvelope

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TaxEnrolmentConnectorImpl @Inject() (http: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext)
    extends TaxEnrolmentConnector with ConnectorErrorResponseHandler {

  val className: String = this.getClass.getSimpleName

  private def headers = Seq(CONTENT_TYPE -> CONTENT_TYPE_JSON)

  override def getTaxEnrolmentSubscription(
    subscriptionId: String,
    taxable: Boolean,
    trn: String
  ): TaxEnrolmentSubscription =
    if (taxable) {
      TaxEnrolmentSubscription(
        serviceName = config.taxEnrolmentsPayloadBodyServiceNameTaxable,
        callback = config.taxEnrolmentsPayloadBodyCallbackTaxable(trn),
        etmpId = subscriptionId
      )
    } else {
      TaxEnrolmentSubscription(
        serviceName = config.taxEnrolmentsPayloadBodyServiceNameNonTaxable,
        callback = config.taxEnrolmentsPayloadBodyCallbackNonTaxable(trn),
        etmpId = subscriptionId
      )
    }

  override def getTaxEnrolmentMigration(subscriptionId: String, urn: String): TaxEnrolmentSubscription =
    TaxEnrolmentSubscription(
      serviceName = config.taxEnrolmentsMigrationPayloadServiceName,
      callback = config.taxEnrolmentsMigrationPayloadBodyCallback(subscriptionId, urn),
      etmpId = subscriptionId
    )

  def getResponse(
    taxEnrolmentsEndpoint: String,
    taxEnrolmentSubscriptionRequest: TaxEnrolmentSubscription,
    methodName: String
  )(implicit hc: HeaderCarrier): TrustEnvelope[TaxEnrolmentSubscriberResponse] = EitherT {

    val taxEnrolmentHeaders = hc.withExtraHeaders(headers: _*).extraHeaders
    http
      .put(url"$taxEnrolmentsEndpoint")
      .setHeader(taxEnrolmentHeaders: _*)
      .withBody(Json.toJson(taxEnrolmentSubscriptionRequest))
      .execute[TaxEnrolmentSubscriberResponse]
      .map {
        case TaxEnrolmentFailureResponse(message) => Left(ServerError(message))
        case response                             => Right(response)
      }
      .recover { case ex =>
        Left(handleError(ex, methodName, taxEnrolmentsEndpoint))
      }
  }

  override def enrolSubscriber(subscriptionId: String, taxable: Boolean, trn: String)(implicit
    hc: HeaderCarrier
  ): TrustEnvelope[TaxEnrolmentSubscriberResponse] = {
    val taxEnrolmentsEndpoint                                     = s"${config.taxEnrolmentsUrl}/tax-enrolments/subscriptions/$subscriptionId/subscriber"
    val taxEnrolmentSubscriptionRequest: TaxEnrolmentSubscription =
      getTaxEnrolmentSubscription(subscriptionId, taxable, trn)
    getResponse(taxEnrolmentsEndpoint, taxEnrolmentSubscriptionRequest, "enrolSubscriber")
  }

  override def migrateSubscriberToTaxable(subscriptionId: String, urn: String)(implicit
    hc: HeaderCarrier
  ): TrustEnvelope[TaxEnrolmentSubscriberResponse] = {
    val taxEnrolmentsEndpoint                                     =
      s"${config.taxEnrolmentsMigrationUrl}/tax-enrolments/subscriptions/$subscriptionId/subscriber"
    val taxEnrolmentSubscriptionRequest: TaxEnrolmentSubscription = getTaxEnrolmentMigration(subscriptionId, urn)
    getResponse(taxEnrolmentsEndpoint, taxEnrolmentSubscriptionRequest, "migrateSubscriberToTaxable")
  }

  override def subscriptions(
    subscriptionId: String
  )(implicit hc: HeaderCarrier): TrustEnvelope[TaxEnrolmentsSubscriptionsSuccessResponse] = EitherT {
    val getSubscriptionsEndpoint = s"${config.taxEnrolmentsMigrationUrl}/tax-enrolments/subscriptions/$subscriptionId"
    http
      .get(url"$getSubscriptionsEndpoint")
      .execute[TaxEnrolmentsSubscriptionsResponse](TaxEnrolmentsSubscriptionsResponse.httpReads(subscriptionId), ec)
      .map {
        case response: TaxEnrolmentsSubscriptionsSuccessResponse => Right(response)
        case response: TaxEnrolmentsSubscriptionsFailureResponse => Left(ServerError(response.message))
      }
      .recover { case ex =>
        Left(handleError(ex, "subscriptions", getSubscriptionsEndpoint))
      }
  }

}

trait TaxEnrolmentConnector {

  def getTaxEnrolmentSubscription(subscriptionId: String, taxable: Boolean, trn: String): TaxEnrolmentSubscription
  def getTaxEnrolmentMigration(subscriptionId: String, urn: String): TaxEnrolmentSubscription

  def enrolSubscriber(subscriptionId: String, taxable: Boolean, trn: String)(implicit
    hc: HeaderCarrier
  ): TrustEnvelope[TaxEnrolmentSubscriberResponse]

  def migrateSubscriberToTaxable(subscriptionId: String, urn: String)(implicit
    hc: HeaderCarrier
  ): TrustEnvelope[TaxEnrolmentSubscriberResponse]

  def subscriptions(subscriptionId: String)(implicit
    hc: HeaderCarrier
  ): TrustEnvelope[TaxEnrolmentsSubscriptionsSuccessResponse]

}
