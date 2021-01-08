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

package controllers

import controllers.actions.FakeIdentifierAction
import models.{NonUKType, ResidentialStatusType, Success}
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsBoolean, JsString, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.TransformationService
import transformers.trustdetails.SetTrustDetailTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class TrustDetailsTransformationControllerSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers
  with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"

  "Trust details transforms" - {

    "when setting express question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setExpress(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "expressTrust"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setExpress(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting property question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setProperty(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustUKProperty"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setProperty(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting recorded question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setRecorded(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustRecorded"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setRecorded(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting resident question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResident(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustUKResident"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResident(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting taxable question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTaxable(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustTaxable"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTaxable(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting uk relation question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setUKRelation(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustUKRelation"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setUKRelation(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting law country question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val body = JsString("FR")

        val request = FakeRequest(POST, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setLawCountry(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "lawCountry"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setLawCountry(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting administration country question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val body = JsString("FR")

        val request = FakeRequest(POST, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setAdministrationCountry(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "administrationCountry"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setAdministrationCountry(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting type of trust question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val body = JsString("Employment Related")

        val request = FakeRequest(POST, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTypeOfTrust(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "typeOfTrust"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTypeOfTrust(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting deed of variation question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val body = JsString("Replaced the will trust")

        val request = FakeRequest(POST, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setDeedOfVariation(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "deedOfVariation"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setDeedOfVariation(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting inter vivos question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val body = JsBoolean(true)

        val request = FakeRequest(POST, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setInterVivos(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "interVivos"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setInterVivos(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting EFRBS start date question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val body = Json.toJson(LocalDate.parse("2021-01-01"))

        val request = FakeRequest(POST, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setEfrbsStartDate(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "efrbsStartDate"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setEfrbsStartDate(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting residential status question" - {

      "must return an OK" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val body = Json.toJson(ResidentialStatusType(None, Some(NonUKType(sch5atcgga92 = true, None, None, None))))

        val request = FakeRequest(POST, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResidentialStatus(utr).apply(request)

        status(result) mustBe OK

        verify(service).set(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "residentialStatus"))
        )
      }

      "return an BadRequest for malformed json" in {
        val service = mock[TransformationService]

        val controller = new TrustDetailsTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.set(any(), any(), any()))
          .thenReturn(Future.successful(Success))

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse(
            """
              |{
              |  "uk": {
              |    "foo": "bar"
              |  }
              |}
              |""".stripMargin
          ))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResidentialStatus(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }
  }
}
