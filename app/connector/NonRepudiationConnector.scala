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

import config.AppConfig

import javax.inject.Inject
import models.nonRepudiation.{NRSSubmission, NrsResponse}
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.Constants.{CONTENT_TYPE, CONTENT_TYPE_JSON, X_API_KEY}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class NonRepudiationConnector @Inject()(http: HttpClient, config: AppConfig) extends Logging {

  private def headers = Seq(CONTENT_TYPE -> CONTENT_TYPE_JSON, X_API_KEY -> config.xApiKey)

   def NonRepudiate(json: NRSSubmission)(implicit hc: HeaderCarrier): Future[NrsResponse] = {

  http.POST[NRSSubmission, NrsResponse](config.nonRepudiationUrl, json, headers)

  }

}
