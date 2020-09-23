/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.repositories

import java.sql.Timestamp
import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.libs.json._
import reactivemongo.api.WriteConcern
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.trusts.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CacheRepositoryImpl @Inject()(
                                          mongo: MongoDriver,
                                          config: AppConfig
                                   )(implicit ec: ExecutionContext) extends CacheRepository {

  private val logger = LoggerFactory.getLogger("application" + classOf[CacheRepositoryImpl].getCanonicalName)
  private val collectionName: String = "trusts"
  private val cacheTtl = config.ttlInSeconds

  private def collection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      res <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
    } yield res


  private val lastUpdatedIndex = Index(
    key = Seq("updatedAt" -> IndexType.Ascending),
    name = Some("etmp-data-updated-at-index"),
    options = BSONDocument("expireAfterSeconds" -> cacheTtl)
  )

  private val idIndex = Index(
    key = Seq("id" -> IndexType.Ascending),
    name = Some("id-index")
  )

  private lazy val ensureIndexes = {
    logger.info("Ensuring collection indexes")
    for {
      collection              <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
      createdLastUpdatedIndex <- collection.indexesManager.ensure(lastUpdatedIndex)
      createdIdIndex          <- collection.indexesManager.ensure(idIndex)
    } yield createdLastUpdatedIndex && createdIdIndex
  }

  override def get(utr: String, internalId: String): Future[Option[JsValue]] = {

    val selector = Json.obj(
      "id" -> createKey(utr, internalId)
    )

    collection.flatMap {collection =>

      collection.find(selector, None).one[JsObject].map(opt =>
        for  {
          document <- opt
          etmpData <- (document \ "etmpData").toOption
        } yield etmpData)
    }
  }

  private def createKey(utr: String, internalId: String) = {
    (utr + '-' + internalId)
  }

  override def set(utr: String, internalId: String, data: JsValue): Future[Boolean] = {

    val selector = Json.obj(
      "id" -> createKey(utr, internalId)
    )

    val modifier = Json.obj(
      "$set" -> Json.obj(
        "id" -> createKey(utr, internalId),
        "updatedAt" ->  Json.obj("$date" -> Timestamp.valueOf(LocalDateTime.now())),
        "etmpData" -> data
      )
    )

    collection.flatMap {
      _.update(ordered = false).one(selector, modifier, upsert = true, multi = false).map {
            result => result.ok
      }
    }
  }

  override def resetCache(utr: String, internalId: String): Future[Option[JsObject]] = {
    val selector = Json.obj(
      "id" -> createKey(utr, internalId)
    )

    collection.flatMap(_.findAndRemove(selector, None, None, WriteConcern.Default, None, None, Seq.empty).map(
      _.value
    ))
  }
}

trait CacheRepository {

  def get(utr: String, internalId: String): Future[Option[JsValue]]

  def set(utr: String, internalId: String, data: JsValue): Future[Boolean]

  def resetCache(utr: String, internalId: String): Future[Option[JsObject]]
}
