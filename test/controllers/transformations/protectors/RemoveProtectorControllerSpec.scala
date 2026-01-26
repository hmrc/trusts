/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import controllers.actions.FakeIdentifierAction
import errors.{ServerError, TrustErrors}
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
import transformers.protectors.RemoveProtectorTransform
import transformers.remove.RemoveProtector
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class RemoveProtectorControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String        = "utr"
  private val index: Int         = 0
  private val endDate: LocalDate = LocalDate.parse("2018-02-24")

  private def removeProtector(protectorType: String): RemoveProtector = RemoveProtector(
    endDate = endDate,
    index = index,
    `type` = protectorType
  )

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(protectorType: String, protectorData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "protectors" \ protectorType).json.put(JsArray(protectorData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Remove protector controller" - {

    "individual protector" - {

      val protector = ProtectorIndividual(
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

      val protectorType: String = "protector"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveProtectorController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, JsObject](
              Future.successful(Right(buildInputJson(protectorType, Seq(Json.toJson(protector)))))
            )
          )

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeProtector(protectorType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveProtectorTransform(Some(index), Json.toJson(protector), endDate, protectorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an Internal Server Error when getTransformedTrustJson fails" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveProtectorController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Left(ServerError()))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeProtector(protectorType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

      }
    }

    "business protector" - {

      val protector = ProtectorCompany(
        lineNo = None,
        bpMatchStatus = None,
        name = "Name",
        identification = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      val protectorType: String = "protectorCompany"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveProtectorController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, JsObject](
              Future.successful(Right(buildInputJson(protectorType, Seq(Json.toJson(protector)))))
            )
          )

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeProtector(protectorType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveProtectorTransform(Some(index), Json.toJson(protector), endDate, protectorType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an Internal Server Error when addNewTransform fails" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveProtectorController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(
            EitherT[Future, TrustErrors, JsObject](
              Future.successful(Right(buildInputJson(protectorType, Seq(Json.toJson(protector)))))
            )
          )

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val body = removeProtector(protectorType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

      }
    }

    "return an error for invalid json" in {

      val mockTransformationService = mock[TransformationService]

      val controller = new RemoveProtectorController(
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
