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
import models.registration.RegistrationSubmissionDraft
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
import services.TaxYearService
import services.dates.LocalDateTimeService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import utils.JsonFixtures

import java.time.LocalDateTime
import scala.concurrent.Future

class AgentSubmissionDraftControllerSpec extends AnyWordSpec with MockitoSugar with JsonFixtures with Inside with ScalaFutures
  with GuiceOneAppPerSuite {

  private val currentDateTime: LocalDateTime = LocalDateTime.of(1999, 3, 14, 13, 33)

  private val draftId: String = "draftId"

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

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


  private object LocalDateTimeServiceStub extends LocalDateTimeService {
    override def now: LocalDateTime = currentDateTime
  }

  ".getAgentAddress" should {

    "respond with OK with the agent address" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new AgentSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new AgentSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new AgentSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new AgentSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new AgentSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new AgentSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getClientReference(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }
  }

}
