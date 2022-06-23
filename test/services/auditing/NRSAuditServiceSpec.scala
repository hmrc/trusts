/*
 * Copyright 2022 HM Revenue & Customs
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
import models.auditing.NrsAuditEvent
import models.nonRepudiation._
import models.requests.CredentialData
import java.time.{LocalDate, LocalDateTime, ZoneId}
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.verify
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class NRSAuditServiceSpec extends BaseSpec {

  ".audit" should {

    "audit a successful registration to NRS" in {
      val connector = mock[AuditConnector]
      val service = new NRSAuditService(connector)

      val identityData = IdentityData(
        internalId = "internalId",
        affinityGroup = Agent,
        deviceId = "deviceId",
        clientIP = "clientIp",
        clientPort = "clientPort",
        sessionId = "sessionId",
        requestId = "requestId",
        declaration = Json.obj("example" -> "name"),
        agentDetails = Some(Json.obj("example" -> "agent details")),
        credential = CredentialData(groupIdentifier = None, loginTimes = LoginTimes(LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.of("Europe/London")).toInstant, None), provider = None, email = None)
      )

      val metaData = MetaData(
        businessId = "trs",
        notableEvent = NotableEvent.TrsRegistration,
        payloadContentType = "application/json",
        payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
        userSubmissionTimestamp = LocalDateTime.of(2021, 10, 5, 10, 4, 3),
        identityData = identityData,
        userAuthToken = "AbCdEf123456",
        headerData = Json.obj(
          "Gov-Client-Public-IP" -> "198.51.100.0",
          "Gov-Client-Public-Port" -> "12345"
        ),
        searchKeys = SearchKeys(SearchKey.TRN, "ABTRUST123456789")
      )

      val event = NrsAuditEvent(metaData = metaData, NRSResponse.Success("1234567890"))

      service.audit(event)

      val expectedAuditData = Json.parse(
        """{
          | "payload": {
          |   "businessId": "trs",
          |   "notableEvent": "trs-registration",
          |   "payloadSha256Checksum": "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          |   "userSubmissionTimestamp": "2021-10-05T10:04:03.000Z",
          |   "identityData": {
          |     "internalId": "internalId",
          |     "affinityGroup": "Agent",
          |     "sessionId": "sessionId",
          |     "requestId": "requestId",
          |     "declaration": {
          |       "example": "name"
          |     },
          |     "agentDetails": {
          |       "example": "agent details"
          |     },
          |     "credential": {
          |       "provider": {
          |           "providerId": "No provider id",
          |           "providerType": "No provider type"
          |       },
          |       "email": "No email",
          |       "groupIdentifier": "No group identifier",
          |       "loginTimes": {
          |           "currentLogin": "2020-10-10T00:00:00.000Z"
          |        }
          |     }
          |   },
          |   "searchKeys": {
          |     "trn": "ABTRUST123456789"
          |   }
          | },
          | "result": {
          |   "nrSubmissionId": "1234567890",
          |   "code": 202
          | }
          |}
          |""".stripMargin)

      verify(connector).sendExplicitAudit(
        equalTo("NonRepudiationTrustRegistration"),
        equalTo(expectedAuditData))(any(), any(), any())
    }

    "audit a successful taxable update to NRS" in {
      val connector = mock[AuditConnector]
      val service = new NRSAuditService(connector)

      val identityData = IdentityData(
        internalId = "internalId",
        affinityGroup = Organisation,
        deviceId = "deviceId",
        clientIP = "clientIp",
        clientPort = "clientPort",
        sessionId = "sessionId",
        requestId = "requestId",
        declaration = Json.obj("example" -> "name"),
        agentDetails = None,
        credential = CredentialData(groupIdentifier = None, loginTimes = LoginTimes(LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.of("Europe/London")).toInstant, None), provider = None, email = None)
      )

      val metaData = MetaData(
        businessId = "trs",
        notableEvent = NotableEvent.TrsUpdateTaxable,
        payloadContentType = "application/json",
        payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
        userSubmissionTimestamp = LocalDateTime.of(2021, 10, 5, 10, 4, 3),
        identityData = identityData,
        userAuthToken = "AbCdEf123456",
        headerData = Json.obj(
          "Gov-Client-Public-IP" -> "198.51.100.0",
          "Gov-Client-Public-Port" -> "12345"
        ),
        searchKeys = SearchKeys(SearchKey.UTR, "1234567890")
      )

      val event = NrsAuditEvent(metaData = metaData, NRSResponse.Success("1234567890"))

      service.audit(event)

      val expectedAuditData = Json.parse(
        """{
          | "payload": {
          |   "businessId": "trs",
          |   "notableEvent": "trs-update-taxable",
          |   "payloadSha256Checksum": "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          |   "userSubmissionTimestamp": "2021-10-05T10:04:03.000Z",
          |   "identityData": {
          |     "internalId": "internalId",
          |     "affinityGroup": "Organisation",
          |     "sessionId": "sessionId",
          |     "requestId": "requestId",
          |     "declaration": {
          |       "example": "name"
          |     },
          |     "credential": {
          |       "provider": {
          |           "providerId": "No provider id",
          |           "providerType": "No provider type"
          |       },
          |       "email": "No email",
          |       "groupIdentifier": "No group identifier",
          |       "loginTimes": {
          |           "currentLogin": "2020-10-10T00:00:00.000Z"
          |        }
          |     }
          |   },
          |   "searchKeys": {
          |     "utr": "1234567890"
          |   }
          | },
          | "result": {
          |   "nrSubmissionId": "1234567890",
          |   "code": 202
          | }
          |}
          |""".stripMargin)

      verify(connector).sendExplicitAudit(
        equalTo("NonRepudiationTrustTaxableUpdate"),
        equalTo(expectedAuditData))(any(), any(), any())
    }

    "audit a successful non-taxable update to NRS" in {
      val connector = mock[AuditConnector]
      val service = new NRSAuditService(connector)

      val identityData = IdentityData(
        internalId = "internalId",
        affinityGroup = Organisation,
        deviceId = "deviceId",
        clientIP = "clientIp",
        clientPort = "clientPort",
        sessionId = "sessionId",
        requestId = "requestId",
        declaration = Json.obj("example" -> "name"),
        agentDetails = None,
        credential = CredentialData(groupIdentifier = None, loginTimes = LoginTimes(LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.of("Europe/London")).toInstant, None), provider = None, email = None)
      )

      val metaData = MetaData(
        businessId = "trs",
        notableEvent = NotableEvent.TrsUpdateNonTaxable,
        payloadContentType = "application/json",
        payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
        userSubmissionTimestamp = LocalDateTime.of(2021, 10, 5, 10, 4, 3),
        identityData = identityData,
        userAuthToken = "AbCdEf123456",
        headerData = Json.obj(
          "Gov-Client-Public-IP" -> "198.51.100.0",
          "Gov-Client-Public-Port" -> "12345"
        ),
        searchKeys = SearchKeys(SearchKey.URN, "ABTRUS12345678")
      )

      val event = NrsAuditEvent(metaData = metaData, NRSResponse.Success("1234567890"))

      service.audit(event)

      val expectedAuditData = Json.parse(
        """{
          | "payload": {
          |   "businessId": "trs",
          |   "notableEvent": "trs-update-non-taxable",
          |   "payloadSha256Checksum": "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          |   "userSubmissionTimestamp": "2021-10-05T10:04:03.000Z",
          |   "identityData": {
          |     "internalId": "internalId",
          |     "affinityGroup": "Organisation",
          |     "sessionId": "sessionId",
          |     "requestId": "requestId",
          |     "declaration": {
          |       "example": "name"
          |     },
          |     "credential": {
          |       "provider": {
          |           "providerId": "No provider id",
          |           "providerType": "No provider type"
          |       },
          |       "email": "No email",
          |       "groupIdentifier": "No group identifier",
          |       "loginTimes": {
          |           "currentLogin": "2020-10-10T00:00:00.000Z"
          |        }
          |     }
          |   },
          |   "searchKeys": {
          |     "urn": "ABTRUS12345678"
          |   }
          | },
          | "result": {
          |   "nrSubmissionId": "1234567890",
          |   "code": 202
          | }
          |}
          |""".stripMargin)

      verify(connector).sendExplicitAudit(
        equalTo("NonRepudiationTrustNonTaxableUpdate"),
        equalTo(expectedAuditData))(any(), any(), any())
    }

    "audit a non-successful event to NRS" in {
      val connector = mock[AuditConnector]
      val service = new NRSAuditService(connector)

      val identityData = IdentityData(
        internalId = "internalId",
        affinityGroup = Organisation,
        deviceId = "deviceId",
        clientIP = "clientIp",
        clientPort = "clientPort",
        sessionId = "sessionId",
        requestId = "requestId",
        declaration = Json.obj("example" -> "name"),
        agentDetails = None,
        credential = CredentialData(groupIdentifier = None, loginTimes = LoginTimes(LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.of("Europe/London")).toInstant, None), provider = None, email = None)
      )

      val metaData = MetaData(
        businessId = "trs",
        notableEvent = NotableEvent.TrsUpdateNonTaxable,
        payloadContentType = "application/json",
        payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
        userSubmissionTimestamp = LocalDateTime.of(2021, 10, 5, 10, 4, 3),
        identityData = identityData,
        userAuthToken = "AbCdEf123456",
        headerData = Json.obj(
          "Gov-Client-Public-IP" -> "198.51.100.0",
          "Gov-Client-Public-Port" -> "12345"
        ),
        searchKeys = SearchKeys(SearchKey.URN, "ABTRUS12345678")
      )

      val event = NrsAuditEvent(metaData = metaData, NRSResponse.ServiceUnavailable)

      service.audit(event)

      val expectedAuditData = Json.parse(
        """{
          | "payload": {
          |   "businessId": "trs",
          |   "notableEvent": "trs-update-non-taxable",
          |   "payloadSha256Checksum": "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          |   "userSubmissionTimestamp": "2021-10-05T10:04:03.000Z",
          |   "identityData": {
          |     "internalId": "internalId",
          |     "affinityGroup": "Organisation",
          |     "sessionId": "sessionId",
          |     "requestId": "requestId",
          |     "declaration": {
          |       "example": "name"
          |     },
          |     "credential": {
          |       "provider": {
          |           "providerId": "No provider id",
          |           "providerType": "No provider type"
          |       },
          |       "email": "No email",
          |       "groupIdentifier": "No group identifier",
          |       "loginTimes": {
          |           "currentLogin": "2020-10-10T00:00:00.000Z"
          |        }
          |     }
          |   },
          |   "searchKeys": {
          |     "urn": "ABTRUS12345678"
          |   }
          | },
          | "result": {
          |   "code": 503,
          |   "reason": "ServiceUnavailable"
          | }
          |}
          |""".stripMargin)

      verify(connector).sendExplicitAudit(
        equalTo("NonRepudiationTrustNonTaxableUpdate"),
        equalTo(expectedAuditData))(any(), any(), any())
    }
  }
}
