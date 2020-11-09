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
import models.{AddressType, NameType, Success}
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
import services.BeneficiaryTransformationService
import transformers.remove.RemoveBeneficiary
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AssetsTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  "Amend nonEeaBusinessAsset" - {

    val index = 0

    "must add a new amend nonEeaBusinessAssetJson transform" in {
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

  "remove nonEeaBusinessAsset" - {

    "add an new remove nonEeaBusinessAsset transform " in {
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

  "Add nonEeaBusinessAsset" - {

    "must add a new add nonEeaBusinessAsset transform" in {
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
}