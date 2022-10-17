/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.transformations.taxliability

import controllers.actions.FakeIdentifierAction
import models.YearsReturns
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.{TaxableMigrationService, TransformationService}
import transformers.taxliability.SetTaxLiabilityTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class TaxLiabilityTransformationControllerSpec extends AnyFreeSpec
  with MockitoSugar
  with ScalaFutures
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"

  private val mockTransformationService = mock[TransformationService]
  private val mockTaxableMigrationService = mock[TaxableMigrationService]

  override def beforeEach(): Unit = {
    reset(mockTransformationService)

    when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
      .thenReturn(Future.successful(Json.obj()))

    when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
      .thenReturn(Future.successful(true))

    reset(mockTaxableMigrationService)

    when(mockTaxableMigrationService.migratingFromNonTaxableToTaxable(any(), any(), any()))
      .thenReturn(Future.successful(false))
  }

  "Trust details transforms" - {

    "when setting years returns" - {

      "must return an OK" in {

        val controller = new TaxLiabilityTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(Json.obj()))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = Json.toJson(YearsReturns(None))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setYearsReturns(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTaxLiabilityTransform(Json.toJson(body)))
        )(any())
      }

      "return a BadRequest for malformed json" in {

        val controller = new TaxLiabilityTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.parse(
            """
              |{
              |  "returns": {
              |    "foo": "bar"
              |  }
              |}
              |""".stripMargin
          ))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setYearsReturns(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }
  }
}
