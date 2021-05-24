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

package uk.gov.hmrc.transformations.taxliability

import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.GetTrustSuccessResponse
import models.{YearReturnType, YearsReturns}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import scala.concurrent.Future

class SetYearsReturnsSpec extends AsyncFreeSpec with MockitoSugar with IntegrationTestBase {

  "a set years return call" - {

    val getTrustResponse : JsValue = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")

    val stubbedTrustsConnector = mock[TrustsConnector]

    when(stubbedTrustsConnector.getTrustInfo(any()))
      .thenReturn(Future.successful(getTrustResponse.as[GetTrustSuccessResponse]))

    val application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(stubbedTrustsConnector)
      ).build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>
      runTest("0123456789", application)
      runTest("0123456789ABCDE", application)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      val initialGetResult = route(application, FakeRequest(GET, s"/trusts/tax-liability/$identifier/transformed")).get
      status(initialGetResult) mustBe OK

      val initialYearsReturns = contentAsJson(initialGetResult).as[JsValue]

      val body = Json.toJson(YearsReturns(
        returns = Some(List(
          YearReturnType("18", taxConsequence = true),
          YearReturnType("19", taxConsequence = true),
          YearReturnType("20", taxConsequence = true)
        ))
      ))

      val setYearsReturnsRequest = FakeRequest(PUT, s"/trusts/tax-liability/$identifier/years-returns")
        .withBody(body)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val setYearsReturnsResponse = route(application, setYearsReturnsRequest).get
      status(setYearsReturnsResponse) mustBe OK

      val subsequentGetResult = route(application, FakeRequest(GET, s"/trusts/tax-liability/$identifier/transformed")).get
      status(subsequentGetResult) mustBe OK

      val subsequentYearsReturns = contentAsJson(subsequentGetResult).as[JsValue]
      subsequentYearsReturns mustBe body
      subsequentYearsReturns mustNot be(initialYearsReturns)

    }
  }
}
