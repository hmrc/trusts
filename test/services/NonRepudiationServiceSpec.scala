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
import connector.NonRepudiationConnector
import models.nonRepudiation._
import models.requests.IdentifierRequest
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => mEq, _}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import retry.RetryHelper
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import utils.JsonFixtures

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

class NonRepudiationServiceSpec extends BaseSpec with JsonFixtures with BeforeAndAfterEach {

  private val mockConnector = mock[NonRepudiationConnector]
  private val mockLocalDateTimeService = mock[LocalDateTimeService]
  private val mockPayloadEncodingService = mock[PayloadEncodingService]
  private val retryHelper = injector.instanceOf[RetryHelper]

  val v4UuidRegex: Regex = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[4][0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}$".r

  override def beforeEach() = {
    reset(mockConnector, mockLocalDateTimeService, mockPayloadEncodingService)
  }

  private val SUT = new NonRepudiationService(mockConnector, mockLocalDateTimeService, mockPayloadEncodingService, retryHelper)
  override implicit lazy val hc = HeaderCarrier(
    authorization = Some(Authorization("Bearer 12345")),
    deviceID = Some("deviceId"),
    trueClientPort = Some("ClientPort"),
    trueClientIp = Some("ClientIP")
  )

  implicit val request: IdentifierRequest[JsValue] =
    IdentifierRequest(
      FakeRequest()
      .withHeaders("test" -> "value")
      .withBody(Json.parse("{}")),
      internalId = "internalId",
      sessionId = "sessionId",
      affinityGroup = AffinityGroup.Agent
    )

  ".register" should {

    "return a SuccessfulNrsResponse" in {

      lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

      val payLoad = Json.toJson(registrationRequest)

      implicit val request: IdentifierRequest[JsValue] =
        IdentifierRequest(fakeRequest, internalId = "internalId", sessionId = "sessionId", affinityGroup = AffinityGroup.Agent)

      val identityData = Json.obj(
        "internalId" -> "internalId",
        "affinityGroup" -> "Agent",
        "deviceId" -> "deviceId",
        "clientIP" -> "ClientIP",
        "clientPort" -> "ClientPort",
        "declaration" -> Json.obj(
          "firstName" ->"John",
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
        .thenReturn(Future.successful(SuccessfulNrsResponse("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

      when(mockLocalDateTimeService.now(ZoneOffset.UTC))
        .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

      when(mockPayloadEncodingService.encode(mEq(payLoad)))
        .thenReturn("encodedPayload")

      when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
        .thenReturn("payloadChecksum")

      val fResult = SUT.register(trn, payLoad)
      whenReady(fResult) { result =>
        result mustBe SuccessfulNrsResponse("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
        payloadCaptor.getValue.payload mustBe "encodedPayload"
        payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
        payloadCaptor.getValue.metadata.businessId mustBe "trs"
        payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
        payloadCaptor.getValue.metadata.notableEvent mustBe "trs-registration"
        payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json; charset=utf-8"
        payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.TRN, trn)
        payloadCaptor.getValue.metadata.identityData mustBe identityData
        (payloadCaptor.getValue.metadata.headerData \ "Draft-Registration-ID").as[String] must fullyMatch regex v4UuidRegex
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
          "declaration" -> Json.obj(
            "firstName" ->"Abram",
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
          .thenReturn(Future.successful(SuccessfulNrsResponse("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(mockLocalDateTimeService.now(ZoneOffset.UTC))
          .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

        when(mockPayloadEncodingService.encode(mEq(payLoad)))
          .thenReturn("encodedPayload")

        when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
          .thenReturn("payloadChecksum")

        val fResult = SUT.maintain(utr, payLoad)

        whenReady(fResult) { result =>
          result mustBe SuccessfulNrsResponse("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
          payloadCaptor.getValue.payload mustBe "encodedPayload"
          payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
          payloadCaptor.getValue.metadata.businessId mustBe "trs"
          payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
          payloadCaptor.getValue.metadata.notableEvent mustBe "trs-update-taxable"
          payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json; charset=utf-8"
          payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.UTR, utr)
          payloadCaptor.getValue.metadata.identityData mustBe identityData
          (payloadCaptor.getValue.metadata.headerData \ "test").as[String] mustBe "value"
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
          "declaration" -> Json.obj(
            "firstName" ->"Abram",
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
          .thenReturn(Future.successful(SuccessfulNrsResponse("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(mockLocalDateTimeService.now(ZoneOffset.UTC))
          .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

        when(mockPayloadEncodingService.encode(mEq(payLoad)))
          .thenReturn("encodedPayload")

        when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
          .thenReturn("payloadChecksum")

        val fResult = SUT.maintain(urn, payLoad)

        whenReady(fResult) { result =>
          result mustBe SuccessfulNrsResponse("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
          payloadCaptor.getValue.payload mustBe "encodedPayload"
          payloadCaptor.getValue.metadata.payloadSha256Checksum mustBe "payloadChecksum"
          payloadCaptor.getValue.metadata.businessId mustBe "trs"
          payloadCaptor.getValue.metadata.userAuthToken mustBe "Bearer 12345"
          payloadCaptor.getValue.metadata.notableEvent mustBe "trs-update-non-taxable"
          payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json; charset=utf-8"
          payloadCaptor.getValue.metadata.searchKeys mustBe SearchKeys(SearchKey.URN, urn)
          payloadCaptor.getValue.metadata.identityData mustBe identityData
          (payloadCaptor.getValue.metadata.headerData \ "test").as[String] mustBe "value"
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

