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

import connector.DesConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.GetTrustSuccessResponse
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.{JsBoolean, JsValue}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import scala.concurrent.Future

class SetTaxableSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  "a set trust details taxable call" - {

      val getTrustResponseFromDES : JsValue = JsonUtils
        .getJsonValueFromFile("trusts-etmp-received.json")

      val stubbedDesConnector = mock[DesConnector]

      when(stubbedDesConnector.getTrustInfo(any()))
        .thenReturn(Future.successful(getTrustResponseFromDES.as[GetTrustSuccessResponse]))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>

      // Ensure passes schema
      val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
      status(result) mustBe OK

      val setTaxableProperty = JsBoolean(true)

      val amendRequest = FakeRequest(PUT, "/trusts/5174384721/trust-details/taxable")
        .withBody(setTaxableProperty)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val addedResponse = route(application, amendRequest).get
      status(addedResponse) mustBe OK

      val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/trust-details")).get
      status(newResult) mustBe OK

      val trustees = (contentAsJson(newResult) \ "trustTaxable").as[JsBoolean]
      trustees mustBe setTaxableProperty

    }
  }
}
