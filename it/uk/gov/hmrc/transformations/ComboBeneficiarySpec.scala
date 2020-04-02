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
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, GET, POST, contentAsJson, route, running, status}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.repositories.TransformIntegrationTest
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustSuccessResponse
import uk.gov.hmrc.trusts.utils.JsonUtils
import play.api.test.Helpers._

import scala.concurrent.Future

class ComboBeneficiarySpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {
  lazy val getTrustResponseFromDES: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  lazy val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "doing a bunch of beneficiary transforms" - {
    "must return amended data in a subsequent 'get' call" in {
      lazy val expectedGetAfterAddBeneficiaryJson: JsValue =
        JsonUtils.getJsonValueFromFile("trusts-integration-get-after-combo-beneficiary.json")

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

          status(addCharityBeneficiary(application)) mustBe OK
          status(addIndividualBeneficiary(application)) mustBe OK
          status(addUnidentifiedBeneficiary(application)) mustBe OK
          status(amendUnidentifiedBeneficiary(application)) mustBe OK
          status(removeCharityBeneficiary(application)) mustBe OK
          status(amendCharityBeneficiary(application)) mustBe OK
          status(removeOtherBeneficiary(application)) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe expectedGetAfterAddBeneficiaryJson

          dropTheDatabase(connection)
        }.get
      }

    }
  }

  private def addCharityBeneficiary(application: Application) = {
    val newBeneficiaryJson = Json.parse(
      """
        |{
        |  "organisationName": "Charity 2",
        |  "beneficiaryDiscretion": false,
        |  "beneficiaryShareOfIncome": "50",
        |  "identification": {
        |    "address": {
        |      "line1": "Line 1",
        |      "line2": "Line 2",
        |      "line3": "Line 3 to be killed later",
        |      "postCode": "NE1 1NE",
        |      "country": "GB"
        |    }
        |  },
        |  "entityStart": "2019-02-03"
        |}
        |""".stripMargin
    )

    val addRequest = FakeRequest(POST, "/trusts/add-charity-beneficiary/5174384721")
      .withBody(newBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def addIndividualBeneficiary(application: Application) = {
    val newBeneficiaryJson = Json.parse(
      """
        |{
        |  "name":{
        |    "firstName":"First",
        |    "lastName":"Last"
        |  },
        |  "dateOfBirth":"2000-01-01",
        |  "vulnerableBeneficiary":false,
        |  "identification": {
        |    "nino": "nino"
        |  },
        |  "entityStart":"1990-10-10"
        |}
        |""".stripMargin
    )

    val addRequest = FakeRequest(POST, "/trusts/add-individual-beneficiary/5174384721")
      .withBody(newBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def addUnidentifiedBeneficiary(application: Application) = {
    val newBeneficiaryJson = Json.parse(
      """
        |{
        | "description": "New Beneficiary Description",
        | "entityStart": "2020-01-01"
        |}
        |""".stripMargin
    )

    val addRequest = FakeRequest(POST, "/trusts/add-unidentified-beneficiary/5174384721")
      .withBody(newBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def amendUnidentifiedBeneficiary(application: Application) = {
    val addRequest = FakeRequest(POST, "/trusts/amend-unidentified-beneficiary/5174384721/0")
      .withBody(JsString("Amended Beneficiary Description"))
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def removeCharityBeneficiary(application: Application) = {
    val removeJson = Json.parse(
      """
        |{
        | "endDate": "2014-03-12",
        | "index": 0,
        | "type": "charity"
        |}
        |""".stripMargin)
    val addRequest = FakeRequest(PUT, "/trusts/5174384721/beneficiaries/remove")
      .withBody(removeJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def amendCharityBeneficiary(application: Application) = {
    val amendedBeneficiaryJson = Json.parse(
      """
        |{
        |  "organisationName": "Nice Charity 2",
        |  "beneficiaryDiscretion": false,
        |  "beneficiaryShareOfIncome": "50",
        |  "identification": {
        |    "address": {
        |      "line1": "Line 1",
        |      "line2": "Line 2",
        |      "postCode": "NE1 1NE",
        |      "country": "GB"
        |    }
        |  },
        |  "entityStart": "2019-02-03"
        |}
        |""".stripMargin
    )

    val addRequest = FakeRequest(POST, "/trusts/amend-charity-beneficiary/5174384721/0")
      .withBody(amendedBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def removeOtherBeneficiary(application: Application) = {
    val removeJson = Json.parse(
      """
        |{
        | "endDate": "2014-03-12",
        | "index": 0,
        | "type": "other"
        |}
        |""".stripMargin)
    val addRequest = FakeRequest(PUT, "/trusts/5174384721/beneficiaries/remove")
      .withBody(removeJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }
}
