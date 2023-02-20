/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.transformations.protectors

import controllers.actions.FakeIdentifierAction
import models.NameType
import models.variation._
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.TransformationService
import services.dates.LocalDateService
import transformers.protectors.AmendProtectorTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AmendProtectorControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"
  private val index: Int = 0
  private val endDate: LocalDate = LocalDate.parse("2021-01-01")

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(protectorType: String, protectorData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "protectors" \ protectorType).json.put(JsArray(protectorData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Amend protector controller" - {

    "individual protector" - {

      val originalProtector = ProtectorIndividual(
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

      val amendedProtector = originalProtector.copy(
        name = NameType("John", None, "Doe")
      )

      val protectorType: String = "protector"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendProtectorController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(protectorType, Seq(Json.toJson(originalProtector)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedProtector))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendIndividual(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendProtectorTransform(Some(index), Json.toJson(amendedProtector), Json.toJson(originalProtector), endDate, protectorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendProtectorController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendIndividual(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "business protector" - {

      val originalProtector = ProtectorCompany(
        lineNo = None,
        bpMatchStatus = None,
        name = "Name",
        identification = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      val amendedProtector = originalProtector.copy(
        name = "Amended Name"
      )

      val protectorType: String = "protectorCompany"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendProtectorController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(protectorType, Seq(Json.toJson(originalProtector)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedProtector))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendBusiness(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendProtectorTransform(Some(index), Json.toJson(amendedProtector), Json.toJson(originalProtector), endDate, protectorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendProtectorController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendBusiness(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}
