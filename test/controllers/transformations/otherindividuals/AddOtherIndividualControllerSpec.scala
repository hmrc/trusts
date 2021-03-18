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

package controllers.transformations.otherindividuals

import controllers.actions.FakeIdentifierAction
import models.NameType
import models.variation.NaturalPersonType
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services.TransformationService
import transformers.otherindividuals.AddOtherIndividualTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AddOtherIndividualControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"

  private val invalidBody: JsValue = Json.parse("{}")

  private val otherIndividual = NaturalPersonType(
    lineNo = None,
    bpMatchStatus = None,
    name = NameType("Joe", None, "Bloggs"),
    dateOfBirth = None,
    identification = None,
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None,
    entityStart = LocalDate.parse("2010-05-03"),
    entityEnd = None
  )
  
  "Add other individual controller" - {

    "must add a new add transform" in {

      val mockTransformationService = mock[TransformationService]

      val controller = new AddOtherIndividualController(
        identifierAction,
        mockTransformationService
      )(Implicits.global, Helpers.stubControllerComponents())

      when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
        .thenReturn(Future.successful(Json.obj()))

      when(mockTransformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest(POST, "path")
        .withBody(Json.toJson(otherIndividual))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.add(utr).apply(request)

      status(result) mustBe OK

      val transform = AddOtherIndividualTransform(Json.toJson(otherIndividual))

      verify(mockTransformationService)
        .addNewTransform(equalTo(utr), any(), equalTo(transform))

    }

    "must return an error for invalid json" in {

      val mockTransformationService = mock[TransformationService]

      val controller = new AddOtherIndividualController(
        identifierAction,
        mockTransformationService
      )(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest(POST, "path")
        .withBody(invalidBody)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.add(utr).apply(request)

      status(result) mustBe BAD_REQUEST

    }
  }
}
