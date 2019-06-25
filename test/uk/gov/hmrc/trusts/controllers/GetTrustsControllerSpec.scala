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
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, BearerTokenExpired, MissingBearerToken}
import uk.gov.hmrc.trusts.connectors.{BaseSpec, FakeAuthConnector}
import play.api.test.Helpers._
import uk.gov.hmrc.trusts.models.TrustFoundResponse
import uk.gov.hmrc.trusts.services.{AuthService, DesService}

import scala.concurrent.Future

class GetTrustsControllerSpec extends BaseSpec with GuiceOneServerPerSuite  {

  override val authConnector = mock[AuthConnector]
  val desService = mock[DesService]

  ".get" should {

    "return 200 - Ok" in {
      mockAuthSuccess

      val utr = "1234567890"

      when(desService.getTrustInfo(any())(any())).thenReturn(Future.successful(TrustFoundResponse(utr)))

      val result = getTrustsController.get(utr).apply(FakeRequest(GET, ""))

      status(result) mustBe OK
    }

    "return 400 - BadRequest" when {
      "the UTR given is invalid" in {
        mockAuthSuccess

        val invalidUTR = "1234567"

        val result = getTrustsController.get(invalidUTR).apply(FakeRequest(GET, s"/trusts/$invalidUTR"))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 401 - Unauthorised" when {
      "the get endpoint is called user hasn't logged in" in {
        val utr = "1234567890"
        val mockAuthService = new AuthService(FakeAuthConnector(MissingBearerToken()))
        val SUT = new GetTrustsController(mockAuthService, desService)

        val result = SUT.get(utr).apply(FakeRequest(GET, ""))
        status(result) mustBe UNAUTHORIZED
      }

      "the get endpoint is called user session has expired" in {
        val utr = "1234567890"
        val mockAuthService = new AuthService(FakeAuthConnector(BearerTokenExpired()))
        val SUT = new GetTrustsController(mockAuthService, desService)

        val result = SUT.get(utr).apply(FakeRequest(GET, ""))
        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  private def getTrustsController: GetTrustsController = {
    val mockAuthService = new AuthService(authConnector)
    new GetTrustsController(mockAuthService, desService)
  }
}
