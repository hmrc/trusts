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

package uk.gov.hmrc.transformations.beneficiaries

import cats.data.EitherT
import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse}
import models.variation.VariationSuccessResponse
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.test.Helpers.{CONTENT_TYPE, GET, POST, contentAsJson, route, status, _}
import play.api.test.{FakeRequest, Helpers}
import services.dates.LocalDateService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.Future

class ComboBeneficiarySpec extends IntegrationTestBase {

  private lazy val getTrustResponse: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  private lazy val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  private object TestLocalDateService extends LocalDateService {
    override def now: LocalDate = LocalDate.of(2020, 4, 1)
  }

  "doing a bunch of beneficiary transforms" should {
    lazy val expectedGetAfterAddBeneficiaryJson: JsValue =
      JsonUtils.getJsonValueFromFile("it/trusts-integration-get-after-combo-beneficiary.json")

    lazy val expectedDeclaredBeneficiaryJson: JsValue =
      JsonUtils.getJsonValueFromFile("it/trusts-integration-declared-combo-beneficiary.json")

    val stubbedTrustsConnector = mock[TrustsConnector]
    when(stubbedTrustsConnector.getTrustInfo(any()))
      .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(getTrustResponse))))

    def application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(stubbedTrustsConnector),
        bind[LocalDateService].toInstance(TestLocalDateService)
      )
      .build()

    "return amended data in a subsequent 'get' call, for identifier '5174384721'" in assertMongoTest(application) { app =>
      runTest("5174384721", app)
    }

    "return amended data in a subsequent 'get' call, for identifier '0123456789ABCDE'" in assertMongoTest(application) { app =>
      runTest("0123456789ABCDE", app)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe expectedInitialGetJson

      status(addCharityBeneficiary(identifier, application)) mustBe OK
      status(addIndividualBeneficiary(identifier, application)) mustBe OK
      status(addUnidentifiedBeneficiary(identifier, application)) mustBe OK
      status(amendUnidentifiedBeneficiary(identifier, application)) mustBe OK
      status(removeCharityBeneficiary(identifier, application)) mustBe OK
      status(amendCharityBeneficiary(identifier, application)) mustBe OK
      status(removeOtherBeneficiary(identifier, application)) mustBe OK
      status(amendCompanyBeneficiary(identifier, application)) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(newResult) mustBe OK
      contentAsJson(newResult) mustBe expectedGetAfterAddBeneficiaryJson

      lazy val variationResponse = VariationSuccessResponse("TVN12345678")
      lazy val payloadCaptor = ArgumentCaptor.forClass(classOf[JsValue])

      when(stubbedTrustsConnector.trustVariation(payloadCaptor.capture()))
        .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Right(variationResponse))))

      val declaration = Json.parse(
        """
          |{
          | "declaration": {
          |     "name": { "firstName": "John", "lastName": "Doe" }
          | }
          |}
          |""".stripMargin)

      val declareRequest = FakeRequest(POST, s"/trusts/declare/$identifier")
        .withBody(declaration)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val declareResult = route(application, declareRequest).get
      status(declareResult) mustBe OK

      expectedDeclaredBeneficiaryJson mustBe payloadCaptor.getValue
    }
  }

  private def addCharityBeneficiary(identifier: String, application: Application) = {
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

    val addRequest = FakeRequest(POST, s"/trusts/beneficiaries/add-charity/$identifier")
      .withBody(newBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def addIndividualBeneficiary(identifier: String, application: Application) = {
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

    val addRequest = FakeRequest(POST, s"/trusts/beneficiaries/add-individual/$identifier")
      .withBody(newBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def addUnidentifiedBeneficiary(identifier: String, application: Application) = {
    val newBeneficiaryJson = Json.parse(
      """
        |{
        | "description": "New Beneficiary Description",
        | "entityStart": "2020-01-01"
        |}
        |""".stripMargin
    )

    val addRequest = FakeRequest(POST, s"/trusts/beneficiaries/add-unidentified/$identifier")
      .withBody(newBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def amendUnidentifiedBeneficiary(identifier: String, application: Application) = {
    val addRequest = FakeRequest(POST, s"/trusts/beneficiaries/amend-unidentified/$identifier/0")
      .withBody(JsString("Amended Beneficiary Description"))
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def removeCharityBeneficiary(identifier: String, application: Application) = {
    val removeJson = Json.parse(
      """
        |{
        | "endDate": "2014-03-12",
        | "index": 0,
        | "type": "charity"
        |}
        |""".stripMargin)
    val addRequest = FakeRequest(PUT, s"/trusts/beneficiaries/$identifier/remove")
      .withBody(removeJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def amendCharityBeneficiary(identifier: String, application: Application) = {
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

    val addRequest = FakeRequest(POST, s"/trusts/beneficiaries/amend-charity/$identifier/0")
      .withBody(amendedBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def removeOtherBeneficiary(identifier: String, application: Application) = {
    val removeJson = Json.parse(
      """
        |{
        | "endDate": "2014-03-16",
        | "index": 0,
        | "type": "other"
        |}
        |""".stripMargin)
    val addRequest = FakeRequest(PUT, s"/trusts/beneficiaries/$identifier/remove")
      .withBody(removeJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }

  private def amendCompanyBeneficiary(identifier: String, application: Application) = {
    val amendedBeneficiaryJson = Json.parse(
      """
        |{
        |  "organisationName": "Nice Company 2",
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

    val addRequest = FakeRequest(POST, s"/trusts/beneficiaries/amend-company/$identifier/0")
      .withBody(amendedBeneficiaryJson)
      .withHeaders(CONTENT_TYPE -> "application/json")

    route(application, addRequest).get
  }
}
