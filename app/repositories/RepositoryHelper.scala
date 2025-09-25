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

import cats.data.EitherT
import errors.ServerError
import models.UpdatedCounterValues
import org.bson.BsonType
import org.bson.types.ObjectId
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonDateTime, BsonDocument}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.{Filters, Sorts, UpdateOptions, Updates}
import org.mongodb.scala.{MongoException, Observable}
import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import utils.TrustEnvelope.TrustEnvelope

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait RepositoryHelper[T] extends Logging {
  self: PlayMongoRepository[T] =>
  implicit val executionContext: ExecutionContext

  val className: String
  val key: String

  def getOpt(identifier: String, internalId: String, sessionId: String)(implicit rds: Reads[T]): TrustEnvelope[Option[T]] = EitherT {
    logger.info(s"getOpt... start $identifier, $internalId, $sessionId")
    collection.find[BsonDocument](selector(identifier, internalId, sessionId))
      .headOption()
      .map { optBsonDocument =>
        Right(optBsonDocument.map {
          bsonDocument =>
            Json.parse(bsonDocument.toJson).as[JsObject]
        }.flatMap(json => (json \ key).asOpt[T]))
      }.recover {
        case e: MongoException =>
          logger.error(s"[$className][getOpt] failed to fetch from $collectionName ${e.getMessage}")
          Left(ServerError(e.getMessage))
        case exception: Exception =>
          logger.error(s"[$className][getOpt] $collectionName ${exception.getMessage}")
          Left(ServerError(exception.getMessage))
      }
  }

  def resetCache(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Boolean] = EitherT {
    collection.deleteOne(selector(identifier, internalId, sessionId))
      .toFutureOption()
      .map { deleteResult => Right(deleteResult.exists(_.wasAcknowledged()))
      }.recover {
        case e: MongoException =>
          logger.error(s"[$className][resetCache] failed to delete one from $collectionName ${e.getMessage}")
          Left(ServerError(e.getMessage))
        case exception: Exception =>
          logger.error(s"[$className][resetCache] $collectionName ${exception.getMessage}")
          Left(ServerError(exception.getMessage))
      }
  }

  def upsert(identifier: String, internalId: String, sessionId: String, data: T)(implicit wts: Writes[T]): TrustEnvelope[Boolean] = EitherT {
    val modifier = combine(
      set("id", toBson(createKey(identifier, internalId, sessionId))),
      set("updatedAt", BsonDateTime(Instant.now().toEpochMilli)),
      set(key, toBson(data))
    )

    val updateOptions = new UpdateOptions().upsert(true)

    collection.updateOne(selector(identifier, internalId, sessionId), modifier, updateOptions)
      .toFutureOption()
      .map {
        updateResult => Right(updateResult.isDefined)
      }.recover {
        case e: MongoException =>
          logger.error(s"[$className][upsert] failed to update $collectionName ${e.getMessage}")
          Left(ServerError(e.getMessage))
        case exception: Exception =>
          logger.error(s"[$className][upsert] $collectionName ${exception.getMessage}")
          Left(ServerError(exception.getMessage))
      }
  }

  private def selector(identifier: String, internalId: String, sessionId: String): Bson =
    equal("id", createKey(identifier, internalId, sessionId))

  private def createKey(identifier: String, internalId: String, sessionId: String): String = s"$identifier-$internalId-$sessionId"

  def getAllInvalidDateDocuments(limit: Int): Observable[ObjectId] = {
    val selector = Filters.not(Filters.`type`("updatedAt", BsonType.DATE_TIME))
    val sortById = Sorts.ascending("_id")
    collection.find[BsonDocument](selector).sort(sortById).limit(limit)
      .map(jsToObjectId)
  }

  private def jsToObjectId(js: BsonDocument): ObjectId =
    Try(js.getObjectId("_id").getValue) match {
      case Failure(exception) => logger.error(s"[$className][jsToObjectId] failed to fetch id from : $collectionName Exception : $exception")
        throw new Exception("_id is not found")
      case Success(value) => value
    }


  def updateAllInvalidDateDocuments(ids: Seq[ObjectId]): Future[UpdatedCounterValues] = {
    val update = Updates.set("updatedAt", BsonDateTime(Instant.now().toEpochMilli))
    val filterIn = Filters.in("_id", ids: _*)
    collection.updateMany(filterIn, update).toFuture()
      .map(_ => UpdatedCounterValues(matched = ids.size, updated = ids.size, errors = 0))
      .recover { case _ => UpdatedCounterValues(matched = ids.size, updated = 0, errors = ids.size) }
  }
}
