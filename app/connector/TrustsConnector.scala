/*
 * Copyright 2024 HM Revenue & Customs
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
import errors.VariationFailureForAudit
import models._
import models.existing_trust.{ExistingCheckRequest, ExistingCheckResponse}
import models.get_trust.GetTrustResponse
import models.registration.RegistrationResponse
import models.variation.{VariationFailureResponse, VariationResponse, VariationSuccessResponse}
import play.api.http.HeaderNames
import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier,  StringContextOps}
import utils.Constants._
import utils.Session
import utils.TrustEnvelope.TrustEnvelope

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TrustsConnector @Inject()(http: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext) extends ConnectorErrorResponseHandler {

  val className: String = this.getClass.getSimpleName

  private lazy val trustsServiceUrl: String =
    s"${config.registrationBaseUrl}/trusts"

  private lazy val matchTrustsEndpoint: String =
    s"$trustsServiceUrl/match"

  private lazy val trustRegistrationEndpoint: String =
    s"$trustsServiceUrl/registration"

  private lazy val getTrustOrEstateUrl: String =
    s"${config.getTrustOrEstateUrl}/trusts"

  def get5MLDTrustOrEstateEndpoint(identifier: String): String = {
    if (identifier.length == 10) {
      s"$getTrustOrEstateUrl/registration/UTR/$identifier"
    } else {
      s"$getTrustOrEstateUrl/registration/URN/$identifier"
    }
  }

  private lazy val trustVariationsEndpoint: String =
    s"${config.varyTrustOrEstateUrl}/trusts/variation"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "CorrelationId"

  private def registrationHeaders(correlationId: String): Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.registrationToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.registrationEnvironment,
      CORRELATION_HEADER -> correlationId
    )

  def checkExistingTrust(existingTrustCheckRequest: ExistingCheckRequest): TrustEnvelope[ExistingCheckResponse] = EitherT {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[$className][checkExistingTrust][Session ID: ${Session.id(hc)}] matching trust for correlationId: $correlationId")

    http.post(url"$matchTrustsEndpoint")
      .withBody(Json.toJson(existingTrustCheckRequest))
      .execute[ExistingCheckResponse]
      .map(Right(_)).recover {
      case ex =>
        Left(handleError(ex, "checkExistingTrust", matchTrustsEndpoint))
    }
  }

  def registerTrust(registration: Registration): TrustEnvelope[RegistrationResponse] = EitherT {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

//    val reads = RegistrationResponse.httpReads

    logger.info(s"[$className][registerTrust][Session ID: ${Session.id(hc)}] registering trust for correlationId: $correlationId")

    http.post(url"$trustRegistrationEndpoint")
      .withBody(Json.toJson(registration))
      .execute[RegistrationResponse]
    .map(Right(_)).recover {
      case ex =>
        Left(handleError(ex, "registerTrust", trustRegistrationEndpoint))
    }
  }

  def getTrustInfo(identifier: String): TrustEnvelope[GetTrustResponse] = EitherT {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[$className][getTrustInfo][Session ID: ${Session.id(hc)}][UTR/URN: $identifier]" +
      s" getting playback for trust for correlationId: $correlationId")
    val fullUrl = get5MLDTrustOrEstateEndpoint(identifier)
    http.get(url"$fullUrl")
      .execute[GetTrustResponse](GetTrustResponse.httpReads(identifier), ec)
      .map(Right(_)).recover {
      case ex =>
        Left(handleError(ex, "getTrustInfo", get5MLDTrustOrEstateEndpoint(identifier)))
    }
  }

  def trustVariation(trustVariations: JsValue): TrustEnvelope[VariationSuccessResponse] = EitherT {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[$className][trustVariation][Session ID: ${Session.id(hc)}]" +
      s" submitting trust variation for correlationId: $correlationId")

    http.post(url"$trustVariationsEndpoint")
      .withBody(trustVariations)
      .execute[VariationResponse]
      .map {
      case response: VariationSuccessResponse => Right(response)
      case response: VariationFailureResponse =>
        logger.warn(s"[$className][trustVariation][Session ID: ${Session.id(hc)}] " +
          s"trust variation failed with status: ${response.status}, with message: ${response.message}")
        Left(VariationFailureForAudit(response.errorType, response.message))
    }.recover {
      case ex =>
        Left(handleError(ex, "trustVariation", trustVariationsEndpoint))
    }
  }
}
