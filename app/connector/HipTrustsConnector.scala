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
import cats.implicits.catsSyntaxEq
import config.AppConfig
import errors.{
  BadRequestErrorResponse, InternalServerErrorResponse => VariationInternalServerErrorResponse,
  ServiceNotAvailableErrorResponse, VariationFailureForAudit
}
import models.Registration
import models.existing_trust.ExistingCheckResponse.{
  AlreadyRegistered, BadRequest, Matched, NotMatched, ServerError, ServiceUnavailable
}
import models.existing_trust.{ExistingCheckRequest, ExistingCheckResponse, HipCustomErrResponse}
import models.get_trust.GetTrustResponse
import models.registration._
import models.variation.{
  HipSuccessVariationTrnResponse, VariationFailureResponse, VariationResponse, VariationSuccessResponse
}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}
import utils.Session
import utils.TrustEnvelope.TrustEnvelope

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class HipTrustsConnector @Inject() (http: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext)
    extends ConnectorErrorResponseHandler with TrustsConnector {

  lazy val trustsServiceUrl: String =
    s"${config.hipRegistrationBaseUrl}/etmp/RESTAdapter/trustsandestates"

  lazy val matchTrustsEndpoint: String =
    s"$trustsServiceUrl/match"

  lazy val trustRegistrationEndpoint: String =
    s"$trustsServiceUrl/registration"

  // note this is the playback service, which is another stub
  lazy val getTrustOrEstateUrl: String =
    s"${config.getTrustOrEstateUrl}/trustsandestates"

  def get5MLDTrustOrEstateEndpoint(identifier: String): String =
    if (identifier.length == 10) {
      s"$getTrustOrEstateUrl/registration/UTR/$identifier"
    } else {
      s"$getTrustOrEstateUrl/registration/URN/$identifier"
    }

  lazy val trustVariationsEndpoint: String =
    s"${config.hipVaryTrustOrEstateUrl}/etmp/RESTAdapter/trustsandestates/registration"

  override val className: String = this.getClass.getSimpleName

  protected def hipHeaders: Seq[(String, String)] =
    Seq(
      "correlationid"         -> UUID.randomUUID().toString,
      "X-Originating-System"  -> "TRS",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
      "X-Transmitting-System" -> "HIP",
      "Authorization"         -> s"Basic ${config.hipAuthorizationToken}"
    )

  override def checkExistingTrust(
    existingTrustCheckRequest: ExistingCheckRequest
  ): TrustEnvelope[ExistingCheckResponse] =
    EitherT {

      implicit val hc: HeaderCarrier                                   = HeaderCarrier(extraHeaders = hipHeaders)
      implicit val hipCustomErrResponse: OFormat[HipCustomErrResponse] = HipCustomErrResponse.formats

      logger.info(
        s"[$className][checkExistingTrust][Session ID: ${Session.id(hc)}] matching trust for " +
          s"correlationid: ${hipHeaders.toMap.getOrElse("correlationid", "NOT FOUND")}"
      )

      val httpReads: HttpReads[ExistingCheckResponse] =
        new HttpReads[ExistingCheckResponse] {
          override def read(method: String, url: String, response: HttpResponse): ExistingCheckResponse =
            response.status match {
              case CREATED                                            =>
                Matched
              case UNPROCESSABLE_ENTITY                               =>
                val code = response.json.as[HipCustomErrResponse].error.errorId
                if (code === "001")
                  NotMatched
                else if (code === "002")
                  AlreadyRegistered
                else if (code === "999")
                  ServerError
                else
                  BadRequest
              case BAD_REQUEST | NOT_FOUND | UNAUTHORIZED | FORBIDDEN =>
                BadRequest
              case INTERNAL_SERVER_ERROR                              =>
                ServerError
              case _                                                  =>
                ServiceUnavailable
            }
        }

      http
        .post(url"$matchTrustsEndpoint")
        .withBody(Json.toJson(existingTrustCheckRequest))
        .execute[ExistingCheckResponse](using httpReads, ec)
        .map(Right(_))
        .recover { case ex =>
          Left(handleError(ex, "checkExistingTrust", matchTrustsEndpoint))
        }
    }

  override def registerTrust(registration: Registration): TrustEnvelope[RegistrationResponse] = EitherT {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = hipHeaders)

    logger.info(
      // todo remove the session stuff maybe
      s"[$className][registerTrust][Session ID: ${Session.id(hc)}] registering trust for " +
        s"correlationid: ${hipHeaders.toMap.getOrElse("correlationid", "NOT FOUND")}"
    )

    val httpReads: HttpReads[RegistrationResponse] =
      (_: String, _: String, response: HttpResponse) =>
        response.status match {
          case CREATED                                =>
            val hip = response.json.as[HipSuccessRegistrationTrnResponse]
            hip.success
          case UNPROCESSABLE_ENTITY                   =>
            val code = response.json.as[HipCustomErrResponse].error.errorId
            if (code === "001") {
              logger.info("[RegistrationResponse] No match response from HIP.")
              NoMatchResponse
            } else if (code === "002") {
              logger.info("[RegistrationResponse] already registered response from HIP.")
              AlreadyRegisteredResponse
            } else if (code === "999") {
              logger.error("[RegistrationResponse] Forbidden response from HIP.")
              InternalServerErrorResponse
            } else
              BadRequestResponse
          case BAD_REQUEST | NOT_FOUND | UNAUTHORIZED =>
            logger.error(s"[RegistrationResponse] ${response.status} from HIP")
            BadRequestResponse
          case INTERNAL_SERVER_ERROR | FORBIDDEN      =>
            InternalServerErrorResponse
          case status                                 =>
            logger.error(s"[RegistrationResponse][parseForbiddenResponse] $status response from des.")
            ServiceUnavailableResponse
        }

    http
      .post(url"$trustRegistrationEndpoint")
      .withBody(Json.toJson(registration))
      .execute[RegistrationResponse](using httpReads, ec)
      .map(Right(_))
      .recover { case ex =>
        Left(handleError(ex, "registerTrust", trustRegistrationEndpoint))
      }
  }

  override def getTrustInfo(identifier: String): TrustEnvelope[GetTrustResponse] = ???

  override def trustVariation(trustVariations: JsValue): TrustEnvelope[VariationSuccessResponse] = EitherT {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = hipHeaders)

    logger.info(
      s"[$className][trustVariation][Session ID: ${Session.id(hc)}]" +
        s" submitting trust variation for correlationid: ${hipHeaders.toMap.getOrElse("correlationid", "NOT FOUND")}"
    )

    val httpReads: HttpReads[VariationResponse] = new HttpReads[VariationResponse] {
      override def read(method: String, url: String, response: HttpResponse): VariationResponse =
        response.status match {
          case OK                    =>
            val hip = response.json.as[HipSuccessVariationTrnResponse]
            hip.success
          case BAD_REQUEST           =>
            logger.error(s"[VariationResponse][httpReads] Bad Request response from hip")
            VariationFailureResponse(BAD_REQUEST, BadRequestErrorResponse, "Bad request")
          case UNPROCESSABLE_ENTITY  =>
            val code = response.json.as[HipCustomErrResponse].error.errorId
            if (code === "004") {
              logger.info("[VariationResponse] Duplicate submission response from HIP.")
              VariationFailureResponse(CONFLICT, VariationInternalServerErrorResponse, "Conflict response from hip")
            } else if (code === "003") {
              logger.info("[VariationResponse] request could not be processed from HIP.")
              // n.b. despite the status being 400 the des connector returned InternalServerErrorResponse
              VariationFailureResponse(
                BAD_REQUEST,
                VariationInternalServerErrorResponse,
                "Invalid correlation id response from hip"
              )
            } else if (code === "999") {
              logger.error("[RegistrationResponse] Forbidden response from HIP.")
              VariationFailureResponse(
                INTERNAL_SERVER_ERROR,
                VariationInternalServerErrorResponse,
                "hip is currently experiencing problems that require live service intervention"
              )
            } else
              VariationFailureResponse(BAD_REQUEST, BadRequestErrorResponse, "Bad request")
          case INTERNAL_SERVER_ERROR =>
            logger.error(s"[VariationResponse][httpReads] Internal server error response from hip")
            VariationFailureResponse(
              INTERNAL_SERVER_ERROR,
              VariationInternalServerErrorResponse,
              "hip is currently experiencing problems that require live service intervention"
            )

          case status =>
            logger.error(s"[VariationResponse][httpReads] $status response from hip.")
            VariationFailureResponse(
              SERVICE_UNAVAILABLE,
              ServiceNotAvailableErrorResponse,
              s"hip dependent service is down."
            )
        }
    }

    http
      .put(url"$trustVariationsEndpoint")
      .withBody(trustVariations)
      .execute[VariationResponse](using httpReads, ec)
      .map {
        case response: VariationSuccessResponse => Right(response)

        case response: VariationFailureResponse =>
          logger.warn(
            s"[$className][trustVariation][Session ID: ${Session.id(hc)}] " +
              s"trust variation failed with status: ${response.status}, with message: ${response.message} with errorType: ${response.errorType}"
          )

          Left(VariationFailureForAudit(response.errorType, response.message))
      }
      .recover { case ex =>
        Left(handleError(ex, "trustVariation", trustVariationsEndpoint))
      }
  }

}
