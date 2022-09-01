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

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.FindOneAndUpdateOptions
import org.mongodb.scala.model.Updates.{combine, set}
import play.api.libs.json.{JsObject, Json, Reads, Writes}
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

trait RepositoryHelper[T] {
  self: PlayMongoRepository[T] =>
  implicit val executionContext: ExecutionContext

  //  type UserData

  //might not be needed
  //  def encryptionMethod: UserData => C
  //
  //  def decryptionMethod: C => UserData

  val key: String

  def get[T](identifier: String, internalId: String, sessionId: String)(implicit rds: Reads[T]): Future[Option[T]] = {
    collection.find[BsonDocument](selector(identifier, internalId, sessionId)).headOption()
      .map(_.map(bsonDocument =>
        Json.parse(bsonDocument.toJson).as[JsObject])
        .flatMap(json => (json \ key).asOpt[T])
      )
  }

  private def selector(identifier: String, internalId: String, sessionId: String): Bson =
    equal("id", createKey(identifier, internalId, sessionId))

  private def createKey(identifier: String, internalId: String, sessionId: String): String = s"$identifier-$internalId-$sessionId"


  def resetCache(identifier: String, internalId: String, sessionId: String): Future[Option[T]] = {
    collection.findOneAndDelete(selector(identifier, internalId, sessionId)).toFutureOption()
  }

  def upsert[T](identifier: String, internalId: String, sessionId: String, data: T)(implicit wts: Writes[T]): Future[Boolean] = {

    val modifier = combine(
      set("id", toBson(createKey(identifier, internalId, sessionId))),
      set("updatedAt", toBson(LocalDateTime.now())),
      set(key, toBson(data))
    )
    val updateOptions = new FindOneAndUpdateOptions().upsert(true)

    collection.findOneAndUpdate(selector(identifier, internalId, sessionId), modifier, updateOptions).toFutureOption().map(_ => true)
  }
}
