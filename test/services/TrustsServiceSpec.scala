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

package services

import base.BaseSpec
import connector.{SubscriptionConnector, TrustsConnector}
import exceptions._
import models.existing_trust.ExistingCheckResponse._
import models.existing_trust._
import models.get_trust._
import models.registration.RegistrationTrnResponse
import models.tax_enrolments.SubscriptionIdResponse
import models.variation.VariationResponse
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{times, verify, verifyNoMoreInteractions, when}
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.JsValue
import repositories.CacheRepositoryImpl
import utils.JsonUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrustsServiceSpec extends BaseSpec {

  private trait TrustsServiceFixture {
    lazy val request = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")
    val mockSubscriptionConnector: TrustsConnector = mock[TrustsConnector]
    val mockTrustsConnector: SubscriptionConnector = mock[SubscriptionConnector]
    val mockRepository: CacheRepositoryImpl = mock[CacheRepositoryImpl]
    when(mockRepository.get(any[String], any[String], any[String])).thenReturn(Future.successful(None))
    when(mockRepository.resetCache(any[String], any[String], any[String])).thenReturn(Future.successful(true))
    val myId = "myId"
    val sessionId: String = "sessionId"

    val SUT = new TrustsService(mockSubscriptionConnector, mockTrustsConnector, mockRepository)
  }

  ".checkExistingTrust" should {

    "return Matched " when {
      "connector returns Matched." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(Matched))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe Matched
        }
      }
    }

    "return NotMatched " when {
      "connector returns NotMatched." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(NotMatched))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(BadRequest))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(AlreadyRegistered))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServiceUnavailable))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }

      }
    }

    "return ServerError " when {
      "connector returns ServerError." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServerError))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }

  ".getTrustInfoFormBundleNo should return formBundle No from ETMP Data" in {
    val etmpData = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
    val mockSubscriptionConnector = mock[TrustsConnector]
    val mockTrustsConnector: SubscriptionConnector = mock[SubscriptionConnector]
    val mockRepository = mock[CacheRepositoryImpl]
    when(mockSubscriptionConnector.getTrustInfo(any())).thenReturn(Future.successful(etmpData))

    val OUT = new TrustsService(mockSubscriptionConnector, mockTrustsConnector, mockRepository)

    whenReady(OUT.getTrustInfoFormBundleNo("75464876")) { formBundleNo =>
      formBundleNo mustBe etmpData.responseHeader.formBundleNo
    }
  }

  ".registerTrust" should {

    "return SuccessRegistrationResponse " when {
      "connector returns SuccessRegistrationResponse." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.registerTrust(registrationRequest)).
          thenReturn(Future.successful(RegistrationTrnResponse("trn123")))
        val futureResult = SUT.registerTrust(registrationRequest)
        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("trn123")
        }
      }
    }

    "return AlreadyRegisteredException " when {
      "connector returns  AlreadyRegisteredException." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(AlreadyRegisteredException))
        val futureResult = SUT.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val futureResult = SUT.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  }

  ".getSubscriptionId" should {

    "return SubscriptionIdResponse " when {
      "connector returns SubscriptionIdResponse." in new TrustsServiceFixture {
        when(mockTrustsConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        val futureResult = SUT.getSubscriptionId("trn123456789")
        whenReady(futureResult) {
          result => result mustBe SubscriptionIdResponse("123456789")
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in new TrustsServiceFixture {
        when(mockTrustsConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val futureResult = SUT.getSubscriptionId("trn123456789")

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".getTrustInfo" should {

    "return TrustFoundResponse" when {

      "TrustFoundResponse is returned from DES Connector with a Processed flag and a trust body when not cached" in new TrustsServiceFixture {
        val utr = "1234567890"
        val fullEtmpResponseJson = get5MLDTrustResponse
        val trustInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String], any[String])).thenReturn(Future.successful(None))

        when(mockRepository.resetCache(any[String], any[String], any[String])).thenReturn(Future.successful(true))

        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(Future.successful(TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1"))))

        when(mockRepository.set(any(), any(), any(), any())).thenReturn(Future.successful(true))

        val futureResult = SUT.getTrustInfo(utr, myId, sessionId)

        whenReady(futureResult) { result =>
          result mustBe models.get_trust.TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1"))
          verify(mockRepository, times(1)).set(utr, myId, sessionId, fullEtmpResponseJson)
        }
      }

      "TrustFoundResponse is returned from repository with a Processed flag and a trust body when cached" in new TrustsServiceFixture {
        val utr = "1234567890"

        val fullEtmpResponseJson = get5MLDTrustResponse
        val trustInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String], any[String])).thenReturn(Future.successful(Some(fullEtmpResponseJson)))
        when(mockSubscriptionConnector.getTrustInfo(any())).thenReturn(Future.failed(new Exception("Connector should not have been called")))

        val futureResult = SUT.getTrustInfo(utr, myId, sessionId)
        whenReady(futureResult) { result =>
          result mustBe models.get_trust.TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1"))
          verifyNoMoreInteractions(mockSubscriptionConnector)
        }
      }
    }

    "return TrustFoundResponse" when {

      "TrustFoundResponse is returned from DES Connector" in new TrustsServiceFixture {

        val utr = "1234567890"

        when(mockSubscriptionConnector.getTrustInfo(any())).thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("In Processing", "1"))))

        val futureResult = SUT.getTrustInfo(utr, myId, sessionId)

        whenReady(futureResult) { result =>
          result mustBe TrustFoundResponse(ResponseHeader("In Processing", "1"))
        }
      }
    }

    "return BadRequestResponse" when {

      "BadRequestResponse is returned from DES Connector" in new TrustsServiceFixture {

        when(mockSubscriptionConnector.getTrustInfo(any())).thenReturn(Future.successful(BadRequestResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId, sessionId)

        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {

      "ResourceNotFoundResponse is returned from DES Connector" in new TrustsServiceFixture {

        when(mockSubscriptionConnector.getTrustInfo(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId, sessionId)

        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return InternalServerErrorResponse" when {

      "InternalServerErrorResponse is returned from DES Connector" in new TrustsServiceFixture {

        when(mockSubscriptionConnector.getTrustInfo(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId, sessionId)

        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {

      "ServiceUnavailableResponse is returned from DES Connector" in new TrustsServiceFixture {

        when(mockSubscriptionConnector.getTrustInfo(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId, sessionId)

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  }

  ".trustVariation" should {

    "return a VariationTvnResponse" when {

      "connector returns VariationResponse." in new TrustsServiceFixture {

        when(mockSubscriptionConnector.trustVariation(trustVariationsRequest)).
          thenReturn(Future.successful(VariationResponse("tvn123")))

        val futureResult = SUT.trustVariation(trustVariationsRequest)

        whenReady(futureResult) {
          result => result mustBe VariationResponse("tvn123")
        }

      }


      "return DuplicateSubmissionException" when {

        "connector returns  DuplicateSubmissionException." in new TrustsServiceFixture {

          when(mockSubscriptionConnector.trustVariation(trustVariationsRequest)).
            thenReturn(Future.failed(DuplicateSubmissionException))

          val futureResult = SUT.trustVariation(trustVariationsRequest)

          whenReady(futureResult.failed) {
            result => result mustBe DuplicateSubmissionException
          }

        }

      }

      "return same Exception " when {
        "connector returns  exception." in new TrustsServiceFixture {

          when(mockSubscriptionConnector.trustVariation(trustVariationsRequest)).
            thenReturn(Future.failed(InternalServerErrorException("")))

          val futureResult = SUT.trustVariation(trustVariationsRequest)

          whenReady(futureResult.failed) {
            result => result mustBe an[InternalServerErrorException]
          }

        }
      }

    }
  }
}

