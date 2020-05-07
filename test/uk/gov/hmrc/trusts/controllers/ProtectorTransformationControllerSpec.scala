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
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.services.ProtectorTransformationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProtectorTransformationControllerSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers {

  val identifierAction = new FakeIdentifierAction(Agent)

  "remove protector" - {

    "add a new remove protector transform " in {

      val protectorTransformationService = mock[ProtectorTransformationService]
      val controller = new ProtectorTransformationController(identifierAction, protectorTransformationService)

      when(protectorTransformationService.removeProtector(any(), any(), any())(any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj(
          "type" -> "protector",
          "endDate" -> LocalDate.of(2018, 2, 24),
          "index" -> 24
        ))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.removeProtector("UTRUTRUTR").apply(request)

      status(result) mustBe OK
      verify(protectorTransformationService)
        .removeProtector(
          equalTo("UTRUTRUTR"),
          equalTo("id"),
          equalTo(RemoveProtector(LocalDate.of(2018, 2, 24), 24, "protector")))(any())
    }

    "return an error when json is invalid" in {
      val OUT = new ProtectorTransformationController(identifierAction, mock[ProtectorTransformationService])

      val request = FakeRequest("POST", "path")
        .withBody(Json.obj("field" -> "value"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = OUT.removeProtector("UTRUTRUTR")(request)

      status(result) mustBe BAD_REQUEST
    }

  }
}
