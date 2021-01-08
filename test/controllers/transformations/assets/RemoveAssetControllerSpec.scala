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

package controllers.transformations.assets

import controllers.actions.FakeIdentifierAction
import models.AddressType
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
import transformers.assets.RemoveAssetTransform
import transformers.remove.RemoveAsset
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class RemoveAssetControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)
  
  private val utr: String = "utr"
  private val index: Int = 0
  private val amount: Long = 1000L
  private val endDate: LocalDate = LocalDate.parse("2018-02-24")

  private def removeAsset(assetType: String): RemoveAsset = RemoveAsset(
    endDate = endDate,
    index = index,
    `type` = assetType
  )

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(assetType: String, assetData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "assets" \ assetType).json.put(JsArray(assetData))

    baseJson.as[JsObject](__.json.update(adder))
  }
  
  "Assets Transformation Controller" - {

    "money asset" - {

      val asset = AssetMonetaryAmount(
        assetMonetaryAmount = amount
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(asset.toString, Seq(Json.toJson(asset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeAsset("monetary")

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(index, Json.toJson(asset), endDate, asset.toString)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr)(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "property or land asset" - {

      val asset = PropertyLandType(
        buildingLandName = None,
        address = None,
        valueFull = amount,
        valuePrevious = None
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(asset.toString, Seq(Json.toJson(asset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeAsset("propertyOrLand")

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(index, Json.toJson(asset), endDate, asset.toString)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr)(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "shares asset" - {

      val asset = SharesType(
        numberOfShares = None,
        orgName = "Name",
        utr = None,
        shareClass = None,
        typeOfShare = None,
        value = None
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(asset.toString, Seq(Json.toJson(asset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeAsset("shares")

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(index, Json.toJson(asset), endDate, asset.toString)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr)(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "business asset" - {

      val asset = BusinessAssetType(
        utr = None,
        orgName = "Name",
        businessDescription = "Description",
        address = None,
        businessValue = None
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(asset.toString, Seq(Json.toJson(asset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeAsset("business")

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(index, Json.toJson(asset), endDate, asset.toString)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr)(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "partnership asset" - {

      val asset = PartnershipType(
        utr = None,
        description = "Description",
        partnershipStart = None
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(asset.toString, Seq(Json.toJson(asset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeAsset("partnerShip")

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(index, Json.toJson(asset), endDate, asset.toString)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr)(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "other asset" - {

      val asset = OtherAssetType(
        description = "Description",
        value = None
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(asset.toString, Seq(Json.toJson(asset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeAsset("other")

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(index, Json.toJson(asset), endDate, asset.toString)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr)(request)

        status(result) mustBe BAD_REQUEST

      }
    }
    
    "non-EEA business asset" - {

      val asset = NonEEABusinessType(
        lineNo = "1",
        orgName = "Name",
        address = AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK"),
        govLawCountry = "UK",
        startDate = LocalDate.parse("2000-01-01"),
        endDate = None
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(asset.toString, Seq(Json.toJson(asset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val body = removeAsset("nonEEABusiness")

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(index, Json.toJson(asset), endDate, asset.toString)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.removeAsset(utr)(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}