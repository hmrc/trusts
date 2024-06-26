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

package controllers

import cats.data.EitherT
import controllers.actions.FakeIdentifierAction
import errors.{ServerError, TrustErrors}
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
import services.dates.TimeService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import utils.JsonFixtures

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CleanupSubmissionDraftControllerSpec extends AnyWordSpec with MockitoSugar with JsonFixtures with Inside with ScalaFutures
  with GuiceOneAppPerSuite {

  private val currentDateTime: Instant =
    LocalDateTime.of(1999, 3, 14, 13, 33).toInstant(ZoneOffset.UTC)

  private val draftId: String = "draftId"
  private val internalId: String = "id"

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private lazy val mockSubmissionDraft = Json.parse(
    """
      |{
      |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
      |    "createdAt" : { "$date" : {  "$numberLong"  : "1597323808000"  } },
      |    "draftData" : {
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

  private object TimeServiceStub extends TimeService {
    override def now: Instant = currentDateTime
  }

  ".removeDraft" should {

    "remove draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new CleanupSubmissionDraftController(
        submissionRepository,
        identifierAction,
        TimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.removeDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

      val request = FakeRequest("GET", "path")

      val result = controller.removeDraft(draftId).apply(request)
      status(result) mustBe OK

      verify(submissionRepository).removeDraft(draftId, internalId)
    }

    "respond with InternalServerError" when {
      "repository returns an exception from Mongo, where message is nonEmpty" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.removeDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError("operation failed due to exception from Mongo")))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDraft(draftId).apply(request)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "repository returns an exception from Mongo, where message is an empty string" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.removeDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDraft(draftId).apply(request)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".reset" should {

    "respond with OK after cleaning up draft answers, status, mapped registration data and print answer sections" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new CleanupSubmissionDraftController(
        submissionRepository,
        identifierAction,
        TimeServiceStub,
        Helpers.stubControllerComponents()
      )

      lazy val expectedAfterCleanup = Json.parse(
        """
          |{
          |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
          |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
          |    "createdAt" : { "$date" : {  "$numberLong"  : "1597323808000"  } },
          |    "draftData" : {
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
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

      when(submissionRepository.setDraft(any()))
        .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

      val request = FakeRequest("DELETE", "path")

      val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

      status(result) mustBe OK

      verify(submissionRepository).getDraft(draftId, internalId)
      verify(submissionRepository).setDraft(expectedAfterCleanup)
    }

    "respond with InternalServerError" when {

      "no draft found" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(None))))

        val request = FakeRequest("GET", "path")

        val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.getDraft returns an exception from Mongo, where message is nonEmpty" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(
            Left(ServerError("operation failed due to exception from Mongo"))
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.getDraft returns an exception from Mongo, where message is an empty string" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(
            Left(ServerError())
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns false - failed to set section" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(false))))

        val request = FakeRequest("DELETE", "path")

        val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns an exception from Mongo, where message is nonEmpty" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError("operation failed due to exception from Mongo")))))

        val request = FakeRequest("DELETE", "path")

        val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns an exception from Mongo, where message is an empty string" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val request = FakeRequest("DELETE", "path")

        val result = controller.reset(draftId, "taxLiability", "yearsReturns").apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".removeRoleInCompany" should {

    "return OK" when {
      "draft data successfully updated" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe OK
      }
    }

    "return internal server error" when {
      "no draft found" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(None))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.getDraft returns an exception from Mongo, where message is nonEmpty" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(
            Left(ServerError("operation failed due to exception from Mongo"))
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.getDraft returns an exception from Mongo, where message is an empty string" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Left(ServerError()))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns false - failed to set draft" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(false))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns an exception from Mongo, where message is nonEmpty" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError("operation failed due to exception from Mongo")))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns an exception from Mongo, where message is an empty string" in {
        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeRoleInCompany(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
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
            |     "$date": {
            |         "$numberLong" : "1597323808000"
            |       }
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
            |     "$date": {
            |         "$numberLong" : "1597323808000"
            |       }
            |  },
            |  "draftData": {
            |    "registration": {
            |    }
            |  }
            |}
            |""".stripMargin).as[RegistrationSubmissionDraft]

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(dataBefore)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe OK

        verify(submissionRepository).setDraft(dataAfter)
      }
    }

    "return NotFound" when {
      "submissionRepository.getDraft returns None" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(None))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "return InternalServerError" when {
      "submissionRepository.getDraft returns an exception from Mongo, where message is nonEmpty" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(
            Left(ServerError("operation failed due to exception from Mongo"))
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.getDraft returns an exception from Mongo, where message is an empty string" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(
            Left(ServerError())
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns false - failed to set draft" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(false))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns an exception from Mongo, where message is nonEmpty" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(
            Left(ServerError("operation failed due to exception from Mongo"))
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.removeDeceasedSettlorMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns an exception from Mongo, where message is an empty string" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(
            Left(ServerError())
          )))

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
            |     "$date": {
            |         "$numberLong" : "1597323808000"
            |       }
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
            |     "$date": {
            |         "$numberLong" : "1597323808000"
            |       }
            |  },
            |  "draftData": {
            |    "registration": {
            |    }
            |  }
            |}
            |""".stripMargin).as[RegistrationSubmissionDraft]

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(dataBefore)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe OK

        verify(submissionRepository).setDraft(dataAfter)
      }
    }

    "return NotFound" when {
      "no draft found" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(None))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "return InternalServerError" when {
      "submissionRepository.getDraft returns an exception from Mongo, where message is nonEmpty" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(
            Left(ServerError("operation failed due to exception from Mongo"))
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.getDraft returns an exception from Mongo, where message is an empty string" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(
            Left(ServerError())
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns false - failed to set draft" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(false))))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns an exception from Mongo, where message is nonEmpty" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(
            Left(ServerError("operation failed due to exception from Mongo"))
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "submissionRepository.setDraft returns an exception from Mongo, where message is an empty string" in {

        val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
        val submissionRepository = mock[RegistrationSubmissionRepository]

        val controller = new CleanupSubmissionDraftController(
          submissionRepository,
          identifierAction,
          TimeServiceStub,
          Helpers.stubControllerComponents()
        )

        when(submissionRepository.getDraft(any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

        when(submissionRepository.setDraft(any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(
            Left(ServerError())
          )))

        val request = FakeRequest("GET", "path")

        val result = controller.removeLivingSettlorsMappedPiece(draftId).apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
