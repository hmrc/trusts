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

package uk.gov.hmrc.trusts.controllers

import java.time.LocalDate

import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.{variation, _}
import uk.gov.hmrc.trusts.services.SettlorTransformationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SettlorTransformationControllerSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers {

  val identifierAction = new FakeIdentifierAction(Agent)

  "Amend individual settlor" - {

    val index = 0

    "must add a new amend individual settlor transform" in {

      val service = mock[SettlorTransformationService]

      val controller = new SettlorTransformationController(identifierAction, service)

      val newSettlor = variation.Settlor(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("First", None, "Last"),
        dateOfBirth = None,
        identification = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      when(service.amendIndividualSettlorTransformer(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newSettlor))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendIndividualSettlor("aUTR", index).apply(request)

      status(result) mustBe OK

      verify(service).amendIndividualSettlorTransformer(
        equalTo("aUTR"),
        equalTo(index),
        equalTo("id"),
        equalTo(newSettlor))(any())
    }

    "must return an error for malformed json" in {
      val service = mock[SettlorTransformationService]
      val controller = new SettlorTransformationController(identifierAction, service)

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendIndividualSettlor("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Amend business settlor" - {

    val index = 0

    "must add a new amend business settlor transform" in {

      val service = mock[SettlorTransformationService]

      val controller = new SettlorTransformationController(identifierAction, service)

      val newSettlor = variation.SettlorCompany(
        lineNo = None,
        bpMatchStatus = None,
        name = "Company",
        companyType = None,
        companyTime = None,
        identification = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      when(service.amendBusinessSettlorTransformer(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newSettlor))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendBusinessSettlor("aUTR", index).apply(request)

      status(result) mustBe OK

      verify(service).amendBusinessSettlorTransformer(
        equalTo("aUTR"),
        equalTo(index),
        equalTo("id"),
        equalTo(newSettlor))(any())
    }

    "must return an error for malformed json" in {
      val service = mock[SettlorTransformationService]
      val controller = new SettlorTransformationController(identifierAction, service)

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendBusinessSettlor("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }

  }

  "remove Settlor" - {
    "add an new remove settlor transform " in {
      val settlorTransformationService = mock[SettlorTransformationService]
      val controller = new SettlorTransformationController(identifierAction, settlorTransformationService)

      when(settlorTransformationService.removeSettlor(any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj(
          "type" -> "settlor",
          "endDate" -> LocalDate.of(2018, 2, 24),
          "index" -> 24
        ))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.removeSettlor("UTRUTRUTR").apply(request)

      status(result) mustBe OK
      verify(settlorTransformationService)
        .removeSettlor(
          equalTo("UTRUTRUTR"),
          equalTo("id"),
          equalTo(RemoveSettlor(LocalDate.of(2018, 2, 24), 24, "settlor")))(any())
    }

    "return an error when json is invalid" in {
      val OUT = new SettlorTransformationController(identifierAction, mock[SettlorTransformationService])

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj("field" -> "value"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = OUT.removeSettlor("UTRUTRUTR")(request)

      status(result) mustBe BAD_REQUEST
    }

  }

  "Amend deceased settlor" - {

    "must add a new amend deceased settlor transform" in {

      val service = mock[SettlorTransformationService]

      val controller = new SettlorTransformationController(identifierAction, service)

      val amendedSettlor = variation.WillType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("First", None, "Last"),
        dateOfBirth = None,
        dateOfDeath = Some(LocalDate.parse("2015-05-03")),
        identification = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      when(service.amendDeceasedSettlor(any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(amendedSettlor))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendDeceasedSettlor("aUTR").apply(request)

      status(result) mustBe OK

      verify(service).amendDeceasedSettlor(
        equalTo("aUTR"),
        equalTo("id"),
        equalTo(amendedSettlor))(any())
    }

    "must return an error for malformed json" in {
      val service = mock[SettlorTransformationService]
      val controller = new SettlorTransformationController(identifierAction, service)

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendDeceasedSettlor("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}