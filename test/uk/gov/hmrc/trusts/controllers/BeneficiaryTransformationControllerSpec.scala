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

package uk.gov.hmrc.trusts.controllers

import java.time.LocalDate

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
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.variation._
import uk.gov.hmrc.trusts.models.{AddressType, NameType, Success}
import uk.gov.hmrc.trusts.services.BeneficiaryTransformationService
import uk.gov.hmrc.trusts.transformers.remove.RemoveBeneficiary

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class BeneficiaryTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  "Amend unidentified beneficiary" - {

    val index = 0

    "must add a new amend unidentified beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newDescription = "Some new description"

      when(beneficiaryTransformationService.amendUnidentifiedBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newDescription))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendUnidentifiedBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).amendUnidentifiedBeneficiaryTransformer(
        equalTo("aUTR"),
        equalTo(index),
        equalTo("id"),
        equalTo(newDescription))
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendUnidentifiedBeneficiary("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "remove Beneficiary" - {
    "add an new remove beneficiary transform " in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      when(beneficiaryTransformationService.removeBeneficiary(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj(
          "type" -> "unidentified",
          "endDate" -> LocalDate.of(2018, 2, 24),
          "index" -> 24
        ))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.removeBeneficiary("UTRUTRUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService)
        .removeBeneficiary(
          equalTo("UTRUTRUTR"),
          equalTo("id"),
          equalTo(RemoveBeneficiary(LocalDate.of(2018, 2, 24), 24, "unidentified")))
    }

    "return an error when json is invalid" in {
      val OUT = new BeneficiaryTransformationController(identifierAction, mock[BeneficiaryTransformationService])(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj("field" -> "value"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = OUT.removeBeneficiary("UTRUTRUTR")(request)

      status(result) mustBe BAD_REQUEST
    }

  }

  "Add unidentified beneficiary" - {

    "must add a new add unidentified beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newBeneficiary = UnidentifiedType(
        None,
        None,
        "Some description",
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.addUnidentifiedBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addUnidentifiedBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addUnidentifiedBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addUnidentifiedBeneficiary("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Amend individual beneficiary" - {

    val index = 0

    "must add a new amend individual beneficiary transform" in {

      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newIndividual = IndividualDetailsType(
        None,
        None,
        NameType("First", None, "Last"),
        None,
        vulnerableBeneficiary = Some(false),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.amendIndividualBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newIndividual))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendIndividualBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService)
        .amendIndividualBeneficiaryTransformer(
          equalTo("aUTR"),
          equalTo(index),
          equalTo("id"),
          equalTo(newIndividual)
        )

    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendIndividualBeneficiary("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }

  }

  "Add individual beneficiary" - {

    "must add a new add individual beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newBeneficiary = IndividualDetailsType(None,
        None,
        NameType("First", None, "Last"),
        Some(LocalDate.parse("2000-01-01")),
        vulnerableBeneficiary = Some(false),
        None,
        None,
        None,
        Some(IdentificationType(Some("nino"), None, None, None)),
        None,
        None,
        None,
        LocalDate.parse("1990-10-10"),
        None
      )

      when(beneficiaryTransformationService.addIndividualBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addIndividualBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addIndividualBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addIndividualBeneficiary("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Add charity beneficiary" - {

    "must add a new add charity beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newBeneficiary = BeneficiaryCharityType(
        None,
        None,
        "Charity",
        Some(false),
        Some("50"),
        Some(IdentificationOrgType(None, Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")), None)),
        None,
        LocalDate.parse("1990-10-10"),
        None
      )

      when(beneficiaryTransformationService.addCharityBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addCharityBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addCharityBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addCharityBeneficiary("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Amend charity beneficiary" - {

    val index = 0

    "must add a new amend charity beneficiary transform" in {

      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newCharity = BeneficiaryCharityType(
        None,
        None,
        "Charity Name",
        None,
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.amendCharityBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newCharity))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendCharityBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService)
        .amendCharityBeneficiaryTransformer(
          equalTo("aUTR"),
          equalTo(index),
          equalTo("id"),
          equalTo(newCharity)
        )

    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendCharityBeneficiary("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }

  }

  "Add other beneficiary" - {

    "must add a new add other beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newBeneficiary = OtherType(
        None,
        None,
        "Other",
        Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
        Some(false),
        None,
        None,
        LocalDate.parse("1990-10-10"),
        None
      )

      when(beneficiaryTransformationService.addOtherBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addOtherBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addOtherBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addOtherBeneficiary("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Add company beneficiary" - {

    "must add a new add company beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newBeneficiary = BeneficiaryCompanyType(
        None,
        None,
        "Organisation Name",
        Some(false),
        Some("50"),
        Some(IdentificationOrgType(
          Some("company utr"),
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")), None)),
        None,
        LocalDate.parse("1990-10-10"),
        None
      )

      when(beneficiaryTransformationService.addCompanyBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addCompanyBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addCompanyBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addCompanyBeneficiary("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Amend company beneficiary" - {

    val index = 0

    "must add a new amend company beneficiary transform" in {

      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newCompany = BeneficiaryCompanyType(
        None,
        None,
        "Company Name",
        None,
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.amendCompanyBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newCompany))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendCompanyBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService)
        .amendCompanyBeneficiaryTransformer(
          equalTo("aUTR"),
          equalTo(index),
          equalTo("id"),
          equalTo(newCompany)
        )

    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendCompanyBeneficiary("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }

  }

  "Add trust beneficiary" - {

    "must add a new add trust beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newBeneficiary = BeneficiaryTrustType(
        None,
        None,
        "Organisation Name",
        Some(false),
        Some("50"),
        Some(IdentificationOrgType(
          Some("company utr"),
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")), None)),
        None,
        LocalDate.parse("1990-10-10"),
        None
      )

      when(beneficiaryTransformationService.addTrustBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addTrustBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addTrustBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addTrustBeneficiary("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }


  "Amend other beneficiary" - {

    val index = 0

    "must add a new amend other beneficiary transform" in {

      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newOther = OtherType(
        None,
        None,
        "Other Name",
        None,
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.amendOtherBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newOther))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendOtherBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService)
        .amendOtherBeneficiaryTransformer(
          equalTo("aUTR"),
          equalTo(index),
          equalTo("id"),
          equalTo(newOther)
        )

    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendOtherBeneficiary("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Amend trust beneficiary" - {

    val index = 0

    "must add a new amend trust beneficiary transform" in {

      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newTrust = BeneficiaryTrustType(
        None,
        None,
        "Trust Name",
        None,
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.amendTrustBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newTrust))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendTrustBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService)
        .amendTrustBeneficiaryTransformer(
          equalTo("aUTR"),
          equalTo(index),
          equalTo("id"),
          equalTo(newTrust)
        )

    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendTrustBeneficiary("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Add large beneficiary" - {

    "must add a new add large beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newBeneficiary = LargeType(
        None,
        None,
        "Name",
        "Description",
        None,
        None,
        None,
        None,
        "501",
        Some(IdentificationOrgType(
          None,
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
          None
        )),
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.addLargeBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addLargeBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addLargeBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addLargeBeneficiary("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "Amend large beneficiary" - {

    val index = 0

    "must add a new amend large beneficiary transform" in {

      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val newLargeBeneficiary = LargeType(
        None,
        None,
        "Name",
        "Description",
        None,
        None,
        None,
        None,
        "501",
        Some(IdentificationOrgType(
          None,
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
          None
        )),
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.amendLargeBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newLargeBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendLargeBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService)
        .amendLargeBeneficiaryTransformer(
          equalTo("aUTR"),
          equalTo(index),
          equalTo("id"),
          equalTo(newLargeBeneficiary)
        )

    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendLargeBeneficiary("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}