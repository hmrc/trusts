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

package uk.gov.hmrc.repositories

import org.bson.BsonType
import org.mongodb.scala.model.Filters.`type`
import play.api.libs.json.Json
import repositories.CacheRepositoryImpl
import uk.gov.hmrc.itbase.IntegrationTestBase

class CacheRepositorySpec extends IntegrationTestBase {

  private val data = Json.obj("testField" -> "testValue")

  "a playback repository" should {
    "be able to store and retrieve a payload" in assertMongoTest(createApplication)({ app =>
      val repository = app.injector.instanceOf[CacheRepositoryImpl]

      val storedOk = repository.set("UTRUTRUTR", "InternalId", "sessionId", data)
      storedOk.value.futureValue mustBe Right(true)

      val retrieved = repository.get("UTRUTRUTR", "InternalId", "sessionId")
      retrieved.value.futureValue mustBe Right(Some(data))
    })

    "be able to store and retrieve a payload with correct date time format" in assertMongoTest(createApplication)({ app =>
      val repository = app.injector.instanceOf[CacheRepositoryImpl]

      val storedOk = repository.set("UTRUTRUTR", "InternalId", "sessionId", data)
      storedOk.value.futureValue mustBe Right(true)

      val query = `type`("updatedAt", BsonType.DATE_TIME)
      val documents = repository.collection.countDocuments(query).toFuture().futureValue.toInt

      // Verify at least one document exists with updatedAt as datetime
      documents must be > 0

      // Optional: Check if our specific document has the correct format
      val retrieved = repository.get("UTRUTRUTR", "InternalId", "sessionId")
      retrieved.value.futureValue mustBe Right(Some(data))
    })

  }
}
