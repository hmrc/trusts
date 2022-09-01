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

package uk.gov.hmrc.repositories

import org.mongodb.scala.Document
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.TaxableMigrationRepositoryImpl
import uk.gov.hmrc.itbase.IntegrationTestBase

class TaxableMigrationRepositorySpec extends AsyncFreeSpec with IntegrationTestBase {

  private val repository = createApplication.injector.instanceOf[TaxableMigrationRepositoryImpl]

  private def dropDB(): Unit = {
    await(repository.collection.deleteMany(filter = Document()).toFuture())
    await(repository.ensureIndexes)
  }

  "TaxableMigrationRepository" - {

    "must be able to store and retrieve a boolean" in {
      dropDB()

      val migratingToTaxable = true

      val storedOk = repository.set("UTRUTRUTR", "InternalId", "sessionId", migratingToTaxable)
      storedOk.futureValue mustBe true

      val retrieved = repository.get("UTRUTRUTR", "InternalId", "sessionId")

      retrieved.futureValue mustBe Some(migratingToTaxable)
    }
  }
}
