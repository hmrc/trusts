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
import services.TransformationService
import transformers.remove.RemoveSettlor
import transformers.settlors.RemoveSettlorTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class RemoveSettlorControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)
  
  private val utr: String = "utr"
  private val index: Int = 0
  private val endDate: LocalDate = LocalDate.parse("2018-02-24")

  private def removeSettlor(settlorType: String): RemoveSettlor = RemoveSettlor(
    endDate = endDate,
    index = index,
    `type` = settlorType
  )

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(settlorType: String, settlorData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "settlors" \ settlorType).json.put(JsArray(settlorData))

    baseJson.as[JsObject](__.json.update(adder))
  }
  
  "Remove settlor controller" - {

    "individual settlor" - {

      val settlor = Settlor(
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

      val settlorType: String = "settlor"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveSettlorController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(settlorType, Seq(Json.toJson(settlor)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeSettlor(settlorType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveSettlorTransform(index, Json.toJson(settlor), endDate, settlorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }
    }

    "business settlor" - {

      val settlor = SettlorCompany(
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

      val settlorType: String = "settlorCompany"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveSettlorController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(settlorType, Seq(Json.toJson(settlor)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeSettlor(settlorType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveSettlorTransform(index, Json.toJson(settlor), endDate, settlorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }
    }

    "return an error for invalid json" in {

      val mockTransformationService = mock[TransformationService]

      val controller = new RemoveSettlorController(
        identifierAction,
        mockTransformationService
      )(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest(POST, "path")
        .withBody(invalidBody)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.remove(utr)(request)

      status(result) mustBe BAD_REQUEST

    }
  }
}
