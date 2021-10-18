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

import java.time.LocalDateTime

import base.BaseSpec
import connector.NonRepudiationConnector
import models.nonRepudiation.{NRSSubmission, SuccessfulNrsResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import utils.JsonFixtures

import scala.concurrent.Future

class NonRepudiationServiceSpec extends BaseSpec with JsonFixtures {


  private val mockConnector = mock[NonRepudiationConnector]
  private val mockLocalDateTimeService = mock[LocalDateTimeService]
  private val SUT = new NonRepudiationService(mockConnector, mockLocalDateTimeService)

  ".register" should {

    "return SuccessfulNrsResponse" in {

      lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[NRSSubmission])

        when(mockConnector.nonRepudiate(payloadCaptor.capture())(any())).
          thenReturn(Future.successful(SuccessfulNrsResponse("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

      when(mockLocalDateTimeService.now).thenReturn(LocalDateTime.of(2021, 10, 18, 12, 5))

      val payLoad = Json.toJson(registrationRequest)

        val fResult = SUT.register(payLoad)
        whenReady(fResult) { result =>
          result mustBe SuccessfulNrsResponse("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
          payloadCaptor.getValue.payload mustBe ""
          payloadCaptor.getValue.metadata.businessId mustBe "trs"
          payloadCaptor.getValue.metadata.notableEvent mustBe "trs-registration"
          payloadCaptor.getValue.metadata.payloadContentType mustBe "application/json; charset=utf-8"
          payloadCaptor.getValue.metadata.searchKeys mustBe "trn"
        }
      }
    }
}

