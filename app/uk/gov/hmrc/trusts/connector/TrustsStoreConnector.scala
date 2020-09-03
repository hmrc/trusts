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

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.models.FeatureResponse

import scala.concurrent.{ExecutionContext, Future}

class TrustsStoreConnector @Inject()(http: HttpClient, config: AppConfig) {

  private def featureUrl(feature: String): String = s"${config.trustsStoreUrl}/trusts-store/features/$feature"

  def getFeature(feature: String)(implicit hc : HeaderCarrier, ec : ExecutionContext): Future[FeatureResponse] = {
    http.GET[FeatureResponse](featureUrl(feature))
  }

}
