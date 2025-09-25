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

package base

import config.AppConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, Inside}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import java.util.UUID

class BaseSpec extends AnyWordSpec
  with ScalaFutures
  with MockitoSugar
  with JsonFixtures
  with BeforeAndAfter
  with Matchers
  with GuiceOneServerPerSuite
  with Inside {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  lazy val application = applicationBuilder().build()

  def injector = application.injector

  def appConfig: AppConfig = injector.instanceOf[AppConfig]

  def applicationBuilder(): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        Seq(
          "metrics.enabled" -> false,
          "auditing.enabled" -> false,
          "nrs.retryWaitMs" -> 10,
          "nrs.retryWaitFactor" -> 1,
          "nrs.totalAttempts" -> 10,
          "features.nonRepudiate" -> true
        ): _*
      )
  }

  val parsers = stubControllerComponents().parsers.defaultBodyParser

  def fakeRequest: FakeRequest[JsValue] = FakeRequest("POST", "")
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withHeaders(Headers.DRAFT_REGISTRATION_ID -> "bbe4c063-2b5a-4f29-bfa6-46c3c8906b0d")
    .withHeaders(Headers.TRUE_USER_AGENT -> "Mozilla")
    .withBody(Json.parse("{}"))

  def postRequestWithPayload(payload: JsValue, withDraftId: Boolean = true): FakeRequest[JsValue] = {
    if (withDraftId) {
      FakeRequest("POST", "/trusts/register")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withHeaders(Headers.DRAFT_REGISTRATION_ID -> UUID.randomUUID().toString)
        .withBody(payload)
    } else {
      FakeRequest("POST", "/trusts/register")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody(payload)
    }
  }
}






