/*
 * Copyright 2026 HM Revenue & Customs
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
import cats.data.EitherT
import connector.{SubscriptionConnector, TrustsConnector}
import errors.{ServerError, TrustErrors, VariationFailureForAudit}
import models.existing_trust.ExistingCheckResponse._
import models.existing_trust._
import models.get_trust._
import models.registration.{AlreadyRegisteredResponse, RegistrationResponse, RegistrationTrnResponse}
import models.tax_enrolments.SubscriptionIdSuccessResponse
import models.variation.VariationSuccessResponse
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
    lazy val request                               = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")
    val mockSubscriptionConnector: TrustsConnector = mock[TrustsConnector]
    val mockTrustsConnector: SubscriptionConnector = mock[SubscriptionConnector]
    val mockRepository: CacheRepositoryImpl        = mock[CacheRepositoryImpl]

    when(mockRepository.get(any[String], any[String], any[String]))
      .thenReturn(EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Right(None))))

    when(mockRepository.resetCache(any[String], any[String], any[String]))
      .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

    val myId              = "myId"
    val sessionId: String = "sessionId"

    val SUT = new TrustsService(mockSubscriptionConnector, mockTrustsConnector, mockRepository)
  }

  ".checkExistingTrust" should {

    "return Matched " when {
      "connector returns Matched." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request))
          .thenReturn(EitherT[Future, TrustErrors, ExistingCheckResponse](Future.successful(Right(Matched))))
        val futureResult = SUT.checkExistingTrust(request).value
        whenReady(futureResult) { result =>
          result mustBe Right(Matched)
        }
      }
    }

    "return NotMatched " when {
      "connector returns NotMatched." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request))
          .thenReturn(EitherT[Future, TrustErrors, ExistingCheckResponse](Future.successful(Right(NotMatched))))
        val futureResult = SUT.checkExistingTrust(request).value
        whenReady(futureResult) { result =>
          result mustBe Right(NotMatched)
        }
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request))
          .thenReturn(EitherT[Future, TrustErrors, ExistingCheckResponse](Future.successful(Right(BadRequest))))
        val futureResult = SUT.checkExistingTrust(request).value
        whenReady(futureResult) { result =>
          result mustBe Right(BadRequest)
        }
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request))
          .thenReturn(EitherT[Future, TrustErrors, ExistingCheckResponse](Future.successful(Right(AlreadyRegistered))))
        val futureResult = SUT.checkExistingTrust(request).value
        whenReady(futureResult) { result =>
          result mustBe Right(AlreadyRegistered)
        }
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request))
          .thenReturn(EitherT[Future, TrustErrors, ExistingCheckResponse](Future.successful(Right(ServiceUnavailable))))
        val futureResult = SUT.checkExistingTrust(request).value
        whenReady(futureResult) { result =>
          result mustBe Right(ServiceUnavailable)
        }

      }
    }

    "return ExistingCheckResponse.ServerError " when {
      "connector returns ExistingCheckResponse.ServerError." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.checkExistingTrust(request))
          .thenReturn(
            EitherT[Future, TrustErrors, ExistingCheckResponse](
              Future.successful(Right(ExistingCheckResponse.ServerError))
            )
          )
        val futureResult = SUT.checkExistingTrust(request).value
        whenReady(futureResult) { result =>
          result mustBe Right(ExistingCheckResponse.ServerError)
        }
      }
    }
  }

  ".getTrustInfoFormBundleNo" should {
    "return formBundle No from ETMP Data" in {
      val etmpData                                   = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
      val mockSubscriptionConnector                  = mock[TrustsConnector]
      val mockTrustsConnector: SubscriptionConnector = mock[SubscriptionConnector]
      val mockRepository                             = mock[CacheRepositoryImpl]
      when(mockSubscriptionConnector.getTrustInfo(any()))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(etmpData))))

      val OUT = new TrustsService(mockSubscriptionConnector, mockTrustsConnector, mockRepository)

      whenReady(OUT.getTrustInfoFormBundleNo("75464876").value) { formBundleNo =>
        formBundleNo mustBe Right(etmpData.responseHeader.formBundleNo)
      }
    }

    "return VariationFailureForAudit when connector.getTrustInfo response isn't GetTrustSuccessResponse" in {
      val errorResponse                              = ResourceNotFoundResponse
      val mockSubscriptionConnector                  = mock[TrustsConnector]
      val mockTrustsConnector: SubscriptionConnector = mock[SubscriptionConnector]
      val mockRepository                             = mock[CacheRepositoryImpl]
      when(mockSubscriptionConnector.getTrustInfo(any()))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(errorResponse))))

      val OUT = new TrustsService(mockSubscriptionConnector, mockTrustsConnector, mockRepository)

      whenReady(OUT.getTrustInfoFormBundleNo("75464876").value) { formBundleNo =>
        formBundleNo mustBe Left(
          VariationFailureForAudit(
            errors.InternalServerErrorResponse,
            s"Submission could not proceed. Failed to retrieve latest form bundle no from ETMP: $errorResponse"
          )
        )
      }
    }

    "return ServerError()" when {
      "connector.getTrustInfo returns ServerError(message), where message is nonEmpty" in {
        val mockSubscriptionConnector                  = mock[TrustsConnector]
        val mockTrustsConnector: SubscriptionConnector = mock[SubscriptionConnector]
        val mockRepository                             = mock[CacheRepositoryImpl]
        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("exception message"))))
          )

        val OUT = new TrustsService(mockSubscriptionConnector, mockTrustsConnector, mockRepository)

        whenReady(OUT.getTrustInfoFormBundleNo("75464876").value) { formBundleNo =>
          formBundleNo mustBe Left(ServerError())
        }
      }

      "connector.getTrustInfo returns ServerError(message), where message is an empty string)" in {
        val mockSubscriptionConnector                  = mock[TrustsConnector]
        val mockTrustsConnector: SubscriptionConnector = mock[SubscriptionConnector]
        val mockRepository                             = mock[CacheRepositoryImpl]
        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError()))))

        val OUT = new TrustsService(mockSubscriptionConnector, mockTrustsConnector, mockRepository)

        whenReady(OUT.getTrustInfoFormBundleNo("75464876").value) { formBundleNo =>
          formBundleNo mustBe Left(ServerError())
        }
      }
    }
  }

  ".registerTrust" should {

    "return SuccessRegistrationResponse " when {
      "connector returns SuccessRegistrationResponse." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.registerTrust(registrationRequest))
          .thenReturn(
            EitherT[Future, TrustErrors, RegistrationResponse](
              Future.successful(Right(RegistrationTrnResponse("trn123")))
            )
          )
        val futureResult: Future[Either[TrustErrors, RegistrationResponse]] =
          SUT.registerTrust(registrationRequest).value
        whenReady(futureResult) { result =>
          result mustBe Right(RegistrationTrnResponse("trn123"))
        }
      }
    }

    "return AlreadyRegisteredResponse " when {
      "connector returns  AlreadyRegisteredResponse." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.registerTrust(registrationRequest)).thenReturn(
          EitherT[Future, TrustErrors, RegistrationResponse](Future.successful(Right(AlreadyRegisteredResponse)))
        )
        val futureResult: Future[Either[TrustErrors, RegistrationResponse]] =
          SUT.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(AlreadyRegisteredResponse)
        }
      }
    }

    "return same ServerError(message) " when {
      "connector handles an exception." in new TrustsServiceFixture {
        when(mockSubscriptionConnector.registerTrust(registrationRequest)).thenReturn(
          EitherT[Future, TrustErrors, RegistrationResponse](Future.successful(Left(ServerError("exception message"))))
        )
        val futureResult: Future[Either[TrustErrors, RegistrationResponse]] =
          SUT.registerTrust(registrationRequest).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("exception message"))
        }
      }
    }

  }

  ".getSubscriptionId" should {

    "return SubscriptionIdResponse " when {
      "connector returns SubscriptionIdResponse." in new TrustsServiceFixture {
        when(mockTrustsConnector.getSubscriptionId("trn123456789")).thenReturn(
          EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
            Future.successful(Right(SubscriptionIdSuccessResponse("123456789")))
          )
        )

        val futureResult: Future[Either[TrustErrors, SubscriptionIdSuccessResponse]] =
          SUT.getSubscriptionId("trn123456789").value

        whenReady(futureResult) { result =>
          result mustBe Right(SubscriptionIdSuccessResponse("123456789"))
        }
      }
    }

    "return the same ServerError(message) as connector" when {
      "connector recovers an exception." in new TrustsServiceFixture {
        when(mockTrustsConnector.getSubscriptionId("trn123456789")).thenReturn(
          EitherT[Future, TrustErrors, SubscriptionIdSuccessResponse](
            Future.successful(
              Left(ServerError("Error response from des 500"))
            )
          )
        )

        val futureResult: Future[Either[TrustErrors, SubscriptionIdSuccessResponse]] =
          SUT.getSubscriptionId("trn123456789").value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Error response from des 500"))
        }
      }
    }
  }

  ".resetCache" should {
    "return Right(Unit) when repository.resetCache is successful" in new TrustsServiceFixture {
      when(mockRepository.resetCache(any(), any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

      val utr                                             = "123456789"
      val futureResult: Future[Either[TrustErrors, Unit]] = SUT.resetCache(utr, myId, sessionId).value

      whenReady(futureResult) { result =>
        result mustBe Right(())
      }
    }

    "return trustErrors when repository.resetCache fails" in new TrustsServiceFixture {
      when(mockRepository.resetCache(any(), any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError("exception message")))))

      val utr                                             = "123456789"
      val futureResult: Future[Either[TrustErrors, Unit]] = SUT.resetCache(utr, myId, sessionId).value

      whenReady(futureResult) { result =>
        result mustBe Left(ServerError("exception message"))
      }
    }
  }

  ".getTrustInfo" should {

    "return TrustFoundResponse" when {

      "TrustFoundResponse is returned from DES Connector with a Processed flag and a trust body when not cached" in new TrustsServiceFixture {
        val utr                           = "1234567890"
        val fullEtmpResponseJson: JsValue = get5MLDTrustResponse
        val trustInfoJson: JsValue        = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Right(None))))

        when(mockRepository.resetCache(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, GetTrustResponse](
              Future.successful(
                Right(TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1")))
              )
            )
          )

        when(mockRepository.set(any(), any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.get_trust.TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1")))
          verify(mockRepository, times(1)).set(utr, myId, sessionId, fullEtmpResponseJson)
        }
      }

      "TrustFoundResponse is returned from repository with a Processed flag and a trust body when cached" in new TrustsServiceFixture {
        val utr = "1234567890"

        val fullEtmpResponseJson: JsValue = get5MLDTrustResponse
        val trustInfoJson: JsValue        = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String], any[String]))
          .thenReturn(
            EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Right(Some(fullEtmpResponseJson))))
          )
        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, GetTrustResponse](
              Future.failed(new Exception("Connector should not have been called"))
            )
          )

        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Right(models.get_trust.TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1")))
          verifyNoMoreInteractions(mockSubscriptionConnector)
        }
      }
    }

    "return TrustFoundResponse" when {

      "TrustFoundResponse is returned from DES Connector" in new TrustsServiceFixture {

        val utr = "1234567890"

        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, GetTrustResponse](
              Future.successful(
                Right(TrustFoundResponse(ResponseHeader("In Processing", "1")))
              )
            )
          )

        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Right(TrustFoundResponse(ResponseHeader("In Processing", "1")))
        }
      }
    }

    Seq(ResourceNotFoundResponse, BadRequestResponse, InternalServerErrorResponse, ServiceUnavailableResponse).foreach {
      getTrustErrorResponse =>
        s"return $getTrustErrorResponse" when {
          s"$getTrustErrorResponse is returned from DES Connector" in new TrustsServiceFixture {

            when(mockSubscriptionConnector.getTrustInfo(any()))
              .thenReturn(
                EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(getTrustErrorResponse)))
              )

            val utr                                                         = "123456789"
            val futureResult: Future[Either[TrustErrors, GetTrustResponse]] =
              SUT.getTrustInfo(utr, myId, sessionId).value

            whenReady(futureResult) { result =>
              result mustBe Right(getTrustErrorResponse)
            }
          }
        }
    }

    "return Left(ServerError())" when {
      "repository.set fails after getting trust info" in new TrustsServiceFixture {
        val utr                           = "1234567890"
        val fullEtmpResponseJson: JsValue = get5MLDTrustResponse
        val trustInfoJson: JsValue        = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Right(None))))

        when(mockRepository.resetCache(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, GetTrustResponse](
              Future.successful(
                Right(TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1")))
              )
            )
          )

        when(mockRepository.set(any(), any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }
      }

      "connector.getTrustInfo returns a ServerError(message), where message is nonEmpty" in new TrustsServiceFixture {
        when(mockRepository.get(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Right(None))))

        when(mockRepository.resetCache(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("exception message"))))
          )

        val utr                                                         = "123456789"
        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }
      }

      "connector.getTrustInfo returns ServerError(message), where message is an empty string" in new TrustsServiceFixture {
        when(mockRepository.get(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Right(None))))

        when(mockRepository.resetCache(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        when(mockSubscriptionConnector.getTrustInfo(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError()))))

        val utr                                                         = "123456789"
        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }
      }

      "repository.resetCache returns an error" in new TrustsServiceFixture {
        when(mockRepository.get(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Right(None))))

        when(mockRepository.resetCache(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val utr                                                         = "123456789"
        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }
      }

      "repository.get returns SeverError(message), where message is nonEmpty" in new TrustsServiceFixture {
        when(mockRepository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Left(ServerError()))))

        val utr                                                         = "123456789"
        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }
      }

      "repository.get returns ServerError(message), where message is an empty string" in new TrustsServiceFixture {
        when(mockRepository.get(any(), any(), any()))
          .thenReturn(
            EitherT[Future, TrustErrors, Option[JsValue]](
              Future.successful(Left(ServerError("operation failed due to exception")))
            )
          )

        val utr                                                         = "123456789"
        val futureResult: Future[Either[TrustErrors, GetTrustResponse]] = SUT.getTrustInfo(utr, myId, sessionId).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }
      }

    }
  }

  ".trustVariation" should {

    "return a VariationTvnResponse" when {

      "connector returns VariationResponse." in new TrustsServiceFixture {

        when(mockSubscriptionConnector.trustVariation(trustVariationsRequest)).thenReturn(
          EitherT[Future, TrustErrors, VariationSuccessResponse](
            Future.successful(Right(VariationSuccessResponse("tvn123")))
          )
        )

        val futureResult: Future[Either[TrustErrors, VariationSuccessResponse]] =
          SUT.trustVariation(trustVariationsRequest).value

        whenReady(futureResult) { result =>
          result mustBe Right(VariationSuccessResponse("tvn123"))
        }
      }
    }

    "return VariationFailureForAudit" when {
      "connector returns ServerError(message), where message is nonEmpty" in new TrustsServiceFixture {

        when(mockSubscriptionConnector.trustVariation(trustVariationsRequest)).thenReturn(
          EitherT[Future, TrustErrors, VariationSuccessResponse](
            Future.successful(Left(ServerError("Duplicate submission")))
          )
        )

        val futureResult: Future[Either[TrustErrors, VariationSuccessResponse]] =
          SUT.trustVariation(trustVariationsRequest).value

        whenReady(futureResult) { result =>
          result mustBe Left(VariationFailureForAudit(errors.InternalServerErrorResponse, "Duplicate submission"))
        }
      }
    }

    "return ServerError()" when {
      "connector returns ServerError(message), where message is an empty string" in new TrustsServiceFixture {

        when(mockSubscriptionConnector.trustVariation(trustVariationsRequest))
          .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Left(ServerError()))))

        val futureResult: Future[Either[TrustErrors, VariationSuccessResponse]] =
          SUT.trustVariation(trustVariationsRequest).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError())
        }

      }
    }
  }

}
