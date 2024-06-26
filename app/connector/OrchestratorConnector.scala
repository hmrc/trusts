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
import models.orchestrator.OrchestratorMigrationRequest
import models.tax_enrolments.{OrchestratorToTaxableFailureResponse, OrchestratorToTaxableResponse, OrchestratorToTaxableSuccessResponse}
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Constants.{CONTENT_TYPE, CONTENT_TYPE_JSON}
import utils.Session
import utils.TrustEnvelope.TrustEnvelope

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class OrchestratorConnector @Inject()(http: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) extends ConnectorErrorResponseHandler {

  val className: String = this.getClass.getSimpleName

  private def headers = Seq(CONTENT_TYPE -> CONTENT_TYPE_JSON)

  def migrateToTaxable(urn: String, utr: String)(implicit hc: HeaderCarrier): TrustEnvelope[OrchestratorToTaxableSuccessResponse] = EitherT {
    logger.info(s"[$className][migrateToTaxable][Session ID: ${Session.id(hc)}][URN: $urn, UTR: $utr] starting migration from non-taxable to taxable")

    val orchestratorHeaders = hc.withExtraHeaders(headers: _*)

    val orchestratorEndpoint = s"${config.orchestratorUrl}/trusts-enrolment-orchestrator/orchestration-process"
    val migrationRequest = OrchestratorMigrationRequest(urn, utr)

    http.POST[JsValue, OrchestratorToTaxableResponse](
      orchestratorEndpoint,
      Json.toJson(migrationRequest)
    )(Writes.jsValueWrites, OrchestratorToTaxableResponse.httpReads, orchestratorHeaders, ec).map {
      case response: OrchestratorToTaxableSuccessResponse => Right(response)
      case response: OrchestratorToTaxableFailureResponse => Left(ServerError(response.message))
    }.recover {
      case ex =>
        Left(handleError(ex, "migrateToTaxable", orchestratorEndpoint))
    }
  }
}

