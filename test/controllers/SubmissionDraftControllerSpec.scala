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

package controllers

import controllers.actions.FakeIdentifierAction
import models.registration.{RegistrationSubmission, RegistrationSubmissionDraft}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc.BodyParsers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.RegistrationSubmissionRepository
import services.dates.LocalDateTimeService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import utils.JsonFixtures

import java.time.LocalDateTime
import scala.concurrent.Future

class SubmissionDraftControllerSpec extends AnyWordSpec with MockitoSugar with JsonFixtures with Inside with ScalaFutures
  with GuiceOneAppPerSuite {

  private val createdAt: LocalDateTime = LocalDateTime.of(1997, 3, 14, 14, 45)
  private val currentDateTime: LocalDateTime = LocalDateTime.of(1999, 3, 14, 13, 33)

  private val draftId: String = "draftId"
  private val internalId: String = "id"

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val existingDraftData = Json.parse(
    """
      |{
      | "anotherKey": {
      |   "foo": "bar",
      |   "fizzbinn": true
      | },
      | "sectionKey": {
      |   "field1": "value1",
      |   "field2": "value2",
      |   "field3": 3
      | }
      |}
      |""".stripMargin
  )
  private val existingDraft = RegistrationSubmissionDraft(
    draftId = draftId,
    internalId = internalId,
    createdAt = createdAt,
    draftData = existingDraftData,
    reference = Some("theRef"),
    inProgress = Some(true)
  )

  private object LocalDateTimeServiceStub extends LocalDateTimeService {
    override def now: LocalDateTime = currentDateTime
  }

  ".setSection" should {

    "return 'bad request' for malformed body" in {

      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )
      val body = Json.parse(
        """
          |{
          | "datum": {
          |  "field1": "value1",
          |  "field2": "value2",
          |  "field3": 3
          | },
          | "referee": "theReferee"
          |}
          |""".stripMargin)

      val request = FakeRequest("POST", "path")
        .withBody(body)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.setSection(draftId, "sectionKey").apply(request)
      status(result) mustBe BAD_REQUEST
    }

    "cause creation of draft with section if none exists" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      when(submissionRepository.setDraft(any()))
        .thenReturn(Future.successful(true))

      val body = Json.parse(
        """
          |{
          | "data": {
          |  "field1": "value1",
          |  "field2": "value2",
          |  "field3": 3
          | },
          | "reference": "theReference"
          |}
          |""".stripMargin)

      val request = FakeRequest("POST", "path")
        .withBody(body)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val draftData = Json.parse(
        """
          |{
          | "sectionKey": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | }
          |}
          |""".stripMargin)

      val expectedDraft = RegistrationSubmissionDraft(draftId, internalId, currentDateTime, draftData, Some("theReference"), Some(true))

      val result = controller.setSection(draftId, "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft(draftId, internalId)
      verify(submissionRepository).setDraft(expectedDraft)
    }

    "modify existing draft if one exists" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(existingDraft)))

      when(submissionRepository.setDraft(any()))
        .thenReturn(Future.successful(true))

      val body = Json.parse(
        """
          |{
          | "data": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | },
          | "reference": "newRef"
          |}
          |""".stripMargin)

      val request = FakeRequest("POST", "path")
        .withBody(body)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val draftData = Json.parse(
        """
          |{
          | "anotherKey": {
          |   "foo": "bar",
          |   "fizzbinn": true
          | },
          | "sectionKey": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | }
          |}
          |""".stripMargin)

      val expectedDraft = RegistrationSubmissionDraft(draftId, internalId, existingDraft.createdAt, draftData, Some("newRef"), Some(true))

      val result = controller.setSection(draftId, "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft(draftId, internalId)
      verify(submissionRepository).setDraft(expectedDraft)
    }
  }

  ".setSectionSet" should {

    "set data into correct sections" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(existingDraft)))

      when(submissionRepository.setDraft(any()))
        .thenReturn(Future.successful(true))

      val data = Json.parse(
        """
          |{
          | "field1": "value1",
          | "field2": "value2",
          | "field3": 3
          |}
          |""".stripMargin)

      val mappedPieces = List(
        RegistrationSubmission.MappedPiece("trust/assets", Json.parse(
          """
            |{
            | "reg1": "regvalue1",
            | "reg2": 42,
            | "reg3": "regvalue3"
            |}
            |""".stripMargin)),
        RegistrationSubmission.MappedPiece("correspondence/name", JsString("My trust"))
      )

      val answerSections = List(
        RegistrationSubmission.AnswerSection(
          headingKey = Some("section1.heading"),
          rows = List(
            RegistrationSubmission.AnswerRow("label1", "answer1", Seq("labelArg1"))
          ),
          sectionKey = Some("section1.key"),
          headingArgs = Nil
        ),
        RegistrationSubmission.AnswerSection(
          headingKey = Some("section2.heading"),
          rows = List(
            RegistrationSubmission.AnswerRow("label2", "answer2", Seq("labelArg2"))
          ),
          sectionKey = Some("section2.key"),
          headingArgs = Nil
        )
      )

      val set = RegistrationSubmission.DataSet(
        data,
        mappedPieces,
        answerSections
      )

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(set))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val draftData = Json.parse(
        """
          |{
          | "anotherKey": {
          |   "foo": "bar",
          |   "fizzbinn": true
          | },
          | "sectionKey": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | },
          | "registration": {
          |   "trust/assets" : {
          |     "reg1": "regvalue1",
          |     "reg2": 42,
          |     "reg3": "regvalue3"
          |   },
          |   "correspondence/name": "My trust"
          | },
          | "answerSections": {
          |   "sectionKey": [
          |     {
          |       "headingKey": "section1.heading",
          |       "rows": [
          |         {
          |           "label": "label1",
          |           "answer": "answer1",
          |           "labelArgs": [
          |             "labelArg1"
          |           ]
          |         }
          |       ],
          |       "sectionKey": "section1.key",
          |       "headingArgs": []
          |     },
          |     {
          |       "headingKey": "section2.heading",
          |       "rows": [
          |         {
          |           "label": "label2",
          |           "answer": "answer2",
          |           "labelArgs": [
          |             "labelArg2"
          |           ]
          |         }
          |       ],
          |       "sectionKey": "section2.key",
          |       "headingArgs": []
          |     }
          |   ]
          | }
          |}
          |""".stripMargin)

      val expectedDraft = RegistrationSubmissionDraft(
        draftId,
        internalId,
        existingDraft.createdAt,
        draftData,
        existingDraft.reference,
        existingDraft.inProgress)

      val result = controller.setDataset(draftId, "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft(draftId, internalId)
      verify(submissionRepository).setDraft(expectedDraft)
    }

    "prune mapped piece when given a path with JsNull" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(existingDraft)))

      when(submissionRepository.setDraft(any()))
        .thenReturn(Future.successful(true))

      val data = Json.parse(
        """
          |{
          | "field1": "value1",
          | "field2": "value2",
          | "field3": 3
          |}
          |""".stripMargin)

      val mappedPieces = List(
        RegistrationSubmission.MappedPiece("trust/assets", JsNull),
        RegistrationSubmission.MappedPiece("correspondence/name", JsNull)
      )

      val answerSections = List(
        RegistrationSubmission.AnswerSection(
          headingKey = Some("section1.heading"),
          rows = List(
            RegistrationSubmission.AnswerRow("label1", "answer1", Seq("labelArg1"))
          ),
          sectionKey = Some("section1.key"),
          headingArgs = Nil
        ),
        RegistrationSubmission.AnswerSection(
          headingKey = Some("section2.heading"),
          rows = List(
            RegistrationSubmission.AnswerRow("label2", "answer2", Seq("labelArg2"))
          ),
          sectionKey = Some("section2.key"),
          headingArgs = Nil
        )
      )

      val set = RegistrationSubmission.DataSet(
        data,
        mappedPieces,
        answerSections
      )

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(set))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val draftData = Json.parse(
        """
          |{
          | "anotherKey": {
          |   "foo": "bar",
          |   "fizzbinn": true
          | },
          | "sectionKey": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | },
          | "registration": {},
          | "answerSections": {
          |   "sectionKey": [
          |     {
          |       "headingKey": "section1.heading",
          |       "rows": [
          |         {
          |           "label": "label1",
          |           "answer": "answer1",
          |           "labelArgs": [
          |             "labelArg1"
          |           ]
          |         }
          |       ],
          |       "sectionKey": "section1.key",
          |       "headingArgs": []
          |     },
          |     {
          |       "headingKey": "section2.heading",
          |       "rows": [
          |         {
          |           "label": "label2",
          |           "answer": "answer2",
          |           "labelArgs": [
          |             "labelArg2"
          |           ]
          |         }
          |       ],
          |       "sectionKey": "section2.key",
          |       "headingArgs": []
          |     }
          |   ]
          | }
          |}
          |""".stripMargin)

      val expectedDraft = RegistrationSubmissionDraft(
        draftId,
        internalId,
        existingDraft.createdAt,
        draftData,
        existingDraft.reference,
        existingDraft.inProgress)

      val result = controller.setDataset(draftId, "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft(draftId, internalId)
      verify(submissionRepository).setDraft(expectedDraft)
    }

  }

  ".getSection" should {

    "get existing draft when one exists" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(existingDraft)))

      val request = FakeRequest("GET", "path")

      val result = controller.getSection(draftId, "sectionKey").apply(request)
      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          | "createdAt": "1997-03-14T14:45:00",
          | "data": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | },
          | "reference": "theRef"
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }
    "return empty data when none exists in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )


      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(existingDraft)))

      val request = FakeRequest("GET", "path")

      val result = controller.getSection(draftId, "sectionKey2").apply(request)
      status(result) mustBe OK
      val expectedDraftJson = Json.parse(
        """
          |{
          | "createdAt": "1997-03-14T14:45:00",
          | "data": {},
          | "reference": "theRef"
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "return not found when there is no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getSection(draftId, "sectionKey2").apply(request)
      status(result) mustBe NOT_FOUND
    }
  }

  ".getDrafts" should {

    "get all drafts when some exist for Organisation" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      val drafts = List(
        RegistrationSubmissionDraft("draftId1", internalId, LocalDateTime.of(2012, 2, 3, 9, 30), Json.obj(), Some("ref"), Some(true)),
        RegistrationSubmissionDraft("draftId2", internalId, LocalDateTime.of(2010, 10, 10, 14, 40), Json.obj(), None, Some(true))
      )

      when(submissionRepository.getRecentDrafts(any(), any()))
        .thenReturn(Future.successful(drafts))

      val request = FakeRequest("GET", "path")

      val result = controller.getDrafts().apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getRecentDrafts(internalId, Organisation)

      val expectedDraftJson = Json.parse(
        """
          |[
          | {
          |   "createdAt": "2012-02-03T09:30:00",
          |   "draftId": "draftId1",
          |   "reference": "ref"
          | },
          | {
          |   "createdAt": "2010-10-10T14:40:00",
          |   "draftId": "draftId2"
          | }
          |]
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "get all drafts when some exist for Agent" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      val drafts = List(
        RegistrationSubmissionDraft("draftId1", internalId, LocalDateTime.of(2012, 2, 3, 9, 30), Json.obj(), Some("ref"), Some(true)),
        RegistrationSubmissionDraft("draftId2", internalId, LocalDateTime.of(2010, 10, 10, 14, 40), Json.obj(), None, Some(true))
      )

      when(submissionRepository.getRecentDrafts(any(), any()))
        .thenReturn(Future.successful(drafts))

      val request = FakeRequest("GET", "path")

      val result = controller.getDrafts().apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getRecentDrafts(internalId, Agent)

      val expectedDraftJson = Json.parse(
        """
          |[
          | {
          |   "createdAt": "2012-02-03T09:30:00",
          |   "draftId": "draftId1",
          |   "reference": "ref"
          | },
          | {
          |   "createdAt": "2010-10-10T14:40:00",
          |   "draftId": "draftId2"
          | }
          |]
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "get all drafts when none exist" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getRecentDrafts(any(), any()))
        .thenReturn(Future.successful(List()))

      val request = FakeRequest("GET", "path")

      val result = controller.getDrafts().apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getRecentDrafts(internalId, Organisation)

      val expectedDraftJson = Json.parse("[]")

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }
  }

}
