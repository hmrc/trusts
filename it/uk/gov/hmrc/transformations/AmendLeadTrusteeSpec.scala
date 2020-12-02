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

package uk.gov.hmrc.transformations

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{Assertion, AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import connector.IfsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.GetTrustSuccessResponse
import models.variation.IdentificationType
import models.{AddressType, NameType}
import play.api.Application
import utils.JsonUtils

import scala.concurrent.Future

class AmendLeadTrusteeSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  "an amend lead trustee call" - {
      val newTrusteeIndInfo = Json.obj(
        "name" -> NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        "dateOfBirth" -> LocalDate.of(1965, 2, 10),
        "phoneNumber" -> "newPhone",
        "email" -> "newEmail",
        "identification" -> IdentificationType(
          Some("newNino"),
          None,
          Some(AddressType(
            "1344 Army Road",
            "Suite 111",
            Some("Telford"),
            Some("Shropshire"),
            Some("TF1 5DR"),
            "GB"
          )),
          None)
      )

      val expectedGetAfterAmendLeadTrusteeJson: JsValue = JsonUtils.getJsonValueFromFile("it/trusts-integration-get-after-amend-lead-trustee.json")

      val stubbedDesConnector = mock[IfsConnector]
      when(stubbedDesConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponseFromDES))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[IfsConnector].toInstance(stubbedDesConnector)
        )
        .build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>
      runTest("5174384721", application)
      runTest("0123456789ABCDE", application)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe expectedInitialGetJson

      val amendRequest = FakeRequest(POST, s"/trusts/trustees/amend-lead/$identifier")
        .withBody(newTrusteeIndInfo)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val amendResult = route(application, amendRequest).get
      status(amendResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(newResult) mustBe OK
      contentAsJson(newResult) mustBe expectedGetAfterAmendLeadTrusteeJson
    }
  }
}
