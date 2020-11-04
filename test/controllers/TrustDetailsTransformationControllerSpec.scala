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

package controllers

import controllers.actions.FakeIdentifierAction
import models.Success
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.BodyParsers
import play.api.test.{FakeRequest, Helpers}
import services.TrustDetailsTransformationService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import play.api.test.Helpers.{CONTENT_TYPE, _}

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class TrustDetailsTransformationControllerSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers
  with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  "Trust details transforms" - {

    "when setting express question" - {

      "must return an OK" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setExpressTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setExpress("aUTR").apply(request)

        status(result) mustBe OK

        verify(service).setExpressTransformer(
          equalTo("aUTR"),
          equalTo("id"),
          equalTo(true))
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setExpressTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setExpress("aUTR").apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting property question" - {

      "must return an OK" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setPropertyTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setProperty("aUTR").apply(request)

        status(result) mustBe OK

        verify(service).setPropertyTransformer(
          equalTo("aUTR"),
          equalTo("id"),
          equalTo(true))
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setPropertyTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setProperty("aUTR").apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting recorded question" - {

      "must return an OK" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setRecordedTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setRecorded("aUTR").apply(request)

        status(result) mustBe OK

        verify(service).setRecordedTransformer(
          equalTo("aUTR"),
          equalTo("id"),
          equalTo(true))
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setRecordedTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setRecorded("aUTR").apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting resident question" - {

      "must return an OK" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setResidentTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResident("aUTR").apply(request)

        status(result) mustBe OK

        verify(service).setResidentTransformer(
          equalTo("aUTR"),
          equalTo("id"),
          equalTo(true))
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setResidentTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResident("aUTR").apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting taxable question" - {

      "must return an OK" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setTaxableTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTaxable("aUTR").apply(request)

        status(result) mustBe OK

        verify(service).setTaxableTransformer(
          equalTo("aUTR"),
          equalTo("id"),
          equalTo(true))
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setTaxableTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTaxable("aUTR").apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting uk relation question" - {

      "must return an OK" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setUKRelationTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setUKRelation("aUTR").apply(request)

        status(result) mustBe OK

        verify(service).setUKRelationTransformer(
          equalTo("aUTR"),
          equalTo("id"),
          equalTo(true))
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TrustDetailsTransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.setUKRelationTransformer(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setUKRelation("aUTR").apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

  }

}