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

package controllers.transformations.assets

import controllers.actions.FakeIdentifierAction
import models.AddressType
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
import transformers.assets.AmendAssetTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AmendAssetControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)
  
  private val utr: String = "utr"
  private val index: Int = 0
  private val amount: Long = 1000L
  private val endDate: LocalDate = LocalDate.parse("2021-01-01")

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(assetType: String, assetData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "assets" \ assetType).json.put(JsArray(assetData))

    baseJson.as[JsObject](__.json.update(adder))
  }
  
  "Amend asset controller" - {

    "money asset" - {

      val originalAsset = AssetMonetaryAmountType(
        assetMonetaryAmount = amount
      )

      val amendedAsset = originalAsset.copy(
        assetMonetaryAmount = amount + 1
      )

      val assetType: String = "monetary"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(assetType, Seq(Json.toJson(originalAsset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedAsset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendMoney(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendAssetTransform(Some(index), Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendMoney(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "property or land asset" - {

      val originalAsset = PropertyLandType(
        buildingLandName = None,
        address = None,
        valueFull = amount,
        valuePrevious = None
      )

      val amendedAsset = originalAsset.copy(
        valueFull = amount + 1
      )

      val assetType: String = "propertyOrLand"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(assetType, Seq(Json.toJson(originalAsset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedAsset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendPropertyOrLand(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendAssetTransform(Some(index), Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendPropertyOrLand(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "shares asset" - {

      val originalAsset = SharesType(
        numberOfShares = None,
        orgName = "Name",
        utr = None,
        shareClass = None,
        typeOfShare = None,
        value = None
      )

      val amendedAsset = originalAsset.copy(
        orgName = "Amended Name"
      )

      val assetType: String = "shares"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(assetType, Seq(Json.toJson(originalAsset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedAsset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendShares(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendAssetTransform(Some(index), Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendShares(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "business asset" - {

      val originalAsset = BusinessAssetType(
        utr = None,
        orgName = "Name",
        businessDescription = "Description",
        address = None,
        businessValue = None
      )

      val amendedAsset = originalAsset.copy(
        orgName = "Amended Name"
      )

      val assetType: String = "business"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(assetType, Seq(Json.toJson(originalAsset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedAsset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendBusiness(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendAssetTransform(Some(index), Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
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

    "partnership asset" - {

      val originalAsset = PartnershipType(
        utr = None,
        description = "Description",
        partnershipStart = None
      )

      val amendedAsset = originalAsset.copy(
        description = "Amended Description"
      )

      val assetType: String = "partnerShip"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(assetType, Seq(Json.toJson(originalAsset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedAsset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendPartnership(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendAssetTransform(Some(index), Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendPartnership(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "other asset" - {

      val originalAsset = OtherAssetType(
        description = "Description",
        value = None
      )

      val amendedAsset = originalAsset.copy(
        description = "Amended Description"
      )

      val assetType: String = "other"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(assetType, Seq(Json.toJson(originalAsset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedAsset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendOther(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendAssetTransform(Some(index), Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendOther(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "non-EEA business asset" - {

      val originalAsset = NonEEABusinessType(
        lineNo = None,
        orgName = "Name",
        address = AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK"),
        govLawCountry = "UK",
        startDate = LocalDate.parse("2000-01-01"),
        endDate = None
      )

      val amendedAsset = originalAsset.copy(
        orgName = "Amended Name"
      )

      val assetType: String = "nonEEABusiness"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(assetType, Seq(Json.toJson(originalAsset)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedAsset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendNonEeaBusiness(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendAssetTransform(Some(index), Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendAssetController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendNonEeaBusiness(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}
