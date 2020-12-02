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
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.{JsonUtils, NonTaxable5MLDFixtures}

import scala.concurrent.Future

class RemoveAssetSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  "a remove nonEEABusinessAsset call" - {

      val stubbedTrustsConnector = mock[TrustsConnector]

    lazy val getTrustResponseFromDES: GetTrustSuccessResponse =
      JsonUtils.getJsonValueFromString(NonTaxable5MLDFixtures.DES.newGet5MLDTrustNonTaxableResponse).as[GetTrustSuccessResponse]

      when(stubbedTrustsConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponseFromDES))

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

      val removeNonEEABusinessAssetAtIndex = Json.parse(
        """
          |{
          |  "index": 0,
          |  "endDate": "2010-10-10",
          |  "type": "nonEEABusiness"
          |}
          |""".stripMargin)
      val removeRequest = FakeRequest(PUT, s"/trusts/assets/$identifier/remove")
        .withBody(Json.toJson(removeNonEEABusinessAssetAtIndex))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val RemoveNonEEABusinessAssetResult = route(application, removeRequest).get
      status(RemoveNonEEABusinessAssetResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(newResult) mustBe OK

      val assets = (contentAsJson(newResult) \ "getTrust" \ "trust" \ "assets" \ "nonEEABusiness").asOpt[JsObject]
      assets mustBe None

    }
  }
}
