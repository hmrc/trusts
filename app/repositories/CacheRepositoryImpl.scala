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

package repositories

import config.AppConfig
import play.api.libs.json._
import reactivemongo.api.indexes.{Index, IndexType}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CacheRepositoryImpl @Inject()(
                                     mongo: MongoDriver,
                                     config: AppConfig
                                   )(implicit ec: ExecutionContext) extends IndexesManager(mongo, config) with CacheRepository {

  override val collectionName: String = "trusts"

  override val cacheTtl: Int = config.ttlInSeconds

  override val lastUpdatedIndexName: String = "etmp-data-updated-at-index"

  override val key: String = "etmpData"

  override val idIndex: Index = Index(
    key = Seq("id" -> IndexType.Ascending),
    name = Some("id-index")
  )

  override def get(identifier: String, internalId: String): Future[Option[JsValue]] = {
    get[JsValue](identifier, internalId)
  }

  override def set(identifier: String, internalId: String, data: JsValue): Future[Boolean] = {
    set[JsValue](identifier, internalId, data)
  }
}

trait CacheRepository {

  def get(identifier: String, internalId: String): Future[Option[JsValue]]

  def set(identifier: String, internalId: String, data: JsValue): Future[Boolean]

  def resetCache(identifier: String, internalId: String): Future[Option[JsObject]]
}
