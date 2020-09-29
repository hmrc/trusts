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
import org.mockito.Mockito._
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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.{NameType, RemoveTrustee, Success}
import uk.gov.hmrc.trusts.services.{LocalDateService, TrusteeTransformationService}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with GuiceOneAppPerSuite {

  lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  object LocalDateServiceStub extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  "amend lead trustee" - {

    "must add a new amend lead trustee transform" in {
      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())

      val newTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = LocalDate.of(1965, 2, 10),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
        entityStart = Some(LocalDate.parse("2012-03-14"))
      )

      when(trusteeTransformationService.addAmendLeadTrusteeTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val newTrusteeInfo = DisplayTrustLeadTrusteeType(Some(newTrusteeIndInfo), None)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newTrusteeIndInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendLeadTrustee("aUTR").apply(request)

      status(result) mustBe OK
      verify(trusteeTransformationService).addAmendLeadTrusteeTransformer("aUTR", "id", newTrusteeInfo)
    }

    "must return an error for malformed json" in {
      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendLeadTrustee("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "add trustee" - {

    "must add a new add trustee transform" in {

      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())

      val newTrusteeIndInfo = DisplayTrustTrusteeIndividualType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = Some(LocalDate.of(1965, 2, 10)),
        phoneNumber = Some("newPhone"),
        identification = Some(DisplayTrustIdentificationType(None, Some("newNino"), None, None)),
        entityStart = LocalDate.parse("2012-03-14")
      )

      when(trusteeTransformationService.addAddTrusteeTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val newTrusteeInfo = DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newTrusteeIndInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addTrustee("aUTR").apply(request)

      status(result) mustBe OK
      verify(trusteeTransformationService).addAddTrusteeTransformer("aUTR", "id", newTrusteeInfo)
    }

    "must return an error for malformed json" in {
      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addTrustee("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "promote trustee" - {

    "must add a new promote trustee transform" in {

      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())
      val index = 3

      val newTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = LocalDate.of(1965, 2, 10),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
        entityStart = Some(LocalDate.parse("2012-03-14"))
      )

      when(trusteeTransformationService.addPromoteTrusteeTransformer(any(), any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val newTrusteeInfo = DisplayTrustLeadTrusteeType(Some(newTrusteeIndInfo), None)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newTrusteeIndInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.promoteTrustee("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(trusteeTransformationService).addPromoteTrusteeTransformer(
        equalTo("aUTR"),
        equalTo("id"),
        equalTo(index),
        equalTo(newTrusteeInfo),
        any())
    }

    "must return an error for malformed json" in {
      val trusteeTransformationService = mock[TrusteeTransformationService]

      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())
      val index = 3

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.promoteTrustee("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "remove trustee" - {

    "must add a 'remove trustee' transform" in {

      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())

      val payload = RemoveTrustee(
        endDate = LocalDate.parse("2020-01-10"), index = 0
      )

      when(trusteeTransformationService.addRemoveTrusteeTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("DELETE", "path")
        .withBody(Json.toJson(payload))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.removeTrustee("aUTR").apply(request)

      status(result) mustBe OK
      verify(trusteeTransformationService).addRemoveTrusteeTransformer(
        equalTo("aUTR"),
        equalTo("id"),
        equalTo(payload))

    }
  }

  "amend trustee" - {

    val index = 0

    "must add a new amend trustee transform for a trustee ind" in {
      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())

      val newTrusteeIndInfo = DisplayTrustTrusteeIndividualType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = Some(LocalDate.of(1965, 2, 10)),
        phoneNumber = Some("newPhone"),
        identification = Some(DisplayTrustIdentificationType(None, Some("newNino"), None, None)),
        entityStart = LocalDate.parse("2012-03-14")
      )

      when(trusteeTransformationService.addAmendTrusteeTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val newTrusteeInfo = DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newTrusteeIndInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendTrustee("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(trusteeTransformationService).addAmendTrusteeTransformer(
        equalTo("aUTR"),
        equalTo(index),
        equalTo("id"),
        equalTo(newTrusteeInfo)
      )
    }

    "must add a new amend trustee transform for a trustee org" in {
      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())

      val newTrusteeOrgInfo = DisplayTrustTrusteeOrgType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = "newFirstName newLastName",
        phoneNumber = Some("newPhone"),
        email = Some("newEmail"),
        identification = Some(DisplayTrustIdentificationOrgType(None, Some("newUtr"), None)),
        entityStart = LocalDate.parse("2012-03-14")
      )

      when(trusteeTransformationService.addAmendTrusteeTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val newTrusteeInfo = DisplayTrustTrusteeType(None, Some(newTrusteeOrgInfo))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newTrusteeOrgInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendTrustee("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(trusteeTransformationService).addAmendTrusteeTransformer(
        equalTo("aUTR"),
        equalTo(index),
        equalTo("id"),
        equalTo(newTrusteeInfo)
      )

    }

    "must return an error for malformed json" in {
      val trusteeTransformationService = mock[TrusteeTransformationService]
      val controller = new TrusteeTransformationController(identifierAction, trusteeTransformationService, LocalDateServiceStub)(ExecutionContext.Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendTrustee("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}
