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

import akka.stream.Materializer
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json._
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.utils.DateFormatter

import scala.concurrent.{ExecutionContext, Future}

class DefaultUiStateRepository @Inject()(
                                          mongo: MongoDriver,
                                          config: AppConfig,
                                          dateFormatter: DateFormatter
                                        )(implicit ec: ExecutionContext, m: Materializer) extends UiStateRepository {

  private val logger = LoggerFactory.getLogger("application." + getClass.getCanonicalName)

  private val collectionName: String = "frontend-ui-state"

  private val cacheTtl = config.registrationTtlInSeconds

  private def collection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      res <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
    } yield res

  private val lastUpdatedIndex = Index(
    key = Seq("updatedAt" -> IndexType.Ascending),
    name = Some("ui-state-updated-at-index"),
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

  private def createKey(draftId: String, internalId: String) = {
    (draftId + '-' + internalId)
  }

  override def get(draftId: String, internalId: String): Future[Option[JsObject]] = {
      val selector = Json.obj(
        "id" -> createKey(draftId, internalId)
      )

      collection.flatMap(_.find(
        selector = selector, None).one[JsObject].map(opt =>
        for  {
          document <- opt
          transforms <- (document \ "state").asOpt[JsObject]
        } yield transforms)
      )
  }

//  override def getDraftRegistrations(internalId: String): Future[List[UserAnswers]] = {
//    val draftIdLimit = 20
//
//    val selector = Json.obj(
//      "internalId" -> internalId,
//      "progress" -> Json.obj("$ne" -> RegistrationStatus.Complete.toString)
//    )
//
//    collection.flatMap(
//      _.find(
//        selector = selector,
//        projection = None
//      )
//        .sort(Json.obj("createdAt" -> -1))
//        .cursor[UserAnswers]()
//        .collect[List](draftIdLimit, Cursor.FailOnError[List[UserAnswers]]()))
//  }
//
//  override def listDrafts(internalId : String) : Future[List[DraftRegistration]] = {
//    getDraftRegistrations(internalId).map {
//      drafts =>
//
//        drafts.flatMap {
//          x =>
//            x.get(AgentInternalReferencePage).map {
//              reference =>
//                DraftRegistration(x.draftId, reference, dateFormatter.savedUntil(x.createdAt))
//            }
//
//        }
//    }
//  }

  override def set(draftId: String, internalId: String, uiState: JsObject): Future[Boolean] = {

    val id = createKey(draftId, internalId)
    val selector = Json.obj(
      "id" -> id
    )

    val modifier = Json.obj(
      "$set" -> Json.obj(
        "id" -> id,
        "updatedAt" -> Json.obj("$date" -> Timestamp.valueOf(LocalDateTime.now())),
        "state" -> uiState
      )
    )

    collection.flatMap {
      _.update(ordered = false).one(selector, modifier, upsert = true).map {
        lastError =>
          lastError.ok
      }
    }
  }
}

trait UiStateRepository {

  def get(draftId: String, internalId: String): Future[Option[JsObject]]

  def set(draftId: String, internalId: String, uiState: JsObject): Future[Boolean]

//  def getDraftRegistrations(internalId: String): Future[List[UserAnswers]]
//
//  def listDrafts(internalId : String) : Future[List[DraftRegistration]]
}
