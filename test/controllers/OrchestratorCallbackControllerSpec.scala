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

package controllers

import base.BaseSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class OrchestratorCallbackControllerSpec extends BaseSpec with GuiceOneServerPerSuite {

  val auditConnector = mock[AuditConnector]
  val urn = "NTTRUST00000001"
  val utr = "123456789"

  ".migrationToTaxableCallback" should {

    "return 200 " when {
      "orchestrator callback for subscription id migration " in {
        val SUT = new OrchestratorCallbackController(Helpers.stubControllerComponents())

        val payloadBody = s"""{ "success" : true, "urn": "$urn", "utr": "$utr"}"""
        val result = SUT.migrationToTaxableCallback(urn, utr).apply(postRequestWithPayload(Json.parse(payloadBody)))
        status(result) mustBe OK
      }
    }
  }
}
