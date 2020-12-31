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

class AddTrusteeSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  "an add trustee call" - {

      val getTrustResponse : JsValue = JsonUtils
        .getJsonValueFromFile("trusts-etmp-received-no-trustees.json")

      val stubbedTrustsConnector = mock[TrustsConnector]

      when(stubbedTrustsConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponse.as[GetTrustSuccessResponse]))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[TrustsConnector].toInstance(stubbedTrustsConnector)
        )
        .build()

    "must return amended data in a subsequent 'get' call with provisional flags" in assertMongoTest(application) { application =>
      runTest("5174384721", application)
      runTest("0123456789ABCDE", application)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      // Ensure passes schema
      val result = route(application, FakeRequest(GET, s"/trusts/trustees/$identifier/transformed/trustee")).get
      status(result) mustBe OK

      val addTrusteeJson = Json.parse(
        """
          |{
          |	"name": {
          |   "firstName": "Adam",
          |   "lastName": "Last"
          | },
          | "entityStart": "2020-03-03"
          |}
          |""".stripMargin)

      val amendRequest = FakeRequest(POST, s"/trusts/trustees/add/$identifier")
        .withBody(addTrusteeJson)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val addedResponse = route(application, amendRequest).get
      status(addedResponse) mustBe OK

      // ensure they're in the trust response with the provisional flag
      val newResult = route(application, FakeRequest(GET, s"/trusts/trustees/$identifier/transformed/trustee")).get
      status(newResult) mustBe OK

      val trustees = (contentAsJson(newResult) \ "trustees").as[JsArray]
      trustees mustBe Json.parse(
        """
          |[
          |            {
          |              "trusteeInd": {
          |                "name": {
          |                 "firstName": "Adam",
          |                 "lastName": "Last"
          |                },
          |                "entityStart": "2020-03-03",
          |                "provisional": true
          |              }
          |            }
          |]
          |""".stripMargin)

    }
  }
}
