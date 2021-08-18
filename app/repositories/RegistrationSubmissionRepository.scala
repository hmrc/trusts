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

import javax.inject.Inject
import play.api.Logging
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.IndexType
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import config.AppConfig
import models.registration.RegistrationSubmissionDraft

import _root_.play.api.libs.json._

import _root_.reactivemongo.api.bson._

// Global compatibility import:
import reactivemongo.play.json.compat._

// Import BSON to JSON extended syntax (default)
import bson2json._ // Required import

// Import lax overrides
import lax._

import reactivemongo.play.json.compat.jsObjectWrites

import scala.concurrent.{ExecutionContext, Future}

trait RegistrationSubmissionRepository {

  def getDraft(draftId: String, internalId: String): Future[Option[RegistrationSubmissionDraft]]

  def setDraft(uiState: RegistrationSubmissionDraft): Future[Boolean]

  def getRecentDrafts(internalId: String, affinityGroup: AffinityGroup): Future[List[RegistrationSubmissionDraft]]

  def removeDraft(draftId: String, internalId: String): Future[Boolean]

  def removeAllDrafts(): Future[Boolean]
}

class RegistrationSubmissionRepositoryImpl @Inject()(
                                                      mongo: MongoDriver,
                                                      config: AppConfig
                                                    )(implicit ec: ExecutionContext) extends RegistrationSubmissionRepository with Logging {

  private val collectionName: String = "registration-submissions"

  private val cacheTtl = config.registrationTtlInSeconds

  private def collection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      res <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
    } yield res

  private val createdAtIndex = MongoIndex(
    key = Seq("createdAt" -> IndexType.Ascending),
    name = "ui-state-created-at-index",
    expireAfterSeconds = Some(cacheTtl)
  )

  private val draftIdIndex = MongoIndex(
    key = Seq("draftId" -> IndexType.Ascending),
    name = "draft-id-index"
  )

  private val internalIdIndex = MongoIndex(
    key = Seq("internalId" -> IndexType.Ascending),
    name = "internal-id-index"
  )

  private lazy val ensureIndexes = {
    logger.info("Ensuring collection indexes")
    for {
      collection <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
      createdCreatedIndex <- collection.indexesManager.ensure(createdAtIndex)
      createdIdIndex <- collection.indexesManager.ensure(draftIdIndex)
      createdInternalIdIndex <- collection.indexesManager.ensure(internalIdIndex)
    } yield createdCreatedIndex && createdIdIndex && createdInternalIdIndex
  }

  override def setDraft(uiState: RegistrationSubmissionDraft): Future[Boolean] = {

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

  override def getDraft(draftId: String, internalId: String): Future[Option[RegistrationSubmissionDraft]] = {
    val selector = Json.obj(
      "draftId" -> draftId,
      "internalId" -> internalId
    )

    collection.flatMap(_.find(
      selector = selector, None).one[RegistrationSubmissionDraft])
  }

  override def getRecentDrafts(internalId: String, affinityGroup: AffinityGroup): Future[List[RegistrationSubmissionDraft]] = {
    val maxDocs = if (affinityGroup == Organisation) 1 else -1

    val selector = Json.obj(
      "internalId" -> internalId,
      "inProgress" -> Json.obj("$eq" -> true)
    )

    collection.flatMap(
      _.find(selector = selector, projection = None)
        .sort(Json.obj("createdAt" -> -1))
        .cursor[RegistrationSubmissionDraft]()
        .collect[List](maxDocs, Cursor.FailOnError[List[RegistrationSubmissionDraft]]())
    )
  }

  override def removeDraft(draftId: String, internalId: String): Future[Boolean] = {
    val selector = Json.obj(
      "draftId" -> draftId,
      "internalId" -> internalId
    )

    collection.flatMap(_.delete().one(selector)).map(_.ok)
  }

  /**
   * All registration submissions will be deleted from mongo on the 30th April
   * This is so that we don't have any lingering 4MLD draft registrations when we switch on 5MLD
   */
  def removeAllDrafts(): Future[Boolean] = for {
    collection <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
    result <- if (config.removeSavedRegistrations) {
      logger.info("Removing all registration submissions.")
      collection.delete().one(Json.obj(), None).map(_.ok)
    } else {
      Future.successful(true)
    }
  } yield result

  final val removeRegistrationSubmissions = removeAllDrafts()

}
