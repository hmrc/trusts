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
import models._
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
import services.{BackwardsCompatibilityService, LocalDateTimeService, TaxYearService}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import utils.{JsonFixtures, JsonUtils}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class SubmissionDraftControllerSpec extends AnyWordSpec with MockitoSugar with JsonFixtures with Inside with ScalaFutures
  with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val currentDateTime: LocalDateTime = LocalDateTime.of(1999, 3, 14, 13, 33)
  private object LocalDateTimeServiceStub extends LocalDateTimeService {
    override def now: LocalDateTime = currentDateTime
  }

  private val draftId: String = "draftId"
  private val internalId: String = "id"
  private val createdAt: LocalDateTime = LocalDateTime.of(1997, 3, 14, 14, 45)

  private val backwardsCompatibilityService = mock[BackwardsCompatibilityService]
  private val taxYearService = mock[TaxYearService]

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

  private lazy val whenTrustSetupAtNewPath = Json.parse(
    """
      |{
      |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
      |    "createdAt" : { "$date" : 1597323808000 },
      |    "draftData" : {
      |        "status" : {
      |           "taxLiability" : "completed"
      |        },
      |        "trustDetails" : {
      |           "data": {
      |               "trustDetails": {
      |                  "whenTrustSetup" : "2015-04-06"
      |               }
      |           }
      |        },
      |        "main" : {
      |            "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |            "data" : {},
      |            "progress" : "InProgress",
      |            "createdAt" : "2020-08-13T13:37:53.787Z",
      |            "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54"
      |        }
      |    },
      |    "inProgress" : true
      |}
      |""".stripMargin).as[RegistrationSubmissionDraft]

  private lazy val mockSubmissionDraft = Json.parse(
    """
      |{
      |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
      |    "createdAt" : { "$date" : 1597323808000 },
      |    "draftData" : {
      |        "status" : {
      |           "taxLiability" : "completed"
      |        },
      |        "taxLiability" : {
      |            "data" : {
      |                "cyMinusFourYesNo" : true,
      |                "trustStartDate" : "2010-10-10",
      |                "didDeclareTaxToHMRCForYear4" : false
      |            }
      |        },
      |        "main" : {
      |            "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |            "data" : {
      |                "trustDetails" : {
      |                    "administrationInsideUK" : true,
      |                    "trusteesBasedInTheUK" : "UKBasedTrustees",
      |                    "trustName" : "Trust Name Details",
      |                    "whenTrustSetup" : "2010-08-21",
      |                    "establishedUnderScotsLaw" : true,
      |                    "governedInsideTheUK" : false,
      |                    "countryGoverningTrust" : "FR",
      |                    "residentOffshore" : false,
      |                    "status" : "completed"
      |                },
      |                "trustRegisteredOnline" : false,
      |                "trustHaveAUTR" : false
      |            },
      |            "progress" : "InProgress",
      |            "createdAt" : "2020-08-13T13:37:53.787Z",
      |            "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54"
      |        },
      |        "answerSections" : {
      |            "taxLiability" : [
      |                {
      |                    "headingKey" : "Tax liability 6 April 2016 to 5 April 2017",
      |                    "rows" : [
      |                        {
      |                            "label" : "Did the trust need to pay any tax from 6 April 2016 to 5 April 2017?",
      |                            "answer" : "Yes",
      |                            "labelArg" : ""
      |                        },
      |                        {
      |                            "label" : "Was the tax from 6 April 2016 to 5 April 2017 declared?",
      |                            "answer" : "No",
      |                            "labelArg" : ""
      |                        }
      |                    ]
      |                }
      |            ]
      |        },
      |        "registration": {
      |          "yearsReturns" : {
      |             "returns": [
      |                {
      |                    "taxReturnYear" : "17",
      |                    "taxConsequence" : true
      |                },
      |                {
      |                    "taxReturnYear" : "18",
      |                    "taxConsequence" : true
      |                }
      |             ]
      |          },
      |          "trust/entities/leadTrustees": {
      |            "leadTrusteeOrg": {
      |              "name": "Lead Org",
      |              "phoneNumber": "07911234567",
      |              "identification": {
      |                "utr": "1234567890"
      |              }
      |            }
      |          },
      |          "correspondence/address": {
      |            "line1": "Address line1",
      |            "line2": "Address line2",
      |            "postCode": "NE1 1EN",
      |            "country": "GB"
      |          }
      |        }
      |    },
      |    "inProgress" : true
      |}
      |""".stripMargin).as[RegistrationSubmissionDraft]

  private lazy val mockSubmissionDraftAgentDetails = Json.parse(
    """
      |{
      |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
      |    "createdAt" : { "$date" : 1597323808000 },
      |    "draftData" : {
      |        "main" : {
      |            "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |            "progress" : "InProgress",
      |            "createdAt" : "2020-08-13T13:37:53.787Z",
      |            "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54"
      |        },
      |        "registration": {
      |          "agentDetails": {
      |            "agentAddress": {
      |              "line1": "Agent address line1",
      |              "line2": "Agent address line2",
      |              "postCode": "AB1 1AB",
      |              "country": "GB"
      |            },
      |            "clientReference": "client-ref"
      |          }
      |        }
      |    },
      |    "inProgress" : true
      |}
      |""".stripMargin).as[RegistrationSubmissionDraft]

  private lazy val mockSubmissionDraftNoData = Json.parse(
    """
      |{
      |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
      |    "createdAt" : { "$date" : 1597323808000 },
      |    "draftData" : {
      |        "main" : {
      |            "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |            "data" : {},
      |            "progress" : "InProgress",
      |            "createdAt" : "2020-08-13T13:37:53.787Z",
      |            "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54"
      |        }
      |    },
      |    "inProgress" : true
      |}
      |""".stripMargin).as[RegistrationSubmissionDraft]

  private val existingDraft = RegistrationSubmissionDraft(
    draftId = draftId,
    internalId = internalId,
    createdAt = createdAt,
    draftData = existingDraftData,
    reference = Some("theRef"),
    inProgress = Some(true)
  )

  ".setSection" should {

    "return 'bad request' for malformed body" in {

      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
            RegistrationSubmission.AnswerRow("label1", "answer1", "labelArg1")
          ),
          sectionKey = Some("section1.key"),
          headingArgs = Nil
        ),
        RegistrationSubmission.AnswerSection(
          headingKey = Some("section2.heading"),
          rows = List(
            RegistrationSubmission.AnswerRow("label2", "answer2", "labelArg2")
          ),
          sectionKey = Some("section2.key"),
          headingArgs = Nil
        )
      )

      val set = RegistrationSubmission.DataSet(
        data,
        Some(registration.Status.Completed),
        mappedPieces,
        answerSections)

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
          | "status": {
          |   "sectionKey": "completed"
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
          |           "labelArg": "labelArg1"
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
          |           "labelArg": "labelArg2"
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

      val result = controller.setSectionSet(draftId, "sectionKey").apply(request)
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
            RegistrationSubmission.AnswerRow("label1", "answer1", "labelArg1")
          ),
          sectionKey = Some("section1.key"),
          headingArgs = Nil
        ),
        RegistrationSubmission.AnswerSection(
          headingKey = Some("section2.heading"),
          rows = List(
            RegistrationSubmission.AnswerRow("label2", "answer2", "labelArg2")
          ),
          sectionKey = Some("section2.key"),
          headingArgs = Nil
        )
      )

      val set = RegistrationSubmission.DataSet(
        data,
        Some(registration.Status.Completed),
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
          | "status": {
          |   "sectionKey": "completed"
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
          |           "labelArg": "labelArg1"
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
          |           "labelArg": "labelArg2"
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

      val result = controller.setSectionSet(draftId, "sectionKey").apply(request)
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
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

  ".removeDraft" should {

    "remove draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.removeDraft(any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("GET", "path")

      val result = controller.removeDraft(draftId).apply(request)
      status(result) mustBe OK

      verify(submissionRepository).removeDraft(draftId, internalId)
    }
  }

  ".getTrustTaxable" should {

    "respond with OK and true when trust is taxable" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      val cache = Json.parse(
        """
          |{
          |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
          |    "createdAt" : { "$date" : 1597323808000 },
          |    "draftData" : {
          |       "main" : {
          |            "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |            "data" : {
          |                    "trustTaxable" : true
          |            },
          |            "progress" : "InProgress",
          |            "createdAt" : "2020-08-13T13:37:53.787Z",
          |            "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54"
          |        }
          |    },
          |    "inProgress" : true
          |}
          |""".stripMargin).as[RegistrationSubmissionDraft]

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(cache)))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustTaxable(draftId).apply(request)

      status(result) mustBe OK

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe JsBoolean(true)
    }

    "respond with OK and false when trust is non taxable" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      val cache = Json.parse(
        """
          |{
          |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
          |    "createdAt" : { "$date" : 1597323808000 },
          |    "draftData" : {
          |       "main" : {
          |            "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |            "data" : {
          |                    "trustTaxable" : false
          |            },
          |            "progress" : "InProgress",
          |            "createdAt" : "2020-08-13T13:37:53.787Z",
          |            "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54"
          |        }
          |    },
          |    "inProgress" : true
          |}
          |""".stripMargin).as[RegistrationSubmissionDraft]

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(cache)))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustTaxable(draftId).apply(request)

      status(result) mustBe OK

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe JsBoolean(false)
    }
  }

  ".getTrustName" should {

    "respond with OK and trust name when answered as part of trust matching" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      val cache = Json.parse(
        """
          |{
          |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
          |    "createdAt" : { "$date" : 1597323808000 },
          |    "draftData" : {
          |       "main" : {
          |            "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |            "data" : {
          |                "matching" : {
          |                    "trustName" : "Trust Name Matching"
          |                }
          |            },
          |            "progress" : "InProgress",
          |            "createdAt" : "2020-08-13T13:37:53.787Z",
          |            "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54"
          |        }
          |    },
          |    "inProgress" : true
          |}
          |""".stripMargin).as[RegistrationSubmissionDraft]

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(cache)))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustName(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          | "trustName": "Trust Name Matching"
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "response with OK and trust name when answered as part of trust details" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      val cache = Json.parse(
        """
          |{
          |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
          |    "createdAt" : { "$date" : 1597323808000 },
          |    "draftData" : {
          |       "trustDetails": {
          |         "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |         "data": {
          |           "trustDetails": {
          |             "trustName": "Trust Name Details"
          |           }
          |         }
          |       }
          |    },
          |    "inProgress" : true
          |}
          |""".stripMargin).as[RegistrationSubmissionDraft]

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(cache)))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustName(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          | "trustName": "Trust Name Details"
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustName(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no start date in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustName(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }

  ".getWhenTrustSetup" should {

    "respond with OK with the start date at the new path" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(whenTrustSetupAtNewPath)))

      val request = FakeRequest("GET", "path")

      val result = controller.getWhenTrustSetup(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          | "startDate": "2015-04-06"
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getWhenTrustSetup(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no start date in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getWhenTrustSetup(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }

  ".getLeadTrustee" should {

    "respond with OK with the lead trustee" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraft)))

      val request = FakeRequest("GET", "path")

      val result = controller.getLeadTrustee(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          |  "leadTrusteeOrg": {
          |    "name": "Lead Org",
          |    "phoneNumber": "07911234567",
          |    "identification": {
          |      "utr": "1234567890"
          |    }
          |  }
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getLeadTrustee(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }
  }

  ".getCorrespondenceAddress" should {

    "respond with OK with the correspondence address" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraft)))

      val request = FakeRequest("GET", "path")

      val result = controller.getCorrespondenceAddress(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          | "line1": "Address line1",
          | "line2": "Address line2",
          | "postCode": "NE1 1EN",
          | "country": "GB"
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getCorrespondenceAddress(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no correspondence address in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getCorrespondenceAddress(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }

  ".reset" should {

    "respond with OK after cleaning up draft answers, status, mapped registration data and print answer sections" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      lazy val expectedAfterCleanup = Json.parse(
        """
          |{
          |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
          |    "createdAt" : { "$date" : 1597323808000 },
          |    "draftData" : {
          |        "status" : {},
          |        "main" : {
          |            "_id" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |            "data" : {
          |                "trustDetails" : {
          |                    "administrationInsideUK" : true,
          |                    "trusteesBasedInTheUK" : "UKBasedTrustees",
          |                    "trustName" : "Trust Name Details",
          |                    "whenTrustSetup" : "2010-08-21",
          |                    "establishedUnderScotsLaw" : true,
          |                    "governedInsideTheUK" : false,
          |                    "countryGoverningTrust" : "FR",
          |                    "residentOffshore" : false,
          |                    "status" : "completed"
          |                },
          |                "trustRegisteredOnline" : false,
          |                "trustHaveAUTR" : false
          |            },
          |            "progress" : "InProgress",
          |            "createdAt" : "2020-08-13T13:37:53.787Z",
          |            "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54"
          |        },
          |        "answerSections": {},
          |        "registration": {
          |          "trust/entities/leadTrustees": {
          |            "leadTrusteeOrg": {
          |              "name": "Lead Org",
          |              "phoneNumber": "07911234567",
          |              "identification": {
          |                "utr": "1234567890"
          |              }
          |            }
          |          },
          |          "correspondence/address": {
          |            "line1": "Address line1",
          |            "line2": "Address line2",
          |            "postCode": "NE1 1EN",
          |            "country": "GB"
          |          }
          |        }
          |    },
          |    "inProgress" : true
          |}
          |""".stripMargin).as[RegistrationSubmissionDraft]

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraft)))

      when(submissionRepository.setDraft(any())).thenReturn(Future.successful(true))

      val request = FakeRequest("GET", "path")

      val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

      status(result) mustBe OK

      verify(submissionRepository).getDraft(draftId, internalId)
      verify(submissionRepository).setDraft(expectedAfterCleanup)
    }

    "respond with InternalServerError when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  ".removeRoleInCompany" should {

    "return OK" when {
      "draft data successfully updated" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(Future.successful(Some(mockSubmissionDraft)))

        when(submissionRepository.setDraft(any())).thenReturn(Future.successful(true))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe OK
      }
    }

    "return internal server error" when {
      "failure getting draft" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "failure setting draft" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(Future.successful(Some(mockSubmissionDraft)))

        when(submissionRepository.setDraft(any())).thenReturn(Future.successful(false))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".getAgentAddress" should {

    "respond with OK with the agent address" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftAgentDetails)))

      val request = FakeRequest("GET", "path")

      val result = controller.getAgentAddress(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          | "line1": "Agent address line1",
          | "line2": "Agent address line2",
          | "postCode": "AB1 1AB",
          | "country": "GB"
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getAgentAddress(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no agent address in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getAgentAddress(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }
  }

  ".getClientReference" should {

    "respond with OK with the client reference" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftAgentDetails)))

      val request = FakeRequest("GET", "path")

      val result = controller.getClientReference(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |"client-ref"
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getClientReference(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no client reference in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getClientReference(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }
  }

  ".removeDeceasedSettlorMappedPiece" should {

    "return Ok" when {
      "draft data successfully updated" in {

        lazy val dataBefore = Json.parse(
          """
            |{
            |  "draftId": "98c002e9-ef92-420b-83f6-62e6fff0c301",
            |  "internalId": "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
            |  "createdAt": {
            |    "$date": 1597323808000
            |  },
            |  "draftData": {
            |    "registration": {
            |      "trust/entities/deceased": {
            |        "key": "value"
            |      }
            |    }
            |  }
            |}
            |""".stripMargin).as[RegistrationSubmissionDraft]

        lazy val dataAfter = Json.parse(
          """
            |{
            |  "draftId": "98c002e9-ef92-420b-83f6-62e6fff0c301",
            |  "internalId": "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
            |  "createdAt": {
            |    "$date": 1597323808000
            |  },
            |  "draftData": {
            |    "registration": {
            |    }
            |  }
            |}
            |""".stripMargin).as[RegistrationSubmissionDraft]

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(Some(dataBefore)))

        when(submissionRepository.setDraft(any())).thenReturn(Future.successful(true))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe OK

        verify(submissionRepository).setDraft(dataAfter)
      }
    }

    "return NotFound" when {
      "failure getting draft" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(None))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "return InternalServerError" when {
      "failure setting draft" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(Some(mockSubmissionDraft)))

        when(submissionRepository.setDraft(any())).thenReturn(Future.successful(false))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".removeLivingSettlorsMappedPiece" should {

    "return Ok" when {
      "draft data successfully updated" in {

        lazy val dataBefore = Json.parse(
          """
            |{
            |  "draftId": "98c002e9-ef92-420b-83f6-62e6fff0c301",
            |  "internalId": "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
            |  "createdAt": {
            |    "$date": 1597323808000
            |  },
            |  "draftData": {
            |    "registration": {
            |      "trust/entities/settlors": {
            |        "key": "value"
            |      }
            |    }
            |  }
            |}
            |""".stripMargin).as[RegistrationSubmissionDraft]

        lazy val dataAfter = Json.parse(
          """
            |{
            |  "draftId": "98c002e9-ef92-420b-83f6-62e6fff0c301",
            |  "internalId": "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
            |  "createdAt": {
            |    "$date": 1597323808000
            |  },
            |  "draftData": {
            |    "registration": {
            |    }
            |  }
            |}
            |""".stripMargin).as[RegistrationSubmissionDraft]

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(Some(dataBefore)))

        when(submissionRepository.setDraft(any())).thenReturn(Future.successful(true))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe OK

        verify(submissionRepository).setDraft(dataAfter)
      }
    }

    "return NotFound" when {
      "failure getting draft" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(None))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "return InternalServerError" when {
      "failure setting draft" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(Some(mockSubmissionDraft)))

        when(submissionRepository.setDraft(any())).thenReturn(Future.successful(false))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".adjustDraft" must {

    val draftId: String = "358df5dd-63e3-4cad-aa93-403c83af97cd"
    val internalId: String = "Int-d387bcea-3ca2-48ab-b6bc-3919a050414d"
    val createdAt: LocalDateTime = LocalDateTime.of(2021, 2, 3, 14, 0)
    val reference: String = "234425525"

    val oldData: JsValue = JsonUtils.getJsonValueFromFile("backwardscompatibility/old_assets_and_agents_draft_data.json")
    val newData: JsValue = JsonUtils.getJsonValueFromFile("backwardscompatibility/new_assets_and_agents_draft_data.json")

    def buildDraft(data: JsValue) = RegistrationSubmissionDraft(draftId, internalId, createdAt, data, Some(reference), Some(true))

    "return Ok" when {
      "draft has old-style data" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(Some(buildDraft(oldData))))
        when(backwardsCompatibilityService.adjustDraftData(any())).thenReturn(newData)
        when(submissionRepository.setDraft(any())).thenReturn(Future.successful(true))

        val request = FakeRequest("GET", "path")

        val result = controller.adjustDraft(draftId).apply(request)

        status(result) mustBe OK

        verify(submissionRepository).setDraft(buildDraft(newData))
      }

      "draft has new-style data" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(Some(buildDraft(newData))))
        when(backwardsCompatibilityService.adjustDraftData(any())).thenReturn(newData)
        when(submissionRepository.setDraft(any())).thenReturn(Future.successful(true))

        val request = FakeRequest("GET", "path")

        val result = controller.adjustDraft(draftId).apply(request)

        status(result) mustBe OK

        verify(submissionRepository).setDraft(buildDraft(newData))
      }
    }

    "return NotFound" when {
      "draft not found" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new SubmissionDraftController(
          submissionRepository,
          identifierAction,
          LocalDateTimeServiceStub,
          Helpers.stubControllerComponents(),
          backwardsCompatibilityService,
          taxYearService
        )

        when(submissionRepository.getDraft(any(), any())).thenReturn(Future.successful(None))

        val request = FakeRequest("GET", "path")

        val result = controller.adjustDraft(draftId).apply(request)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  ".updateTaxLiabilityStatus" should {

    def mockDraft(draftData: JsValue) = RegistrationSubmissionDraft(
      draftId = draftId,
      internalId = internalId,
      createdAt = createdAt,
      draftData = draftData,
      reference = None,
      inProgress = None
    )

    "response with Ok when the dates are not present" in {

      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      val initialDraftData = Json.obj()

      val expectedDraftData = initialDraftData

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockDraft(initialDraftData))))

      val request = FakeRequest("GET", "path")

      val result = controller.updateTaxLiability(draftId).apply(request)

      status(result) mustBe OK

      verify(submissionRepository, times(0)).setDraft(mockDraft(expectedDraftData))
    }

    "respond with Ok and not update the status when the dates are the same" in {

      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      val initialDraftData = Json.parse(
        """
          |{
          |  "taxLiability": {
          |    "data": {
          |      "trustStartDate": "1996-02-03"
          |    }
          |  },
          |  "trustDetails": {
          |    "data": {
          |      "trustDetails": {
          |        "whenTrustSetup": "1996-02-03"
          |      }
          |    }
          |  },
          |  "status": {
          |    "taxLiability": "completed"
          |  },
          |  "registration": {
          |    "yearsReturns": {
          |      "foo": "bar"
          |    }
          |  },
          |  "answerSections": {
          |    "taxLiability": [
          |      {
          |        "foo": "bar"
          |      }
          |    ]
          |  }
          |}
          |""".stripMargin)

      val expectedDraftData = initialDraftData

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockDraft(initialDraftData))))

      val request = FakeRequest("GET", "path")

      val result = controller.updateTaxLiability(draftId).apply(request)

      status(result) mustBe OK

      verify(submissionRepository, times(0)).setDraft(mockDraft(expectedDraftData))
    }

    "respond with Ok and update the status when the dates are different" in {

      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      val initialDraftData = Json.parse(
        """
          |{
          |  "taxLiability": {
          |    "data": {
          |      "trustStartDate": "1996-02-03"
          |    }
          |  },
          |  "trustDetails": {
          |    "data": {
          |      "trustDetails": {
          |        "whenTrustSetup": "1997-02-03"
          |      }
          |    }
          |  },
          |  "status": {
          |    "taxLiability": "completed"
          |  },
          |  "registration": {
          |    "yearsReturns": {
          |      "foo": "bar"
          |    }
          |  },
          |  "answerSections": {
          |    "taxLiability": [
          |      {
          |        "foo": "bar"
          |      }
          |    ]
          |  }
          |}
          |""".stripMargin)

      val expectedDraftData = Json.parse(
        """
          |{
          |  "taxLiability": {
          |    "data": {
          |      "trustStartDate": "1996-02-03"
          |    }
          |  },
          |  "trustDetails": {
          |    "data": {
          |      "trustDetails": {
          |        "whenTrustSetup": "1997-02-03"
          |      }
          |    }
          |  },
          |  "status": {
          |  },
          |  "registration": {
          |  },
          |  "answerSections": {
          |  }
          |}
          |""".stripMargin)

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockDraft(initialDraftData))))

      when(submissionRepository.setDraft(any())).thenReturn(Future.successful(true))

      val request = FakeRequest("GET", "path")

      val result = controller.updateTaxLiability(draftId).apply(request)

      status(result) mustBe OK

      verify(submissionRepository, times(1)).setDraft(mockDraft(expectedDraftData))
    }

    "respond with InternalServerError when error setting the updated draft data" in {

      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      val initialDraftData = Json.parse(
        """
          |{
          |  "taxLiability": {
          |    "data": {
          |      "trustStartDate": "1996-02-03"
          |    }
          |  },
          |  "trustDetails": {
          |    "data": {
          |      "trustDetails": {
          |        "whenTrustSetup": "1997-02-03"
          |      }
          |    }
          |  },
          |  "status": {
          |    "taxLiability": "completed"
          |  }
          |}
          |""".stripMargin)

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockDraft(initialDraftData))))

      when(submissionRepository.setDraft(any())).thenReturn(Future.successful(false))

      val request = FakeRequest("GET", "path")

      val result = controller.updateTaxLiability(draftId).apply(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "respond with NotFound when no draft" in {

      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.updateTaxLiability(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }
  }

  ".getFirstTaxYearAvailable" should {

    val date = LocalDate.parse("2015-04-06")

    "respond with OK with the first tax year available" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(whenTrustSetupAtNewPath)))

      val firstTaxYearAvailable = FirstTaxYearAvailable(4, earlierYearsToDeclare = true)

      when(taxYearService.firstTaxYearAvailable(any())).thenReturn(firstTaxYearAvailable)

      val request = FakeRequest("GET", "path")

      val result = controller.getFirstTaxYearAvailable(draftId).apply(request)

      status(result) mustBe OK

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(firstTaxYearAvailable)

      verify(taxYearService).firstTaxYearAvailable(date)
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getFirstTaxYearAvailable(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no start date in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        backwardsCompatibilityService,
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getFirstTaxYearAvailable(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }
}
