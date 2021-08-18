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
import transformers.ComposedDeltaTransform

import _root_.play.api.libs.json._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TransformationRepositoryImpl @Inject()(
                                              mongo: MongoDriver,
                                              config: AppConfig
                                            )(implicit ec: ExecutionContext) extends RepositoryManager(mongo, config) with TransformationRepository {

  override val collectionName: String = "transforms"

  override val lastUpdatedIndexName: String = "transformation-data-updated-at-index"

  override val key: String = "transforms"

  override def get(identifier: String, internalId: String): Future[Option[ComposedDeltaTransform]] = {

    val x = get[ComposedDeltaTransform](identifier, internalId)

    println("\t\t !!DEBUG!!: TRANSFORMATION REPO GET " + x)

    x
  }

  override def set(identifier: String, internalId: String, transforms: ComposedDeltaTransform): Future[Boolean] = {

    println("\t\t !!DEBUG!!: TRANSFORMATION REPO SET " + transforms)

    set[ComposedDeltaTransform](identifier, internalId, transforms)
  }
}

trait TransformationRepository {

  def  get(identifier: String, internalId: String): Future[Option[ComposedDeltaTransform]]

  def set(identifier: String, internalId: String, transforms: ComposedDeltaTransform): Future[Boolean]

  def resetCache(identifier: String, internalId: String): Future[Option[JsObject]]
}
