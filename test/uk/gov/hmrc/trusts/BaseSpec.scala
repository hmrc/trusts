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

package uk.gov.hmrc.trusts

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, Inside, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.utils._

class BaseSpec extends WordSpec
  with MustMatchers
  with ScalaFutures
  with MockitoSugar
  with JsonRequests
  with BeforeAndAfter
  with GuiceOneServerPerSuite
  with Inside {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  lazy val application = applicationBuilder().build()

  def injector = application.injector

  def appConfig : AppConfig = injector.instanceOf[AppConfig]

  def applicationBuilder(): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        Seq(
          "metrics.enabled" -> false,
          "auditing.enabled" -> false): _*
      )
  }

  def fakeRequest : FakeRequest[JsValue] = FakeRequest("POST", "")
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withHeaders(Headers.DraftRegistrationId -> UUID.randomUUID().toString)
    .withBody(Json.parse("{}"))

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  def postRequestWithPayload(payload: JsValue, withDraftId: Boolean = true): FakeRequest[JsValue] = {
    if (withDraftId) {
      FakeRequest("POST", "/trusts/register")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withHeaders(Headers.DraftRegistrationId -> UUID.randomUUID().toString)
        .withBody(payload)
    } else {
      FakeRequest("POST", "/trusts/register")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody(payload)
    }
  }
}






