/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates.{combine, set}
import play.api.libs.json.{JsObject, Json, Reads, Writes}
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

trait RepositoryHelper[T] {
  self: PlayMongoRepository[T] =>
  implicit val executionContext: ExecutionContext

  val key: String

  private def selector(identifier: String, internalId: String, sessionId: String): Bson =
    equal("id", createKey(identifier, internalId, sessionId))

  private def createKey(identifier: String, internalId: String, sessionId: String): String = s"$identifier-$internalId-$sessionId"

  def getOpt(identifier: String, internalId: String, sessionId: String)(implicit rds: Reads[T]): Future[Option[T]] = {
    collection.find[BsonDocument](selector(identifier, internalId, sessionId)).headOption()
      .map(_.map(bsonDocument =>
        Json.parse(bsonDocument.toJson).as[JsObject])
        .flatMap(json => (json \ key).asOpt[T])
      )
  }

  def resetCache(identifier: String, internalId: String, sessionId: String): Future[Boolean] = {
    collection.deleteOne(selector(identifier, internalId, sessionId)).toFutureOption()
      .map(_.exists(_.wasAcknowledged()))
  }

  def upsert(identifier: String, internalId: String, sessionId: String, data: T)(implicit wts: Writes[T]): Future[Boolean] = {

    val modifier = combine(
      set("id", toBson(createKey(identifier, internalId, sessionId))),
      set("updatedAt", toBson(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli)),
      set(key, toBson(data))
    )

    val updateOptions = new UpdateOptions().upsert(true)

    collection.updateOne(selector(identifier, internalId, sessionId), modifier, updateOptions).toFutureOption()
      .map(_.isDefined)
  }
}
