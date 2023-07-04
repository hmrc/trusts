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

class TrusteeSubmissionDraftControllerSpec extends AnyWordSpec with MockitoSugar with JsonFixtures with Inside with ScalaFutures
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


  private object LocalDateTimeServiceStub extends LocalDateTimeService {
    override def now: LocalDateTime = currentDateTime
  }

  ".getLeadTrustee" should {

    "respond with OK with the lead trustee" in {
      val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]

      val controller = new TrusteeSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(Some(mockSubmissionDraft)))))

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

      val controller = new TrusteeSubmissionDraftController(
        submissionRepository,
        identifierAction,
        LocalDateTimeServiceStub,
        Helpers.stubControllerComponents()
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[RegistrationSubmissionDraft]](Future.successful(Right(None))))

      val request = FakeRequest("GET", "path")

      val result = controller.getLeadTrustee(draftId).apply(request)

      status(result) mustBe NOT_FOUND
    }
  }

}
