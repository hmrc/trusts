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

import java.util.UUID

import com.google.inject.ImplementedBy
import javax.inject.Inject

import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.trusts.config.{AppConfig, WSHttp}
import uk.gov.hmrc.trusts.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.trusts.utils.Constants._


class DesConnectorImpl @Inject()(http: WSHttp, config: AppConfig) extends DesConnector {

  lazy val trustsServiceUrl : String = s"${config.desUrl}/trusts"
  lazy val matchEndpoint : String = trustsServiceUrl + "/match"
  lazy val registrationEndpoint : String = trustsServiceUrl + "/registration"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "Correlation-Id"


  override def checkExistingTrust(existingTrustCheckRequest: ExistingTrustCheckRequest)
                                 (implicit hc: HeaderCarrier): Future[ExistingTrustResponse] = {
    val desHeaders = hc.copy(authorization = Some(Authorization(s"Bearer ${config.desToken}"))).withExtraHeaders(headers: _*)

    http.POST[JsValue, ExistingTrustResponse](matchEndpoint, Json.toJson(existingTrustCheckRequest), desHeaders.headers)
  }

  def headers : Seq[(String, String)] =
    Seq(
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.desEnvironment,
      CORRELATION_HEADER -> UUID.randomUUID().toString
    )

  override def registerTrust(registration: Registration)
                            (implicit hc: HeaderCarrier): Future[RegistrationResponse] = {
    val desHeaders = hc.copy(authorization = Some(Authorization(s"Bearer ${config.desToken}"))).withExtraHeaders(headers: _*)

    http.POST[JsValue, RegistrationResponse](registrationEndpoint, Json.toJson(registration), desHeaders.headers)


  }

  override def getSubscriptionId(trn: String)(implicit hc: HeaderCarrier): Future[SubscriptionIdResponse] = {
    val desHeaders = hc.copy(authorization = Some(Authorization(s"Bearer ${config.desToken}"))).withExtraHeaders(headers: _*)

    val subscriptionIdEndpointUrl = s"${trustsServiceUrl}/trn/$trn/subscription"
    Logger.debug(s"[getSubscriptionId] Sending get subscription id request to DES, url=$subscriptionIdEndpointUrl")

    http.GET[SubscriptionIdResponse](subscriptionIdEndpointUrl)(SubscriptionIdResponse.httpReads, hc = desHeaders, ec = global)
  }
}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {
  def checkExistingTrust(existingTrustCheckRequest: ExistingTrustCheckRequest)(implicit hc: HeaderCarrier): Future[ExistingTrustResponse]
  def registerTrust(registration: Registration)(implicit hc: HeaderCarrier): Future[RegistrationResponse]
  def getSubscriptionId(trn: String)(implicit hc: HeaderCarrier): Future[SubscriptionIdResponse]

}