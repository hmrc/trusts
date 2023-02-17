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

package controllers.transformations.assets

import controllers.actions.FakeIdentifierAction
import models.AddressType
import models.variation._
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.{TaxableMigrationService, TransformationService}
import transformers.assets.AddAssetTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AddAssetControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"
  private val amount: Long = 1000L

  private val invalidBody: JsValue = Json.parse("{}")

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

  "Add asset controller" - {

    "money asset" - {

      val asset = AssetMonetaryAmountType(
        assetMonetaryAmount = amount
      )

      val assetType: String = "monetary"

      "must add a new add transform" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(asset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addMoney(utr).apply(request)

        status(result) mustBe OK

        val transform = AddAssetTransform(Json.toJson(asset), assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addMoney(utr).apply(request)

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

      val assetType: String = "propertyOrLand"

      "must add a new add transform" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(asset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addPropertyOrLand(utr).apply(request)

        status(result) mustBe OK

        val transform = AddAssetTransform(Json.toJson(asset), assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addPropertyOrLand(utr).apply(request)

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

      val assetType: String = "shares"

      "must add a new add transform" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(asset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addShares(utr).apply(request)

        status(result) mustBe OK

        val transform = AddAssetTransform(Json.toJson(asset), assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addShares(utr).apply(request)

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

      val assetType: String = "business"

      "must add a new add transform" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(asset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addBusiness(utr).apply(request)

        status(result) mustBe OK

        val transform = AddAssetTransform(Json.toJson(asset), assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addBusiness(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "partnership asset" - {

      val asset = PartnershipType(
        utr = None,
        description = "Description",
        partnershipStart = None
      )

      val assetType: String = "partnerShip"

      "must add a new add transform" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(asset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addPartnership(utr).apply(request)

        status(result) mustBe OK

        val transform = AddAssetTransform(Json.toJson(asset), assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addPartnership(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "other asset" - {

      val asset = OtherAssetType(
        description = "Description",
        value = None
      )

      val assetType: String = "other"

      "must add a new add transform" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(asset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addOther(utr).apply(request)

        status(result) mustBe OK

        val transform = AddAssetTransform(Json.toJson(asset), assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addOther(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "non-EEA business asset" - {

      val asset = NonEEABusinessType(
        lineNo = Some("1"),
        orgName = "Name",
        address = AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK"),
        govLawCountry = "UK",
        startDate = LocalDate.parse("2000-01-01"),
        endDate = None
      )

      val assetType: String = "nonEEABusiness"

      "must add a new add transform" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(asset))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addNonEeaBusiness(utr).apply(request)

        status(result) mustBe OK

        val transform = AddAssetTransform(Json.toJson(asset), assetType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }

      "must return an error for invalid json" in {

        val controller = new AddAssetController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addNonEeaBusiness(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}
