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
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.TransformationRepositoryImpl
import transformers.remove.RemoveAsset
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.{JsonUtils, NonTaxable5MLDFixtures}

import scala.concurrent.Future

class RemoveAssetSpec extends AsyncFreeSpec with MockitoSugar with IntegrationTestBase {

  private val utr: String = "0123456789"
  private val urn: String = "0123456789ABCDE"

  private def runTest(identifier: String, application: Application, assetType: String): Assertion = {
    dropDB()

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

    val assetsAfterRemoval = (contentAsJson(subsequentGetResult) \ "getTrust" \ "trust" \ "assets" \ assetType).asOpt[JsObject]
    assetsAfterRemoval mustBe None
  }

  private val getTrustResponse: GetTrustSuccessResponse = JsonUtils
    .getJsonValueFromString(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponseWithAllAssetTypes)
    .as[GetTrustSuccessResponse]

  private lazy val application: Application = {
    val mockTrustsConnector = mock[TrustsConnector]
    when(mockTrustsConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponse))

    applicationBuilder.overrides(
      bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
      bind[TrustsConnector].toInstance(mockTrustsConnector)
    ).build()
  }

  private val repository = application.injector.instanceOf[TransformationRepositoryImpl]

  private def dropDB(): Unit = {
    await(repository.collection.deleteMany(filter = Document()).toFuture())
    await(repository.ensureIndexes)
  }

  "Remove asset" - {
    "must return amended data in a subsequent 'get' call for each asset type" in {

      for (assetType <- RemoveAsset.validAssetTypes) {
        runTest(utr, application, assetType)
        runTest(urn, application, assetType)
      }

      RemoveAsset.validAssetTypes.length mustBe 7
    }
  }
}
