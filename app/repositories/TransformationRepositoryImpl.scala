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
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import transformers.ComposedDeltaTransform
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TransformationRepositoryImpl @Inject()(
                                              mongo: MongoComponent,
                                              config: AppConfig
                                            )(implicit ec: ExecutionContext) extends PlayMongoRepository[ComposedDeltaTransform](
  mongoComponent = mongo,
  collectionName = "transforms",
  domainFormat = ComposedDeltaTransform.format,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("updatedAt"),
      IndexOptions().name("transformation-data-updated-at-index").expireAfter(config.ttlInSeconds, TimeUnit.SECONDS).unique(false)
    ),
    IndexModel(
      Indexes.ascending("id"),
      IndexOptions().name("id-index").unique(false)
    )
  )
) with RepositoryHelper[ComposedDeltaTransform] with TransformationRepository {
  override implicit val executionContext: ExecutionContext = ec
  override val key: String = "transforms"

  override def get(identifier: String, internalId: String, sessionId: String): Future[Option[ComposedDeltaTransform]] = {
    get[ComposedDeltaTransform](identifier, internalId, sessionId)
  }

  override def set(identifier: String, internalId: String, sessionId: String, transforms: ComposedDeltaTransform): Future[Boolean] = {
    upsert[ComposedDeltaTransform](identifier, internalId, sessionId, transforms)
  }
}

trait TransformationRepository {

  def get(identifier: String, internalId: String, sessionId: String): Future[Option[ComposedDeltaTransform]]

  def set(identifier: String, internalId: String, sessionId: String, transforms: ComposedDeltaTransform): Future[Boolean]

  def resetCache(identifier: String, internalId: String, sessionId: String): Future[Option[ComposedDeltaTransform]]
}
