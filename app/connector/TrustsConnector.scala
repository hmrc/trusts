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

import java.util.UUID

import config.AppConfig
import javax.inject.Inject
import models._
import models.existing_trust.{ExistingCheckRequest, ExistingCheckResponse}
import models.get_trust.GetTrustResponse
import models.registration.{RegistrationResponse, RegistrationTrnResponse}
import models.variation.VariationResponse
import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json._
import services.TrustsStoreService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Constants._
import utils.Session

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class TrustsConnector @Inject()(http: HttpClient, config: AppConfig, trustsStoreService: TrustsStoreService) extends Logging {

  private lazy val trustsServiceUrl : String = s"${config.registrationBaseUrl}/trusts"
  private lazy val matchTrustsEndpoint : String = s"$trustsServiceUrl/match"
  private lazy val trustRegistrationEndpoint : String = s"$trustsServiceUrl/registration"
  private lazy val getTrustOrEstateUrl: String =  s"${config.getTrustOrEstateUrl}/trusts"

  def get4MLDTrustOrEstateEndpoint(utr: String): String = s"$getTrustOrEstateUrl/registration/$utr"

  def get5MLDTrustOrEstateEndpoint(identifier: String): String = {
    if (identifier.length == 10) {
      s"$getTrustOrEstateUrl/registration/UTR/$identifier"
    } else {
      s"$getTrustOrEstateUrl/registration/URN/$identifier"
    }
  }

  private lazy val trustVariationsEndpoint : String = s"${config.varyTrustOrEstateUrl}/trusts/variation"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "CorrelationId"

  private def registrationHeaders(correlationId : String) : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.registrationToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.registrationEnvironment,
      CORRELATION_HEADER -> correlationId
    )

  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest)
                                 : Future[ExistingCheckResponse] = {

    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[Session ID: ${Session.id(hc)}] matching trust for correlationId: $correlationId")

    val response = http.POST[JsValue, ExistingCheckResponse](matchTrustsEndpoint, Json.toJson(existingTrustCheckRequest))
    (implicitly[Writes[JsValue]], ExistingCheckResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])

    response
  }

  def registerTrust(registration: Registration)
                            : Future[RegistrationTrnResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    val reads = RegistrationResponse.httpReads

    logger.info(s"[Session ID: ${Session.id(hc)}] registering trust for correlationId: $correlationId")

    val response = http.POST[JsValue, RegistrationTrnResponse](
      trustRegistrationEndpoint,
      Json.toJson(registration)
    )(implicitly[Writes[JsValue]], reads, implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])

    response
  }

  def getTrustInfo(identifier: String): Future[GetTrustResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc : HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[Session ID: ${Session.id(hc)}][UTR/URN: $identifier]" +
      s" getting playback for trust for correlationId: $correlationId")

    trustsStoreService.is5mldEnabled.flatMap { is5MLD =>
      if (is5MLD) {
        http.GET[GetTrustResponse](get5MLDTrustOrEstateEndpoint(identifier))(GetTrustResponse.httpReads(identifier), implicitly[HeaderCarrier](hc), global)
      } else {
        http.GET[GetTrustResponse](get4MLDTrustOrEstateEndpoint(identifier))(GetTrustResponse.httpReads(identifier), implicitly[HeaderCarrier](hc), global)
      }
    }
  }

  def trustVariation(trustVariations: JsValue): Future[VariationResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[Session ID: ${Session.id(hc)}]" +
      s" submitting trust variation for correlationId: $correlationId")

    http.POST[JsValue, VariationResponse](trustVariationsEndpoint, Json.toJson(trustVariations))(
      implicitly[Writes[JsValue]],
      VariationResponse.httpReads,
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    )
  }
}
