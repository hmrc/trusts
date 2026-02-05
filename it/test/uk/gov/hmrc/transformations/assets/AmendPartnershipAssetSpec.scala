/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.transformations.assets

import cats.data.EitherT
import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{GET, contentAsJson, route, status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.{JsonUtils, NonTaxable5MLDFixtures}

import scala.concurrent.Future

class AmendPartnershipAssetSpec extends IntegrationTestBase with ScalaFutures {

  private lazy val getTrustResponse: GetTrustSuccessResponse =
    JsonUtils
      .getJsonValueFromString(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponseWithAllAssetTypes)
      .as[GetTrustSuccessResponse]

  private lazy val expectedInitialGetJson: JsValue =
    NonTaxable5MLDFixtures.Trusts.getTransformedNonTaxableTrustResponseWithAllAssetTypes

  private lazy val expectedSubsequentGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("5MLD/NonTaxable/transforms/assets/partnership/amend-after-etmp-call.json")

  "an amend partnership asset call" should {

    val payload = Json.parse("""
        |{
        |  "description": "Changed partnership description"
        |}
        |""".stripMargin)

    val mockTrustsConnector = mock[TrustsConnector]
    when(mockTrustsConnector.getTrustInfo(any()))
      .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(getTrustResponse))))

    def application = applicationBuilder
      .overrides(
        bind[IdentifierAction]
          .toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(mockTrustsConnector)
      )
      .build()

    "return amended data in a subsequent 'get' call, for identifier '0123456789'" in assertMongoTest(application) { app =>
      runTest("0123456789", app)
    }

    "return amended data in a subsequent 'get' call, for identifier '0123456789ABCDE'" in assertMongoTest(application) {
      app =>
        runTest("0123456789ABCDE", app)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      val initialGetResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(initialGetResult)        mustBe OK
      contentAsJson(initialGetResult) mustBe expectedInitialGetJson

      val amendRequest = FakeRequest(POST, s"/trusts/assets/amend-partnership/$identifier/0")
        .withBody(payload)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val amendResult = route(application, amendRequest).get
      status(amendResult) mustBe OK

      val subsequentGetResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(subsequentGetResult) mustBe OK
      contentAsJson(subsequentGetResult) mustEqual expectedSubsequentGetJson
    }
  }

}
