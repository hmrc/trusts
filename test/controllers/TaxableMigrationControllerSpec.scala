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

import base.BaseSpec
import controllers.actions.FakeIdentifierAction
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.libs.json.{JsBoolean, JsNull}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

import scala.concurrent.Future

class TaxableMigrationControllerSpec extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach  with IntegrationPatience {

  private lazy val bodyParsers = Helpers.stubControllerComponents().parsers.default

  private val mockTaxableMigrationService = mock[TaxableMigrationService]

  private val mockTransformationService = mock[TransformationService]

  private val identifier: String = "utr"

  private def taxableMigrationController = {
    new TaxableMigrationController(
      new FakeIdentifierAction(bodyParsers, Organisation),
      mockTaxableMigrationService,
      mockTransformationService,
      Helpers.stubControllerComponents()
    )
  }

  override def beforeEach(): Unit = {
    reset(mockTransformationService)
    reset(mockTaxableMigrationService)
  }

  ".getTaxableMigrationFlag" should {
    "get taxable migration flag" in {

      when(mockTaxableMigrationService.getTaxableMigrationFlag(any(), any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest(GET, "path")

      val result = taxableMigrationController.getTaxableMigrationFlag(identifier).apply(request)
      status(result) mustBe OK

      verify(mockTaxableMigrationService).getTaxableMigrationFlag(identifier, "id")
    }
  }

  ".setTaxableMigrationFlag" should {
    "return OK" when {

      "setting to true" in {
        when(mockTaxableMigrationService.setTaxableMigrationFlag(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val result = taxableMigrationController.setTaxableMigrationFlag(identifier)
          .apply(postRequestWithPayload(JsBoolean(true)))

        status(result) mustBe OK

        verify(mockTaxableMigrationService).setTaxableMigrationFlag(identifier, "id", migratingToTaxable = true)
      }

      "setting to false" in {
        when(mockTaxableMigrationService.setTaxableMigrationFlag(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val result = taxableMigrationController.setTaxableMigrationFlag(identifier)
          .apply(postRequestWithPayload(JsBoolean(false)))

        status(result) mustBe OK

        verify(mockTaxableMigrationService).setTaxableMigrationFlag(identifier, "id", migratingToTaxable = false)
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "request body cannot be validated" in {
        val result = taxableMigrationController.setTaxableMigrationFlag(identifier)
          .apply(postRequestWithPayload(JsNull))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".removeTransforms" should {
    "remove all transforms" in {

      when(mockTransformationService.removeAllTransformations(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(DELETE, "path")

      val result = taxableMigrationController.removeTransforms(identifier).apply(request)
      status(result) mustBe OK

      verify(mockTransformationService).removeAllTransformations(identifier, "id")
    }
  }
}
