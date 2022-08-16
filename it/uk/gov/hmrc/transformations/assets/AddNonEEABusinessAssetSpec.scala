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

package uk.gov.hmrc.transformations.assets

import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.GetTrustSuccessResponse
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mongodb.scala.Document
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.TransformationRepositoryImpl
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.{JsonUtils, NonTaxable5MLDFixtures}

import scala.concurrent.Future

class AddNonEEABusinessAssetSpec extends AsyncFreeSpec with MockitoSugar with IntegrationTestBase {

  private lazy val getTrustResponse: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromString(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponseWithAllAssetTypes).as[GetTrustSuccessResponse]

  private lazy val expectedInitialGetJson: JsValue = NonTaxable5MLDFixtures.Trusts.getTransformedNonTaxableTrustResponseWithAllAssetTypes

  private lazy val expectedSubsequentGetJson: JsValue = JsonUtils.getJsonValueFromFile("5MLD/NonTaxable/transforms/assets/noneeabusiness/add-after-etmp-call.json")

  "an add non-EEA business asset call" - {

    val payload = Json.parse(
      """
        |{
        |  "lineNo": "1",
        |  "orgName": "TestOrg",
        |  "address": {
        |    "line1": "Line 1",
        |    "line2": "Line 2",
        |    "postCode": "NE1 1NE",
        |    "country": "GB"
        |  },
        |  "govLawCountry": "GB",
        |  "startDate": "2002-01-01"
        |}
        |""".stripMargin)

    val mockTrustsConnector = mock[TrustsConnector]
    when(mockTrustsConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponse))

    val application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(mockTrustsConnector)
      ).build()

    val repository = application.injector.instanceOf[TransformationRepositoryImpl]

    def dropDB(): Unit = {
      await(repository.collection.deleteMany(filter = Document()).toFuture())
      await(repository.ensureIndexes)
    }

    "must return amended data in a subsequent 'get' call, for identifier '0123456789'" in {
      runTest("0123456789", application)
    }

    "must return amended data in a subsequent 'get' call, for identifier '0123456789ABCDE'" in {
      runTest("0123456789ABCDE", application)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      dropDB()
      val initialGetResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(initialGetResult) mustBe OK
      contentAsJson(initialGetResult) mustBe expectedInitialGetJson

      val addRequest = FakeRequest(POST, s"/trusts/assets/add-non-eea-business/$identifier")
        .withBody(payload)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val addResult = route(application, addRequest).get
      status(addResult) mustBe OK

      val subsequentGetResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(subsequentGetResult) mustBe OK
      contentAsJson(subsequentGetResult) mustBe expectedSubsequentGetJson
    }
  }
}
