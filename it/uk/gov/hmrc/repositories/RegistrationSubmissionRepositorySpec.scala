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

package uk.gov.hmrc.repositories

import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.MongoDateTimeFormats
import models.registration.RegistrationSubmissionDraft
import org.mongodb.scala.Document
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json._
import play.api.test.Helpers.{await, defaultAwaitTimeout, stubControllerComponents}
import repositories.RegistrationSubmissionRepositoryImpl
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.itbase.IntegrationTestBase

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RegistrationSubmissionRepositorySpec extends AsyncFreeSpec with IntegrationTestBase with MongoDateTimeFormats {

  // Make sure we use value of LocalDateTime that survives JSON round trip - and isn't expired.
  private val testDateTime: LocalDateTime = Json.toJson(LocalDateTime.now()).as[LocalDateTime]

  private val data1 = Json.obj(
    "field1" -> "value1",
    "field2" -> "value2",
    "theAnswer" -> 42
  )
  private val data2 = Json.obj(
    "field1" -> "valueX",
    "field2" -> "valueY",
    "theAnswer" -> 3.14
  )
  private val data3 = Json.obj(
    "field1" -> "valueA",
    "field2" -> "valueB",
    "theAnswer" -> 6.28
  )

  private val repository = createApplication.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

  private def dropDB(): Unit = {
    await(repository.collection.deleteMany(filter = Document()).toFuture())
    await(repository.ensureIndexes)
  }


  "the registration submission repository" - {

    "must be able to store and retrieve data" in {
      dropDB()
      repository.getRecentDrafts("InternalId", Agent).futureValue mustBe Seq.empty[RegistrationSubmissionDraft]

      val state1 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId",
        testDateTime,
        data1,
        Some("reference1"),
        Some(true)
      )

      repository.setDraft(state1).futureValue mustBe true

      val state2 = RegistrationSubmissionDraft(
        "draftId2",
        "InternalId",
        testDateTime,
        data2,
        Some("reference2"),
        Some(true)
      )

      repository.setDraft(state2).futureValue mustBe true

      val state3 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId2",
        testDateTime,
        data3,
        None,
        None
      )

      repository.setDraft(state3).futureValue mustBe true

      val state4 = RegistrationSubmissionDraft(
        "draftId3",
        "InternalId",
        testDateTime,
        data2,
        Some("reference3"),
        Some(false)
      )

      repository.setDraft(state4).futureValue mustBe true

      repository.getDraft("draftId1", "InternalId").futureValue mustBe Some(state1)
      repository.getDraft("draftId2", "InternalId").futureValue mustBe Some(state2)
      repository.getDraft("draftId1", "InternalId2").futureValue mustBe Some(state3)
      repository.getDraft("draftId3", "InternalId").futureValue mustBe Some(state4)

      repository.getRecentDrafts("InternalId", Agent).futureValue mustBe Seq(state2, state1)
    }
  }

    "must be able to remove drafts no longer being used" in {
      dropDB()
      repository.removeDraft("draftId1", "InternalId").futureValue mustBe true

      val state1 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId",
        testDateTime,
        data1,
        Some("ref1"),
        Some(true)
      )

      repository.setDraft(state1).futureValue mustBe true

      repository.getDraft("draftId1", "InternalId").futureValue mustBe Some(state1)

      repository.removeDraft("draftId1", "InternalId").futureValue mustBe true

      repository.getDraft("draftId1", "InternalId").futureValue mustBe None
    }

    "must be able to store and retrieve more than 20 drafts" in {
      dropDB()
      repository.getRecentDrafts("InternalId", Agent).futureValue mustBe Seq.empty[RegistrationSubmissionDraft]

      for (i <- 0 until 50) {
        val state = RegistrationSubmissionDraft(
          s"draftId$i",
          "InternalId",
          testDateTime,
          data1,
          Some("reference1"),
          Some(true)
        )
        repository.setDraft(state).futureValue mustBe true
      }

      repository.getRecentDrafts("InternalId", Agent).futureValue.size mustBe 50
      repository.getRecentDrafts("InternalId", Organisation).futureValue.size mustBe 1
    }

    "must remove all documents from registration-submissions when feature enabled" in {
      val cc = stubControllerComponents()
      val appConfiguration = applicationBuilder.configure(Seq("features.removeSavedRegistrations" -> true): _*)
      val app: Application = appConfiguration.overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Agent))
      ).build()

      val repository = app.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

      dropDB()

      for (i <- 0 until 50) {
        val draft = RegistrationSubmissionDraft(
          s"draftId$i",
          "InternalId",
          testDateTime,
          data1,
          Some("reference1"),
          Some(true)
        )
        Await.result(repository.setDraft(draft), Duration.Inf)
      }

      repository.getRecentDrafts("InternalId", Agent).futureValue.size mustBe 50

      Await.result(repository.removeAllDrafts(), Duration.Inf) //TODO - INA - add test for removeDrafts too (removing one draft)

      repository.getRecentDrafts("InternalId", Agent).futureValue.size mustBe 0
    }

  "must not remove all documents from registration-submissions when feature disabled" in {
    val cc = stubControllerComponents()
    val appConfiguration = applicationBuilder.configure(Seq("features.removeSavedRegistrations" -> false): _*)
    val app: Application = appConfiguration.overrides(
      bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Agent))
    ).build()

    val repository = app.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

    dropDB()

    for (i <- 0 until 50) {
      val draft = RegistrationSubmissionDraft(
        s"draftId$i",
        "InternalId",
        testDateTime,
        data1,
        Some("reference1"),
        Some(true)
      )
      Await.result(repository.setDraft(draft), Duration.Inf)
    }

    repository.getRecentDrafts("InternalId", Agent).futureValue.size mustBe 50

    Await.result(repository.removeAllDrafts(), Duration.Inf)

    repository.getRecentDrafts("InternalId", Agent).futureValue.size mustBe 50
  }

}
