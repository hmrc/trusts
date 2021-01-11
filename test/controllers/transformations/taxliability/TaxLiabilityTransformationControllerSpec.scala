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

package controllers.transformations.taxliability

import controllers.actions.FakeIdentifierAction
import models.YearsReturns
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
import services.TransformationService
import transformers.taxliability.SetTaxLiabilityTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class TaxLiabilityTransformationControllerSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers
  with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"

  "Trust details transforms" - {

    "when setting years returns" - {

      "must return an OK" in {

        val service = mock[TransformationService]

        val controller = new TaxLiabilityTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = Json.toJson(YearsReturns(None))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setYearsReturns(utr).apply(request)

        status(result) mustBe OK

        verify(service).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTaxLiabilityTransform(Json.toJson(body)))
        )
      }

      "return an BadRequest for malformed json" in {

        val service = mock[TransformationService]

        val controller = new TaxLiabilityTransformationController(identifierAction, service)(Implicits.global, Helpers.stubControllerComponents())

        when(service.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

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
