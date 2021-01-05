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

package controllers

import controllers.actions.FakeIdentifierAction
import models.variation._
import models.{AddressType, Success}
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.AssetsTransformationService
import transformers.remove.RemoveAsset
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AssetsTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)
  
  private val utr: String = "utr"
  private val index: Int = 0
  private val amount: Long = 1000L

  private def removeAsset(assetType: String): RemoveAsset = RemoveAsset(
    endDate = LocalDate.parse("2018-02-24"),
    index = index,
    `type` = assetType
  )

  private val invalidBody: JsValue = Json.parse("{}")
  
  "Assets Transformation Controller" - {

    "money asset" - {

      val asset = AssetMonetaryAmount(
        assetMonetaryAmount = amount
      )

      "add" - {

        "must add a new add transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.addAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addMoney(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .addAsset(equalTo(utr), any(), equalTo(asset))(any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addMoney(utr).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "amend" - {

        "must add a new amend transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.amendAsset(any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Success))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendMoney(utr, index).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .amendAsset(equalTo(utr), equalTo(index), any(), equalTo(asset))(any(), any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendMoney(utr, index).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "remove" - {

        "add a new remove transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.removeAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(Success))

          val body = removeAsset("monetary")

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(body))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .removeAsset(equalTo(utr), any(), equalTo(body))(any())

        }

        "return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr)(request)

          status(result) mustBe BAD_REQUEST

        }
      }
    }

    "property or land asset" - {

      val asset = PropertyLandType(
        buildingLandName = None,
        address = None,
        valueFull = amount,
        valuePrevious = None
      )

      "add" - {

        "must add a new add transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.addAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addPropertyOrLand(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .addAsset(equalTo(utr), any(), equalTo(asset))(any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addPropertyOrLand(utr).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "amend" - {

        "must add a new amend transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.amendAsset(any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Success))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendPropertyOrLand(utr, index).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .amendAsset(equalTo(utr), equalTo(index), any(), equalTo(asset))(any(), any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendPropertyOrLand(utr, index).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "remove" - {

        "add a new remove transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.removeAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(Success))

          val body = removeAsset("propertyOrLand")

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(body))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .removeAsset(equalTo(utr), any(), equalTo(body))(any())

        }

        "return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr)(request)

          status(result) mustBe BAD_REQUEST

        }
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

      "add" - {

        "must add a new add transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.addAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addShares(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .addAsset(equalTo(utr), any(), equalTo(asset))(any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addShares(utr).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "amend" - {

        "must add a new amend transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.amendAsset(any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Success))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendShares(utr, index).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .amendAsset(equalTo(utr), equalTo(index), any(), equalTo(asset))(any(), any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendShares(utr, index).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "remove" - {

        "add a new remove transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.removeAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(Success))

          val body = removeAsset("shares")

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(body))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .removeAsset(equalTo(utr), any(), equalTo(body))(any())

        }

        "return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr)(request)

          status(result) mustBe BAD_REQUEST

        }
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

      "add" - {

        "must add a new add transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.addAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addBusiness(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .addAsset(equalTo(utr), any(), equalTo(asset))(any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addBusiness(utr).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "amend" - {

        "must add a new amend transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.amendAsset(any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Success))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendBusiness(utr, index).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .amendAsset(equalTo(utr), equalTo(index), any(), equalTo(asset))(any(), any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendBusiness(utr, index).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "remove" - {

        "add a new remove transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.removeAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(Success))

          val body = removeAsset("business")

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(body))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .removeAsset(equalTo(utr), any(), equalTo(body))(any())

        }

        "return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr)(request)

          status(result) mustBe BAD_REQUEST

        }
      }
    }

    "partnership asset" - {

      val asset = PartnershipType(
        utr = None,
        description = "Description",
        partnershipStart = None
      )

      "add" - {

        "must add a new add transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.addAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addPartnership(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .addAsset(equalTo(utr), any(), equalTo(asset))(any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addPartnership(utr).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "amend" - {

        "must add a new amend transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.amendAsset(any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Success))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendPartnership(utr, index).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .amendAsset(equalTo(utr), equalTo(index), any(), equalTo(asset))(any(), any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendPartnership(utr, index).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "remove" - {

        "add a new remove transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.removeAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(Success))

          val body = removeAsset("partnerShip")

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(body))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .removeAsset(equalTo(utr), any(), equalTo(body))(any())

        }

        "return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr)(request)

          status(result) mustBe BAD_REQUEST

        }
      }
    }

    "other asset" - {

      val asset = OtherAssetType(
        description = "Description",
        value = None
      )

      "add" - {

        "must add a new add transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.addAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addOther(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .addAsset(equalTo(utr), any(), equalTo(asset))(any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addOther(utr).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "amend" - {

        "must add a new amend transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.amendAsset(any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Success))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendOther(utr, index).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .amendAsset(equalTo(utr), equalTo(index), any(), equalTo(asset))(any(), any())

        }

        "must return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendOther(utr, index).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }

      "remove" - {

        "add a new remove transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.removeAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(Success))

          val body = removeAsset("other")

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(body))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .removeAsset(equalTo(utr), any(), equalTo(body))(any())

        }

        "return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr)(request)

          status(result) mustBe BAD_REQUEST

        }
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
      
      "add" - {

        "must add a new add transform" in {
          
          val mockAssetsTransformationService = mock[AssetsTransformationService]
          
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.addAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addNonEeaBusiness(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .addAsset(equalTo(utr), any(), equalTo(asset))(any())

        }

        "must return an error for invalid json" in {
          
          val mockAssetsTransformationService = mock[AssetsTransformationService]
          
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.addNonEeaBusiness(utr).apply(request)

          status(result) mustBe BAD_REQUEST

        }
      }
      
      "amend" - {

        "must add a new amend transform" in {
          
          val mockAssetsTransformationService = mock[AssetsTransformationService]
          
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.amendAsset(any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Success))

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(asset))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendNonEeaBusiness(utr, index).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .amendAsset(equalTo(utr), equalTo(index), any(), equalTo(asset))(any(), any())

        }

        "must return an error for invalid json" in {
          
          val mockAssetsTransformationService = mock[AssetsTransformationService]
          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(POST, "path")
            .withBody(invalidBody)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.amendNonEeaBusiness(utr, index).apply(request)
          
          status(result) mustBe BAD_REQUEST

        }
      }
      
      "remove" - {

        "add a new remove transform" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockAssetsTransformationService.removeAsset(any(), any(), any())(any()))
            .thenReturn(Future.successful(Success))

          val body = removeAsset("nonEEABusiness")

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(body))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.removeAsset(utr).apply(request)

          status(result) mustBe OK

          verify(mockAssetsTransformationService)
            .removeAsset(equalTo(utr), any(), equalTo(body))(any())

        }

        "return an error for invalid json" in {

          val mockAssetsTransformationService = mock[AssetsTransformationService]

          val controller = new AssetsTransformationController(
            identifierAction,
            mockAssetsTransformationService
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
}