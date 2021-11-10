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

package services.nonRepudiation

import base.BaseSpec
import connector.NonRepudiationConnector
import models.nonRepudiation._
import models.requests.{CredentialData, IdentifierRequest}
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => mEq, _}
import org.mockito.Mockito.{doNothing, reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import retry.RetryHelper
import services.auditing.NRSAuditService
import services.dates.LocalDateTimeService
import services.encoding.PayloadEncodingService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.{Credentials, LoginTimes}
import uk.gov.hmrc.http.{Authorization, ForwardedFor, HeaderCarrier, RequestId, SessionId}
import utils.{Headers, JsonFixtures}

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

class NonRepudiationServiceSpec extends BaseSpec with JsonFixtures with BeforeAndAfterEach {

  private val mockConnector = mock[NonRepudiationConnector]
  private val mockLocalDateTimeService = mock[LocalDateTimeService]
  private val mockPayloadEncodingService = mock[PayloadEncodingService]
  private val retryHelper = injector.instanceOf[RetryHelper]
  private val mockNrsAuditService = mock[NRSAuditService]

  val v4UuidRegex: Regex = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[4][0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}$".r

  override def beforeEach() = {
    reset(mockConnector, mockLocalDateTimeService, mockPayloadEncodingService)
  }

  private val SUT = new NonRepudiationService(mockConnector, mockLocalDateTimeService, mockPayloadEncodingService, retryHelper, mockNrsAuditService)

  doNothing().when(mockNrsAuditService).audit(any())(any())

  private val credential: CredentialData = CredentialData(
    Some("groupIdentifier"),
    LoginTimes(DateTime.parse("2020-10-10"), Some(DateTime.parse("2020-10-05"))),
    Some(Credentials("12345", "governmentGateway")),
    Some("client@email.com")
  )

  private val credentialNotPopulated: CredentialData = CredentialData(
    None,
    LoginTimes(DateTime.parse("2020-10-10"), None),
    None,
    None
  )

  override implicit lazy val hc: HeaderCarrier = HeaderCarrier(
    authorization = Some(Authorization("Bearer 12345")),
    deviceID = Some("deviceId"),
    trueClientPort = Some("ClientPort"),
    trueClientIp = Some("ClientIP"),
    sessionId = Some(SessionId("SessionID")),
    requestId = Some(RequestId("RequestID"))
  )

  implicit val request: IdentifierRequest[JsValue] =
    IdentifierRequest(
      fakeRequest
        .withHeaders("test" -> "value")
        .withBody(Json.parse("{}")),
      internalId = "internalId",
      sessionId = "sessionId",
      affinityGroup = AffinityGroup.Agent,
      credentialData = credential
    )

  ".register" should {

    "have the correct headerData" in {
      lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

      val payLoad = Json.toJson(registrationRequest)

      val fakeRequestWithHeaders = fakeRequest
        .withHeaders(play.api.mvc.Headers(
          ("user-agent", "to be removed"),
          ("User-Agent", "to be removed"),
          ("True-User-Agent", "true agent to be kept"),
          ("true-user-agent", "to be removed")
        ))

      implicit val request: IdentifierRequest[JsValue] =
        IdentifierRequest(fakeRequestWithHeaders, internalId = "internalId", sessionId = "sessionId", affinityGroup = AffinityGroup.Agent, credentialData = credential)

      val trn = "ABTRUST12345678"

      when(mockConnector.nonRepudiate(payloadCaptor.capture())(any()))
        .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

      when(mockLocalDateTimeService.now(ZoneOffset.UTC))
        .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

      when(mockPayloadEncodingService.encode(mEq(payLoad)))
        .thenReturn("encodedPayload")

      when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
        .thenReturn("payloadChecksum")

      val fResult = SUT.register(trn, payLoad)

      whenReady(fResult) { _ =>

        payloadCaptor.getValue.metadata.headerData mustBe Json.parse(
          """
            |{
            | "user-agent":"true agent to be kept"
            |}
            |""".stripMargin
        )
      }
    }

    "send a registration event for an Agent" in {

      lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

      val payLoad = Json.toJson(registrationRequest)

      implicit val request: IdentifierRequest[JsValue] =
        IdentifierRequest(fakeRequest, internalId = "internalId", sessionId = "sessionId", affinityGroup = AffinityGroup.Agent, credentialData = credential)

      val identityDataJson = Json.obj(
        "internalId" -> "internalId",
        "affinityGroup" -> "Agent",
        "deviceId" -> "deviceId",
        "clientIP" -> "ClientIP",
        "clientPort" -> "ClientPort",
        "sessionId" -> "SessionID",
        "requestId" -> "RequestID",
        "declaration" -> Json.obj(
          "firstName" -> "John",
          "middleName" -> "William",
          "lastName" -> "O'Connor"
        ),
        "credential" -> Json.obj(
          "email" -> "client@email.com",
          "loginTimes" -> Json.obj(
            "currentLogin" -> "2020-10-10T00:00:00.000Z",
            "previousLogin" -> "2020-10-05T00:00:00.000Z"
          ),
          "groupIdentifier" -> "groupIdentifier",
          "provider" -> Json.obj(
            "providerId" -> "12345",
            "providerType" -> "governmentGateway"
          )
        ),
        "agentDetails" -> Json.obj(
          "arn" -> "AARN1234567",
          "agentName" -> "Mr . xys abcde",
          "agentAddress" -> Json.obj(
            "line1" -> "line1",
            "line2" -> "line2",
            "postCode" -> "TF3 2BX",
            "country" -> "GB"
          ),
          "agentTelephoneNumber" -> "07912180120",
          "clientReference" -> "clientReference"
        )
      )

      val trn = "ABTRUST12345678"

      when(mockConnector.nonRepudiate(payloadCaptor.capture())(any()))
        .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

      when(mockLocalDateTimeService.now(ZoneOffset.UTC))
        .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

      when(mockPayloadEncodingService.encode(mEq(payLoad)))
        .thenReturn("encodedPayload")

      when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
        .thenReturn("payloadChecksum")

      val fResult = SUT.register(trn, payLoad)

      whenReady(fResult) { result =>
        result mustBe NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
        payloadCaptor.getValue.payload mustBe "encodedPayload"
        payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
        payloadCaptor.getValue.metadata.businessId mustBe "trs"
        payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
        payloadCaptor.getValue.metadata.notableEvent mustBe NotableEvent.TrsRegistration
        payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json"
        payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.TRN, trn)
        Json.toJson(payloadCaptor.getValue.metadata.identityData) mustBe identityDataJson

        payloadCaptor.getValue.metadata.headerData mustBe Json.parse(
          """
            |{
            | "Host":"localhost",
            | "Content-Type":"application/json",
            | "Draft-Registration-ID":"bbe4c063-2b5a-4f29-bfa6-46c3c8906b0d",
            | "user-agent":"Mozilla"
            |}
            |""".stripMargin
        )
      }
    }

    "send a registration event for an Organisation" in {

      lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

      val payLoadWithoutAgentDetails = Json.toJson(registrationRequest).as[JsObject] - "agentDetails"

      implicit val request: IdentifierRequest[JsValue] =
        IdentifierRequest(fakeRequest, internalId = "internalId", sessionId = "sessionId", affinityGroup = AffinityGroup.Organisation, credentialData = credentialNotPopulated)

      val identityData = Json.obj(
        "internalId" -> "internalId",
        "affinityGroup" -> "Organisation",
        "deviceId" -> "deviceId",
        "clientIP" -> "ClientIP",
        "clientPort" -> "ClientPort",
        "sessionId" -> "SessionID",
        "requestId" -> "RequestID",
        "credential" -> Json.obj(
          "email" -> "No email",
          "loginTimes" -> Json.obj(
            "currentLogin" -> "2020-10-10T00:00:00.000Z"
          ),
          "groupIdentifier" -> "No group identifier",
          "provider" -> Json.obj(
            "providerId" -> "No provider id",
            "providerType" -> "No provider type"
          )
        ),
        "declaration" -> Json.obj(
          "firstName" -> "John",
          "middleName" -> "William",
          "lastName" -> "O'Connor"
        )
      )

      val trn = "ABTRUST12345678"

      when(mockConnector.nonRepudiate(payloadCaptor.capture())(any()))
        .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

      when(mockLocalDateTimeService.now(ZoneOffset.UTC))
        .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

      when(mockPayloadEncodingService.encode(mEq(payLoadWithoutAgentDetails)))
        .thenReturn("encodedPayload")

      when(mockPayloadEncodingService.generateChecksum(mEq(payLoadWithoutAgentDetails)))
        .thenReturn("payloadChecksum")

      val fResult = SUT.register(trn, payLoadWithoutAgentDetails)
      whenReady(fResult) { result =>
        result mustBe NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
        payloadCaptor.getValue.payload mustBe "encodedPayload"
        payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
        payloadCaptor.getValue.metadata.businessId mustBe "trs"
        payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
        payloadCaptor.getValue.metadata.notableEvent mustBe NotableEvent.TrsRegistration
        payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json"
        payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.TRN, trn)
        Json.toJson(payloadCaptor.getValue.metadata.identityData) mustBe identityData

        payloadCaptor.getValue.metadata.headerData mustBe Json.parse(
          """
            |{
            | "Host":"localhost",
            | "Content-Type":"application/json",
            | "Draft-Registration-ID":"bbe4c063-2b5a-4f29-bfa6-46c3c8906b0d",
            | "user-agent":"Mozilla"
            |}
            |""".stripMargin
        )
      }
    }
  }

  ".maintain" when {

    "taxable" must {

      "return a SuccessfulNrsResponse" in {

        lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

        val payLoad = trustVariationsRequest

        val identityData = Json.obj(
          "internalId" -> "internalId",
          "affinityGroup" -> "Agent",
          "deviceId" -> "deviceId",
          "clientIP" -> "ClientIP",
          "clientPort" -> "ClientPort",
          "sessionId" -> "SessionID",
          "requestId" -> "RequestID",
          "credential" -> Json.obj(
            "email" -> "client@email.com",
            "loginTimes" -> Json.obj(
              "currentLogin" -> "2020-10-10T00:00:00.000Z",
              "previousLogin" -> "2020-10-05T00:00:00.000Z"
            ),
            "groupIdentifier" -> "groupIdentifier",
            "provider" -> Json.obj(
              "providerId" -> "12345",
              "providerType" -> "governmentGateway"
            )
          ),
          "declaration" -> Json.obj(
            "firstName" -> "Abram",
            "middleName" -> "Joe",
            "lastName" -> "James"),
          "agentDetails" -> Json.obj(
            "arn" -> "AARN1234567",
            "agentName" -> "Mr. xys abcde",
            "agentAddress" -> Json.obj(
              "line1" -> "line1",
              "line2" -> "line2",
              "postCode" -> "TF3 2BX",
              "country" -> "GB"),
            "agentTelephoneNumber" -> "07912180120",
            "clientReference" -> "clientReference")
        )

        val utr = "1234567890"

        when(mockConnector.nonRepudiate(payloadCaptor.capture())(any()))
          .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(mockLocalDateTimeService.now(ZoneOffset.UTC))
          .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

        when(mockPayloadEncodingService.encode(mEq(payLoad)))
          .thenReturn("encodedPayload")

        when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
          .thenReturn("payloadChecksum")

        val fResult = SUT.maintain(utr, payLoad)

        whenReady(fResult) { result =>
          result mustBe NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
          payloadCaptor.getValue.payload mustBe "encodedPayload"
          payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
          payloadCaptor.getValue.metadata.businessId mustBe "trs"
          payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
          payloadCaptor.getValue.metadata.notableEvent mustBe NotableEvent.TrsUpdateTaxable
          payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json"
          payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.UTR, utr)
          Json.toJson(payloadCaptor.getValue.metadata.identityData) mustBe identityData

          payloadCaptor.getValue.metadata.headerData mustBe Json.parse(
            """
              |{
              | "Host":"localhost",
              | "Content-Type":"application/json",
              | "Draft-Registration-ID":"bbe4c063-2b5a-4f29-bfa6-46c3c8906b0d",
              | "user-agent":"Mozilla",
              | "test": "value"
              |}
              |""".stripMargin
          )
        }
      }
    }

    "nonTaxable" must {

      "return a SuccessfulNrsResponse" in {

        lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

        val payLoad = trustVariationsRequest

        val identityData = Json.obj(
          "internalId" -> "internalId",
          "affinityGroup" -> "Agent",
          "deviceId" -> "deviceId",
          "clientIP" -> "ClientIP",
          "clientPort" -> "ClientPort",
          "sessionId" -> "SessionID",
          "requestId" -> "RequestID",
          "credential" -> Json.obj(
            "email" -> "client@email.com",
            "loginTimes" -> Json.obj(
              "currentLogin" -> "2020-10-10T00:00:00.000Z",
              "previousLogin" -> "2020-10-05T00:00:00.000Z"
            ),
            "groupIdentifier" -> "groupIdentifier",
            "provider" -> Json.obj(
              "providerId" -> "12345",
              "providerType" -> "governmentGateway"
            )
          ),
          "declaration" -> Json.obj(
            "firstName" -> "Abram",
            "middleName" -> "Joe",
            "lastName" -> "James"),
          "agentDetails" -> Json.obj(
            "arn" -> "AARN1234567",
            "agentName" -> "Mr. xys abcde",
            "agentAddress" -> Json.obj(
              "line1" -> "line1",
              "line2" -> "line2",
              "postCode" -> "TF3 2BX",
              "country" -> "GB"),
            "agentTelephoneNumber" -> "07912180120",
            "clientReference" -> "clientReference")
        )
        val urn = "NTTRUST12345678"

        when(mockConnector.nonRepudiate(payloadCaptor.capture())(any()))
          .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(mockLocalDateTimeService.now(ZoneOffset.UTC))
          .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

        when(mockPayloadEncodingService.encode(mEq(payLoad)))
          .thenReturn("encodedPayload")

        when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
          .thenReturn("payloadChecksum")

        val fResult = SUT.maintain(urn, payLoad)

        whenReady(fResult) { result =>
          result mustBe NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
          payloadCaptor.getValue.payload mustBe "encodedPayload"
          payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
          payloadCaptor.getValue.metadata.businessId mustBe "trs"
          payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
          payloadCaptor.getValue.metadata.notableEvent mustBe NotableEvent.TrsUpdateNonTaxable
          payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json"
          payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.URN, urn)
          Json.toJson(payloadCaptor.getValue.metadata.identityData) mustBe identityData

          payloadCaptor.getValue.metadata.headerData mustBe Json.parse(
            """
              |{
              | "Host":"localhost",
              | "Content-Type":"application/json",
              | "Draft-Registration-ID":"bbe4c063-2b5a-4f29-bfa6-46c3c8906b0d",
              | "user-agent":"Mozilla",
              | "test": "value"
              |}
              |""".stripMargin
          )
        }
      }
    }

    ".sendEvent" must {

      "parse X-Forwarded-For header when there is no True-Client-IP or True-Client-Port" in {
        lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

        val payLoad = Json.toJson(registrationRequest)

        implicit val headerCarrierWithXForwardedFor: HeaderCarrier = HeaderCarrier(
          authorization = Some(Authorization("Bearer 12345")),
          deviceID = Some("deviceId"),
          sessionId = Some(SessionId("SessionID")),
          requestId = Some(RequestId("RequestID")),
          forwarded = Some(ForwardedFor("xForwardedForIpAddress"))
        )

        implicit val request: IdentifierRequest[JsValue] =
          IdentifierRequest(fakeRequest, internalId = "internalId", sessionId = "sessionId", affinityGroup = AffinityGroup.Agent, credentialData = credential)

        val identityData = Json.obj(
          "internalId" -> "internalId",
          "affinityGroup" -> "Agent",
          "deviceId" -> "deviceId",
          "clientIP" -> "xForwardedForIpAddress",
          "clientPort" -> "No Client Port",
          "sessionId" -> "SessionID",
          "requestId" -> "RequestID",
          "credential" -> Json.obj(
            "email" -> "client@email.com",
            "loginTimes" -> Json.obj(
              "currentLogin" -> "2020-10-10T00:00:00.000Z",
              "previousLogin" -> "2020-10-05T00:00:00.000Z"
            ),
            "groupIdentifier" -> "groupIdentifier",
            "provider" -> Json.obj(
              "providerId" -> "12345",
              "providerType" -> "governmentGateway"
            )
          ),
          "declaration" -> Json.obj(
            "firstName" -> "John",
            "middleName" -> "William",
            "lastName" -> "O'Connor"),
          "agentDetails" -> Json.obj(
            "arn" -> "AARN1234567",
            "agentName" -> "Mr . xys abcde",
            "agentAddress" -> Json.obj(
              "line1" -> "line1",
              "line2" -> "line2",
              "postCode" -> "TF3 2BX",
              "country" -> "GB"),
            "agentTelephoneNumber" -> "07912180120",
            "clientReference" -> "clientReference")
        )

        val trn = "ABTRUST12345678"

        when(mockConnector.nonRepudiate(payloadCaptor.capture())(any()))
          .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(mockLocalDateTimeService.now(ZoneOffset.UTC))
          .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

        when(mockPayloadEncodingService.encode(mEq(payLoad)))
          .thenReturn("encodedPayload")

        when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
          .thenReturn("payloadChecksum")

        val fResult = SUT.register(trn, payLoad)(headerCarrierWithXForwardedFor, request)
        whenReady(fResult) { result =>
          result mustBe NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
          payloadCaptor.getValue.payload mustBe "encodedPayload"
          payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
          payloadCaptor.getValue.metadata.businessId mustBe "trs"
          payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
          payloadCaptor.getValue.metadata.notableEvent mustBe NotableEvent.TrsRegistration
          payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json"
          payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.TRN, trn)
          Json.toJson(payloadCaptor.getValue.metadata.identityData) mustBe identityData

          payloadCaptor.getValue.metadata.headerData mustBe Json.parse(
            """
              |{
              | "Host":"localhost",
              | "Content-Type":"application/json",
              | "Draft-Registration-ID":"bbe4c063-2b5a-4f29-bfa6-46c3c8906b0d",
              | "user-agent":"Mozilla"
              |}
              |""".stripMargin
          )
        }
      }

      "parse the first X-Forwarded-For header when there is no True-Client-IP or True-Client-Port" in {

        lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

        val payLoad = Json.toJson(registrationRequest)

        implicit val headerCarrierWithXForwardedFor: HeaderCarrier = HeaderCarrier(
          authorization = Some(Authorization("Bearer 12345")),
          deviceID = Some("deviceId"),
          sessionId = Some(SessionId("SessionID")),
          requestId = Some(RequestId("RequestID")),
          forwarded = Some(ForwardedFor("xForwardedForIpAddress, secondXForwardedIpAddress"))
        )

        implicit val request: IdentifierRequest[JsValue] =
          IdentifierRequest(fakeRequest, internalId = "internalId", sessionId = "sessionId", affinityGroup = AffinityGroup.Agent, credentialData = credential)

        val identityData = Json.obj(
          "internalId" -> "internalId",
          "affinityGroup" -> "Agent",
          "deviceId" -> "deviceId",
          "clientIP" -> "xForwardedForIpAddress",
          "clientPort" -> "No Client Port",
          "sessionId" -> "SessionID",
          "requestId" -> "RequestID",
          "credential" -> Json.obj(
            "email" -> "client@email.com",
            "loginTimes" -> Json.obj(
              "currentLogin" -> "2020-10-10T00:00:00.000Z",
              "previousLogin" -> "2020-10-05T00:00:00.000Z"
            ),
            "groupIdentifier" -> "groupIdentifier",
            "provider" -> Json.obj(
              "providerId" -> "12345",
              "providerType" -> "governmentGateway"
            )
          ),
          "declaration" -> Json.obj(
            "firstName" -> "John",
            "middleName" -> "William",
            "lastName" -> "O'Connor"),
          "agentDetails" -> Json.obj(
            "arn" -> "AARN1234567",
            "agentName" -> "Mr . xys abcde",
            "agentAddress" -> Json.obj(
              "line1" -> "line1",
              "line2" -> "line2",
              "postCode" -> "TF3 2BX",
              "country" -> "GB"),
            "agentTelephoneNumber" -> "07912180120",
            "clientReference" -> "clientReference")
        )

        val trn = "ABTRUST12345678"

        when(mockConnector.nonRepudiate(payloadCaptor.capture())(any()))
          .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(mockLocalDateTimeService.now(ZoneOffset.UTC))
          .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

        when(mockPayloadEncodingService.encode(mEq(payLoad)))
          .thenReturn("encodedPayload")

        when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
          .thenReturn("payloadChecksum")

        val fResult = SUT.register(trn, payLoad)(headerCarrierWithXForwardedFor, request)
        whenReady(fResult) { result =>
          result mustBe NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
          payloadCaptor.getValue.payload mustBe "encodedPayload"
          payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
          payloadCaptor.getValue.metadata.businessId mustBe "trs"
          payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
          payloadCaptor.getValue.metadata.notableEvent mustBe NotableEvent.TrsRegistration
          payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json"
          payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.TRN, trn)
          Json.toJson(payloadCaptor.getValue.metadata.identityData) mustBe identityData

          payloadCaptor.getValue.metadata.headerData mustBe Json.parse(
            """
              |{
              | "Host":"localhost",
              | "Content-Type":"application/json",
              | "Draft-Registration-ID":"bbe4c063-2b5a-4f29-bfa6-46c3c8906b0d",
              | "user-agent":"Mozilla"
              |}
              |""".stripMargin
          )

        }
      }

    }

    ".getDeclaration" must {
      "successfully get declaration name for a registration" in {
        val payLoad = trustVariationsRequest
        val result = SUT.getDeclaration(payLoad)

        result mustBe Json.parse(
          """{
            |          "firstName": "Abram",
            |          "middleName": "Joe",
            |          "lastName": "James"
            |        }""".stripMargin)
      }
    }

    ".getAgentDetails" must {
      "successfully get Agent Details for a registration when they exist" in {
        val payLoad = trustVariationsRequest
        val result = SUT.getAgentDetails(payLoad)

        result mustBe Some(Json.parse(
          """{"arn":"AARN1234567",
            |"agentName":"Mr. xys abcde",
            |"agentAddress":{
            |"line1":"line1",
            |"line2":"line2",
            |"postCode":"TF3 2BX",
            |"country":"GB"
            |},
            |"agentTelephoneNumber":"07912180120",
            |"clientReference":"clientReference"}""".stripMargin))
      }

      "return a None when no Agent Details exist for a registration" in {
        val payLoad = Json.obj()
        val result = SUT.getAgentDetails(payLoad)

        result mustBe None
      }
    }
  }
}

