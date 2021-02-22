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

package controllers.transformations.settlors

import controllers.actions.FakeIdentifierAction
import models.NameType
import models.variation._
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.{LocalDateService, TransformationService}
import transformers.settlors.AmendSettlorTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AmendSettlorControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)
  
  private val utr: String = "utr"
  private val index: Int = 0
  private val endDate: LocalDate = LocalDate.parse("2021-01-01")

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(settlorType: String, settlorData: JsValue): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "settlors" \ settlorType).json.put(JsArray(Seq(settlorData)))

    baseJson.as[JsObject](__.json.update(adder))
  }
  
  "Amend settlor controller" - {

    "individual settlor" - {

      val originalSettlor = SettlorIndividual(
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

      val amendedSettlor = originalSettlor.copy(
        name = NameType("John", None, "Doe")
      )

      val settlorType: String = "settlor"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendSettlorController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(settlorType, Json.toJson(originalSettlor))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedSettlor))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendIndividual(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendSettlorTransform(Some(index), Json.toJson(amendedSettlor), Json.toJson(originalSettlor), endDate, settlorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendSettlorController(
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

    "business settlor" - {

      val originalSettlor = SettlorCompany(
        lineNo = None,
        bpMatchStatus = None,
        name = "Name",
        companyType = None,
        companyTime = None,
        identification = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      val amendedSettlor = originalSettlor.copy(
        name = "Amended Name"
      )

      val settlorType: String = "settlorCompany"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendSettlorController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(settlorType, Json.toJson(originalSettlor))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedSettlor))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendBusiness(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendSettlorTransform(Some(index), Json.toJson(amendedSettlor), Json.toJson(originalSettlor), endDate, settlorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendSettlorController(
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

    "deceased settlor" - {

      val originalSettlor = Json.parse(
        """
          |{
          |  "lineNo":"1",
          |  "bpMatchStatus": "01",
          |  "name":{
          |    "firstName":"John",
          |    "middleName":"William",
          |    "lastName":"O'Connor"
          |  },
          |  "dateOfBirth":"1956-02-12",
          |  "dateOfDeath":"2016-01-01",
          |  "identification":{
          |    "nino":"KC456736"
          |  },
          |  "entityStart":"1998-02-12"
          |}
          |""".stripMargin)

      val amendedSettlor = AmendDeceasedSettlor(
        name = NameType("John", None, "Doe"),
        dateOfBirth = None,
        dateOfDeath = None,
        identification = None
      )

      val settlorType: String = "deceased"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendSettlorController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(desResponse.as[JsObject]))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedSettlor))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendDeceased(utr).apply(request)

        status(result) mustBe OK

          val transform = AmendSettlorTransform(None, Json.toJson(amendedSettlor), Json.toJson(originalSettlor), endDate, settlorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendSettlorController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendDeceased(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}
