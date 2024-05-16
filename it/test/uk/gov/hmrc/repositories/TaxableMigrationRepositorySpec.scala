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

import org.scalatest.matchers.must.Matchers._
import repositories.TaxableMigrationRepositoryImpl
import uk.gov.hmrc.itbase.IntegrationTestBase

class TaxableMigrationRepositorySpec extends IntegrationTestBase {

  "TaxableMigrationRepository" should {

    "be able to store and retrieve a boolean" in assertMongoTest(createApplication)({ (app) =>
      val repository = app.injector.instanceOf[TaxableMigrationRepositoryImpl]
      val migratingToTaxable = true

      val storedOk = repository.set("UTRUTRUTR", "InternalId", "sessionId", migratingToTaxable)
      storedOk.value.futureValue mustBe Right(true)

      val retrieved = repository.get("UTRUTRUTR", "InternalId", "sessionId")

      retrieved.value.futureValue mustBe Right(Some(migratingToTaxable))
    })
  }
}
