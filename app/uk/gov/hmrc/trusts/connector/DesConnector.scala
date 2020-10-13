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

package uk.gov.hmrc.trusts.connector

import java.util.UUID

import javax.inject.Inject
import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustResponse
import uk.gov.hmrc.trusts.models.variation.VariationResponse
import uk.gov.hmrc.trusts.utils.Constants._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class DesConnector @Inject()(http: HttpClient, config: AppConfig) extends Logging {

  private lazy val trustsServiceUrl : String = s"${config.registerTrustsUrl}/trusts"
  private lazy val matchTrustsEndpoint : String = s"$trustsServiceUrl/match"
  private lazy val trustRegistrationEndpoint : String = s"$trustsServiceUrl/registration"
  private lazy val getTrustOrEstateUrl: String =  s"${config.getTrustOrEstateUrl}/trusts"

  def createGetTrustOrEstateEndpoint(utr: String): String = s"$getTrustOrEstateUrl/registration/$utr"

  private lazy val trustVariationsEndpoint : String = s"${config.varyTrustOrEstateUrl}/trusts/variation"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "CorrelationId"
  val OLD_CORRELATION_HEADER = "Correlation-Id"

  private def desHeaders(correlationId : String) : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.desToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.desEnvironment,
      CORRELATION_HEADER -> correlationId,
      OLD_CORRELATION_HEADER -> correlationId
    )

  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest)
                                 : Future[ExistingCheckResponse] = {

    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))

    logger.info(s"[DesConnector] matching trust for correlationId: $correlationId")

    val response = http.POST[JsValue, ExistingCheckResponse](matchTrustsEndpoint, Json.toJson(existingTrustCheckRequest))
    (implicitly[Writes[JsValue]], ExistingCheckResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])

    response
  }

  def registerTrust(registration: Registration)
                            : Future[RegistrationResponse] = {
    if (config.desEnvironment == "ist0") {
      Future.successful(RegistrationTrnResponse("XXTRN1234567890"))
    } else {

      val correlationId = UUID.randomUUID().toString

      implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))

      logger.warn(s"[DesConnector] registration: $registration")
      logger.info(s"[DesConnector] registering trust for correlationId: $correlationId")

      val response = http.POST[JsValue, RegistrationResponse](trustRegistrationEndpoint, Json.toJson(registration))
      (implicitly[Writes[JsValue]], RegistrationResponse.httpReads, implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])

      response
    }
  }

  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {

    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))

    val subscriptionIdEndpointUrl = s"$trustsServiceUrl/trn/$trn/subscription"
    logger.debug(s"[getSubscriptionId] Sending get subscription id request to DES, url=$subscriptionIdEndpointUrl")

    val response = http.GET[SubscriptionIdResponse](subscriptionIdEndpointUrl)
    (SubscriptionIdResponse.httpReads, implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])

    response
  }

  def getTrustInfo(utr: String): Future[GetTrustResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc : HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))

    logger.info(s"[DesConnector] getting playback for trust for correlationId: $correlationId")

    http.GET[GetTrustResponse](createGetTrustOrEstateEndpoint(utr))(GetTrustResponse.httpReads, implicitly[HeaderCarrier](hc), global)
  }

  def trustVariation(trustVariations: JsValue): Future[VariationResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))

    logger.info(s"[DesConnector] submitting trust variation for correlationId: $correlationId")
    if (config.desEnvironment == "ist0") {
      Future.successful(VariationResponse("XXTVN1234567890"))
    } else {
      http.POST[JsValue, VariationResponse](trustVariationsEndpoint, Json.toJson(trustVariations))(
        implicitly[Writes[JsValue]], VariationResponse.httpReads, implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])
    }

  }
}
