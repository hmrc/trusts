/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.services

import org.mockito.Matchers._
import org.mockito.Mockito.{times, verify, verifyZeroInteractions, when}
import play.api.libs.json.JsValue
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.ExistingCheckResponse._
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.EstateFoundResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.variation.VariationResponse
import uk.gov.hmrc.trusts.repositories.CacheRepositoryImpl
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class DesServiceSpec extends BaseSpec {

  private trait DesServiceFixture {
    lazy val request = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")
    val mockConnector: DesConnector = mock[DesConnector]
    val mockRepository: CacheRepositoryImpl = mock[CacheRepositoryImpl]
    when(mockRepository.get(any[String], any[String])).thenReturn(Future.successful(None))

    val myId = "myId"

    val SUT = new DesService(mockConnector, mockRepository)
  }

  ".checkExistingTrust" should {

    "return Matched " when {
      "connector returns Matched." in new DesServiceFixture {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(Matched))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe Matched
        }
      }
    }

    "return NotMatched " when {
      "connector returns NotMatched." in new DesServiceFixture {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(NotMatched))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in new DesServiceFixture {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(BadRequest))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in new DesServiceFixture {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(AlreadyRegistered))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in new DesServiceFixture {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServiceUnavailable))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }

      }
    }

    "return ServerError " when {
      "connector returns ServerError." in new DesServiceFixture {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServerError))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }


  ".checkExistingEstate" should {
    "return Matched " when {
      "connector returns Matched." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(Matched))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe Matched
        }
      }
    }


    "return NotMatched " when {
      "connector returns NotMatched." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(NotMatched))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(BadRequest))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(AlreadyRegistered))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(ServiceUnavailable))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }

      }
    }

    "return ServerError " when {
      "connector returns ServerError." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(ServerError))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }

  ".getTrustInfoFormBundleNo should return formBundle No from ETMP Data" in {
    val etmpData = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
    val mockDesconnector = mock[DesConnector]
    val mockRepository = mock[CacheRepositoryImpl]
    when(mockDesconnector.getTrustInfo(any())(any())).thenReturn(Future.successful(etmpData))

    val OUT = new DesService(mockDesconnector, mockRepository)

    whenReady(OUT.getTrustInfoFormBundleNo("75464876")) {formBundleNo =>
      formBundleNo mustBe etmpData.responseHeader.formBundleNo
    }
  }

  ".registerTrust" should {

    "return SuccessRegistrationResponse " when {
      "connector returns SuccessRegistrationResponse." in new DesServiceFixture {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.successful(RegistrationTrnResponse("trn123")))
        val futureResult = SUT.registerTrust(registrationRequest)
        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("trn123")
        }
      }
    }

    "return AlreadyRegisteredException " when {
      "connector returns  AlreadyRegisteredException." in new DesServiceFixture {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(AlreadyRegisteredException))
        val futureResult = SUT.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in new DesServiceFixture {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val futureResult = SUT.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  }

  ".registerEstate" should {

    "return RegistrationTrnResponse " when {
      "connector returns RegistrationTrnResponse." in new DesServiceFixture {
        when(mockConnector.registerEstate(estateRegRequest)).
          thenReturn(Future.successful(RegistrationTrnResponse("trn123")))
        val futureResult = SUT.registerEstate(estateRegRequest)
        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("trn123")
        }
      }
    }

    "return AlreadyRegisteredException " when {
      "connector returns  AlreadyRegisteredException." in new DesServiceFixture {
        when(mockConnector.registerEstate(estateRegRequest)).
          thenReturn(Future.failed(AlreadyRegisteredException))
        val futureResult = SUT.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in new DesServiceFixture {
        when(mockConnector.registerEstate(estateRegRequest)).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val futureResult = SUT.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  }

  ".getSubscriptionId" should {

    "return SubscriptionIdResponse " when {
      "connector returns SubscriptionIdResponse." in new DesServiceFixture {
        when(mockConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        val futureResult = SUT.getSubscriptionId("trn123456789")
        whenReady(futureResult) {
          result => result mustBe SubscriptionIdResponse("123456789")
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in new DesServiceFixture {
        when(mockConnector.getSubscriptionId("trn123456789")).
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
      "TrustFoundResponse is returned from DES Connector with a Processed flag and a trust body when not cached" in new DesServiceFixture {
        val utr = "1234567890"
        val fullEtmpResponseJson = getTrustResponse
        val trustInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String])).thenReturn(Future.successful(None))
        when(mockConnector.getTrustInfo(any())(any()))
          .thenReturn(Future.successful(TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1"))))

        val futureResult = SUT.getTrustInfo(utr, myId)
        whenReady(futureResult) { result =>
          result mustBe TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1"))
          verify(mockRepository, times(1)).set(utr, myId, fullEtmpResponseJson)
        }
      }

      "TrustFoundResponse is returned from repository with a Processed flag and a trust body when cached" in new DesServiceFixture {
        val utr = "1234567890"

        val fullEtmpResponseJson = getTrustResponse
        val trustInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String])).thenReturn(Future.successful(Some(fullEtmpResponseJson)))
        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.failed(new Exception("Connector should not have been called")))

        val futureResult = SUT.getTrustInfo(utr, myId)
        whenReady(futureResult) { result =>
          result mustBe TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1"))
          verifyZeroInteractions(mockConnector)
        }
      }
    }

    "return TrustFoundResponse" when {

      "TrustFoundResponse is returned from DES Connector" in new DesServiceFixture {

        val utr = "1234567890"

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("In Processing", "1"))))

        val futureResult = SUT.getTrustInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe TrustFoundResponse(ResponseHeader("In Processing", "1"))
        }
      }
    }

    "return InvalidUTRResponse" when {

      "InvalidUTRResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(InvalidUTRResponse))

        val invalidUtr = "123456789"
        val futureResult = SUT.getTrustInfo(invalidUtr, myId)

        whenReady(futureResult) { result =>
          result mustBe InvalidUTRResponse
        }
      }
    }

    "return InvalidRegimeResponse" when {
      "InvalidRegimeResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(InvalidRegimeResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe InvalidRegimeResponse
        }
      }
    }

    "return BadRequestResponse" when {
      "BadRequestResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(BadRequestResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {
      "ResourceNotFoundResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "InternalServerErrorResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {
      "ServiceUnavailableResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  }

  ".getEstateInfo" should {
    "return EstateFoundResponse" when {
      "EstateFoundResponse is returned from DES Connector" in new DesServiceFixture {

        val utr = "1234567890"

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(EstateFoundResponse(None, ResponseHeader("In Processing", "1"))))

        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("In Processing", "1"))
        }
      }
    }

    "return InvalidUTRResponse" when {
      "InvalidUTRResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(InvalidUTRResponse))

        val invalidUtr = "123456789"
        val futureResult = SUT.getEstateInfo(invalidUtr)

        whenReady(futureResult) { result =>
          result mustBe InvalidUTRResponse
        }
      }
    }

    "return InvalidRegimeResponse" when {
      "InvalidRegimeResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(InvalidRegimeResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe InvalidRegimeResponse
        }
      }
    }

    "return BadRequestResponse" when {
      "BadRequestResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(BadRequestResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {
      "ResourceNotFoundResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "InternalServerErrorResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {
      "ServiceUnavailableResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  } // getTrustInfo

  ".trustVariation" should {
    "return a VariationTvnResponse" when {

      "connector returns VariationResponse." in new DesServiceFixture {

        when(mockConnector.trustVariation(trustVariationsRequest)).
          thenReturn(Future.successful(VariationResponse("tvn123")))

        val futureResult = SUT.trustVariation(trustVariationsRequest)

        whenReady(futureResult) {
          result => result mustBe VariationResponse("tvn123")
        }

      }


      "return DuplicateSubmissionException" when {

        "connector returns  DuplicateSubmissionException." in new DesServiceFixture {

          when(mockConnector.trustVariation(trustVariationsRequest)).
            thenReturn(Future.failed(DuplicateSubmissionException))

          val futureResult = SUT.trustVariation(trustVariationsRequest)

          whenReady(futureResult.failed) {
            result => result mustBe DuplicateSubmissionException
          }

        }

      }

      "return same Exception " when {
        "connector returns  exception." in new DesServiceFixture {

          when(mockConnector.trustVariation(trustVariationsRequest)).
            thenReturn(Future.failed(InternalServerErrorException("")))

          val futureResult = SUT.trustVariation(trustVariationsRequest)

          whenReady(futureResult.failed) {
            result => result mustBe an[InternalServerErrorException]
          }

        }
      }

    }
  }

  ".estateVariation" should {
    "return a VariationTvnResponse" when {

      "connector returns VariationResponse." in new DesServiceFixture {

        when(mockConnector.estateVariation(estateVariationsRequest)).
          thenReturn(Future.successful(VariationResponse("tvn123")))

        val futureResult = SUT.estateVariation(estateVariationsRequest)

        whenReady(futureResult) {
          result => result mustBe VariationResponse("tvn123")
        }

      }


      "return DuplicateSubmissionException" when {

        "connector returns  DuplicateSubmissionException." in new DesServiceFixture {

          when(mockConnector.estateVariation(estateVariationsRequest)).
            thenReturn(Future.failed(DuplicateSubmissionException))

          val futureResult = SUT.estateVariation(estateVariationsRequest)

          whenReady(futureResult.failed) {
            result => result mustBe DuplicateSubmissionException
          }

        }

      }

      "return same Exception " when {
        "connector returns  exception." in new DesServiceFixture {

          when(mockConnector.estateVariation(estateVariationsRequest)).
            thenReturn(Future.failed(InternalServerErrorException("")))

          val futureResult = SUT.estateVariation(estateVariationsRequest)

          whenReady(futureResult.failed) {
            result => result mustBe an[InternalServerErrorException]
          }

        }
      }

    }
  }

}

