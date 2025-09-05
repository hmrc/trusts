/*
 * Copyright 2024 HM Revenue & Customs
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
import models.UpdatedCounterValues
import org.apache.pekko.stream.scaladsl.Source
import org.bson.BsonType
import org.bson.types.ObjectId
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.model._
import play.api.libs.json.{Format, JsObject, JsString, JsValue}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import utils.TrustEnvelope.TrustEnvelope

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
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
  ),
  replaceIndexes = config.dropIndexesEnabled
) with RepositoryHelper[JsValue] with CacheRepository {

  override implicit val executionContext: ExecutionContext = ec
  override val className: String = "CacheRepositoryImpl"
  override val key: String = "etmpData"

  override def get(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Option[JsValue]] = {
    getOpt(identifier, internalId, sessionId)
  }

  override def set(identifier: String, internalId: String, sessionId: String, data: JsValue): TrustEnvelope[Boolean] = {
    upsert(identifier, internalId, sessionId, data)
  }


  private def jsToObjectId(js: JsValue): Option[ObjectId] =
    (js \ "_id").toOption.flatMap {
      case JsString(s) if ObjectId.isValid(s) => Some(new ObjectId(s))
      case o: JsObject =>
        (o \ "$oid").asOpt[String].filter(ObjectId.isValid).map(new ObjectId(_))
      case _ => None
    }

  def fixBadUpdatedAt(limit: Int = 1000): Source[UpdatedCounterValues, _] = {
    logger.info("################### fixBadUpdatedAt started ###################")

    val selector = Filters.not(Filters.`type`("updatedAt", BsonType.DATE_TIME))
    val sortById = Sorts.ascending("_id")
    val update = Updates.set("updatedAt", BsonDateTime(Instant.now().toEpochMilli))

    Source
      .fromPublisher(collection.find(selector).sort(sortById).limit(limit))
      .map(jsToObjectId)
      .collect { case Some(oid: ObjectId) => oid }
      .fold(List.empty[ObjectId])((acc, id) => id :: acc)
      .mapAsync(parallelism = 1) { ids =>
        if (ids.isEmpty) {
          scala.concurrent.Future.successful(UpdatedCounterValues(0, 0, 0))
        } else {
          val filterIn = Filters.in("_id", ids: _*)
          collection.updateMany(filterIn, update).toFuture()(ec)
            .map(_ => UpdatedCounterValues(matched = ids.size, updated = ids.size, errors = 0))(ec)
            .recover { case _ => UpdatedCounterValues(matched = ids.size, updated = 0, errors = ids.size) }(ec)
        }
      }
  }
}

trait CacheRepository {

  def get(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Option[JsValue]]

  def set(identifier: String, internalId: String, sessionId: String, data: JsValue): TrustEnvelope[Boolean]

  def resetCache(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Boolean]

  def fixBadUpdatedAt(limit: Int = 1000): Source[UpdatedCounterValues, _]
}
