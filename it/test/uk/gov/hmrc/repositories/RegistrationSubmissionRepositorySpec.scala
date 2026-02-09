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

package uk.gov.hmrc.repositories

import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.registration.RegistrationSubmissionDraft
import org.bson.BsonType
import org.mongodb.scala.model.Filters.`type`
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.stubControllerComponents
import repositories.RegistrationSubmissionRepositoryImpl
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.itbase.IntegrationTestBase
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RegistrationSubmissionRepositorySpec extends IntegrationTestBase with MongoSupport {

  // Make sure we use value of Instant that survives JSON round trip - and isn't expired.
  private val testDateTime: Instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  private val data1 = Json.obj(
    "field1"    -> "value1",
    "field2"    -> "value2",
    "theAnswer" -> 42
  )

  private val data2 = Json.obj(
    "field1"    -> "valueX",
    "field2"    -> "valueY",
    "theAnswer" -> 3.14
  )

  private val data3 = Json.obj(
    "field1"    -> "valueA",
    "field2"    -> "valueB",
    "theAnswer" -> 6.28
  )

  val cc: ControllerComponents = stubControllerComponents()

  val appWithoutSavedRegistration: Application = applicationBuilder
    .configure(Seq("features.removeSavedRegistrations" -> false): _*)
    .overrides(
      bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Agent))
    )
    .build()

  val appWithSavedRegistration: Application = applicationBuilder
    .configure(Seq("features.removeSavedRegistrations" -> true): _*)
    .overrides(
      bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Agent))
    )
    .build()

  "the registration submission repository" should {

    "be able to store and retrieve data" in assertMongoTest(createApplication) { app =>
      val repository = app.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

      repository.getRecentDrafts("InternalId", Agent).value.futureValue mustBe Right(
        Seq.empty[RegistrationSubmissionDraft]
      )

      val state1 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId",
        testDateTime,
        data1,
        Some("reference1"),
        Some(true)
      )

      repository.setDraft(state1).value.futureValue mustBe Right(true)

      val state2 = RegistrationSubmissionDraft(
        "draftId2",
        "InternalId",
        testDateTime,
        data2,
        Some("reference2"),
        Some(true)
      )

      repository.setDraft(state2).value.futureValue mustBe Right(true)

      val state3 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId2",
        testDateTime,
        data3,
        None,
        None
      )

      repository.setDraft(state3).value.futureValue mustBe Right(true)

      val state4 = RegistrationSubmissionDraft(
        "draftId3",
        "InternalId",
        testDateTime,
        data2,
        Some("reference3"),
        Some(false)
      )

      repository.setDraft(state4).value.futureValue mustBe Right(true)

      repository.getDraft("draftId1", "InternalId").value.futureValue  mustBe Right(Some(state1))
      repository.getDraft("draftId2", "InternalId").value.futureValue  mustBe Right(Some(state2))
      repository.getDraft("draftId1", "InternalId2").value.futureValue mustBe Right(Some(state3))
      repository.getDraft("draftId3", "InternalId").value.futureValue  mustBe Right(Some(state4))

      repository.getRecentDrafts("InternalId", Agent).value.futureValue mustBe Right(Seq(state2, state1))
    }

    "be able to remove drafts no longer being used" in assertMongoTest(createApplication) { app =>
      val repository = app.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

      repository.removeDraft("draftId1", "InternalId").value.futureValue mustBe Right(true)

      val state1 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId",
        testDateTime,
        data1,
        Some("ref1"),
        Some(true)
      )

      repository.setDraft(state1).value.futureValue mustBe Right(true)

      repository.getDraft("draftId1", "InternalId").value.futureValue mustBe Right(Some(state1))

      repository.removeDraft("draftId1", "InternalId").value.futureValue mustBe Right(true)

      repository.getDraft("draftId1", "InternalId").value.futureValue mustBe Right(None)
    }

    "be able to store and retrieve more than 20 drafts" in assertMongoTest(createApplication) { app =>
      val repository = app.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

      repository.getRecentDrafts("InternalId", Agent).value.futureValue mustBe Right(
        Seq.empty[RegistrationSubmissionDraft]
      )

      for (i <- 0 until 50) {
        val state = RegistrationSubmissionDraft(
          s"draftId$i",
          "InternalId",
          testDateTime,
          data1,
          Some("reference1"),
          Some(true)
        )
        repository.setDraft(state).value.futureValue mustBe Right(true)
      }

      repository.getRecentDrafts("InternalId", Agent).value.futureValue.map(_.size)        mustBe Right(50)
      repository.getRecentDrafts("InternalId", Organisation).value.futureValue.map(_.size) mustBe Right(1)
    }

    "remove all documents from registration-submissions when feature enabled" in assertMongoTest(
      appWithSavedRegistration
    ) { app =>
      val repository = app.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

      for (i <- 0 until 50) {
        val draft = RegistrationSubmissionDraft(
          s"draftId$i",
          "InternalId",
          testDateTime,
          data1,
          Some("reference1"),
          Some(true)
        )
        Await.result(repository.setDraft(draft).value, Duration.Inf)
      }

      repository.getRecentDrafts("InternalId", Agent).value.futureValue.map(_.size) mustBe Right(50)

      Await.result(repository.removeAllDrafts().value, Duration.Inf)

      repository.getRecentDrafts("InternalId", Agent).value.futureValue.map(_.size) mustBe Right(0)
    }

    "not remove all documents from registration-submissions when feature disabled" in assertMongoTest(
      appWithoutSavedRegistration
    ) { app =>
      val repository = app.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

      for (i <- 0 until 50) {
        val draft = RegistrationSubmissionDraft(
          s"draftId$i",
          "InternalId",
          testDateTime,
          data1,
          Some("reference1"),
          Some(true)
        )
        Await.result(repository.setDraft(draft).value, Duration.Inf)
      }

      repository.getRecentDrafts("InternalId", Agent).value.futureValue.map(_.size) mustBe Right(50)

      Await.result(repository.removeAllDrafts().value, Duration.Inf)

      repository.getRecentDrafts("InternalId", Agent).value.futureValue.map(_.size) mustBe Right(50)
    }

    "be able to store and retrieve a payload with correct date time format" in assertMongoTest(createApplication) {
      app =>
        val repository = app.injector.instanceOf[RegistrationSubmissionRepositoryImpl]

        for (i <- 0 until 50) {
          val draft = RegistrationSubmissionDraft(
            s"draftId$i",
            "InternalId",
            testDateTime,
            data1,
            Some("reference1"),
            Some(true)
          )
          Await.result(repository.setDraft(draft).value, Duration.Inf)
        }

        val query     = `type`("createdAt", BsonType.DATE_TIME)
        val documents = repository.collection.countDocuments(query).toFuture().futureValue.toInt

        documents                                                                       must be > 0
        repository.getRecentDrafts("InternalId", Agent).value.futureValue.map(_.size) mustBe Right(50)
    }
  }

}
