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

package uk.gov.hmrc.trusts.controllers.actions

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.trusts.models.ApiResponse.invalidUTRErrorResponse

import scala.concurrent.Future

class ValidateIdentifierActionSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {

  def createSUT(identifier: String) =
    app.injector.instanceOf[ValidateIdentifierActionProvider].apply(identifier)

  "The validate identifier action" when {

    "provided a UTR" should {

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

    "provided a URN" should {

      "accept a valid 15 character URN" in {
        val action = createSUT("1234567827ACDEF")
        val response = action.invokeBlock[AnyContent](FakeRequest(GET, "/"),
          _ => Future.successful(Ok))
        await(response) mustBe Ok
      }

      "reject a URN with less than 15 characters" in {
        val action = createSUT("1234567827ACDE")
        val response = action.invokeBlock[AnyContent](FakeRequest(GET, "/"),
          _ => Future.successful(Ok))
        await(response) mustBe BadRequest(Json.toJson(invalidUTRErrorResponse))
      }

      "reject a URN with more than 15 characters" in {
        val action = createSUT("1234567827ACDEFC")
        val response = action.invokeBlock[AnyContent](FakeRequest(GET, "/"),
          _ => Future.successful(Ok))
        await(response) mustBe BadRequest(Json.toJson(invalidUTRErrorResponse))
      }
    }

  }
}
