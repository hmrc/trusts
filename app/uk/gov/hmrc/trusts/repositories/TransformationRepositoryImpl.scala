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

import java.time.LocalDateTime

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.transformers.{ComposedDeltaTransform, DeltaTransform}
import uk.gov.hmrc.trusts.utils.DateFormatter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransformationRepositoryImpl @Inject()(
                            mongo: MongoDriver,
                            config: AppConfig,
                            dateFormatter: DateFormatter
                          )(implicit ec: ExecutionContext, m: Materializer) extends TransformationRepository {

  private val logger = LoggerFactory.getLogger("application." + getClass.getCanonicalName)
  private val collectionName: String = "transforms"
  private val cacheTtl = config.ttlInSeconds

  private def collection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      res <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
    } yield res


  private val lastUpdatedIndex = Index(
    key = Seq("updatedAt" -> IndexType.Ascending),
    name = Some("transformation-data-updated-at-index"),
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

  override def get(utr: String, internalId: String): Future[Option[ComposedDeltaTransform]] = {

    val selector = Json.obj(
      "id" -> createKey(utr, internalId)
    )

    collection.flatMap {collection =>

      collection.find(selector, None).one[JsObject].map(opt =>
        for  {
          document <- opt
          transforms <- (document \ "transforms").asOpt[ComposedDeltaTransform]
        } yield transforms)
    }
  }

  private def createKey(utr: String, internalId: String) = {
    (utr + '-' + internalId)
  }

  override def set(utr: String, internalId: String, transforms: ComposedDeltaTransform): Future[Boolean] = {

    val selector = Json.obj(
      "id" -> createKey(utr, internalId)
    )

    val modifier = Json.obj(
      "$set" -> Json.obj(
        "id" -> createKey(utr, internalId),
        "updatedAt" -> LocalDateTime.now,
        "transforms" -> Json.toJson(transforms)
      )
    )

    collection.flatMap {
      _.update(ordered = false).one(selector, modifier, upsert = true, multi = false).map {
        result => result.ok
      }
    }
  }
}

trait TransformationRepository {

  def get(utr: String, internalId: String): Future[Option[ComposedDeltaTransform]]

  def set(utr: String, internalId: String, transforms: ComposedDeltaTransform): Future[Boolean]

}
