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
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CacheRepositoryImpl @Inject()(
                                     mongo: MongoComponent,
                                     config: AppConfig
                                   )(implicit ec: ExecutionContext) extends PlayMongoRepository[JsValue](
  mongoComponent = mongo,
  collectionName = "trusts",
  domainFormat = implicitly[Format[JsValue]],
  indexes = Seq(
    IndexModel(
      Indexes.ascending("updatedAt"),
      IndexOptions().name("etmp-data-updated-at-index").expireAfter(config.ttlInSeconds, TimeUnit.SECONDS).unique(false)
    ),
    IndexModel(
      Indexes.ascending("id"),
      IndexOptions().name("id-index").unique(false)
    )
  )
) with RepositoryHelper[JsValue] with CacheRepository {

  override implicit val executionContext: ExecutionContext = ec
  override val key: String = "etmpData"

  override def get(identifier: String, internalId: String, sessionId: String): Future[Option[JsValue]] = {
    get[JsValue](identifier, internalId, sessionId)
  }

  override def set(identifier: String, internalId: String, sessionId: String, data: JsValue): Future[Boolean] = {
    upsert[JsValue](identifier, internalId, sessionId, data)
  }
}

trait CacheRepository {

  def get(identifier: String, internalId: String, sessionId: String): Future[Option[JsValue]]

  def set(identifier: String, internalId: String, sessionId: String, data: JsValue): Future[Boolean]

  def resetCache(identifier: String, internalId: String, sessionId: String): Future[Option[JsValue]]
}
