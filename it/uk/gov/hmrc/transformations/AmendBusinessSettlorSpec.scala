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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{GET, contentAsJson, route, status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust.get_trust.GetTrustSuccessResponse
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class AmendBusinessSettlorSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase with ScalaFutures {

  val getTrustResponseFromDES: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  "an amend individual settlor call" - {

      val expectedGetAfterAmendJson: JsValue =
        JsonUtils.getJsonValueFromFile("it/trusts-integration-get-after-amend-business-settlor.json")

      val stubbedDesConnector = mock[DesConnector]
      when(stubbedDesConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponseFromDES))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>

      // initial get
      val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
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
      val amendRequest = FakeRequest(POST, s"/trusts/settlors/amend-business/5174384721/$index")
        .withBody(payload)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val amendResult = route(application, amendRequest).get
      status(amendResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
      status(newResult) mustBe OK
      contentAsJson(newResult) mustEqual expectedGetAfterAmendJson
    }
  }
}
