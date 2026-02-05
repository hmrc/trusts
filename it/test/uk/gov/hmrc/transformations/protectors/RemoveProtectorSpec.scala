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

package uk.gov.hmrc.transformations.protectors

import cats.data.EitherT
import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mongodb.scala.Document
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.TransformationRepositoryImpl
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import scala.concurrent.Future

class RemoveProtectorSpec extends IntegrationTestBase {

  "a remove protector call" should {

    val stubbedTrustsConnector = mock[TrustsConnector]

    val getTrustResponse: JsValue = JsonUtils
      .getJsonValueFromFile("trusts-etmp-received-multiple-protectors.json")

    when(stubbedTrustsConnector.getTrustInfo(any()))
      .thenReturn(
        EitherT[Future, TrustErrors, GetTrustResponse](
          Future.successful(Right(getTrustResponse.as[GetTrustSuccessResponse]))
        )
      )

    def application = applicationBuilder
      .overrides(
        bind[IdentifierAction]
          .toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
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

    "return amended data in a subsequent 'get' call, for identifier '0123456789ABCDE'" in assertMongoTest(application) {
      app =>
        runTest("0123456789ABCDE", app)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      dropDB()

      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result) mustBe OK

      val removeProtectorAtIndex = Json.parse("""
          |{
          |	"index": 0,
          |	"endDate": "2010-10-10",
          | "type": "protector"
          |}
          |""".stripMargin)

      val removeProtectorRequest = FakeRequest(PUT, s"/trusts/protectors/$identifier/remove")
        .withBody(Json.toJson(removeProtectorAtIndex))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val removeProtectorResult = route(application, removeProtectorRequest).get
      status(removeProtectorResult) mustBe OK

      val removeProtectorCompanyAtIndex = Json.parse("""
          |{
          |	"index": 0,
          |	"endDate": "2010-10-10",
          | "type": "protectorCompany"
          |}
          |""".stripMargin)

      val removeProtectorCompanyRequest = FakeRequest(PUT, s"/trusts/protectors/$identifier/remove")
        .withBody(Json.toJson(removeProtectorCompanyAtIndex))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val removeProtectorCompanyResult = route(application, removeProtectorCompanyRequest).get
      status(removeProtectorCompanyResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/protectors/$identifier/transformed")).get
      status(newResult) mustBe OK

      val protectors = (contentAsJson(newResult) \ "protectors" \ "protector").as[JsArray]
      protectors mustBe Json.parse("""
          |[
          |]
          |""".stripMargin)

      val protectorCompanies = (contentAsJson(newResult) \ "protectors" \ "protectorCompany").as[JsArray]
      protectorCompanies mustBe Json.parse("""
          |[
          |]
          |""".stripMargin)

    }
  }

}
