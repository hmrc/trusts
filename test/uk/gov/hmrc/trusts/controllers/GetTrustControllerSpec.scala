/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.controllers

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.services.DesService

import scala.concurrent.Future

class GetTrustControllerSpec extends BaseSpec  {

  val desService = mock[DesService]



  private def getTrustsController = {
    val SUT = new GetTrustController(new FakeIdentifierAction(Organisation), desService)
    SUT
  }

  ".get" should {

    "return 200 - Ok" in {

      val utr = "1234567890"

      when(desService.getTrustInfo(any())(any())).thenReturn(Future.successful(TrustFoundResponse(None, ResponseHeader("TODO", 1))))

      val result = getTrustsController.get(utr).apply(FakeRequest(GET, ""))

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
    }

    "return 400 - BadRequest" when {
      "the UTR given is invalid" in {
        

        val invalidUTR = "1234567"

        val result = getTrustsController.get(invalidUTR).apply(FakeRequest(GET, s"/trusts/$invalidUTR"))

        status(result) mustBe BAD_REQUEST
      }
    }

//    "return 401 - Unauthorised" when {
//      "the get endpoint is called user hasn't logged in" in {
//        val utr = "1234567890"
//        val mockAuthService = new AuthService(FakeAuthConnector(MissingBearerToken()))
//        val SUT = new GetTrustController(mockAuthService, desService)
//
//        val result = SUT.get(utr).apply(FakeRequest(GET, ""))
//        status(result) mustBe UNAUTHORIZED
//      }
//
//      "the get endpoint is called user session has expired" in {
//        val utr = "1234567890"
//        val mockAuthService = new AuthService(FakeAuthConnector(BearerTokenExpired()))
//        val SUT = new GetTrustController(mockAuthService, desService)
//
//        val result = SUT.get(utr).apply(FakeRequest(GET, ""))
//        status(result) mustBe UNAUTHORIZED
//      }
//    }

    "return 500 - InternalServerError" when {
      "the get endpoint returns a InvalidUTRResponse" in {
        when(desService.getTrustInfo(any())(any())).thenReturn(Future.successful(InvalidUTRResponse))

        val utr = "1234567890"
        val result = getTrustsController.get(utr).apply(FakeRequest(GET, ""))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the get endpoint returns a InvalidRegimeResponse" in {

        when(desService.getTrustInfo(any())(any())).thenReturn(Future.successful(InvalidRegimeResponse))

        val utr = "1234567890"
        val result = getTrustsController.get(utr).apply(FakeRequest(GET, ""))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the get endpoint returns a BadRequestResponse" in {

        when(desService.getTrustInfo(any())(any())).thenReturn(Future.successful(BadRequestResponse))

        val utr = "1234567890"
        val result = getTrustsController.get(utr).apply(FakeRequest(GET, ""))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the get endpoint returns a ResourceNotFoundResponse" in {

        when(desService.getTrustInfo(any())(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val utr = "1234567890"
        val result = getTrustsController.get(utr).apply(FakeRequest(GET, ""))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the get endpoint returns a InternalServerErrorResponse" in {

        when(desService.getTrustInfo(any())(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val utr = "1234567890"
        val result = getTrustsController.get(utr).apply(FakeRequest(GET, ""))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the get endpoint returns a ServiceUnavailableResponse" in {

        when(desService.getTrustInfo(any())(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val utr = "1234567890"
        val result = getTrustsController.get(utr).apply(FakeRequest(GET, ""))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
