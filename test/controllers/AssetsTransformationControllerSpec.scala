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

import java.time.LocalDate

import controllers.actions.FakeIdentifierAction
import models.variation._
import models.{AddressType, Success}
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.AssetsTransformationService
import transformers.remove.RemoveAsset
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AssetsTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "Amend nonEeaBusinessAsset" - {

    val nonEEABusiness = NonEEABusinessType(
      "1",
      "TestOrg",
      AddressType(
        "Line 1",
        "Line 2",
        None,
        None,
        Some("NE11NE"), "UK"),
      "UK",
      LocalDate.parse("2000-01-01"),
      None
    )

    val index = 0

    "must add a new amend nonEeaBusinessAssetJson transform" in {
      val nonEeaBusinessAssetTransformationService = mock[AssetsTransformationService]
      val controller = new AssetsTransformationController(identifierAction, nonEeaBusinessAssetTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      when(nonEeaBusinessAssetTransformationService.amendNonEeaBusinessAssetTransformer(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(nonEEABusiness))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendNonEeaBusiness("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(nonEeaBusinessAssetTransformationService).amendNonEeaBusinessAssetTransformer(
        equalTo("aUTR"),
        equalTo(index),
        equalTo("id"),
        equalTo(nonEEABusiness)
      )(any())
    }

    "must return an error for malformed json" in {
      val nonEeaBusinessAssetTransformationService = mock[AssetsTransformationService]
      val controller = new AssetsTransformationController(identifierAction, nonEeaBusinessAssetTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendNonEeaBusiness("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "remove nonEeaBusinessAsset" - {

    "add a new remove nonEeaBusinessAsset transform " in {
      val nonEeaBusinessAssetTransformationService = mock[AssetsTransformationService]
      val controller = new AssetsTransformationController(identifierAction, nonEeaBusinessAssetTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      when(nonEeaBusinessAssetTransformationService.removeAsset(any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj(
          "type" -> "nonEEABusiness",
          "endDate" -> LocalDate.of(2018, 2, 24),
          "index" -> 24
        ))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.removeAsset("UTRUTRUTR").apply(request)

      status(result) mustBe OK
      verify(nonEeaBusinessAssetTransformationService)
        .removeAsset(
          equalTo("UTRUTRUTR"),
          equalTo("id"),
          equalTo(RemoveAsset(LocalDate.of(2018, 2, 24), 24, "nonEEABusiness"))
        )(any())
    }

    "return an error when json is invalid" in {
      val OUT = new AssetsTransformationController(identifierAction, mock[AssetsTransformationService])(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj("field" -> "value"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = OUT.removeAsset("UTRUTRUTR")(request)

      status(result) mustBe BAD_REQUEST
    }

  }

  "Add nonEeaBusinessAsset" - {

    "must add a new add nonEeaBusinessAsset transform" in {
      val nonEeaBusinessAssetTransformationService = mock[AssetsTransformationService]
      val controller = new AssetsTransformationController(identifierAction, nonEeaBusinessAssetTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newNonEeaBusinessAsset = NonEEABusinessType("1",
        "TestOrg",
        AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK"),
        "UK",
        LocalDate.parse("2000-01-01"),
        None
      )

      when(nonEeaBusinessAssetTransformationService.addNonEeaBusinessAssetTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newNonEeaBusinessAsset))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addNonEeaBusiness("aUTR").apply(request)

      status(result) mustBe OK
      verify(nonEeaBusinessAssetTransformationService).addNonEeaBusinessAssetTransformer("aUTR", "id", newNonEeaBusinessAsset)
    }

    "must return an error for malformed json" in {
      val nonEeaBusinessAssetTransformationService = mock[AssetsTransformationService]
      val controller = new AssetsTransformationController(identifierAction, nonEeaBusinessAssetTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addNonEeaBusiness("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}