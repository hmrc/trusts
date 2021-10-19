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
import config.AppConfig
import connector.NonRepudiationConnector
import models.nonRepudiation._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => mEq, _}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import utils.JsonFixtures

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class NonRepudiationServiceSpec extends BaseSpec with JsonFixtures with BeforeAndAfterEach {

  override def beforeEach() = {
    reset(mockConnector, mockLocalDateTimeService, mockPayloadEncodingService)
  }

  private val mockConnector = mock[NonRepudiationConnector]
  private val mockLocalDateTimeService = mock[LocalDateTimeService]
  private val mockPayloadEncodingService = mock[PayloadEncodingService]

  private val SUT = new NonRepudiationService(mockConnector, mockLocalDateTimeService, mockPayloadEncodingService, appConfig)
  override implicit lazy val hc = HeaderCarrier(authorization = Some(Authorization("Bearer 12345")))

  ".register" should {

    "return a SuccessfulNrsResponse" in {

      lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

      val payLoad = Json.toJson(registrationRequest)

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
      }
    }
  }

  ".maintain" when {

    "taxable" must {

      "return a SuccessfulNrsResponse" in {

        lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

        val payLoad = trustVariationsRequest

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
        }
      }
    }

    "nonTaxable" must {

      "return a SuccessfulNrsResponse" in {

        lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

        val payLoad = trustVariationsRequest

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
        }
      }
    }

    ".sendEvent" should {
      "not attempt a retry when error response indicates retry is unnecessary" in {

        val payLoad = trustVariationsRequest

        val urn = "NTTRUST12345678"

        when(mockConnector.nonRepudiate(any())(any()))
          .thenReturn(Future.successful(BadRequestResponse))

        when(mockLocalDateTimeService.now(ZoneOffset.UTC))
          .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

        when(mockPayloadEncodingService.encode(mEq(payLoad)))
          .thenReturn("encodedPayload")

        when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
          .thenReturn("payloadChecksum")

        val fResult = SUT.maintain(urn, payLoad)

        whenReady(fResult) { _ =>
          verify(mockConnector, times(1)).nonRepudiate(any())(any())
        }
      }

      "attempt a retry when error response indicates retry is necessary" in {

        val payLoad = trustVariationsRequest

        val urn = "NTTRUST12345678"

        when(mockConnector.nonRepudiate(any())(any()))
          .thenReturn(Future.successful(BadGatewayResponse))

        when(mockLocalDateTimeService.now(ZoneOffset.UTC))
          .thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

        when(mockPayloadEncodingService.encode(mEq(payLoad)))
          .thenReturn("encodedPayload")

        when(mockPayloadEncodingService.generateChecksum(mEq(payLoad)))
          .thenReturn("payloadChecksum")

        val fResult = SUT.maintain(urn, payLoad)

        whenReady(fResult, timeout(5.seconds)) { _ =>
          verify(mockConnector, times(10)).nonRepudiate(any())(any())
        }
      }
    }
  }
}

