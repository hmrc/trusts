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
import uk.gov.hmrc.trusts.models.get_trust.get_trust.DisplayTrustNaturalPersonType
import uk.gov.hmrc.trusts.models.variation.NaturalPersonType
import uk.gov.hmrc.trusts.models.{NameType, Success}
import uk.gov.hmrc.trusts.services.OtherIndividualTransformationService
import uk.gov.hmrc.trusts.transformers.remove.RemoveOtherIndividual

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class OtherIndividualTransformationControllerSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers
  with GuiceOneAppPerSuite {

  lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  "remove otherIndividual" - {

    "add a new remove otherIndividual transform " in {

      val otherIndividualTransformationService: OtherIndividualTransformationService = mock[OtherIndividualTransformationService]

      when(otherIndividualTransformationService.removeOtherIndividual(any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val controller = new OtherIndividualTransformationController(identifierAction, otherIndividualTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj(
          "endDate" -> LocalDate.of(2018, 2, 24),
          "index" -> 24
        ))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.removeOtherIndividual("UTRUTRUTR").apply(request)

      status(result) mustBe OK
      verify(otherIndividualTransformationService)
        .removeOtherIndividual(
          equalTo("UTRUTRUTR"),
          equalTo("id"),
          equalTo(RemoveOtherIndividual(LocalDate.of(2018, 2, 24), 24)))(any())
    }

    "return an error when json is invalid" in {
      val OUT = new OtherIndividualTransformationController(identifierAction, mock[OtherIndividualTransformationService])(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj("field" -> "value"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = OUT.removeOtherIndividual("UTRUTRUTR")(request)

      status(result) mustBe BAD_REQUEST
    }

  }

  "Amend other individual" - {

    val index = 0

    "must add a new amend other individual transform" in {

      val otherIndividualTransformationService: OtherIndividualTransformationService = mock[OtherIndividualTransformationService]

      val newOtherIndividual = NaturalPersonType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("First", None, "Last"),
        dateOfBirth = None,
        identification = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      when(otherIndividualTransformationService.amendOtherIndividualTransformer(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val controller = new OtherIndividualTransformationController(identifierAction, otherIndividualTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newOtherIndividual))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendOtherIndividual("aUTR", index).apply(request)

      status(result) mustBe OK

      verify(otherIndividualTransformationService).amendOtherIndividualTransformer(
        equalTo("aUTR"),
        equalTo(index),
        equalTo("id"),
        equalTo(newOtherIndividual))(any())
    }

    "must return an error for malformed json" in {

      val otherIndividualTransformationService: OtherIndividualTransformationService = mock[OtherIndividualTransformationService]

      val controller = new OtherIndividualTransformationController(identifierAction, otherIndividualTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendOtherIndividual("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Add other individual" - {

    "must add a new add other  individual transform" in {
      val otherIndividualTransformationService = mock[OtherIndividualTransformationService]
      val controller = new OtherIndividualTransformationController(identifierAction, otherIndividualTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newOtherIndividual = DisplayTrustNaturalPersonType(
        None,
        None,
        name = NameType("First", None, "Last"),
        None,
        None,
        None,
        None,
        None,
        entityStart = LocalDate.parse("2000-01-01")
      )

      when(otherIndividualTransformationService.addOtherIndividualTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newOtherIndividual))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addOtherIndividual("aUTR").apply(request)

      status(result) mustBe OK
      verify(otherIndividualTransformationService).addOtherIndividualTransformer("aUTR", "id", newOtherIndividual)
    }

    "must return an error for malformed json" in {
      val otherIndividualTransformationService = mock[OtherIndividualTransformationService]
      val controller = new OtherIndividualTransformationController(identifierAction, otherIndividualTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addOtherIndividual("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

}
