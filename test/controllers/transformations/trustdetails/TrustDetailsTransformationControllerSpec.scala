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

package controllers.transformations.trustdetails

import controllers.actions.FakeIdentifierAction
import models.variation.{MigratingTrustDetails, NonMigratingTrustDetails}
import models.{NonUKType, ResidentialStatusType, UkType}
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsBoolean, JsString, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.{TaxableMigrationService, TransformationService}
import transformers.trustdetails.{SetTrustDetailTransform, SetTrustDetailsTransform}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class TrustDetailsTransformationControllerSpec extends AnyFreeSpec
  with MockitoSugar
  with ScalaFutures
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"

  private val mockTransformationService = mock[TransformationService]
  private val mockTaxableMigrationService = mock[TaxableMigrationService]

  override def beforeEach(): Unit = {
    reset(mockTransformationService)

    when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
      .thenReturn(Future.successful(Json.obj()))

    when(mockTransformationService.addNewTransform(any(), any(), any()))
      .thenReturn(Future.successful(true))

    reset(mockTaxableMigrationService)

    when(mockTaxableMigrationService.migratingFromNonTaxableToTaxable(any(), any()))
      .thenReturn(Future.successful(false))
  }

  "Trust details transforms" - {

    "when setting express question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setExpress(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "expressTrust"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setExpress(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting property question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setProperty(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustUKProperty"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setProperty(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting recorded question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setRecorded(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustRecorded"))
        )
      }

      "return a BadRequest for malformed json" in {
        
        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setRecorded(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting resident question" - {

      "must return an OK" in {
        
        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResident(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustUKResident"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResident(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting taxable question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTaxable(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustTaxable"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTaxable(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting uk relation question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(JsBoolean(true))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setUKRelation(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          equalTo("id"),
          equalTo(SetTrustDetailTransform(JsBoolean(true), "trustUKRelation"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setUKRelation(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting law country question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val body = JsString("FR")

        val request = FakeRequest(PUT, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setLawCountry(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "lawCountry"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setLawCountry(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting administration country question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val body = JsString("FR")

        val request = FakeRequest(PUT, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setAdministrationCountry(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "administrationCountry"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setAdministrationCountry(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting type of trust question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val body = JsString("Employment Related")

        val request = FakeRequest(PUT, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTypeOfTrust(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "typeOfTrust"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setTypeOfTrust(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting deed of variation question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val body = JsString("Replaced the will trust")

        val request = FakeRequest(PUT, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setDeedOfVariation(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "deedOfVariation"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setDeedOfVariation(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting inter vivos question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val body = JsBoolean(true)

        val request = FakeRequest(PUT, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setInterVivos(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "interVivos"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setInterVivos(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting EFRBS start date question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val body = Json.toJson(LocalDate.parse("2021-01-01"))

        val request = FakeRequest(PUT, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setEfrbsStartDate(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "efrbsStartDate"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setEfrbsStartDate(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    "when setting residential status question" - {

      "must return an OK" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val body = Json.toJson(ResidentialStatusType(None, Some(NonUKType(sch5atcgga92 = true, None, None, None))))

        val request = FakeRequest(PUT, "path")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResidentialStatus(utr).apply(request)

        status(result) mustBe OK

        verify(mockTransformationService).addNewTransform(
          equalTo(utr),
          any(),
          equalTo(SetTrustDetailTransform(body, "residentialStatus"))
        )
      }

      "return a BadRequest for malformed json" in {

        val controller = new TrustDetailsTransformationController(
          identifierAction,
          mockTransformationService,
          mockTaxableMigrationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(PUT, "path")
          .withBody(Json.parse(
            """
              |{
              |  "uk": {
              |    "foo": "bar"
              |  }
              |}
              |""".stripMargin
          ))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.setResidentialStatus(utr).apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }
    
    "when setting trust details" - {
      
      "when migrating from non-taxable to taxable" - {

        "must return an OK" in {

          when(mockTaxableMigrationService.migratingFromNonTaxableToTaxable(any(), any()))
            .thenReturn(Future.successful(true))

          val controller = new TrustDetailsTransformationController(
            identifierAction,
            mockTransformationService,
            mockTaxableMigrationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val body = Json.toJson(MigratingTrustDetails(
            lawCountry = None,
            administrationCountry = "GB",
            residentialStatus = ResidentialStatusType(
              uk = Some(UkType(
                scottishLaw = true,
                preOffShore = None
              )),
              nonUK = None
            ),
            trustUKProperty = true,
            trustRecorded = true,
            trustUKRelation = None,
            trustUKResident = true,
            typeOfTrust = "Will Trust or Intestacy Trust",
            deedOfVariation = None,
            interVivos = None,
            efrbsStartDate = None
          ))

          val request = FakeRequest(PUT, "path")
            .withBody(body)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.setMigratingTrustDetails(utr).apply(request)

          status(result) mustBe OK

          verify(mockTransformationService).addNewTransform(
            equalTo(utr),
            any(),
            equalTo(SetTrustDetailsTransform(body, migratingFromNonTaxableToTaxable = true))
          )
        }

        "must return a BadRequest for malformed json" in {

          val controller = new TrustDetailsTransformationController(
            identifierAction,
            mockTransformationService,
            mockTaxableMigrationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(PUT, "path")
            .withBody(Json.parse("{}"))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.setMigratingTrustDetails(utr).apply(request)

          status(result) mustBe BAD_REQUEST
        }
        
      }
      
      "when not migrating" - {

        "must return an OK" in {

          when(mockTaxableMigrationService.migratingFromNonTaxableToTaxable(any(), any()))
            .thenReturn(Future.successful(false))

          val controller = new TrustDetailsTransformationController(
            identifierAction,
            mockTransformationService,
            mockTaxableMigrationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val body = Json.toJson(NonMigratingTrustDetails(trustUKProperty = true, trustRecorded = true, None, trustUKResident = true))

          val request = FakeRequest(PUT, "path")
            .withBody(body)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.setNonMigratingTrustDetails(utr).apply(request)

          status(result) mustBe OK

          verify(mockTransformationService).addNewTransform(
            equalTo(utr),
            any(),
            equalTo(SetTrustDetailsTransform(body, migratingFromNonTaxableToTaxable = false))
          )
        }

        "must return a BadRequest for malformed json" in {

          val controller = new TrustDetailsTransformationController(
            identifierAction,
            mockTransformationService,
            mockTaxableMigrationService
          )(Implicits.global, Helpers.stubControllerComponents())

          val request = FakeRequest(PUT, "path")
            .withBody(Json.parse("{}"))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.setNonMigratingTrustDetails(utr).apply(request)

          status(result) mustBe BAD_REQUEST
        }
        
      }
    }
  }
}
