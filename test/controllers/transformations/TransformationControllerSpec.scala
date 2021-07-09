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

package controllers.transformations

import base.BaseSpec
import controllers.actions.FakeIdentifierAction
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.must.Matchers._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

import scala.concurrent.Future

class TransformationControllerSpec extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach  with IntegrationPatience {

  private lazy val bodyParsers = Helpers.stubControllerComponents().parsers.default

  private val mockTransformationService = mock[TransformationService]

  private val identifier: String = "utr"

  private def transformationController = {
    new TransformationController(
      new FakeIdentifierAction(bodyParsers, Organisation),
      mockTransformationService,
      Helpers.stubControllerComponents()
    )
  }

  override def beforeEach(): Unit = {
    reset(mockTransformationService)
  }

  "TransformationController" when {

    ".removeTransforms" should {

      "return OK" when {
        "successfully removed transforms" in {
          when(mockTransformationService.removeAllTransformations(any(), any()))
            .thenReturn(Future.successful(None))

          val request = FakeRequest(DELETE, "path")

          val result = transformationController.removeTransforms(identifier).apply(request)
          status(result) mustBe OK

          verify(mockTransformationService).removeAllTransformations(identifier, "id")
        }
      }

      "return INTERNAL_SERVER_ERROR" when {
        "failed to remove transforms" in {
          when(mockTransformationService.removeAllTransformations(any(), any()))
            .thenReturn(Future.failed(new Throwable("repository reset failed")))

          val request = FakeRequest(DELETE, "path")

          val result = transformationController.removeTransforms(identifier).apply(request)
          status(result) mustBe INTERNAL_SERVER_ERROR

          verify(mockTransformationService).removeAllTransformations(identifier, "id")
        }
      }
    }

    ".removeTrustTypeDependentMigrationTransforms" should {

      "return OK" when {
        "successfully removed transforms" in {
          when(mockTransformationService.amendTrustTypeDependentMigrationTransforms(any(), any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(DELETE, "path")

          val result = transformationController.amendTrustTypeDependentMigrationTransforms(identifier).apply(request)
          status(result) mustBe OK

          verify(mockTransformationService).amendTrustTypeDependentMigrationTransforms(identifier, "id")
        }
      }

      "return INTERNAL_SERVER_ERROR" when {
        "failed to remove transforms" in {
          when(mockTransformationService.amendTrustTypeDependentMigrationTransforms(any(), any()))
            .thenReturn(Future.failed(new Throwable()))

          val request = FakeRequest(DELETE, "path")

          val result = transformationController.amendTrustTypeDependentMigrationTransforms(identifier).apply(request)
          status(result) mustBe INTERNAL_SERVER_ERROR

          verify(mockTransformationService).amendTrustTypeDependentMigrationTransforms(identifier, "id")
        }
      }
    }

    ".removeOptionalTrustDetailTransforms" should {

      "return OK" when {
        "successfully removed transforms" in {
          when(mockTransformationService.removeOptionalTrustDetailTransforms(any(), any()))
            .thenReturn(Future.successful(true))

          val request = FakeRequest(DELETE, "path")

          val result = transformationController.removeOptionalTrustDetailTransforms(identifier).apply(request)
          status(result) mustBe OK

          verify(mockTransformationService).removeOptionalTrustDetailTransforms(identifier, "id")
        }
      }

      "return INTERNAL_SERVER_ERROR" when {
        "failed to remove transforms" in {
          when(mockTransformationService.removeOptionalTrustDetailTransforms(any(), any()))
            .thenReturn(Future.failed(new Throwable()))

          val request = FakeRequest(DELETE, "path")

          val result = transformationController.removeOptionalTrustDetailTransforms(identifier).apply(request)
          status(result) mustBe INTERNAL_SERVER_ERROR

          verify(mockTransformationService).removeOptionalTrustDetailTransforms(identifier, "id")
        }
      }
    }
  }
}
