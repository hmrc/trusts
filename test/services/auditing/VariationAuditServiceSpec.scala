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

package services.auditing

import base.BaseSpec
import models.auditing.VariationAuditEvent
import models.variation.VariationResponse
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import org.mockito.Mockito.verify
import org.mockito.ArgumentMatchers.{any, eq => equalTo}

class VariationAuditServiceSpec  extends BaseSpec {

  "auditVariationSubmitted" should {

    "send Variation Submitted by Organisation" when {
      "there are no special JSON fields" in {
        val connector = mock[AuditConnector]
        val service = new VariationAuditService(connector)

        val request = Json.obj()

        val response = VariationResponse("TRN123456")

        service.auditVariationSuccess("internalId", false, request, response)

        val expectedAuditData = VariationAuditEvent(
          request = request,
          internalAuthId = "internalId",
          migrateToTaxable = false,
          response = Json.toJson(response)
        )

        verify(connector).sendExplicitAudit[VariationAuditEvent](
          equalTo("VariationSubmittedByOrganisation"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Variation Submitted by Agent" when {
      "there is an AgentDetails JSON field" in {
        val connector = mock[AuditConnector]
        val service = new VariationAuditService(connector)

        val request = Json.obj(
          "agentDetails" -> Json.obj() // Doesn't care about contents of object
        )

        val response = VariationResponse("TRN123456")
        service.auditVariationSuccess("internalId", false, request, response)

        val expectedAuditData = VariationAuditEvent(
          request = request,
          internalAuthId = "internalId",
          migrateToTaxable = false,
          response = Json.toJson(response)
        )

        verify(connector).sendExplicitAudit[VariationAuditEvent](
          equalTo("VariationSubmittedByAgent"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Closure Submitted by Organisation" when {
      "there is an endTrustDate field" in {
        val connector = mock[AuditConnector]
        val service = new VariationAuditService(connector)

        val request = Json.obj(
          "trustEndDate" -> "2012-02-12",
          "details" -> Json.obj(
            "trust" -> Json.obj(
              "details" -> Json.obj(
                "trustTaxable" -> true
              )
            )
          )
        )

        val response = Json.obj(
          "tvn" -> "TRN123456",
          "trustTaxable" -> true
        )

        service.auditVariationSuccess("internalId", false, request, VariationResponse("TRN123456"))

        val expectedAuditData = VariationAuditEvent(
          request = request,
          internalAuthId = "internalId",
          migrateToTaxable = false,
          response = Json.toJson(response)
        )

        verify(connector).sendExplicitAudit[VariationAuditEvent](
          equalTo("ClosureSubmittedByOrganisation"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Closure Submitted by Agent" when {
      "there are agentDetails and endTrustDate JSON fields" in {
        val connector = mock[AuditConnector]
        val service = new VariationAuditService(connector)

        val request = Json.obj(
          "trustEndDate" -> "2012-02-12",
          "agentDetails" -> Json.obj(), // Doesn't care about contents of object
          "details" -> Json.obj(
            "trust" -> Json.obj(
              "details" -> Json.obj(
                "trustTaxable" -> false
              )
            )
          )
        )

        val response = Json.obj(
          "tvn" -> "TRN123456",
          "trustTaxable" -> false
        )

        service.auditVariationSuccess("internalId", false, request, VariationResponse("TRN123456"))

        val expectedAuditData = VariationAuditEvent(
          request = request,
          internalAuthId = "internalId",
          migrateToTaxable = false,
          response = Json.toJson(response)
        )

        verify(connector).sendExplicitAudit[VariationAuditEvent](
          equalTo("ClosureSubmittedByAgent"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }
  }

}
