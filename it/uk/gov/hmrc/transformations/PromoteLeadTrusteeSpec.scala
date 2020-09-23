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

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.repositories.TransformIntegrationTest
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, GetTrustSuccessResponse}
import uk.gov.hmrc.trusts.models.{AddressType, NameType}
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class PromoteLeadTrusteeSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("it/trusts-integration-get-initial.json")

  "a promote lead trustee call" - {

    "must return amended data in a subsequent 'get' call" in {

      val newTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("John", Some("William"), "O'Connor"),
        dateOfBirth = LocalDate.of(1965, 2, 10),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(
          None,
          Some("ST123456"),
          None,
          Some(AddressType(
            "221B Baker Street",
            "Suite 16",
            Some("Newcastle upon Tyne"),
            None,
            Some("NE1 2LA"),
            "GB"
          ))),
          None
      )

      val expectedGetAfterPromoteTrusteeJson: JsValue = JsonUtils.getJsonValueFromFile("it/trusts-integration-get-after-promote-trustee.json")

      val stubbedDesConnector = mock[DesConnector]
      when(stubbedDesConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponseFromDES))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .build()

      running(application) {
        getConnection(application).map { connection =>
          dropTheDatabase(connection)
          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedInitialGetJson

          val promoteRequest = FakeRequest(POST, "/trusts/trustees/promote/5174384721/0")
            .withBody(Json.toJson(newTrusteeIndInfo))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val promoteResult = route(application, promoteRequest).get
          status(promoteResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe expectedGetAfterPromoteTrusteeJson

          dropTheDatabase(connection)
        }.get
      }
    }
  }

}
