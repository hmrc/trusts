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

import play.api.http.HeaderNames
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

  lazy val trustsServiceUrl : String = s"${config.desTrustsUrl}/trusts"
  lazy val estatesServiceUrl : String = s"${config.desEstatesUrl}/estates"

  lazy val matchTrustsEndpoint : String = trustsServiceUrl + "/match"
  lazy val matchEstatesEndpoint : String = estatesServiceUrl + "/match"

  lazy val trustRegistrationEndpoint : String = trustsServiceUrl + "/registration"
  lazy val estateRegistrationEndpoint : String = estatesServiceUrl + "/registration"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "Correlation-Id"


  private def desHeaders : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.desToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.desEnvironment,
      CORRELATION_HEADER -> UUID.randomUUID().toString
    )


  override def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest)
                                 : Future[ExistingCheckResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val response = http.POST[JsValue, ExistingCheckResponse](matchTrustsEndpoint, Json.toJson(existingTrustCheckRequest))
    (implicitly[Writes[JsValue]], ExistingCheckResponse.httpReads, implicitly[HeaderCarrier](hc),global)

    response
  }

  override def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest)
  : Future[ExistingCheckResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val response = http.POST[JsValue, ExistingCheckResponse](matchEstatesEndpoint, Json.toJson(existingEstateCheckRequest))
    (implicitly[Writes[JsValue]], ExistingCheckResponse.httpReads, implicitly[HeaderCarrier](hc),global)

    response
  }

  override def registerTrust(registration: Registration)
                            : Future[RegistrationResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val response = http.POST[JsValue, RegistrationResponse](trustRegistrationEndpoint, Json.toJson(registration))
    (implicitly[Writes[JsValue]], RegistrationResponse.httpReads, implicitly[HeaderCarrier](hc),global)

    response
  }

  override def registerEstate(registration: EstateRegistration): Future[RegistrationResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)
    val response = http.POST[JsValue, RegistrationResponse](estateRegistrationEndpoint, Json.toJson(registration))
    (implicitly[Writes[JsValue]], RegistrationResponse.httpReads, implicitly[HeaderCarrier](hc),global)
    response
  }

  override def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val subscriptionIdEndpointUrl = s"${trustsServiceUrl}/trn/$trn/subscription"
    Logger.debug(s"[getSubscriptionId] Sending get subscription id request to DES, url=$subscriptionIdEndpointUrl")

    val response = http.GET[SubscriptionIdResponse](subscriptionIdEndpointUrl)
    (SubscriptionIdResponse.httpReads, implicitly[HeaderCarrier](hc),global)

    response
  }
}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {
  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse]
  def checkExistingEstate(existingEsateCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse]

  def registerTrust(registration: Registration): Future[RegistrationResponse]
  def registerEstate(registration: EstateRegistration): Future[RegistrationResponse]
  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse]

}