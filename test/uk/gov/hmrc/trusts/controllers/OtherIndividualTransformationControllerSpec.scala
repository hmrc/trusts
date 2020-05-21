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

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustNaturalPersonType
import uk.gov.hmrc.trusts.services.{BeneficiaryTransformationService, OtherIndividualTransformationService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OtherIndividualTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {
  val identifierAction = new FakeIdentifierAction(Agent)

  "Add other individual" - {

    "must add a new add other  individual transform" in {
      val otherIndividualTransformationService = mock[OtherIndividualTransformationService]
      val controller = new OtherIndividualTransformationController(identifierAction, otherIndividualTransformationService)

      val newOtherIndividual = DisplayTrustNaturalPersonType(
        None,
        None,
        NameType("efs", None, ""),
        None,
        None,
        "2010-01-01"
      )

      when(otherIndividualTransformationService.addOtherIndividualTransformer(any(), any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newOtherIndividual))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addOtherIndividual("aUTR").apply(request)

      status(result) mustBe OK
      verify(otherIndividualTransformationService).addOtherIndividualTransformer("aUTR", "id", newOtherIndividual)
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

  }