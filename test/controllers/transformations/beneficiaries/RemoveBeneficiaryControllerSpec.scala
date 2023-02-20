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

package controllers.transformations.beneficiaries

import controllers.actions.FakeIdentifierAction
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
import transformers.beneficiaries.RemoveBeneficiaryTransform
import transformers.remove.RemoveBeneficiary
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class RemoveBeneficiaryControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"
  private val index: Int = 0
  private val startDate: LocalDate = LocalDate.parse("2020-01-01")
  private val endDate: LocalDate = LocalDate.parse("2021-01-01")

  private def removeBeneficiary(beneficiaryType: String): RemoveBeneficiary = RemoveBeneficiary(
    endDate = endDate,
    index = index,
    `type` = beneficiaryType
  )

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(beneficiaryType: String, beneficiaryData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "beneficiary" \ beneficiaryType).json.put(JsArray(beneficiaryData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Remove beneficiary controller" - {

    "unidentified beneficiary" - {

      val beneficiary = UnidentifiedType(
        lineNo = None,
        bpMatchStatus = None,
        description = "Description",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        entityStart = startDate,
        entityEnd = None
      )

      val beneficiaryType: String = "unidentified"

      "must add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(beneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = removeBeneficiary(beneficiaryType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveBeneficiaryTransform(Some(index), Json.toJson(beneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "individual beneficiary" - {

      val beneficiary = IndividualDetailsType(
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

      val beneficiaryType: String = "individualDetails"

      "must add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(beneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = removeBeneficiary(beneficiaryType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveBeneficiaryTransform(Some(index), Json.toJson(beneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "charity beneficiary" - {

      val beneficiary = BeneficiaryCharityType(
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

      val beneficiaryType: String = "charity"

      "must add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(beneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = removeBeneficiary(beneficiaryType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveBeneficiaryTransform(Some(index), Json.toJson(beneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "other beneficiary" - {

      val beneficiary = OtherType(
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

      val beneficiaryType: String = "other"

      "must add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(beneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = removeBeneficiary(beneficiaryType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveBeneficiaryTransform(Some(index), Json.toJson(beneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "company beneficiary" - {

      val beneficiary = BeneficiaryCompanyType(
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

      val beneficiaryType: String = "company"

      "must add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(beneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = removeBeneficiary(beneficiaryType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveBeneficiaryTransform(Some(index), Json.toJson(beneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "trust beneficiary" - {

      val beneficiary = BeneficiaryTrustType(
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

      val beneficiaryType: String = "trust"

      "must add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(beneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = removeBeneficiary(beneficiaryType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveBeneficiaryTransform(Some(index), Json.toJson(beneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "large beneficiary" - {

      val beneficiary = LargeType(
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

      val beneficiaryType: String = "large"

      "must add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(beneficiaryType, Seq(Json.toJson(beneficiary)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = removeBeneficiary(beneficiaryType)

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveBeneficiaryTransform(Some(index), Json.toJson(beneficiary), endDate, beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "must return an error for invalid json" in {

      val mockTransformationService = mock[TransformationService]

      val controller = new RemoveBeneficiaryController(
        identifierAction,
        mockTransformationService
      )(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest(POST, "path")
        .withBody(invalidBody)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.remove(utr).apply(request)

      status(result) mustBe BAD_REQUEST

    }
  }
}
