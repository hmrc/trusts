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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.repositories.TransformIntegrationTest
import connector.DesConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.get_trust._
import utils.JsonUtils

import scala.concurrent.Future

class AddTrusteeSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  trait JsonFixtures {

    val getTrustResponseFromDES : JsValue = JsonUtils
      .getJsonValueFromFile("trusts-etmp-received-no-trustees.json")
  }

  "an add trustee call" - {

    "must return amended data in a subsequent 'get' call with provisional flags" in new JsonFixtures {

      val stubbedDesConnector = mock[DesConnector]

      when(stubbedDesConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponseFromDES.as[GetTrustSuccessResponse]))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .build()

      running(application) {

        getConnection(application).map { connection =>
          dropTheDatabase(connection)

          // Ensure passes schema
          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
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

          val amendRequest = FakeRequest(POST, "/trusts/trustees/add/5174384721")
            .withBody(addTrusteeJson)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val addedResponse = route(application, amendRequest).get
          status(addedResponse) mustBe OK

          // ensure they're in the trust response with the provisional flag
          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/trustees")).get
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

          dropTheDatabase(connection)
        }.get
      }
    }

  }
}
