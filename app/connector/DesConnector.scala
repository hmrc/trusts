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

package connector

import java.util.UUID

import config.AppConfig
import javax.inject.Inject
import models.tax_enrolments.SubscriptionIdResponse
import play.api.Logging
import play.api.http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Constants._
import utils.Session

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class DesConnector @Inject()(http: HttpClient, config: AppConfig) extends Logging {

  private lazy val trustsServiceUrl : String = s"${config.subscriptionBaseUrl}/trusts"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "CorrelationId"

  private def desHeaders(correlationId : String) : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.subscriptionToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.subscriptionEnvironment,
      CORRELATION_HEADER -> correlationId
    )

   def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {

    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))

    val subscriptionIdEndpointUrl = s"$trustsServiceUrl/trn/$trn/subscription"
    logger.debug(s"[getSubscriptionId][Session ID: ${Session.id(hc)}][TRN: $trn]" +
      s" Sending get subscription id request to DES, url=$subscriptionIdEndpointUrl")

    val response = http.GET[SubscriptionIdResponse](subscriptionIdEndpointUrl)
    (SubscriptionIdResponse.httpReads, implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])

    response
  }

}
