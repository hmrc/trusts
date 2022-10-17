/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.repositories

import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import repositories.CacheRepositoryImpl
import uk.gov.hmrc.itbase.IntegrationTestBase

class CacheRepositorySpec extends IntegrationTestBase {

  private val data = Json.obj("testField" -> "testValue")

  "a playback repository" should {
    "be able to store and retrieve a payload" in {
      val repository = app.injector.instanceOf[CacheRepositoryImpl]

      val storedOk = repository.set("UTRUTRUTR", "InternalId", "sessionId", data)
      storedOk.futureValue mustBe true

      val retrieved = repository.get("UTRUTRUTR", "InternalId", "sessionId")
      retrieved.futureValue mustBe Some(data)
    }
  }
}
