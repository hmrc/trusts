/*
 * Copyright 2019 HM Revenue & Customs
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

import org.mockito.Mockito.when
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.ExistingTrustResponse._
import uk.gov.hmrc.trusts.models.{ExistingTrustCheckRequest, RegistrationTrustResponse, SubscriptionIdResponse}

import scala.concurrent.Future

class DesServiceSpec extends BaseSpec {

  lazy val request = ExistingTrustCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")
  val mockConnector = mock[DesConnector]

  val SUT = new DesServiceImpl(mockConnector)

  ".checkExistingTrust" should {

    "return Matched " when {
      "connector returns Matched." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(Matched))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe Matched
        }
      }
    }


    "return NotMatched " when {
      "connector returns NotMatched." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(NotMatched))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(BadRequest))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(AlreadyRegistered))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServiceUnavailable))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }

      }
    }

    "return ServerError " when {
      "connector returns ServerError." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServerError))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }


  ".registerTrust" should {

    "return SuccessRegistrationResponse " when {
      "connector returns SuccessRegistrationResponse." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.successful(RegistrationTrustResponse("trn123")))
        val futureResult = SUT.registerTrust(registrationRequest)
        whenReady(futureResult) {
          result => result mustBe RegistrationTrustResponse("trn123")
        }
      }
    }

    "return AlreadyRegisteredException " when {
      "connector returns  AlreadyRegisteredException." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(AlreadyRegisteredException))
          val futureResult = SUT.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(InternalServerErrorException("")))
          val futureResult = SUT.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  } //registerTrust

  ".getSubscriptionId" should {

    "return SubscriptionIdResponse " when {
      "connector returns SubscriptionIdResponse." in {
        when(mockConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        val futureResult = SUT.getSubscriptionId("trn123456789")
        whenReady(futureResult) {
          result => result mustBe SubscriptionIdResponse("123456789")
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in {
        when(mockConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.failed(new InternalServerErrorException("")))
        val futureResult = SUT.getSubscriptionId("trn123456789")

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  } //getSubscriptionId


}
