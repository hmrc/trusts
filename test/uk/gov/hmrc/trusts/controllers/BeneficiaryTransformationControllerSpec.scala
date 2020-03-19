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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.{NameType, RemoveBeneficiary, RemoveTrustee, Success}
import uk.gov.hmrc.trusts.services.{BeneficiaryTransformationService, TrusteeTransformationService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BeneficiaryTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {
  val identifierAction = new FakeIdentifierAction(Agent)

  "amend unidentified beneficiary" - {

    val index = 0

    "must add a new amend unidentified beneficiary transform" in {
      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new BeneficiaryTransformationController(identifierAction, beneficiaryTransformationService)

      val newDescription = "Some new description"

      when(beneficiaryTransformationService.addAmendUnidentifiedBeneficiaryTransformer(any(), any(), any(), any()))
        .thenReturn(Future.successful(()))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newDescription))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendUnidentifiedBeneficiary("aUTR", index).apply(request)

      status(result) mustBe OK
      verify(beneficiaryTransformationService).addAmendUnidentifiedBeneficiaryTransformer("aUTR", index, "id", newDescription)
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
      verify(beneficiaryTransformationService).removeBeneficiary("UTRUTRUTR", "id", RemoveBeneficiary.Unidentified(LocalDate.of(2018, 2, 24), 24))
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
}