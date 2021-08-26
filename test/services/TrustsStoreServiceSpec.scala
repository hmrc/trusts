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

package services

import base.BaseSpec
import connector.TrustsStoreConnector
import models.FeatureResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class TrustsStoreServiceSpec extends BaseSpec {

  private val mockTrustsStoreConnector: TrustsStoreConnector = mock[TrustsStoreConnector]
  private val service = new TrustsStoreService(mockTrustsStoreConnector)

  "Trusts Store Service" when {

    ".is5mldEnabled" must {

      val feature: String = "5mld"

      "return true when feature is enabled" in {

        when(mockTrustsStoreConnector.getFeature(any())(any(), any())).thenReturn(Future.successful(FeatureResponse(feature, isEnabled = true)))

        val resultF = service.is5mldEnabled()

        whenReady(resultF) {
          _ mustBe true
        }
      }

      "return false when feature is disabled" in {

        when(mockTrustsStoreConnector.getFeature(any())(any(), any())).thenReturn(Future.successful(FeatureResponse(feature, isEnabled = false)))

        val resultF = service.is5mldEnabled()

        whenReady(resultF) {
          _ mustBe false
        }
      }
    }
  }
}
