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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import connector.DesConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.get_trust._
import utils.JsonUtils

import scala.concurrent.Future

class RemoveOtherIndividualSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  "a remove otherIndividual call" - {

      val stubbedDesConnector = mock[DesConnector]

      val getTrustResponseFromDES : JsValue = JsonUtils
        .getJsonValueFromFile("trusts-etmp-received-multiple-otherIndividuals.json")

      when(stubbedDesConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponseFromDES.as[GetTrustSuccessResponse]))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>

      val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
      status(result) mustBe OK

      val removeOtherIndividualAtIndex = Json.parse(
        """
          |{
          |	"index": 0,
          |	"endDate": "2010-10-10"
          |}
          |""".stripMargin)

      val removeOtherIndividualRequest = FakeRequest(PUT, "/trusts/other-individuals/5174384721/remove")
        .withBody(Json.toJson(removeOtherIndividualAtIndex))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val removeOtherIndividualResult = route(application, removeOtherIndividualRequest).get
      status(removeOtherIndividualResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/other-individuals")).get
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
