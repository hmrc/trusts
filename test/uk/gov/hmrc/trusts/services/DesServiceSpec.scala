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
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.ExistingTrustResponse._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class DesServiceSpec extends BaseSpec {

  lazy val request = ExistingTrustCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")
  val mockConnector = mock[DesConnector]

  val SUT = new DesServiceImpl(mockConnector)

  ".checkExistingTrust" should {

    "return Matched " when {
      "connector returns Matched." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(Matched))
        val result = Await.result(SUT.checkExistingTrust(request), Duration.Inf)
        result mustBe Matched
      }
    }


    "return NotMatched " when {
      "connector returns NotMatched." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(NotMatched))
        val result = Await.result(SUT.checkExistingTrust(request), Duration.Inf)
        result mustBe NotMatched
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(BadRequest))
        val result = Await.result(SUT.checkExistingTrust(request), Duration.Inf)
        result mustBe BadRequest
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(AlreadyRegistered))
        val result = Await.result(SUT.checkExistingTrust(request), Duration.Inf)
        result mustBe AlreadyRegistered
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServiceUnavailable))
        val result = Await.result(SUT.checkExistingTrust(request), Duration.Inf)
        result mustBe ServiceUnavailable
      }
    }

    "return ServerError " when {
      "connector returns ServerError." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServerError))
        val result = Await.result(SUT.checkExistingTrust(request), Duration.Inf)
        result mustBe ServerError
      }
    }
  }


  ".registerTrust" should {

    "return SuccessRegistrationResponse " when {
      "connector returns SuccessRegistrationResponse." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.successful(RegistrationTrustResponse("trn123")))
        val result = Await.result(SUT.registerTrust(registrationRequest), Duration.Inf)
        result mustBe RegistrationTrustResponse("trn123")
      }
    }

    "throw AlreadyRegisteredException " when {
      "connector returns  AlreadyRegisteredException." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(new AlreadyRegisteredException))
         assertThrows[AlreadyRegisteredException] {
          val result = Await.result(SUT.registerTrust(registrationRequest), Duration.Inf)
        }
      }
    }

    "throws same Exception " when {
      "connector returns  exception." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(new InternalServerErrorException("")))
        assertThrows[InternalServerErrorException] {
          val result = Await.result(SUT.registerTrust(registrationRequest), Duration.Inf)
        }
      }
    }

  }//registerTrust

}
