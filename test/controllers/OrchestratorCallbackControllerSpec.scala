/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import base.BaseSpec
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers.{status, _}
import services.auditing.MigrationAuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global

class OrchestratorCallbackControllerSpec extends BaseSpec with GuiceOneServerPerSuite {

  val auditConnector: AuditConnector = mock[AuditConnector]
  val urn = "NTTRUST00000001"
  val utr = "123456789"

  ".migrationToTaxableCallback" should {

    "return NoContent when sent an success message" when {
      "orchestrator callback for subscription id migration " in {
        val auditService = mock[MigrationAuditService]
        val SUT = new OrchestratorCallbackController(auditService, Helpers.stubControllerComponents())

        val payloadBody = s"""{ "success" : true, "urn": "$urn", "utr": "$utr"}"""
        val result = SUT.migrationToTaxableCallback(urn, utr).apply(postRequestWithPayload(Json.parse(payloadBody)))
        status(result) mustBe NO_CONTENT

        verify(auditService).auditOrchestratorSuccess(eqTo(urn), eqTo(utr))(any[HeaderCarrier])
      }

      "orchestrator callback for subscription id migration with error message" in {
        val auditService = mock[MigrationAuditService]
        val SUT = new OrchestratorCallbackController(auditService, Helpers.stubControllerComponents())

        val payloadBody = s"""{ "success" : false, "urn": "$urn", "utr": "$utr", "errorMessage": "An error message"}"""
        val result = SUT.migrationToTaxableCallback(urn, utr).apply(postRequestWithPayload(Json.parse(payloadBody)))
        status(result) mustBe NO_CONTENT

        verify(auditService).auditOrchestratorFailure(eqTo(urn), eqTo(utr), eqTo("An error message"))(any[HeaderCarrier])
      }
    }

  }
}
