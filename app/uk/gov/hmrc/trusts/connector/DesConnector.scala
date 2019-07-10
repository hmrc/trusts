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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.config.{AppConfig, WSHttp}
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.utils.Constants._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.GetEstateResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustResponse
import uk.gov.hmrc.trusts.models.variation.{Variation, VariationResponse}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}


class DesConnectorImpl @Inject()(http: WSHttp, config: AppConfig) extends DesConnector {

  lazy val trustsServiceUrl : String = s"${config.desTrustsUrl}/trusts"
  lazy val estatesServiceUrl : String = s"${config.desEstatesUrl}/estates"
  lazy val getTrustOrEstateUrl: String =  s"${config.getTrustOrEstateUrl}/trusts"

  lazy val matchTrustsEndpoint : String = s"$trustsServiceUrl/match"
  lazy val matchEstatesEndpoint : String = s"$estatesServiceUrl/match"

  lazy val trustRegistrationEndpoint : String = s"$trustsServiceUrl/registration"
  lazy val estateRegistrationEndpoint : String = s"$estatesServiceUrl/registration"

  lazy val trustVariationsEndpoint : String = s"$trustsServiceUrl/variations"

  def createGetTrustOrEsateEndpoint(utr: String): String = s"$getTrustOrEstateUrl/registration/$utr"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "CorrelationId"
  val OLD_CORRELATION_HEADER = "Correlation-Id"

  def correlationId = UUID.randomUUID().toString

  private def desHeaders : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.desToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.desEnvironment,
      CORRELATION_HEADER -> correlationId,
      OLD_CORRELATION_HEADER -> correlationId
    )

  override def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest)
                                 : Future[ExistingCheckResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val response = http.POST[JsValue, ExistingCheckResponse](matchTrustsEndpoint, Json.toJson(existingTrustCheckRequest))
    (implicitly[Writes[JsValue]], ExistingCheckResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])

    response
  }

  override def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest)
  : Future[ExistingCheckResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val response = http.POST[JsValue, ExistingCheckResponse](matchEstatesEndpoint, Json.toJson(existingEstateCheckRequest))
    (implicitly[Writes[JsValue]], ExistingCheckResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])

    response
  }

  override def registerTrust(registration: Registration)
                            : Future[RegistrationResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val response = http.POST[JsValue, RegistrationResponse](trustRegistrationEndpoint, Json.toJson(registration))
    (implicitly[Writes[JsValue]], RegistrationResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])

    response
  }

  override def registerEstate(registration: EstateRegistration): Future[RegistrationResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)
    val response = http.POST[JsValue, RegistrationResponse](estateRegistrationEndpoint, Json.toJson(registration))
    (implicitly[Writes[JsValue]], RegistrationResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])
    response
  }

  override def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val subscriptionIdEndpointUrl = s"$trustsServiceUrl/trn/$trn/subscription"
    Logger.debug(s"[getSubscriptionId] Sending get subscription id request to DES, url=$subscriptionIdEndpointUrl")

    val response = http.GET[SubscriptionIdResponse](subscriptionIdEndpointUrl)
    (SubscriptionIdResponse.httpReads, implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])

    response
  }

  override def getTrustInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetTrustResponse] = {
    val updatedHeaderCarrier = hc.copy(extraHeaders = desHeaders)

    http.GET[GetTrustResponse](createGetTrustOrEsateEndpoint(utr))(GetTrustResponse.httpReads, updatedHeaderCarrier, global)
  }

  override def getEstateInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    val updatedHeaderCarrier = hc.copy(extraHeaders = desHeaders)

    http.GET[GetEstateResponse](createGetTrustOrEsateEndpoint(utr))(GetEstateResponse.httpReads, updatedHeaderCarrier, global)
  }

  override def variations(variation: Variation)(implicit hc: HeaderCarrier): Future[VariationResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val response = http.POST[JsValue, VariationResponse](trustVariationsEndpoint, Json.toJson(variation))
    (implicitly[Writes[JsValue]], VariationResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])

    response
  }


}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {
  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse]
  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse]

  def registerTrust(registration: Registration): Future[RegistrationResponse]
  def registerEstate(registration: EstateRegistration): Future[RegistrationResponse]
  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse]

  def getTrustInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetTrustResponse]
  def getEstateInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse]

  def variations(variation: Variation)(implicit hc: HeaderCarrier): Future[VariationResponse]
}
