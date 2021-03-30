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

import play.api.Logging
import repositories.TaxableMigrationRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxableMigrationService @Inject()(taxableMigrationRepository: TaxableMigrationRepository) extends Logging {

  def migratingFromNonTaxableToTaxable(identifier: String, internalId: String): Future[Boolean] = {
    taxableMigrationRepository.get(identifier, internalId).map {
      case Some(value) => value
      case None => false
    }.recoverWith {
      case e =>
        logger.error(s"Error getting taxable migration flag from repository: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def setTaxableMigrationFlag(identifier: String, internalId: String, migrationToTaxable: Boolean): Future[Boolean] = {
    taxableMigrationRepository.set(identifier, internalId, migrationToTaxable)
  }
}
