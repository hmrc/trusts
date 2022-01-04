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

package repositories

import config.AppConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxableMigrationRepositoryImpl @Inject()(
                                                mongo: MongoDriver,
                                                config: AppConfig
                                              )(implicit ec: ExecutionContext) extends RepositoryManager(mongo, config) with TaxableMigrationRepository {

  override val collectionName: String = "taxable-migration"

  override val lastUpdatedIndexName: String = "taxable-migration-updated-at-index"

  override val key: String = "migratingToTaxable"

  override def get(identifier: String, internalId: String): Future[Option[Boolean]] = {
    get[Boolean](identifier, internalId)
  }

  override def set(identifier: String, internalId: String, migratingToTaxable: Boolean): Future[Boolean] = {
    set[Boolean](identifier, internalId, migratingToTaxable)
  }
}

trait TaxableMigrationRepository {

  def get(identifier: String, internalId: String): Future[Option[Boolean]]

  def set(identifier: String, internalId: String, migratingToTaxable: Boolean): Future[Boolean]
}
