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
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.repositories.TransformIntegrationTest
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustSuccessResponse
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class AddLargeBeneficiarySpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  lazy val getTrustResponseFromDES: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  lazy val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  "an add large beneficiary call" - {
    "must return amended data in a subsequent 'get' call" in {

      val newBeneficiaryJson = Json.parse(
        """
          |{
          |  "organisationName": "New Organisation Name",
          |  "identification": {
          |    "address": {
          |      "line1": "Line 1",
          |      "line2": "Line 2",
          |      "postCode": "NE1 1NE",
          |      "country": "GB"
          |    }
          |  },
          |  "description": "New Description",
          |  "numberOfBeneficiary": "501",
          |  "entityStart": "1990-10-10"
          |}
          |""".stripMargin
      )

      lazy val expectedGetAfterAddBeneficiaryJson: JsValue =
        JsonUtils.getJsonValueFromFile("it/trusts-integration-get-after-add-large-beneficiary.json")

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

          val addRequest = FakeRequest(POST, "/trusts/beneficiaries/add-large/5174384721")
            .withBody(newBeneficiaryJson)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val addResult = route(application, addRequest).get
          status(addResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe expectedGetAfterAddBeneficiaryJson

          dropTheDatabase(connection)
        }.get
      }
    }
  }
}

