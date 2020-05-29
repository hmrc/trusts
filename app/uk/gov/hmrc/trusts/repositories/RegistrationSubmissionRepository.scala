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
import uk.gov.hmrc.trusts.models.RegistrationSubmissionDraft
import uk.gov.hmrc.trusts.utils.DateFormatter

import scala.concurrent.{ExecutionContext, Future}

trait RegistrationSubmissionRepository {

  def get(draftId: String, internalId: String): Future[Option[RegistrationSubmissionDraft]]

  def set(uiState: RegistrationSubmissionDraft): Future[Boolean]

  def getAll(internalId: String): Future[List[RegistrationSubmissionDraft]]

  def remove(draftId: String, internalId: String): Future[Boolean]
}

class RegistrationSubmissionRepositoryImpl @Inject()(
                                          mongo: MongoDriver,
                                          config: AppConfig,
                                          dateFormatter: DateFormatter
                                        )(implicit ec: ExecutionContext, m: Materializer) extends RegistrationSubmissionRepository {

  private val logger = LoggerFactory.getLogger("application." + getClass.getCanonicalName)

  private val collectionName: String = "registration-submissions"

  private val cacheTtl = config.registrationTtlInSeconds

  private def collection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      res <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
    } yield res

  private val createdAtIndex = Index(
    key = Seq("createdAt" -> IndexType.Ascending),
    name = Some("ui-state-created-at-index"),
    options = BSONDocument("expireAfterSeconds" -> cacheTtl)
  )

  private val draftIdIndex = Index(
    key = Seq("draftId" -> IndexType.Ascending),
    name = Some("draft-id-index")
  )

  private val internalIdIndex = Index(
    key = Seq("internalId" -> IndexType.Ascending),
    name = Some("internal-id-index")
  )

  private lazy val ensureIndexes = {
    logger.info("Ensuring collection indexes")
    for {
      collection              <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
      createdCreatedIndex <- collection.indexesManager.ensure(createdAtIndex)
      createdIdIndex          <- collection.indexesManager.ensure(draftIdIndex)
      createdInternalIdIndex  <- collection.indexesManager.ensure(internalIdIndex)
    } yield createdCreatedIndex && createdIdIndex && createdInternalIdIndex
  }

  override def set(uiState: RegistrationSubmissionDraft): Future[Boolean] = {

    val selector = Json.obj(
      "draftId" -> uiState.draftId,
      "internalId" -> uiState.internalId
    )

    val modifier = Json.obj(
      "$set" -> uiState
    )

    collection.flatMap {
      _.update(ordered = false).one(selector, modifier, upsert = true).map {
        lastError =>
          lastError.ok
      }
    }
  }

  override def get(draftId: String, internalId: String): Future[Option[RegistrationSubmissionDraft]] = {
    val selector = Json.obj(
      "draftId" -> draftId,
      "internalId" -> internalId
    )

    collection.flatMap(_.find(
      selector = selector, None).one[RegistrationSubmissionDraft])
  }

  override def getAll(internalId: String): Future[List[RegistrationSubmissionDraft]] = {
    val draftIdLimit = 20

    val selector = Json.obj(
      "internalId" -> internalId
    )

    collection.flatMap(_.find(
      selector = selector, projection = None).sort(Json.obj("createdAt" -> -1))
            .cursor[RegistrationSubmissionDraft]()
            .collect[List](draftIdLimit, Cursor.FailOnError[List[RegistrationSubmissionDraft]]()))
  }

  override def remove(draftId: String, internalId: String): Future[Boolean] = {
    val selector = Json.obj(
      "draftId" -> draftId,
      "internalId" -> internalId
    )

    collection.flatMap(_.delete().one(selector)).map(_.ok)
  }

}
