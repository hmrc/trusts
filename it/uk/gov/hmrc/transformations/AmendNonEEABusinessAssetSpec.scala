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
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{GET, contentAsJson, route, status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.{JsonUtils, NonTaxable5MLDFixtures}

import scala.concurrent.Future

class AmendNonEEABusinessAssetSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase with ScalaFutures {

  lazy val getTrustResponse: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromString(NonTaxable5MLDFixtures.DES.newGet5MLDTrustNonTaxableResponse).as[GetTrustSuccessResponse]

  lazy val expectedInitialGetJson: JsValue =
    NonTaxable5MLDFixtures.Trusts.newGetTransformedNonTaxableTrustResponse

  "an amend NonEEABusiness asset call" - {

      val expectedGetAfterAmendNonEEABusinessAssetJson: JsValue =
        JsonUtils.getJsonValueFromFile("5MLD/NonTaxable/amended-nonEEABusiness-asset-after-etmp-call.json")

      val stubbedTrustsConnector = mock[TrustsConnector]
      when(stubbedTrustsConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponse))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[TrustsConnector].toInstance(stubbedTrustsConnector)
        ).build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>
      runTest("5174384721", application)
      runTest("0123456789ABCDE", application)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe expectedInitialGetJson

      val payload = Json.parse(
        """
          |{
          |  "address": {
          |      "line1": "Changed Line 1",
          |      "line2": "Changed Line 2",
          |      "postCode": "NE1 1NE",
          |      "country": "GB"
          |  },
          |  "govLawCountry": "GB",
          |  "startDate": "2002-01-01",
          |  "lineNo": "1",
          |  "orgName": "TestOrg"
          |}
          |""".stripMargin)

      val amendRequest = FakeRequest(POST, s"/trusts/assets/amend-non-eea-business/$identifier/0")
        .withBody(payload)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val amendResult = route(application, amendRequest).get
      status(amendResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(newResult) mustBe OK
      contentAsJson(newResult) mustEqual expectedGetAfterAmendNonEEABusinessAssetJson

    }
  }
}
