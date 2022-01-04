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
import play.api.Logging
import play.api.libs.json.{JsObject, Json, Reads, Writes}
import reactivemongo.api.WriteConcern
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import java.sql.Timestamp
import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

abstract class RepositoryManager @Inject()(
                                         mongo: MongoDriver,
                                         config: AppConfig
                                       )(implicit ec: ExecutionContext) extends Logging {

  val collectionName: String

  val cacheTtl: Int = config.ttlInSeconds

  val lastUpdatedIndexName: String

  val key: String

  def get[T](identifier: String, internalId: String)(implicit rds: Reads[T]): Future[Option[T]] = {
    collection.flatMap { collection =>
      collection.find(selector(identifier, internalId), None).one[JsObject].map(opt =>
        for  {
          document <- opt
          data <- (document \ key).asOpt[T]
        } yield data
      )
    }
  }

  def set[T](identifier: String, internalId: String, data: T)(implicit wts: Writes[T]): Future[Boolean] = {

    val modifier = Json.obj(
      "$set" -> Json.obj(
        "id" -> createKey(identifier, internalId),
        "updatedAt" -> Json.obj("$date" -> Timestamp.valueOf(LocalDateTime.now())),
        key -> Json.toJson(data)
      )
    )

    collection.flatMap {
      _.update(ordered = false).one(selector(identifier, internalId), modifier, upsert = true, multi = false).map {
        result => result.ok
      }
    }
  }

  def resetCache(identifier: String, internalId: String): Future[Option[JsObject]] = {
    collection.flatMap(_.findAndRemove(selector(identifier, internalId), None, None, WriteConcern.Default, None, None, Seq.empty).map(
      _.value
    ))
  }

  private def collection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      res <- Future.successful(mongo.api.collection[JSONCollection](collectionName))
    } yield res

  private def ensureIndexes: Future[Boolean] = {
    logger.info("Ensuring collection indexes")

    lazy val lastUpdatedIndex: Index = Index(
      key = Seq("updatedAt" -> IndexType.Ascending),
      name = Some(lastUpdatedIndexName),
      options = BSONDocument("expireAfterSeconds" -> cacheTtl)
    )

    lazy val idIndex: Index = Index(
      key = Seq("id" -> IndexType.Ascending),
      name = Some("id-index")
    )

    for {
      collection              <- Future.successful(mongo.api.collection[JSONCollection](collectionName))
      createdLastUpdatedIndex <- collection.indexesManager.ensure(lastUpdatedIndex)
      createdIdIndex          <- collection.indexesManager.ensure(idIndex)
    } yield createdLastUpdatedIndex && createdIdIndex
  }

  private def selector(identifier: String, internalId: String): JsObject = Json.obj(
    "id" -> createKey(identifier, internalId)
  )

  private def createKey(identifier: String, internalId: String): String = s"$identifier-$internalId"

  final val dropIndexes: Unit = {

    val dropIndexesFeatureEnabled: Boolean = config.dropIndexesEnabled

    def logIndexes: Future[Unit] = {
      for {
        collection <- Future.successful(mongo.api.collection[JSONCollection](collectionName))
        indexes <- collection.indexesManager.list()
      } yield {
        logger.info(s"[IndexesManager] indexes found on mongo collection $collectionName: $indexes")
        ()
      }
    }

    for {
      _ <- logIndexes
      _ <- if (dropIndexesFeatureEnabled) {
        for {
          collection <- Future.successful(mongo.api.collection[JSONCollection](collectionName))
          _ <- collection.indexesManager.dropAll()
          _ <- Future.successful(logger.info(s"[IndexesManager] dropped indexes on collection $collectionName"))
          _ <- logIndexes
        } yield ()
      } else {
        logger.info(s"[IndexesManager] indexes not modified on collection $collectionName")
        Future.successful(())
      }
    } yield ()
  }

}
