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

package services.auditing

import models.auditing.{OrchestratorAuditEvent, TrustAuditing}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MigrationAuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext)
    extends AuditService(auditConnector) {

  def auditOrchestratorSuccess(urn: String, utr: String)(implicit hc: HeaderCarrier): Unit = {
    val request = Json.obj(
      "urn" -> urn,
      "utr" -> utr
    )

    auditOrchestrator(
      event = TrustAuditing.ORCHESTRATOR_TO_TAXABLE_SUCCESS,
      request = request,
      response = Json.obj("success" -> true)
    )
  }

  def auditOrchestratorFailure(urn: String, utr: String, errorReason: String)(implicit hc: HeaderCarrier): Unit = {
    val request = Json.obj(
      "urn" -> urn,
      "utr" -> utr
    )

    auditOrchestrator(
      event = TrustAuditing.ORCHESTRATOR_TO_TAXABLE_FAILED,
      request = request,
      response = Json.obj("errorReason" -> errorReason)
    )
  }

  def auditTaxEnrolmentFailure(subscriptionId: String, urn: String, errorMessage: String)(implicit
    hc: HeaderCarrier
  ): Unit =
    auditErrorResponse(
      TrustAuditing.TAX_ENROLMENT_TO_TAXABLE_FAILED,
      Json.obj("urn" -> urn),
      subscriptionId,
      errorMessage
    )

  private def auditOrchestrator(
    event: String,
    request: JsValue,
    response: JsValue
  )(implicit hc: HeaderCarrier): Unit = {

    val auditPayload = OrchestratorAuditEvent(
      request = request,
      response = response
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

}
