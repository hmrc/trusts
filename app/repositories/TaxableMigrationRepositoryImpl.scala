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
import org.mongodb.scala.model._
import repositories.MongoFormats._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxableMigrationRepositoryImpl @Inject()(
                                                mongo: MongoComponent,
                                                config: AppConfig
                                              )(implicit ec: ExecutionContext) extends PlayMongoRepository[Boolean](
  mongoComponent = mongo,
  collectionName = "taxable-migration",
  domainFormat = booleanFormat,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("updatedAt"),
      IndexOptions().name("taxable-migration-updated-at-index").expireAfter(config.ttlInSeconds, TimeUnit.SECONDS).unique(false)
    ),
    IndexModel(
      Indexes.ascending("id"),
      IndexOptions().name("id-index").unique(false)
    )
  )
) with RepositoryHelper[Boolean] with TaxableMigrationRepository {

  override implicit val executionContext: ExecutionContext = ec
  override val key: String = "migratingToTaxable"

  override def get(identifier: String, internalId: String, sessionId: String): Future[Option[Boolean]] = {
    getOpt(identifier, internalId, sessionId)
  }

  override def set(identifier: String, internalId: String, sessionId: String, migratingToTaxable: Boolean): Future[Boolean] = {
    upsert(identifier, internalId, sessionId, migratingToTaxable)
  }
}

trait TaxableMigrationRepository {

  def get(identifier: String, internalId: String, sessionId: String): Future[Option[Boolean]]

  def set(identifier: String, internalId: String, sessionId: String, migratingToTaxable: Boolean): Future[Boolean]
}
