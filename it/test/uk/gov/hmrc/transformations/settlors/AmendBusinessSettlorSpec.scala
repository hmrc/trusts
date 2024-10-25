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

package uk.gov.hmrc.transformations.settlors

import cats.data.EitherT
import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mongodb.scala.Document
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{GET, contentAsJson, route, status, _}
import play.api.test.{FakeRequest, Helpers}
import repositories.TransformationRepositoryImpl
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import scala.concurrent.Future

class AmendBusinessSettlorSpec extends IntegrationTestBase with ScalaFutures {

  private val getTrustResponse: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  private val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  "an amend individual settlor call" should {

    val expectedGetAfterAmendJson: JsValue =
      JsonUtils.getJsonValueFromFile("it/trusts-integration-get-after-amend-business-settlor.json")

    val stubbedTrustsConnector = mock[TrustsConnector]
    when(stubbedTrustsConnector.getTrustInfo(any()))
      .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(getTrustResponse))))

    def application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(stubbedTrustsConnector)
      )
      .build()

    def repository = application.injector.instanceOf[TransformationRepositoryImpl]

    def dropDB(): Unit = {
      await(repository.collection.deleteMany(filter = Document()).toFuture())
      await(repository.ensureIndexes())
    }

    "return amended data in a subsequent 'get' call, for identifier '5174384721'" in assertMongoTest(application) { app =>
      runTest("5174384721", app)
    }

    "return amended data in a subsequent 'get' call, for identifier '0123456789ABCDE'" in assertMongoTest(application) { app =>
      runTest("0123456789ABCDE", app)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      dropDB()

      // initial get
      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe expectedInitialGetJson

      val payload = Json.parse(
        """
          |{
          |  "name": "Updated Company",
          |  "entityStart": "1998-02-12"
          |}
          |""".stripMargin)

      val index = 0

      // amend individual settlor
      val amendRequest = FakeRequest(POST, s"/trusts/settlors/amend-business/$identifier/$index")
        .withBody(payload)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val amendResult = route(application, amendRequest).get
      status(amendResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(newResult) mustBe OK
      contentAsJson(newResult) mustEqual expectedGetAfterAmendJson
    }
  }
}