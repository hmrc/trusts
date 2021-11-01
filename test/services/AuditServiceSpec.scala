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

package services

import base.BaseSpec
import models.auditing.{NrsAuditEvent, OrchestratorAuditEvent, VariationAuditEvent}
import models.nonRepudiation.{MetaData, SearchKey, SearchKeys, SuccessfulNrsResponse}
import models.variation.VariationResponse
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.verify
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.LocalDateTime

class AuditServiceSpec extends BaseSpec {

  "auditVariationSubmitted" should {
    "send Variation Submitted by Organisation" when {
      "there are no special JSON fields" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector)

        val request = Json.obj()

        val response = VariationResponse("TRN123456")
        service.auditVariationSubmitted("internalId", false, request, response)

        val expectedAuditData = VariationAuditEvent(
          request,
          "internalId",
          false,
          Json.toJson(response)
        )

        verify(connector).sendExplicitAudit[VariationAuditEvent](
          equalTo("VariationSubmittedByOrganisation"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Variation Submitted by Agent" when {
      "there is an AgentDetails JSON field" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector)

        val request = Json.obj(
          "agentDetails" -> Json.obj() // Doesn't care about contents of object
        )

        val response = VariationResponse("TRN123456")
        service.auditVariationSubmitted("internalId", false, request, response)

        val expectedAuditData = VariationAuditEvent(
          request,
          "internalId",
          false,
          Json.toJson(response)
        )

        verify(connector).sendExplicitAudit[VariationAuditEvent](
          equalTo("VariationSubmittedByAgent"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Closure Submitted by Organisation" when {
      "there is an endTrustDate field" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector)

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

        service.auditVariationSubmitted("internalId", false, request, VariationResponse("TRN123456"))

        val expectedAuditData = VariationAuditEvent(
          request,
          "internalId",
          false,
          Json.toJson(response)
        )

        verify(connector).sendExplicitAudit[VariationAuditEvent](
          equalTo("ClosureSubmittedByOrganisation"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }

    "send Closure Submitted by Agent" when {
      "there are agentDetails and endTrustDate JSON fields" in {
        val connector = mock[AuditConnector]
        val service = new AuditService(connector)

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

        service.auditVariationSubmitted("internalId", false, request, VariationResponse("TRN123456"))

        val expectedAuditData = VariationAuditEvent(
          request,
          "internalId",
          false,
          Json.toJson(response)
        )

        verify(connector).sendExplicitAudit[VariationAuditEvent](
          equalTo("ClosureSubmittedByAgent"),
          equalTo(expectedAuditData))(any(), any(), any())
      }
    }
  }

  "auditOrchestratorResponse" should {

    "send Success message" when {

      val connector = mock[AuditConnector]
      val service = new AuditService(connector)

      val urn = "NTTRUST00000001"
      val utr = "123456789"

      val request = Json.obj(
        "urn" -> urn,
        "utr" -> utr
      )
      service.auditOrchestratorTransformationToTaxableSuccess(urn, utr)

      val expectedAuditData = OrchestratorAuditEvent(
        request,
        Json.obj("success" -> true)
      )

      verify(connector).sendExplicitAudit[OrchestratorAuditEvent](
        equalTo("OrchestratorNonTaxableTrustToTaxableSuccess"),
        equalTo(expectedAuditData))(any(), any(), any())
    }

    "send Failure message" when {
      val connector = mock[AuditConnector]
      val service = new AuditService(connector)

      val urn = "NTTRUST00000001"
      val utr = "123456789"

      val request = Json.obj(
        "urn" -> urn,
        "utr" -> utr
      )

      service.auditOrchestratorTransformationToTaxableError(urn, utr, "Error happened")

      val expectedAuditData = OrchestratorAuditEvent(
        request,
        response = Json.obj("errorReason" -> "Error happened")
      )

      verify(connector).sendExplicitAudit[OrchestratorAuditEvent](
        equalTo("OrchestratorNonTaxableTrustToTaxableFailed"),
        equalTo(expectedAuditData))(any(), any(), any())
    }
  }

  "auditNrsResponse" should {
    "audit a successful NRS submission" ignore {
      val connector = mock[AuditConnector]
      val service = new AuditService(connector)

      val metaData = MetaData(businessId = "trs",
        notableEvent = "trs-registration",
        payloadContentType = "application/json",
        payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
        userSubmissionTimestamp = LocalDateTime.parse("2021-10-10"),
        identityData = Json.obj("internalId" -> "some-id"),
        userAuthToken = "AbCdEf123456",
        headerData = Json.obj(
          "Gov-Client-Public-IP" -> "198.51.100.0",
          "Gov-Client-Public-Port" -> "12345"
        ),
        searchKeys = SearchKeys(SearchKey.TRN, "ABTRUST123456789")
      )

      val request = NrsAuditEvent(auditType = "trs-registration", metaData = metaData, SuccessfulNrsResponse("1234567890"))

      service.auditNrsResponse(request)

      val expectedAuditData = Json.parse(
        """{
          |"businessId": "trs",
          |"notableEvent": "trs-registration",
          |"payloadSha256Checksum": "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          |"userSubmissionTimestamp": "2021-10-10",
          |"identityData": "",
          |"userAuthToken": "AbCdEf123456",
          |"searchKeys": {
          |"trn": "ABTRUST123456789"
          |},
          |"result": {
          |"nrSubmissionId": "1234567890",
          |"code": 200
          |}
          |}
          |""".stripMargin)

      verify(connector).sendExplicitAudit(
        equalTo("OrchestratorNonTaxableTrustToTaxableSuccess"),
        equalTo(expectedAuditData))(any(), any(), any())
    }
  }
}
