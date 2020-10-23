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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust.get_trust._
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class RemoveTrusteeSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  "a remove trustee call" - {

      val stubbedDesConnector = mock[DesConnector]

      val getTrustResponseFromDES : JsValue = JsonUtils
        .getJsonValueFromFile("trusts-etmp-received-multiple-trustees.json")

      when(stubbedDesConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponseFromDES.as[GetTrustSuccessResponse]))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>

      val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
      status(result) mustBe OK

      val removeAtIndex = Json.parse(
        """
          |{
          |	"index": 0,
          |	"endDate": "2010-10-10"
          |}
          |""".stripMargin)

      val amendRequest = FakeRequest(PUT, "/trusts/5174384721/trustees/remove")
        .withBody(Json.toJson(removeAtIndex))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val firstRemoveResult = route(application, amendRequest).get
      status(firstRemoveResult) mustBe OK

      val secondRemoveResult = route(application, amendRequest).get
      status(secondRemoveResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/trustees")).get
      status(newResult) mustBe OK

      val trustees = (contentAsJson(newResult) \ "trustees").as[JsArray]
      trustees mustBe Json.parse(
        """
          |[
          |            {
          |              "trusteeOrg": {
          |                "name": "Trustee Org 2",
          |                "phoneNumber": "0121546546",
          |                "identification": {
          |                  "utr": "5465416546"
          |                },
          |                "entityStart": "1998-02-12",
          |                "provisional": true
          |              }
          |            }
          |]
          |""".stripMargin)

    }
  }
}
