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

import base.BaseSpec
import models.auditing.OrchestratorAuditEvent
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.verify
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global

class MigrationAuditServiceSpec extends BaseSpec {

  "auditOrchestratorResponse" should {

    "send Success message" when {
      val connector = mock[AuditConnector]
      val service   = new MigrationAuditService(connector)

      val urn = "NTTRUST00000001"
      val utr = "123456789"

      val request = Json.obj(
        "urn" -> urn,
        "utr" -> utr
      )

      service.auditOrchestratorSuccess(urn, utr)

      val expectedAuditData = OrchestratorAuditEvent(
        request,
        Json.obj("success" -> true)
      )

      verify(connector).sendExplicitAudit[OrchestratorAuditEvent](
        equalTo("OrchestratorNonTaxableTrustToTaxableSuccess"),
        equalTo(expectedAuditData)
      )(any(), any(), any())
    }

    "send Failure message" when {
      val connector = mock[AuditConnector]
      val service   = new MigrationAuditService(connector)

      val urn = "NTTRUST00000001"
      val utr = "123456789"

      val request = Json.obj(
        "urn" -> urn,
        "utr" -> utr
      )

      service.auditOrchestratorFailure(urn, utr, "Error happened")

      val expectedAuditData = OrchestratorAuditEvent(
        request,
        response = Json.obj("errorReason" -> "Error happened")
      )

      verify(connector).sendExplicitAudit[OrchestratorAuditEvent](
        equalTo("OrchestratorNonTaxableTrustToTaxableFailed"),
        equalTo(expectedAuditData)
      )(any(), any(), any())
    }
  }

}
