/*
 * Copyright 2022 HM Revenue & Customs
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

import base.BaseSpec
import controllers.actions.FakeIdentifierAction
import models.taxable_migration.TaxableMigrationFlag
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.must.Matchers._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.libs.json.{JsBoolean, JsNull, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import utils.Session

import scala.concurrent.Future

class TaxableMigrationControllerSpec extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach with IntegrationPatience {

  private lazy val bodyParsers = Helpers.stubControllerComponents().parsers.default

  private val mockTaxableMigrationService = mock[TaxableMigrationService]

  private val identifier: String = "utr"
  private val sessionId: String = Session.id(hc)

  private def taxableMigrationController = {
    new TaxableMigrationController(
      new FakeIdentifierAction(bodyParsers, Organisation),
      mockTaxableMigrationService,
      Helpers.stubControllerComponents()
    )
  }

  override def beforeEach(): Unit = {
    reset(mockTaxableMigrationService)
  }

  ".getTaxableMigrationFlag" should {

    "return OK" when {
      "taxable migration defined" in {
        when(mockTaxableMigrationService.getTaxableMigrationFlag(any(), any(), any()))
          .thenReturn(Future.successful(Some(true)))

        val request = FakeRequest(GET, "path")

        val result = taxableMigrationController.getTaxableMigrationFlag(identifier).apply(request)
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(TaxableMigrationFlag(Some(true)))

        verify(mockTaxableMigrationService).getTaxableMigrationFlag(identifier, "id", sessionId)
      }

      "taxable migration flag undefined" in {
        when(mockTaxableMigrationService.getTaxableMigrationFlag(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, "path")

        val result = taxableMigrationController.getTaxableMigrationFlag(identifier).apply(request)
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(TaxableMigrationFlag(None))

        verify(mockTaxableMigrationService).getTaxableMigrationFlag(identifier, "id", sessionId)
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "repository get fails" in {
        when(mockTaxableMigrationService.getTaxableMigrationFlag(any(), any(), any()))
          .thenReturn(Future.failed(new Throwable("repository get failed")))

        val request = FakeRequest(GET, "path")

        val result = taxableMigrationController.getTaxableMigrationFlag(identifier).apply(request)
        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockTaxableMigrationService).getTaxableMigrationFlag(identifier, "id", sessionId)
      }
    }
  }

  ".setTaxableMigrationFlag" should {
    "return OK" when {

      "setting to true" in {
        when(mockTaxableMigrationService.setTaxableMigrationFlag(any(), any(), any(), any()))
          .thenReturn(Future.successful(true))

        val result = taxableMigrationController.setTaxableMigrationFlag(identifier)
          .apply(postRequestWithPayload(JsBoolean(true)))

        status(result) mustBe OK

        verify(mockTaxableMigrationService).setTaxableMigrationFlag(identifier, "id", sessionId, migratingToTaxable = true)
      }

      "setting to false" in {
        when(mockTaxableMigrationService.setTaxableMigrationFlag(any(), any(), any(), any()))
          .thenReturn(Future.successful(true))

        val result = taxableMigrationController.setTaxableMigrationFlag(identifier)
          .apply(postRequestWithPayload(JsBoolean(false)))

        status(result) mustBe OK

        verify(mockTaxableMigrationService).setTaxableMigrationFlag(identifier, "id", sessionId, migratingToTaxable = false)
      }
    }

    "return BAD_REQUEST" when {
      "request body cannot be validated" in {
        val result = taxableMigrationController.setTaxableMigrationFlag(identifier)
          .apply(postRequestWithPayload(JsNull))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "repository set fails" in {
        when(mockTaxableMigrationService.setTaxableMigrationFlag(any(), any(), any(), any()))
          .thenReturn(Future.failed(new Throwable("repository set failed")))

        val result = taxableMigrationController.setTaxableMigrationFlag(identifier)
          .apply(postRequestWithPayload(JsBoolean(true)))

        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockTaxableMigrationService).setTaxableMigrationFlag(identifier, "id", sessionId, migratingToTaxable = true)
      }
    }
  }
}
