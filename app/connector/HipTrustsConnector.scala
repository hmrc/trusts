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
import models.Registration
import models.existing_trust.ExistingCheckResponse.{
  AlreadyRegistered, BadRequest, Matched, NotMatched, ServerError, ServiceUnavailable
}
import models.existing_trust.{ExistingCheckRequest, ExistingCheckResponse, HipCustomErrResponse}
import models.get_trust.GetTrustResponse
import models.registration.RegistrationResponse
import models.variation.VariationSuccessResponse
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
    s"${config.varyTrustOrEstateUrl}/trusts/variation"

  override val className: String = this.getClass.getSimpleName

  protected def hipHeaders: Seq[(String, String)] =
    Seq(
      "correlationid"         -> UUID.randomUUID().toString,
      "X-Originating-System"  -> "TRS",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
      "X-Transmitting-System" -> "HIP"
    )

  override def checkExistingTrust(
    existingTrustCheckRequest: ExistingCheckRequest
  ): TrustEnvelope[ExistingCheckResponse] =
    EitherT {
      val correlationId = UUID.randomUUID().toString

      implicit val hc: HeaderCarrier                                   = HeaderCarrier(extraHeaders = hipHeaders)
      implicit val hipCustomErrResponse: OFormat[HipCustomErrResponse] = HipCustomErrResponse.formats

      logger.info(
        s"[$className][checkExistingTrust][Session ID: ${Session.id(hc)}] matching trust for correlationId: $correlationId"
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

  // N.b. these will be implemented in near future
  override def registerTrust(registration: Registration): TrustEnvelope[RegistrationResponse] = ???

  override def getTrustInfo(identifier: String): TrustEnvelope[GetTrustResponse] = ???

  override def trustVariation(trustVariations: JsValue): TrustEnvelope[VariationSuccessResponse] = ???
}
