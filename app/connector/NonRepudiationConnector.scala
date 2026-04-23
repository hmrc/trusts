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

import config.AppConfig

import javax.inject.{Inject, Singleton}
import models.nonRepudiation.{NRSResponse, NRSSubmission}
import play.api.Logging
import play.api.libs.json.Json
import play.api.http.ContentTypes.JSON
import uk.gov.hmrc.http.client.HttpClientV2
import utils.Constants.{CONTENT_TYPE, X_API_KEY}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NonRepudiationConnector @Inject() (http: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext)
    extends Logging {

  private def headers = Seq(CONTENT_TYPE -> JSON, X_API_KEY -> config.xApiKey)
  val fullUrl: String = config.nonRepudiationUrl

  def nonRepudiate(payload: NRSSubmission)(implicit hc: HeaderCarrier): Future[NRSResponse] =
    http
      .post(url"$fullUrl")
      .setHeader(headers: _*)
      .withBody(Json.toJson(payload))
      .execute[NRSResponse]

}
