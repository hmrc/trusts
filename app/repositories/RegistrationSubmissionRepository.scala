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

import config.AppConfig
import models.registration.{RegistrationSubmissionDraft, RegistrationSubmissionDraftDB}
import org.mongodb.scala.model.Filters.{and, empty, equal}
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json.Format
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait RegistrationSubmissionRepository {

  def getDraft(draftId: String, internalId: String): Future[Option[RegistrationSubmissionDraft]]

  def setDraft(uiState: RegistrationSubmissionDraft): Future[Boolean]

  def getRecentDrafts(internalId: String, affinityGroup: AffinityGroup): Future[Seq[RegistrationSubmissionDraft]]

  def removeDraft(draftId: String, internalId: String): Future[Boolean]

  def removeAllDrafts(): Future[Boolean]
}

class RegistrationSubmissionRepositoryImpl @Inject()(
                                                      mongoComponent: MongoComponent,
                                                      config: AppConfig
                                                    )(implicit ec: ExecutionContext) extends
  PlayMongoRepository[RegistrationSubmissionDraftDB](
    mongoComponent = mongoComponent,
    collectionName = "registration-submissions",
    domainFormat = Format(RegistrationSubmissionDraftDB.reads, RegistrationSubmissionDraftDB.writes),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("createdAt"),
        IndexOptions().name("ui-state-created-at-index").expireAfter(config.registrationTtlInSeconds, TimeUnit.SECONDS)
      ),
      IndexModel(
        Indexes.ascending("draftId"),
        IndexOptions().name("draft-id-index")
      ),
      IndexModel(
        Indexes.ascending("internalId"),
        IndexOptions().name("internal-id-index")
      )
    )
  ) with
  RegistrationSubmissionRepository with Logging {

  private def convertFromDBModel(dbModel: RegistrationSubmissionDraftDB): RegistrationSubmissionDraft = {
    RegistrationSubmissionDraft(
      draftId = dbModel.draftId,
      internalId = dbModel.internalId,
      createdAt = dbModel.createdAt,
      draftData = dbModel.draftData,
      reference = dbModel.reference,
      inProgress = dbModel.inProgress
    )
  }

  private def convertToDBModel(ogModel: RegistrationSubmissionDraft): RegistrationSubmissionDraftDB = {
    RegistrationSubmissionDraftDB(
      draftId = ogModel.draftId,
      internalId = ogModel.internalId,
      createdAt = ogModel.createdAt,
      draftData = ogModel.draftData,
      reference = ogModel.reference,
      inProgress = ogModel.inProgress
    )
  }

  override def setDraft(uiState: RegistrationSubmissionDraft): Future[Boolean] = {

    val selector = and(
      equal("draftId",  uiState.draftId),
      equal("internalId", uiState.internalId)
    )

    val updateOptions = new ReplaceOptions().upsert(true)

    collection.replaceOne(selector, convertToDBModel(uiState), updateOptions).toFutureOption().map {
      case Some(_) => true
      case None => false
    }
  }

  override def getDraft(draftId: String, internalId: String): Future[Option[RegistrationSubmissionDraft]] = {
    val selector = and(
      equal("draftId", draftId),
      equal("internalId", internalId)
    )

    collection.find(selector).first().toFutureOption().map{
      optModel => optModel.map(convertFromDBModel)
    }
  }

  override def getRecentDrafts(internalId: String, affinityGroup: AffinityGroup): Future[Seq[RegistrationSubmissionDraft]] = {
    val maxDocs = if (affinityGroup == Organisation) 1 else Int.MaxValue

    val selector = and(
      equal("internalId", internalId),
      equal("inProgress", true)
    )

    val sort = Sorts.descending("createdAt")

    collection.find(selector).sort(sort).limit(maxDocs).toFuture().map{
      seq => seq.map(convertFromDBModel)
    }
  }

  override def removeDraft(draftId: String, internalId: String): Future[Boolean] = {
    val selector = and(
      equal("draftId", draftId),
      equal("internalId", internalId)
    )
    collection.deleteOne(selector).toFuture().map(_.wasAcknowledged())
  }

  /**
   * All registration submissions will be deleted from mongo on the 30th April
   * This is so that we don't have any lingering 4MLD draft registrations when we switch on 5MLD
   */
  def removeAllDrafts(): Future[Boolean] =
    if(config.removeSavedRegistrations) {
      collection.deleteMany(empty()).toFuture().map { deleteResult =>
        logger.info("[RegistrationSubmissionRepository][removeAllDrafts] Removing all registration submissions.")
        deleteResult.wasAcknowledged()
      }
    } else {
      Future.successful(true)
    }
}
