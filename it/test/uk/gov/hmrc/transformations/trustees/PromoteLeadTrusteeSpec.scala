/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.transformations.trustees

import cats.data.EitherT
import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse}
import models.variation.{IdentificationType, LeadTrusteeIndType}
import models.{AddressType, NameType}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.Future

class PromoteLeadTrusteeSpec extends IntegrationTestBase {

  val getTrustResponse: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  "a promote lead trustee call" should {

    val newTrusteeIndInfo = LeadTrusteeIndType(
      lineNo = None,
      bpMatchStatus = None,
      name = NameType("John", Some("William"), "O'Connor"),
      dateOfBirth = LocalDate.of(1965, 2, 10),
      phoneNumber = "newPhone",
      email = Some("newEmail"),
      identification = IdentificationType(
        Some("ST123456"),
        None,
        Some(
          AddressType(
            "221B Baker Street",
            "Suite 16",
            Some("Newcastle upon Tyne"),
            None,
            Some("NE1 2LA"),
            "GB"
          )
        ),
        None
      ),
      countryOfResidence = None,
      legallyIncapable = None,
      nationality = None,
      entityStart = LocalDate.of(2012, 2, 20),
      entityEnd = None
    )

    val expectedGetAfterPromoteTrusteeJson: JsValue =
      JsonUtils.getJsonValueFromFile("it/trusts-integration-get-after-promote-trustee.json")

    val stubbedTrustsConnector = mock[TrustsConnector]
    when(stubbedTrustsConnector.getTrustInfo(any()))
      .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(getTrustResponse))))

    def application = applicationBuilder
      .overrides(
        bind[IdentifierAction]
          .toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(stubbedTrustsConnector)
      )
      .build()

    "return amended data in a subsequent 'get' call, for identifier '5174384721'" in assertMongoTest(application)(app =>
      runTest("5174384721", app)
    )

    "return amended data in a subsequent 'get' call, for identifier '0123456789ABCDE'" in assertMongoTest(application)(
      app => runTest("0123456789ABCDE", app)
    )

    def runTest(identifier: String, application: Application): Assertion = {
      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result)        mustBe OK
      contentAsJson(result) mustBe expectedInitialGetJson

      val promoteRequest = FakeRequest(POST, s"/trusts/trustees/promote/$identifier/0")
        .withBody(Json.toJson(newTrusteeIndInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val promoteResult = route(application, promoteRequest).get
      status(promoteResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(newResult)        mustBe OK
      contentAsJson(newResult) mustBe expectedGetAfterPromoteTrusteeJson

    }
  }

}
