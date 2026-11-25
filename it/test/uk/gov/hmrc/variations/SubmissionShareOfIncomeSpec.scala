/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.variations

import cats.data.EitherT
import connector.DesTrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse, ResponseHeader}
import models.variation.{DeclarationForApi, VariationSuccessResponse}
import models.{DeclarationName, NameType}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.CacheRepository
import services.dates.LocalDateService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.Future

class SubmissionShareOfIncomeSpec extends IntegrationTestBase {

  private val utr = "1000000120"

  "submitting a declaration" should {

    "strip leading zeros from beneficiaryShareOfIncome on beneficiaries the user did not change" in {

      val stubbedTrustsConnector = mock[DesTrustsConnector]
      val stubbedCacheRepository = mock[CacheRepository]

      val declaration = DeclarationForApi(DeclarationName(NameType("First", None, "Last")), None, None)

      val trustResponse: GetTrustSuccessResponse = new GetTrustSuccessResponse {
        val responseHeader: ResponseHeader = ResponseHeader("Processed", "123456789012")
      }

      val cachedEtmpResponse =
        JsonUtils.getJsonValueFromFile("5MLD/Taxable/des/invalid-get-trust-leading-zeroes-share-of-income.json")
      val expectedVariation  =
        JsonUtils.getJsonValueFromFile("5MLD/Taxable/des/variation-leading-zeroes-share-of-income-removed.json")

      val stubbedLocalDateService = mock[LocalDateService]
      when(stubbedLocalDateService.now).thenReturn(LocalDate.of(2020, 1, 30))

      when(stubbedCacheRepository.get(eqTo(utr), any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, Option[JsValue]](Future.successful(Right(Some(cachedEtmpResponse)))))

      when(stubbedTrustsConnector.getTrustInfo(eqTo(utr)))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(trustResponse))))

      when(stubbedTrustsConnector.trustVariation(any()))
        .thenReturn(
          EitherT[Future, TrustErrors, VariationSuccessResponse](
            Future.successful(Right(VariationSuccessResponse("tvn")))
          )
        )

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction]
            .toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
          bind[DesTrustsConnector].toInstance(stubbedTrustsConnector),
          bind[CacheRepository].toInstance(stubbedCacheRepository),
          bind[LocalDateService].toInstance(stubbedLocalDateService)
        )
        .build()

      assertMongoTest(application) { app =>
        val variationRequest = FakeRequest(POST, s"/trusts/declare/$utr")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(Json.toJson(declaration))

        val result = route(app, variationRequest).get
        status(result) mustBe OK

        val payloadCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
        verify(stubbedTrustsConnector).trustVariation(payloadCaptor.capture())

        payloadCaptor.getValue mustBe expectedVariation
      }
    }
  }

}
