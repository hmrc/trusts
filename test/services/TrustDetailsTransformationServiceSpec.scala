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

import models.get_trust.{ResponseHeader, TrustProcessedResponse}
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsBoolean
import transformers.trustdetails._
import utils.{JsonFixtures, JsonUtils}

import scala.concurrent.Future

class TrustDetailsTransformationServiceSpec extends FreeSpec
  with MockitoSugar
  with ScalaFutures
  with MustMatchers
  with JsonFixtures {

  "the trust details transformation service" - {

    "must add a new transform" - {

      "for express" in {

        val transformationService = mock[TransformationService]
        val service = new TrustDetailsTransformationService(transformationService)

        val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        when(transformationService.getTransformedData(any(), any())(any())).thenReturn {
          Future.successful {
            TrustProcessedResponse(desResponse, ResponseHeader("status", "formBundlNo"))
          }
        }

        val transform = SetTrustDetailTransform(JsBoolean(true), "expressTrust")

        val result = service.set("utr", "internalId", transform)

        whenReady(result) { _ =>
          verify(transformationService).addNewTransform("utr", "internalId", transform)
        }
      }

      "for property" in {

        val transformationService = mock[TransformationService]
        val service = new TrustDetailsTransformationService(transformationService)

        val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        when(transformationService.getTransformedData(any(), any())(any())).thenReturn {
          Future.successful {
            TrustProcessedResponse(desResponse, ResponseHeader("status", "formBundlNo"))
          }
        }

        val transform = SetTrustDetailTransform(JsBoolean(true), "trustUKProperty")

        val result = service.set("utr", "internalId", transform)

        whenReady(result) { _ =>
          verify(transformationService).addNewTransform("utr", "internalId", transform)
        }
      }

      "for recorded" in {

        val transformationService = mock[TransformationService]
        val service = new TrustDetailsTransformationService(transformationService)

        val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        when(transformationService.getTransformedData(any(), any())(any())).thenReturn {
          Future.successful {
            TrustProcessedResponse(desResponse, ResponseHeader("status", "formBundlNo"))
          }
        }

        val transform = SetTrustDetailTransform(JsBoolean(true), "trustRecorded")

        val result = service.set("utr", "internalId", transform)

        whenReady(result) { _ =>
          verify(transformationService).addNewTransform("utr", "internalId", transform)
        }
      }

      "for resident" in {

        val transformationService = mock[TransformationService]
        val service = new TrustDetailsTransformationService(transformationService)

        val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        when(transformationService.getTransformedData(any(), any())(any())).thenReturn {
          Future.successful {
            TrustProcessedResponse(desResponse, ResponseHeader("status", "formBundlNo"))
          }
        }

        val transform = SetTrustDetailTransform(JsBoolean(true), "trustUKResident")

        val result = service.set("utr", "internalId", transform)

        whenReady(result) { _ =>
          verify(transformationService).addNewTransform("utr", "internalId", transform)
        }
      }

      "for taxable" in {

        val transformationService = mock[TransformationService]
        val service = new TrustDetailsTransformationService(transformationService)

        val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        when(transformationService.getTransformedData(any(), any())(any())).thenReturn {
          Future.successful {
            TrustProcessedResponse(desResponse, ResponseHeader("status", "formBundlNo"))
          }
        }

        val transform = SetTrustDetailTransform(JsBoolean(true), "trustTaxable")

        val result = service.set("utr", "internalId", transform)

        whenReady(result) { _ =>
          verify(transformationService).addNewTransform("utr", "internalId", transform)
        }
      }

      "for uk relation" in {

        val transformationService = mock[TransformationService]
        val service = new TrustDetailsTransformationService(transformationService)

        val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        when(transformationService.getTransformedData(any(), any())(any())).thenReturn {
          Future.successful {
            TrustProcessedResponse(desResponse, ResponseHeader("status", "formBundlNo"))
          }
        }

        val transform = SetTrustDetailTransform(JsBoolean(true), "trustUKRelation")

        val result = service.set("utr", "internalId", transform)

        whenReady(result) { _ =>
          verify(transformationService).addNewTransform("utr", "internalId", transform)
        }
      }
    }
  }
}
