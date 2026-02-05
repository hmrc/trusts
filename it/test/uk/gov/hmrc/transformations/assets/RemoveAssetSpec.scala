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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import transformers.remove.RemoveAsset
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.{JsonUtils, NonTaxable5MLDFixtures}

import scala.concurrent.Future

class RemoveAssetSpec extends IntegrationTestBase {

  private val utr: String = "0123456789"
  private val urn: String = "0123456789ABCDE"

  private def runTest(identifier: String, application: Application, assetType: String): Assertion = {
    val body: JsValue = Json.parse(
      s"""
         |{
         |  "index": 0,
         |  "endDate": "2010-10-10",
         |  "type": "$assetType"
         |}
         |""".stripMargin
    )

    val initialGetResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
    status(initialGetResult) mustBe OK

    val removeRequest = FakeRequest(PUT, s"/trusts/assets/$identifier/remove")
      .withBody(Json.toJson(body))
      .withHeaders(CONTENT_TYPE -> "application/json")

    val removeResult = route(application, removeRequest).get
    status(removeResult) mustBe OK

    val subsequentGetResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
    status(subsequentGetResult) mustBe OK

    val assetsAfterRemoval =
      (contentAsJson(subsequentGetResult) \ "getTrust" \ "trust" \ "assets" \ assetType).asOpt[JsObject]
    assetsAfterRemoval mustBe None
  }

  private val getTrustResponse: GetTrustSuccessResponse = JsonUtils
    .getJsonValueFromString(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponseWithAllAssetTypes)
    .as[GetTrustSuccessResponse]

  private def application: Application = {
    val mockTrustsConnector = mock[TrustsConnector]
    when(mockTrustsConnector.getTrustInfo(any()))
      .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(getTrustResponse))))

    applicationBuilder
      .overrides(
        bind[IdentifierAction]
          .toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(mockTrustsConnector)
      )
      .build()
  }

  "Remove asset" should {
    "return amended data in a subsequent 'get' call for each asset type" in assertMongoTest(application) { app =>
      for (assetType <- RemoveAsset.validAssetTypes) {
        runTest(utr, app, assetType)
        runTest(urn, app, assetType)
      }

      RemoveAsset.validAssetTypes.length mustBe 7
    }
  }

}
