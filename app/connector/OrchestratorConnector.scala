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
import models.orchestrator.{OrchestratorMigrationRequest, OrchestratorMigrationResponse}
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future


class OrchestratorConnector @Inject()(http: HttpClient, config: AppConfig) extends Logging {

  def migrateToTaxable(urn: String, utr: String)(implicit hc: HeaderCarrier): Future[OrchestratorMigrationResponse] = {
    val taxEnrolmentsEndpoint = s"${config.orchestratorUrl}/trusts-enrolment-orchestrator/orchestration-process"
    val migrationRequest = OrchestratorMigrationRequest(urn, utr)
    http.POST[JsValue, OrchestratorMigrationResponse](
      taxEnrolmentsEndpoint,
      Json.toJson(migrationRequest)
    )
  }
}

