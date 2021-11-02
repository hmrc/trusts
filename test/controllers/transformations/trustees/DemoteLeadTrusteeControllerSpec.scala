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

package controllers.transformations.trustees

import controllers.actions.FakeIdentifierAction
import models.NameType
import models.variation._
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services.dates.LocalDateService
import services.{TaxableMigrationService, TransformationService}
import transformers.trustees.PromoteTrusteeTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class DemoteLeadTrusteeControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"
  private val endDate: LocalDate = LocalDate.parse("2021-01-01")

  private val invalidBody: JsValue = Json.parse("{}")

  private val mockTransformationService = mock[TransformationService]
  private val mockTaxableMigrationService = mock[TaxableMigrationService]
  private val mockLocalDateService = mock[LocalDateService]

  override def beforeEach(): Unit = {
    reset(mockTransformationService)

    when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
      .thenReturn(Future.successful(Json.obj()))

    when(mockTransformationService.addNewTransform(any(), any(), any()))
      .thenReturn(Future.successful(true))

    reset(mockTaxableMigrationService)

    when(mockTaxableMigrationService.migratingFromNonTaxableToTaxable(any(), any()))
      .thenReturn(Future.successful(false))

    reset(mockLocalDateService)

    when(mockLocalDateService.now).thenReturn(endDate)
  }
  
  "Demote lead trustee controller" - {

    "individual trustee" - {

      val trustee = LeadTrusteeIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("Joe", None, "Bloggs"),
        dateOfBirth = LocalDate.parse("1996-02-03"),
        phoneNumber = "1234567",
        identification = IdentificationType(Some("AA000000A"), None, None, None),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        entityStart = LocalDate.parse("2012-03-14"),
        entityEnd = None
      )

      val trusteeType: String = "leadTrusteeInd"

      "must add a new transform" in {

        val controller = new DemoteLeadTrusteeController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(trustee))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.demote(utr).apply(request)

        status(result) mustBe OK

        val transform = PromoteTrusteeTransform(None, Json.toJson(trustee), Json.obj(), endDate, trusteeType, isTaxable = true)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val controller = new DemoteLeadTrusteeController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.demote(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "business trustee" - {

      val trustee = LeadTrusteeOrgType(
        lineNo = None,
        bpMatchStatus = None,
        name = "Name",
        phoneNumber = "1234567",
        email = None,
        identification = IdentificationOrgType(Some("1234567890"), None, None),
        countryOfResidence = None,
        entityStart = LocalDate.parse("2012-03-14"),
        entityEnd = None
      )

      val trusteeType: String = "leadTrusteeOrg"

      "must add a new add transform" in {

        val controller = new DemoteLeadTrusteeController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(trustee))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.demote(utr).apply(request)

        status(result) mustBe OK

        val transform = PromoteTrusteeTransform(None, Json.toJson(trustee), Json.obj(), endDate, trusteeType, isTaxable = true)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val controller = new DemoteLeadTrusteeController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.demote(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}
