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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.libs.json.{JsString, JsValue}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.repositories.TransformIntegrationTest
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustSuccessResponse
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class AmendUnidentifiedBeneficiarySpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar with TransformIntegrationTest {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  "an amend unidentified beneficiary call" - {

    "must return amended data in a subsequent 'get' call" in {

      val newDescription = "Updated description"

      val expectedGetAfterAmendBeneficiaryJson: JsValue = JsonUtils.getJsonValueFromFile("it/trusts-integration-get-after-amend-unidentified-beneficiary.json")

      val stubbedDesConnector = mock[DesConnector]
      when(stubbedDesConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(getTrustResponseFromDES))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .build()

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)
          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedInitialGetJson

          val amendRequest = FakeRequest(POST, "/trusts/beneficiaries/amend-unidentified/5174384721/0")
            .withBody(JsString(newDescription))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val amendResult = route(application, amendRequest).get
          status(amendResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe expectedGetAfterAmendBeneficiaryJson

          dropTheDatabase(connection)
        }.get
      }
    }
  }
}
