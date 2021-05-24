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

package uk.gov.hmrc.transformations.otherindividuals

import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.GetTrustSuccessResponse
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
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

class RemoveOtherIndividualSpec extends AsyncFreeSpec with MockitoSugar with IntegrationTestBase {

  "a remove otherIndividual call" - {

      val stubbedTrustsConnector = mock[TrustsConnector]

      val getTrustResponse : JsValue = JsonUtils
        .getJsonValueFromFile("trusts-etmp-received-multiple-otherIndividuals.json")

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

      val removeOtherIndividualAtIndex = Json.parse(
        """
          |{
          |	"index": 0,
          |	"endDate": "2010-10-10"
          |}
          |""".stripMargin)

      val removeOtherIndividualRequest = FakeRequest(PUT, s"/trusts/other-individuals/$identifier/remove")
        .withBody(Json.toJson(removeOtherIndividualAtIndex))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val removeOtherIndividualResult = route(application, removeOtherIndividualRequest).get
      status(removeOtherIndividualResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/other-individuals/$identifier/transformed")).get
      status(newResult) mustBe OK

      val otherIndividuals = (contentAsJson(newResult) \ "naturalPerson").as[JsArray]
      otherIndividuals mustBe Json.parse(
        """
          |[
          | {
          |   "lineNo": "2",
          |   "name": {
          |     "firstName": "James",
          |     "middleName": "David",
          |     "lastName": "O'Connor"
          |   },
          |   "dateOfBirth": "1950-01-10",
          |   "identification": {
          |     "nino": "AB187812"
          |   },
          |   "entityStart": "1998-02-12",
          |   "provisional": false
          | }
          |]
          |""".stripMargin)
    }
  }
}
