/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.EitherT
import controllers.actions.FakeIdentifierAction
import errors.TrustErrors
import models._
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

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxLiabilitySubmissionDraftControllerSpec extends AnyWordSpec with MockitoSugar with JsonFixtures with Inside with ScalaFutures
  with GuiceOneAppPerSuite {

  private val currentDateTime: LocalDateTime = LocalDateTime.of(1999, 3, 14, 13, 33)

  private val draftId: String = "draftId"

  private val taxYearService = mock[TaxYearService]

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private lazy val whenTrustSetupAtNewPath = Json.parse(
    """
      |{
      |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
      |    "createdAt" : { "$date" : 1597323808000 },
      |    "draftData" : {
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

  private lazy val draftWithTaxLiabilityStartDate = Json.parse(
    """
      |{
      |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
      |    "createdAt" : { "$date" : 1597323808000 },
      |    "draftData" : {
      |        "taxLiability" : {
      |            "_id" : "5027c148-d7b4-4e48-ac46-21cce366dfd7",
      |            "data" : {
      |                "trustStartDate" : "2020-10-10",
      |                "cyMinusOneYesNo" : true,
      |                "didDeclareTaxToHMRCForYear1" : false
      |            },
      |            "internalId" : "Int-0e7d32ac-ccba-4f2c-a3b6-d71ede5a6dba"
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

  ".getTaxLiabilityStartDate" should {

    "respond with OK with the start date" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TaxLiabilitySubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(draftWithTaxLiabilityStartDate)))))

      val request = FakeRequest("GET", "path")

      val result = controller.getTaxLiabilityStartDate(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          | "startDate": "2020-10-10"
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TaxLiabilitySubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(None))))

      val request = FakeRequest("GET", "path")

      val result = controller.getTaxLiabilityStartDate(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no start date in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TaxLiabilitySubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraftNoData)))))

      val request = FakeRequest("GET", "path")

      val result = controller.getTaxLiabilityStartDate(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }

  ".getFirstTaxYearAvailable" should {

    val date = LocalDate.parse("2015-04-06")

    "respond with OK with the first tax year available" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TaxLiabilitySubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(whenTrustSetupAtNewPath)))))

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

      val controller = new TaxLiabilitySubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(None))))

      val request = FakeRequest("GET", "path")

      val result = controller.getFirstTaxYearAvailable(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no start date in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TaxLiabilitySubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents(),
        taxYearService
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraftNoData)))))

      val request = FakeRequest("GET", "path")

      val result = controller.getFirstTaxYearAvailable(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }
}
