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
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import scala.concurrent.Future

class AddBusinessSettlorSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  lazy val getTrustResponse: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  lazy val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  "an add business settlor call" - {
    val newCompanySettlorJson = (
      """
        |{
        |  "companyTime": false,
        |  "entityStart": "2002-01-01",
        |  "identification": {
        |    "utr": "ST019091"
        |  },
        |  "lineNo": "1",
        |  "name": "Test"
        |}
        |""".stripMargin
      )

    lazy val expectedGetAfterAddSettlorJson: JsValue =
      JsonUtils.getJsonValueFromFile("add-business-settlor-after-etmp-call.json")

    val stubbedTrustsConnector = mock[TrustsConnector]
    when(stubbedTrustsConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponse))

    val application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(stubbedTrustsConnector)
      )
      .build()

    "must return add data in a subsequent 'get' call with a UTR" in assertMongoTest(application) { application =>
      runTest("5465416546", application)
      runTest("0123456789ABCDE", application)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe expectedInitialGetJson

      val addRequest = FakeRequest(POST, s"/trusts/settlors/add-business/$identifier")
        .withBody(newCompanySettlorJson)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val addResult = route(application, addRequest).get
      status(addResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(newResult) mustBe OK
      contentAsJson(newResult) mustBe expectedGetAfterAddSettlorJson
    }
  }
}
