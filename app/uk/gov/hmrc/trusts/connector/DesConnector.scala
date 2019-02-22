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

import play.api.http.HeaderNames
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.trusts.config.{AppConfig, WSHttp}
import uk.gov.hmrc.trusts.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DesConnectorImpl @Inject()(http: WSHttp, config: AppConfig) extends DesConnector {

  lazy val trustsServiceUrl : String = s"${config.desUrl}/trusts"
  lazy val matchEndpoint : String = trustsServiceUrl + "/match"
  lazy val registrationEndpoint : String = trustsServiceUrl + "/registration"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "Correlation-Id"
  val CONTENT_TYPE = "Content-Type"
  val CONTENT_TYPE_JSON = "application/json"

  override def checkExistingTrust(existingTrustCheckRequest: ExistingTrustCheckRequest)
                                 : Future[ExistingTrustResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)


    val response = http.POST[JsValue, ExistingTrustResponse](matchEndpoint, Json.toJson(existingTrustCheckRequest))
    (implicitly[Writes[JsValue]], ExistingTrustResponse.httpReads, implicitly[HeaderCarrier](hc),global)

    response
  }


  def desHeaders : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.desToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.desEnvironment,
      CORRELATION_HEADER -> UUID.randomUUID().toString
    )

  override def registerTrust(registration: Registration)
                            : Future[RegistrationResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders)

    val response = http.POST[JsValue, RegistrationResponse](registrationEndpoint, Json.toJson(registration),hc.headers)
    (implicitly[Writes[JsValue]], RegistrationResponse.httpReads, implicitly[HeaderCarrier](hc),global)

    response
  }
}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {
  def checkExistingTrust(existingTrustCheckRequest: ExistingTrustCheckRequest): Future[ExistingTrustResponse]

  def registerTrust(registration: Registration): Future[RegistrationResponse]
}