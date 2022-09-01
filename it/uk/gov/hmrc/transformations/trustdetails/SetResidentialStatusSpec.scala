///*
// * Copyright 2020 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.transformations.trustdetails
//
//import connector.TrustsConnector
//import controllers.actions.{FakeIdentifierAction, IdentifierAction}
//import models.get_trust.GetTrustSuccessResponse
//import models.{NonUKType, ResidentialStatusType}
//import org.mockito.ArgumentMatchers._
//import org.mockito.Mockito._
//import org.scalatest.Assertion
//import org.scalatest.matchers.must.Matchers._
//import org.scalatest.freespec.AsyncFreeSpec
//import org.scalatestplus.mockito.MockitoSugar
//import play.api.Application
//import play.api.inject.bind
//import play.api.libs.json.{JsValue, Json}
//import play.api.test.Helpers._
//import play.api.test.{FakeRequest, Helpers}
//import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
//import uk.gov.hmrc.itbase.IntegrationTestBase
//import utils.JsonUtils
//
//import scala.concurrent.Future
//
//class SetResidentialStatusSpec extends AsyncFreeSpec with MockitoSugar with IntegrationTestBase {
//
//  "a set residential status call" - {
//
//    val getTrustResponse : JsValue = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
//
//    val stubbedTrustsConnector = mock[TrustsConnector]
//
//    when(stubbedTrustsConnector.getTrustInfo(any()))
//      .thenReturn(Future.successful(getTrustResponse.as[GetTrustSuccessResponse]))
//
//    val application = applicationBuilder
//      .overrides(
//        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
//        bind[TrustsConnector].toInstance(stubbedTrustsConnector)
//      ).build()
//
//    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { application =>
//      runTest("0123456789", application)
//      runTest("0123456789ABCDE", application)
//    }
//
//    def runTest(identifier: String, application: Application): Assertion = {
//      val initialGetResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
//      status(initialGetResult) mustBe OK
//
//      val body = Json.toJson(ResidentialStatusType(None, Some(NonUKType(sch5atcgga92 = true, None, None, None))))
//
//      val setValueRequest = FakeRequest(PUT, s"/trusts/trust-details/$identifier/residential-status")
//        .withBody(body)
//        .withHeaders(CONTENT_TYPE -> "application/json")
//
//      val setValueResponse = route(application, setValueRequest).get
//      status(setValueResponse) mustBe OK
//
//      val subsequentGetResult = route(application, FakeRequest(GET, s"/trusts/trust-details/$identifier/transformed")).get
//      status(subsequentGetResult) mustBe OK
//
//      val detail = (contentAsJson(subsequentGetResult) \ "residentialStatus").as[JsValue]
//      detail mustBe body
//
//    }
//  }
//}
