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

package controllers.transformations.beneficiaries

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
import services.{LocalDateService, TransformationService}
import transformers.beneficiaries.AmendBeneficiaryTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AmendBeneficiaryControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)
  
  private val utr: String = "utr"
  private val index: Int = 0
  private val startDate: LocalDate = LocalDate.parse("2020-01-01")
  private val endDate: LocalDate = LocalDate.parse("2021-01-01")

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(beneficiaryType: String, beneficiaryData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "beneficiary" \ beneficiaryType).json.put(JsArray(beneficiaryData))

    baseJson.as[JsObject](__.json.update(adder))
  }
  
  "Amend beneficiary controller" - {

    "unidentified beneficiary" - {

      val originalBeneficiary: UnidentifiedType = UnidentifiedType(
        lineNo = None,
        bpMatchStatus = None,
        description = "Description",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        entityStart = startDate,
        entityEnd = None
      )

      val amendedBeneficiary: String = "Amended Description"

      val beneficiaryType: String = "unidentified"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(originalBeneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedBeneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendUnidentified(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendBeneficiaryTransform(Some(index), Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendUnidentified(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "individual beneficiary" - {

      val originalBeneficiary = IndividualDetailsType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("Joe", None, "Bloggs"),
        dateOfBirth = None,
        vulnerableBeneficiary = None,
        beneficiaryType = None,
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = None,
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        entityStart = startDate,
        entityEnd = None
      )

      val amendedBeneficiary = originalBeneficiary.copy(
        name = NameType("John", None, "Doe")
      )

      val beneficiaryType: String = "individualDetails"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(originalBeneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedBeneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendIndividual(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendBeneficiaryTransform(Some(index), Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendIndividual(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "charity beneficiary" - {

      val originalBeneficiary = BeneficiaryCharityType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val amendedBeneficiary = originalBeneficiary.copy(
        organisationName = "Amended Name"
      )

      val beneficiaryType: String = "charity"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(originalBeneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedBeneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendCharity(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendBeneficiaryTransform(Some(index), Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendCharity(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "other beneficiary" - {

      val originalBeneficiary = OtherType(
        lineNo = None,
        bpMatchStatus = None,
        description = "Description",
        address = None,
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val amendedBeneficiary = originalBeneficiary.copy(
        description = "Amended Description"
      )

      val beneficiaryType: String = "other"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(originalBeneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedBeneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendOther(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendBeneficiaryTransform(Some(index), Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
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

    "company beneficiary" - {

      val originalBeneficiary = BeneficiaryCompanyType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val amendedBeneficiary = originalBeneficiary.copy(
        organisationName = "Amended Name"
      )

      val beneficiaryType: String = "company"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(originalBeneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedBeneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendCompany(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendBeneficiaryTransform(Some(index), Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendCompany(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "trust beneficiary" - {

      val originalBeneficiary = BeneficiaryTrustType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val amendedBeneficiary = originalBeneficiary.copy(
        organisationName = "Amended Name"
      )

      val beneficiaryType: String = "trust"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(originalBeneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedBeneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendTrust(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendBeneficiaryTransform(Some(index), Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendTrust(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "large beneficiary" - {

      val originalBeneficiary = LargeType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        description = "Description",
        description1 = None,
        description2 = None,
        description3 = None,
        description4 = None,
        numberOfBeneficiary = "501",
        identification = None,
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val amendedBeneficiary = originalBeneficiary.copy(
        organisationName = "Amended Name"
      )

      val beneficiaryType: String = "large"

      "must add a new amend transform" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(originalBeneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockLocalDateService.now).thenReturn(endDate)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(amendedBeneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendLarge(utr, index).apply(request)

        status(result) mustBe OK

        val transform = AmendBeneficiaryTransform(Some(index), Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new AmendBeneficiaryController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendLarge(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}
