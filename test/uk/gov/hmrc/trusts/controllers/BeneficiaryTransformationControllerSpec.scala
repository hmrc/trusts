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

import org.joda.time.DateTime
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.variation.{IdentificationType, IndividualDetailsType, UnidentifiedType}
import uk.gov.hmrc.trusts.models.{NameType, RemoveBeneficiary, Success}
import uk.gov.hmrc.trusts.services.BeneficiaryTransformationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BeneficiaryTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {
  val identifierAction = new FakeIdentifierAction(Agent)

  "Amend unidentified beneficiary" - {

    val index = 0

    "must add a new amend unidentified beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

      val newDescription = "Some new description"

      when(beneficiaryTransformationService.amendUnidentifiedBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(()))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newDescription))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendUnidentifiedBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).amendUnidentifiedBeneficiaryTransformer("aUTR", index, "id", newDescription)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

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
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

      when(beneficiaryTransformationService.removeBeneficiary(any(), any(), any())(any()))
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
          equalTo(RemoveBeneficiary(LocalDate.of(2018, 2, 24), 24, "unidentified")))(any())
    }

    "return an error when json is invalid" in {
      val OUT = new BeneficiaryTransformationController(identifierAction, mock[BeneficiaryTransformationService])

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
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

      val newBeneficiary = UnidentifiedType(
        None,
        None,
        "Some description",
        None,
        None,
        DateTime.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.addUnidentifiedBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(()))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addUnidentifiedBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addUnidentifiedBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

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
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

      val newIndividual = IndividualDetailsType(
        None,
        None,
        NameType("First", None, "Last"),
        None,
        vulnerableBeneficiary = false,
        None,
        None,
        None,
        None,
        DateTime.parse("2010-01-01"),
        None
      )

      when(beneficiaryTransformationService.amendIndividualBeneficiaryTransformer(any(), any(), any(), any())(any()))
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
        )(any())

    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

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
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

      val newBeneficiary = IndividualDetailsType(None,
        None,
        NameType("First", None, "Last"),
        Some(DateTime.parse("2000-01-01")),
        false,
        None,
        None,
        None,
        Some(IdentificationType(Some("nino"), None, None, None)),
        DateTime.parse("1990-10-10"),
        None
      )

      when(beneficiaryTransformationService.addIndividualBeneficiaryTransformer(any(), any(), any()))
        .thenReturn(Future.successful(()))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newBeneficiary))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addIndividualBeneficiary("aUTR").apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addIndividualBeneficiaryTransformer("aUTR", "id", newBeneficiary)
    }

    "must return an error for malformed json" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addIndividualBeneficiary("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}