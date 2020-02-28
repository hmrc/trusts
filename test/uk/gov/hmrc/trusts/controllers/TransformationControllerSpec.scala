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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.{NameType, RemoveTrustee}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.services.TransformationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {
  val identifierAction = new FakeIdentifierAction(Agent)

  "amend lead trustee" - {

    "must add a new amend lead trustee transform" in {
      val transformationService = mock[TransformationService]
      val controller = new TransformationController(identifierAction, transformationService)

      val newTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = new DateTime(1965, 2, 10, 0, 0),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
        entityStart = Some(DateTime.parse("2012-03-14"))
      )

      when(transformationService.addAmendLeadTrusteeTransformer(any(), any(), any()))
        .thenReturn(Future.successful(()))

      val newTrusteeInfo = DisplayTrustLeadTrusteeType(Some(newTrusteeIndInfo), None)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newTrusteeIndInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendLeadTrustee("aUTR").apply(request)

      status(result) mustBe OK
      verify(transformationService).addAmendLeadTrusteeTransformer("aUTR", "id", newTrusteeInfo)
    }

    "must return an error for malformed json" in {
      val transformationService = mock[TransformationService]
      val controller = new TransformationController(identifierAction, transformationService)

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendLeadTrustee("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "add trustee" - {

    "must add a new add trustee transform" in {

      val transformationService = mock[TransformationService]
      val controller = new TransformationController(identifierAction, transformationService)

      val newTrusteeIndInfo = DisplayTrustTrusteeIndividualType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = Some(new DateTime(1965, 2, 10, 0, 0)),
        phoneNumber = Some("newPhone"),
        identification = Some(DisplayTrustIdentificationType(None, Some("newNino"), None, None)),
        entityStart = DateTime.parse("2012-03-14")
      )

      when(transformationService.addAddTrusteeTransformer(any(), any(), any()))
        .thenReturn(Future.successful(()))

      val newTrusteeInfo = DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newTrusteeIndInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addTrustee("aUTR").apply(request)

      status(result) mustBe OK
      verify(transformationService).addAddTrusteeTransformer("aUTR", "id", newTrusteeInfo)
    }

    "must return an error for malformed json" in {
      val transformationService = mock[TransformationService]
      val controller = new TransformationController(identifierAction, transformationService)

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addTrustee("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "remove trustee" - {

    "must add a 'remove trustee' transform" in {

      val transformationService = mock[TransformationService]
      val controller = new TransformationController(identifierAction, transformationService)

      val payload = RemoveTrustee(
        endDate = DateTime.parse("2020-01-10"), index = 0
      )

      when(transformationService.addRemoveTrusteeTransformer(any(), any(), any()))
        .thenReturn(Future.successful(()))

      val request = FakeRequest("DELETE", "path")
        .withBody(Json.toJson(payload))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.removeTrustee("aUTR").apply(request)

      status(result) mustBe OK
      verify(transformationService).addRemoveTrusteeTransformer("aUTR", "id", payload)
    }
  }
}