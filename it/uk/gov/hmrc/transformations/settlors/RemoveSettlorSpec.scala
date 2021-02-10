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

package uk.gov.hmrc.transformations.settlors

import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.GetTrustSuccessResponse
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{Assertion, AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import scala.concurrent.Future

class RemoveSettlorSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  "a remove settlor call" - {

      val stubbedTrustsConnector = mock[TrustsConnector]

      val getTrustResponse : JsValue = JsonUtils
        .getJsonValueFromFile("trusts-etmp-received-multiple-settlors.json")

      when(stubbedTrustsConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponse.as[GetTrustSuccessResponse]))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[TrustsConnector].toInstance(stubbedTrustsConnector)
        )
        .build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>
      runTest("5174384721", application)
      runTest("0123456789ABCDE", application)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result) mustBe OK

      val removeSettlorAtIndex = Json.parse(
        """
          |{
          |	"index": 0,
          |	"endDate": "2010-10-10",
          | "type": "settlor"
          |}
          |""".stripMargin)

      val removeSettlorRequest = FakeRequest(PUT, s"/trusts/settlors/$identifier/remove")
        .withBody(Json.toJson(removeSettlorAtIndex))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val removeSettlorResult = route(application, removeSettlorRequest).get
      status(removeSettlorResult) mustBe OK

      val removeSettlorCompanyAtIndex = Json.parse(
        """
          |{
          |	"index": 0,
          |	"endDate": "2010-10-10",
          | "type": "settlorCompany"
          |}
          |""".stripMargin)

      val removeSettlorCompanyRequest = FakeRequest(PUT, s"/trusts/settlors/$identifier/remove")
        .withBody(Json.toJson(removeSettlorCompanyAtIndex))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val removeSettlorCompanyResult = route(application, removeSettlorCompanyRequest).get
      status(removeSettlorCompanyResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/settlors/$identifier/transformed")).get
      status(newResult) mustBe OK

      val settlors = (contentAsJson(newResult) \ "settlors" \ "settlor").as[JsArray]
      settlors mustBe Json.parse(
        """
          |[
          |]
          |""".stripMargin)

      val settlorCompanies = (contentAsJson(newResult) \ "settlors" \ "settlorCompany").as[JsArray]
      settlorCompanies mustBe Json.parse(
        """
          |[
          |]
          |""".stripMargin)

    }
  }
}
