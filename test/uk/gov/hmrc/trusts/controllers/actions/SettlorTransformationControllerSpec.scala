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

package uk.gov.hmrc.trusts.controllers.actions

import java.time.LocalDate

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.trusts.controllers.SettlorTransformationController
import uk.gov.hmrc.trusts.models.{variation, _}

import scala.concurrent.ExecutionContext.Implicits.global

class SettlorTransformationControllerSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers {

  val identifierAction = new FakeIdentifierAction(Agent)

  "Amend individual settlor" - {

    val index = 0

    "must add a new amend individual settlor transform" in {

//      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]

      val controller = new SettlorTransformationController(identifierAction)

      val newSettlor = variation.Settlor(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("First", None, "Last"),
        dateOfBirth = None,
        identification = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

//      when(beneficiaryTransformationService.amendUnidentifiedBeneficiaryTransformer(any(), any(), any(), any())(any()))
//        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newSettlor))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendIndividualSettlor("aUTR", index).apply(request)

      status(result) mustBe OK

//      verify(beneficiaryTransformationService).amendUnidentifiedBeneficiaryTransformer(
//        equalTo("aUTR"),
//        equalTo(index),
//        equalTo("id"),
//        equalTo(newDescription))(any())
    }

    "must return an error for malformed json" in {
//      val beneficiaryTransformationService = mock[BeneficiaryTransformationService]
      val controller = new SettlorTransformationController(identifierAction)

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendIndividualSettlor("aUTR", index).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}