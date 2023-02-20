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
import services.dates.LocalDateTimeService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import utils.JsonFixtures

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrustDetailsSubmissionDraftControllerSpec extends AnyWordSpec with MockitoSugar with JsonFixtures with Inside with ScalaFutures
  with GuiceOneAppPerSuite {

  private val currentDateTime: LocalDateTime = LocalDateTime.of(1999, 3, 14, 13, 33)

  private val draftId: String = "draftId"

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private lazy val mockSubmissionDraft = Json.parse(
    """
      |{
      |    "draftId" : "98c002e9-ef92-420b-83f6-62e6fff0c301",
      |    "internalId" : "Int-b25955c7-6565-4702-be4b-3b5cddb71f54",
      |    "createdAt" : { "$date" : 1597323808000 },
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

  ".getTrustTaxable" should {

    "respond with OK and true when trust is taxable" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TrustDetailsSubmissionDraftController(
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

      val controller = new TrustDetailsSubmissionDraftController(
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

      val controller = new TrustDetailsSubmissionDraftController(
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

      val controller = new TrustDetailsSubmissionDraftController(
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

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustName(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }

  ".getCorrespondenceAddress" should {

    "respond with OK with the correspondence address" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getCorrespondenceAddress(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }

  ".getTrustUtr" should {

    "respond with OK and trust utr when answered as part of trust matching" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TrustDetailsSubmissionDraftController(
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
          |                    "whatIsTheUTR" : "1234567890"
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

      val result = controller.getTrustUtr(draftId).apply(request)

      status(result) mustBe OK

      val expectedDraftJson = JsString("1234567890")

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }

    "respond with NotFound when no draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustUtr(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

    "respond with NotFound when no trust utr in draft" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getTrustUtr(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }

  ".getWhenTrustSetup" should {

    "respond with OK with the start date at the new path" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
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

      val controller = new TrustDetailsSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(mockSubmissionDraftNoData)))

      val request = FakeRequest("GET", "path")

      val result = controller.getWhenTrustSetup(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }

  }

}
