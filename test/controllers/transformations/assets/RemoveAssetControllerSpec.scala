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

package controllers.transformations.assets

import cats.data.EitherT
import controllers.actions.FakeIdentifierAction
import errors.{ServerError, TrustErrors}
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
import transformers.assets.RemoveAssetTransform
import transformers.remove.RemoveAsset
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class RemoveAssetControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

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

  "Remove asset controller" - {

    "money asset" - {

      val asset = AssetMonetaryAmountType(
        assetMonetaryAmount = amount
      )

      val assetType: String = "monetary"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Right(buildInputJson(assetType, Seq(Json.toJson(asset)))))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(Some(index), Json.toJson(asset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an Internal Server Error when addNewTransform fails" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Right(buildInputJson(assetType, Seq(Json.toJson(asset)))))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError("exception message")))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

      }
    }

    "property or land asset" - {

      val asset = PropertyLandType(
        buildingLandName = None,
        address = None,
        valueFull = amount,
        valuePrevious = None
      )

      val assetType: String = "propertyOrLand"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Right(buildInputJson(assetType, Seq(Json.toJson(asset)))))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(Some(index), Json.toJson(asset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an Internal Server Error when getTransformedTrustJson fails" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Left(ServerError()))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

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

      val assetType: String = "shares"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Right(buildInputJson(assetType, Seq(Json.toJson(asset)))))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(Some(index), Json.toJson(asset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

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

      val assetType: String = "business"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Right(buildInputJson(assetType, Seq(Json.toJson(asset)))))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(Some(index), Json.toJson(asset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "partnership asset" - {

      val asset = PartnershipType(
        utr = None,
        description = "Description",
        partnershipStart = None
      )

      val assetType: String = "partnerShip"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Right(buildInputJson(assetType, Seq(Json.toJson(asset)))))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(Some(index), Json.toJson(asset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "other asset" - {

      val asset = OtherAssetType(
        description = "Description",
        value = None
      )

      val assetType: String = "other"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Right(buildInputJson(assetType, Seq(Json.toJson(asset)))))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(Some(index), Json.toJson(asset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "non-EEA business asset" - {

      val asset = NonEEABusinessType(
        lineNo = None,
        orgName = "Name",
        address = AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK"),
        govLawCountry = "UK",
        startDate = LocalDate.parse("2000-01-01"),
        endDate = None
      )

      val assetType: String = "nonEEABusiness"

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveAssetController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, JsObject](Future.successful(Right(buildInputJson(assetType, Seq(Json.toJson(asset)))))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val body = removeAsset(assetType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveAssetTransform(Some(index), Json.toJson(asset), endDate, assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
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

      val result = controller.remove(utr)(request)

      status(result) mustBe BAD_REQUEST

    }
  }
}
