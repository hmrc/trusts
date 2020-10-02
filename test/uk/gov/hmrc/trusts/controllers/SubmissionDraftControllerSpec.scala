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

package uk.gov.hmrc.trusts.controllers

import java.time.LocalDateTime

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsNull, JsString, Json}
import play.api.mvc.BodyParsers
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.repositories.RegistrationSubmissionRepository
import uk.gov.hmrc.trusts.services.LocalDateTimeService
import uk.gov.hmrc.trusts.utils.JsonRequests

import scala.concurrent.Future

class SubmissionDraftControllerSpec extends WordSpec with MockitoSugar
  with MustMatchers with JsonRequests with Inside with ScalaFutures
  with GuiceOneAppPerSuite {

  lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val currentDateTime: LocalDateTime = LocalDateTime.of(1999, 3, 14, 13, 33)
  private object LocalDateTimeServiceStub extends LocalDateTimeService {
    override def now: LocalDateTime = currentDateTime
  }

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
    "DRAFTID",
    "id",
    LocalDateTime.of(1997, 3, 14, 14, 45),
    existingDraftData,
    Some("theRef"),
    Some(true)
  )

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

      val result = controller.setSection("DRAFTID", "sectionKey").apply(request)
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

      val expectedDraft = RegistrationSubmissionDraft("DRAFTID", "id", currentDateTime, draftData, Some("theReference"), Some(true))

      val result = controller.setSection("DRAFTID", "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft("DRAFTID", "id")
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

      val expectedDraft = RegistrationSubmissionDraft("DRAFTID", "id", existingDraft.createdAt, draftData, Some("newRef"), Some(true))

      val result = controller.setSection("DRAFTID", "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft("DRAFTID", "id")
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
          Some("section1.heading"),
          List(
            RegistrationSubmission.AnswerRow("label1", "answer1", "labelArg1")
          ),
          Some("section1.key")
        ),
        RegistrationSubmission.AnswerSection(
          Some("section2.heading"),
          List(
            RegistrationSubmission.AnswerRow("label2", "answer2", "labelArg2")
          ),
          Some("section2.key")
        )
      )

      val set = RegistrationSubmission.DataSet(
        data,
        Some(Status.Completed),
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
          |       "sectionKey": "section1.key"
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
          |       "sectionKey": "section2.key"
          |     }
          |   ]
          | }
          |}
          |""".stripMargin)

      val expectedDraft = RegistrationSubmissionDraft(
        "DRAFTID",
        "id",
        existingDraft.createdAt,
        draftData,
        existingDraft.reference,
        existingDraft.inProgress)

      val result = controller.setSectionSet("DRAFTID", "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft("DRAFTID", "id")
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
          Some("section1.heading"),
          List(
            RegistrationSubmission.AnswerRow("label1", "answer1", "labelArg1")
          ),
          Some("section1.key")
        ),
        RegistrationSubmission.AnswerSection(
          Some("section2.heading"),
          List(
            RegistrationSubmission.AnswerRow("label2", "answer2", "labelArg2")
          ),
          Some("section2.key")
        )
      )

      val set = RegistrationSubmission.DataSet(
        data,
        Some(Status.Completed),
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
          |       "sectionKey": "section1.key"
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
          |       "sectionKey": "section2.key"
          |     }
          |   ]
          | }
          |}
          |""".stripMargin)

      val expectedDraft = RegistrationSubmissionDraft(
        "DRAFTID",
        "id",
        existingDraft.createdAt,
        draftData,
        existingDraft.reference,
        existingDraft.inProgress)

      val result = controller.setSectionSet("DRAFTID", "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft("DRAFTID", "id")
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

      val result = controller.getSection("DRAFTID", "sectionKey").apply(request)
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

      val result = controller.getSection("DRAFTID", "sectionKey2").apply(request)
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

      val result = controller.getSection("DRAFTID", "sectionKey2").apply(request)
      status(result) mustBe NOT_FOUND
    }
  }

  ".getDrafts" should {

    "get all drafts when some exist" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      val drafts = List(
        RegistrationSubmissionDraft("draftId1", "id", LocalDateTime.of(2012, 2, 3, 9, 30), Json.obj(), Some("ref"), Some(true)),
        RegistrationSubmissionDraft("draftId2", "id", LocalDateTime.of(2010, 10, 10, 14, 40), Json.obj(), None, Some(true))
      )

      when(submissionRepository.getAllDrafts(any()))
        .thenReturn(Future.successful(drafts))

      val request = FakeRequest("GET", "path")

      val result = controller.getDrafts().apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getAllDrafts("id")

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

      when(submissionRepository.getAllDrafts(any()))
        .thenReturn(Future.successful(List()))

      val request = FakeRequest("GET", "path")

      val result = controller.getDrafts().apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getAllDrafts("id")

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
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.removeDraft(any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest("GET", "path")

      val result = controller.removeDraft("DRAFTID").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).removeDraft("DRAFTID", "id")
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
        Helpers.stubControllerComponents()
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

      val result = controller.getTrustName("DRAFTID").apply(request)

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
        Helpers.stubControllerComponents()
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

      val result = controller.getTrustName("DRAFTID").apply(request)

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

  }

  ".getWhenTrustSetup" should {

     "respond with OK with the start date at the old path" in {
       val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
       val submissionRepository = mock[RegistrationSubmissionRepository]

       val controller = new SubmissionDraftController(
         submissionRepository,
         identifierAction,
         LocalDateTimeServiceStub,
         Helpers.stubControllerComponents()
       )

       when(submissionRepository.getDraft(any(), any()))
         .thenReturn(Future.successful(Some(mockSubmissionDraft)))

       val request = FakeRequest("GET", "path")

       val result = controller.getWhenTrustSetup("DRAFTID").apply(request)

       status(result) mustBe OK

       val expectedDraftJson = Json.parse(
         """
           |{
           | "startDate": "2010-08-21"
           |}
           |""".stripMargin)

       contentType(result) mustBe Some(JSON)
       contentAsJson(result) mustBe expectedDraftJson
     }

    "respond with OK with the start date at the new path" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(whenTrustSetupAtNewPath)))

      val request = FakeRequest("GET", "path")

      val result = controller.getWhenTrustSetup("DRAFTID").apply(request)

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
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getWhenTrustSetup("DRAFTID").apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no start date in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getWhenTrustSetup("DRAFTID").apply(request)

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
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraft)))

      val request = FakeRequest("GET", "path")

      val result = controller.getLeadTrustee("DRAFTID").apply(request)

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
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getLeadTrustee("DRAFTID").apply(request)

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
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraft)))

      val request = FakeRequest("GET", "path")

      val result = controller.getCorrespondenceAddress("DRAFTID").apply(request)

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
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getCorrespondenceAddress("DRAFTID").apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no start date in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )


      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getCorrespondenceAddress("DRAFTID").apply(request)

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
        Helpers.stubControllerComponents()
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

      val result = controller.reset("DRAFTID", "taxLiability", "yearsReturns").apply(request)

      status(result) mustBe OK

      verify(submissionRepository).getDraft("DRAFTID", "id")
      verify(submissionRepository).setDraft(expectedAfterCleanup)
    }

    "respond with InternalServerError when no draft" in {
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

      val result = controller.reset("DRAFTID", "taxLiability", "yearsReturns").apply(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
