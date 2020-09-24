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
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class RemoveSettlorSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  trait JsonFixtures {

    val getTrustResponseFromDES : JsValue = JsonUtils
      .getJsonValueFromFile("trusts-etmp-received-multiple-settlors.json")
  }

  "a remove settlor call" - {

    "must return amended data in a subsequent 'get' call" in new JsonFixtures {

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

          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK

          val removeSettlorAtIndex = Json.parse(
            """
              |{
              |	"index": 0,
              |	"endDate": "2010-10-10",
              | "type": "settlor"
              |}
              |""".stripMargin)

          val removeSettlorRequest = FakeRequest(PUT, "/trusts/5174384721/settlors/remove")
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

          val removeSettlorCompanyRequest = FakeRequest(PUT, "/trusts/5174384721/settlors/remove")
            .withBody(Json.toJson(removeSettlorCompanyAtIndex))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val removeSettlorCompanyResult = route(application, removeSettlorCompanyRequest).get
          status(removeSettlorCompanyResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/settlors")).get
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

          dropTheDatabase(connection)
        }.get
      }
    }
  }

}
