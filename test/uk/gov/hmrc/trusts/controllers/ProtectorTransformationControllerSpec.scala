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
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustProtector, DisplayTrustProtectorCompany}
import uk.gov.hmrc.trusts.models.variation.{Protector, ProtectorCompany}
import uk.gov.hmrc.trusts.models.{NameType, RemoveProtector, Success}
import uk.gov.hmrc.trusts.services.ProtectorTransformationService

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class ProtectorTransformationControllerSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers
  with GuiceOneAppPerSuite {

  lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  "Add individual protector" - {

    "must add a new individual protector transform" in {

      val protectorTransformationService: ProtectorTransformationService = mock[ProtectorTransformationService]

      val newProtector = DisplayTrustProtector(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("First", None, "Last"),
        dateOfBirth = None,
        identification = None,
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        entityStart = LocalDate.parse("2010-05-03")
      )

      when(protectorTransformationService.addIndividualProtectorTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newProtector))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addIndividualProtector("aUTR").apply(request)

      status(result) mustBe OK
      verify(protectorTransformationService).addIndividualProtectorTransformer(
        equalTo("aUTR"),
        equalTo("id"),
        equalTo(newProtector))
    }

    "must return an error for malformed json" in {

      val protectorTransformationService: ProtectorTransformationService = mock[ProtectorTransformationService]

      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addIndividualProtector("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Add business protector" - {

    "must add a new business protector transform" in {

      val protectorTransformationService = mock[ProtectorTransformationService]
      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newCompanyProtector = DisplayTrustProtectorCompany(
        name = "TestCompany",
        identification = None,
        lineNo = None,
        bpMatchStatus = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("2010-05-03")
      )

      when(protectorTransformationService.addBusinessProtectorTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newCompanyProtector))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addBusinessProtector("aUTR").apply(request)

      status(result) mustBe OK
      verify(protectorTransformationService).addBusinessProtectorTransformer(
        equalTo("aUTR"),
        equalTo("id"),
        equalTo(newCompanyProtector))
    }

    "must return an error for malformed json" in {

      val protectorTransformationService = mock[ProtectorTransformationService]
      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addBusinessProtector("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Amend individual protector" - {

    val index = 0

    "must add a new amend individual protector transform" in {

      val protectorTransformationService: ProtectorTransformationService = mock[ProtectorTransformationService]

      val newProtector = Protector(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("First", None, "Last"),
        dateOfBirth = None,
        identification = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      when(protectorTransformationService.amendIndividualProtectorTransformer(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newProtector))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendIndividualProtector("aUTR", index).apply(request)

      status(result) mustBe OK

      verify(protectorTransformationService).amendIndividualProtectorTransformer(
        equalTo("aUTR"),
        equalTo(index),
        equalTo("id"),
        equalTo(newProtector))(any())
    }

    "must return an error for malformed json" in {

      val protectorTransformationService: ProtectorTransformationService = mock[ProtectorTransformationService]

      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendIndividualProtector("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }

  }

  "Amend business protector" - {

    val index = 0

    "must add a new amend business protector transform" in {

      val protectorTransformationService = mock[ProtectorTransformationService]
      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newCompany = ProtectorCompany(
        None,
        None,
        "Company Name",
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(protectorTransformationService.amendBusinessProtectorTransformer(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newCompany))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendBusinessProtector("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(protectorTransformationService)
        .amendBusinessProtectorTransformer(
          equalTo("aUTR"),
          equalTo(index),
          equalTo("id"),
          equalTo(newCompany)
        )(any())

    }

    "must return an error for malformed json" in {
      val protectorTransformationService = mock[ProtectorTransformationService]
      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendBusinessProtector("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "remove protector" - {

    "add a new remove protector transform " in {

      val protectorTransformationService: ProtectorTransformationService = mock[ProtectorTransformationService]

      when(protectorTransformationService.removeProtector(any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj(
          "type" -> "protector",
          "endDate" -> LocalDate.of(2018, 2, 24),
          "index" -> 24
        ))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.removeProtector("UTRUTRUTR").apply(request)

      status(result) mustBe OK
      verify(protectorTransformationService)
        .removeProtector(
          equalTo("UTRUTRUTR"),
          equalTo("id"),
          equalTo(RemoveProtector(LocalDate.of(2018, 2, 24), 24, "protector")))(any())
    }

    "return an error when json is invalid" in {
      val OUT = new ProtectorTransformationController(identifierAction, mock[ProtectorTransformationService])(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj("field" -> "value"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = OUT.removeProtector("UTRUTRUTR")(request)

      status(result) mustBe BAD_REQUEST
    }

  }
}
