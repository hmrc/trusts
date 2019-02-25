/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.connector

import javax.inject.Inject

import com.google.inject.ImplementedBy
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.trusts.config.{AppConfig, WSHttp}
import uk.gov.hmrc.trusts.models.{ExistingTrustResponse, TaxEnrolmentSubscription, TaxEnrolmentSuscriberResponse}
import uk.gov.hmrc.trusts.utils.Constants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentConnectorImpl @Inject()(http: WSHttp, config: AppConfig) extends TaxEnrolmentConnector {

  def headers =
    Seq(
      CONTENT_TYPE -> CONTENT_TYPE_JSON
    )

  override  def enrolSubscriber(subscriptionId: String)(implicit hc: HeaderCarrier) :  Future[TaxEnrolmentSuscriberResponse] = {
    val taxEnrolmentsEndpoint = s"${config.taxEnrolmentsUrl}/tax-enrolments/subscriptions/$subscriptionId/subscriber"
    val taxEnolmentHeaders = hc.withExtraHeaders(headers: _*)

    val taxEnrolmentSubscriptionRequest = TaxEnrolmentSubscription(
      serviceName = config.taxEnrolmentsPayloadBodyServiceName,
      callback = config.taxEnrolmentsPayloadBodyCallback,
      etmpId = subscriptionId)

    val response = http.PUT[JsValue, TaxEnrolmentSuscriberResponse](taxEnrolmentsEndpoint, Json.toJson(taxEnrolmentSubscriptionRequest))
    (Writes.JsValueWrites ,TaxEnrolmentSuscriberResponse.httpReads,taxEnolmentHeaders.headers, global)
    response
  }

}

@ImplementedBy(classOf[TaxEnrolmentConnectorImpl])
trait TaxEnrolmentConnector {
  def enrolSubscriber(subscriptionId: String)(implicit hc: HeaderCarrier):  Future[TaxEnrolmentSuscriberResponse]
}
