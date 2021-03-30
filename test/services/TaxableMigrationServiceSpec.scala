/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.RecoverMethods.recoverToSucceededIf
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import repositories.TaxableMigrationRepositoryImpl
import utils.JsonFixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxableMigrationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonFixtures {

  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private val identifier: String = "utr"
  private val internalId = "internalId"

  "TaxableMigrationService" - {

    ".migratingFromNonTaxableToTaxable" - {

      "must return true" - {
        "when true returned from repository" in {
          val mockRepository = mock[TaxableMigrationRepositoryImpl]
          val service = new TaxableMigrationService(mockRepository)

          when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(true)))

          val result = service.migratingFromNonTaxableToTaxable(identifier, internalId)

          whenReady(result) { r =>
            r mustBe true
          }
        }
      }

      "must return false" - {
        "when false returned from repository" in {
          val mockRepository = mock[TaxableMigrationRepositoryImpl]
          val service = new TaxableMigrationService(mockRepository)

          when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(false)))

          val result = service.migratingFromNonTaxableToTaxable(identifier, internalId)

          whenReady(result) { r =>
            r mustBe false
          }
        }
      }

      "must return error" - {
        "failed to get the transforms from the repository" in {
          val mockRepository = mock[TaxableMigrationRepositoryImpl]
          val service = new TaxableMigrationService(mockRepository)

          when(mockRepository.get(any(), any())).thenReturn(Future.failed(new Throwable("repository get failed")))

          val result = service.migratingFromNonTaxableToTaxable(identifier, internalId)

          recoverToSucceededIf[Exception](result)
        }
      }
    }

    ".setTaxableMigrationFlag" - {
      "must call repository set method" in {
        val mockRepository = mock[TaxableMigrationRepositoryImpl]
        val service = new TaxableMigrationService(mockRepository)

        when(mockRepository.set(any(), any(), any())).thenReturn(Future.successful(true))

        val result = service.setTaxableMigrationFlag(identifier, internalId, migrationToTaxable = true)

        whenReady(result) { r =>
          r mustBe true
          verify(mockRepository).set(identifier, internalId, migratingToTaxable = true)
        }
      }
    }
  }
}
