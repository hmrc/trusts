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

package uk.gov.hmrc.trusts.actions

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.{ActionRefiner, AnyContent, Request}
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.trusts.controllers.actions.ValidateUTRAction

import scala.concurrent.Future
import uk.gov.hmrc.trusts.models.ApiResponse.invalidUTRErrorResponse

class TrustsActionsSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite{

  def createSUT(utr: String) = ValidateUTRAction(utr)

  "The validateUTR action" should {
    "accept a valid 10 digit UTR" in {
      val action = createSUT("1234567890")
      val response = action.invokeBlock[AnyContent](FakeRequest(GET, "/"),
        _ => Future.successful(Ok))
      await(response) mustBe Ok
    }

    "reject a UTR with less than 10 digits" in {
      val action = createSUT("123456789")
      val response = action.invokeBlock[AnyContent](FakeRequest(GET, "/"),
        _ => Future.successful(Ok))
      await(response) mustBe BadRequest(Json.toJson(invalidUTRErrorResponse))
    }

    "reject a UTR with more than 10 digits" in {
      val action = createSUT("12345678901")
      val response = action.invokeBlock[AnyContent](FakeRequest(GET, "/"),
        _ => Future.successful(Ok))
      await(response) mustBe BadRequest(Json.toJson(invalidUTRErrorResponse))
    }

    "reject a UTR containing letters" in {
      val action = createSUT("12345678AB")
      val response = action.invokeBlock[AnyContent](FakeRequest(GET, "/"),
        _ => Future.successful(Ok))
      await(response) mustBe BadRequest(Json.toJson(invalidUTRErrorResponse))
    }
  }
}
