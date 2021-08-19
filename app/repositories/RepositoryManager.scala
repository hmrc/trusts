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

import _root_.play.api.libs.json._
import config.AppConfig
import play.api.Logging
import reactivemongo.api.WriteConcern
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.indexes.IndexType
import reactivemongo.play.json.compat.bson2json.{fromDocumentWriter, fromWriter}
import reactivemongo.play.json.compat.jsObjectWrites
import reactivemongo.play.json.compat.json2bson.{toDocumentReader, toDocumentWriter}
import reactivemongo.play.json.compat.lax._

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
      collection
        .find(selector(identifier, internalId), None)
        .one[JsObject]
        .map { opt =>
          for {
            document <- opt
            data <- (document \ key).asOpt[T]
          } yield data
        }
    }
  }

  def set[T](identifier: String, internalId: String, data: T)(implicit wts: Writes[T]): Future[Boolean] = {

    val dataToWrite = Json.toJson(data)

    val modifier = Json.obj(
      "$set" -> Json.obj("id" -> createKey(identifier, internalId), key -> dataToWrite),
      "$currentDate" -> Json.obj("updatedAt" -> true)
    )

    collection.flatMap {
      _.update(ordered = false)
        .one(selector(identifier, internalId), modifier, upsert = true, multi = false)
        .map { x => x.writeErrors.isEmpty }
    }
  }

  def resetCache(identifier: String, internalId: String): Future[Option[JsObject]] = {
    collection.flatMap {
      _.findAndRemove(selector(identifier, internalId), None, None, WriteConcern.Default, None, None, Seq.empty)
        .map { x =>
          x.value.map(y => Json.toJson(y).as[JsObject])
        }
    }
  }

  private def collection: Future[BSONCollection] =
    for {
      _ <- ensureIndexes
      res <- mongo.api.database.map(_.collection[BSONCollection](collectionName))
    } yield res

  private def ensureIndexes: Future[Boolean] = {

    lazy val lastUpdatedIndex = MongoIndex(
      key = Seq("updatedAt" -> IndexType.Ascending),
      name = lastUpdatedIndexName,
      expireAfterSeconds = Some(cacheTtl)
    )

    lazy val idIndex = MongoIndex(
      key = Seq("id" -> IndexType.Ascending),
      name = "id-index"
    )

    for {
      collection              <- mongo.api.database.map(_.collection[BSONCollection](collectionName))
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
        collection <- mongo.api.database.map(_.collection[BSONCollection](collectionName))
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
          collection <- mongo.api.database.map(_.collection[BSONCollection](collectionName))
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
