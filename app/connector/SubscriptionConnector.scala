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
import errors.ServerError
import models.tax_enrolments.{SubscriptionIdFailureResponse, SubscriptionIdResponse, SubscriptionIdSuccessResponse}
import play.api.http.HeaderNames
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.Constants._
import utils.TrustEnvelope.TrustEnvelope

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubscriptionConnector @Inject() (http: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext)
    extends ConnectorErrorResponseHandler {

  val className: String = this.getClass.getSimpleName

  private lazy val trustsServiceUrl: String = s"${config.subscriptionBaseUrl}/trusts"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "CorrelationId"

  private def desHeaders(correlationId: String): Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.subscriptionToken}",
      CONTENT_TYPE              -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER        -> config.subscriptionEnvironment,
      CORRELATION_HEADER        -> correlationId
    )

  def getSubscriptionId(trn: String): TrustEnvelope[SubscriptionIdSuccessResponse] = EitherT {

    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))

    val subscriptionIdEndpointUrl = s"$trustsServiceUrl/trn/$trn/subscription"
    http
      .get(url"$subscriptionIdEndpointUrl")
      .execute[SubscriptionIdResponse]
      .map {
        case response: SubscriptionIdSuccessResponse => Right(response)
        case response: SubscriptionIdFailureResponse => Left(ServerError(response.message))
      }
      .recover { case ex =>
        Left(handleError(ex, "getSubscriptionId", subscriptionIdEndpointUrl))
      }
  }

}
