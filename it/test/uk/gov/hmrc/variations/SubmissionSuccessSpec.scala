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
import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse, ResponseHeader}
import models.variation.{DeclarationForApi, VariationSuccessResponse}
import models.{DeclarationName, NameType}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers._
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.CacheRepository
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.NonTaxable5MLDFixtures.getJsonFromFile

import scala.concurrent.Future

class SubmissionSuccessSpec extends IntegrationTestBase {

  val utr        = "5174384721"
  val internalId = "internalId"

  "submit a successful variation" in {

    val stubbedTrustsConnector = mock[TrustsConnector]
    val stubbedCacheRepository = mock[CacheRepository]

    val declaration = DeclarationForApi(
      DeclarationName(NameType("First", None, "Last")),
      None,
      None
    )

    val trustResponse: GetTrustSuccessResponse = new GetTrustSuccessResponse {
      val responseHeader: ResponseHeader = ResponseHeader("Processed", "123456789012")
    }

    lazy val get5MLDTrustNonTaxableResponse: String =
      getJsonFromFile("5MLD/NonTaxable/des/valid-get-trust-5mld-non-taxable-des-response.json")

    when(stubbedCacheRepository.get(eqTo(utr), any(), any()))
      .thenReturn(
        EitherT[Future, TrustErrors, Option[JsValue]](
          Future.successful(Right(Some(Json.parse(get5MLDTrustNonTaxableResponse))))
        )
      )

    when(stubbedTrustsConnector.getTrustInfo(eqTo(utr)))
      .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(trustResponse))))

    when(stubbedTrustsConnector.trustVariation(any()))
      .thenReturn(
        EitherT[Future, TrustErrors, VariationSuccessResponse](
          Future.successful(Right(VariationSuccessResponse("tvn")))
        )
      )

    def application = applicationBuilder
      .overrides(
        bind[IdentifierAction]
          .toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(stubbedTrustsConnector),
        bind[CacheRepository].toInstance(stubbedCacheRepository)
      )
      .build()

    val variationRequest = FakeRequest(POST, s"/trusts/declare/$utr")
      .withHeaders(CONTENT_TYPE -> "application/json")
      .withBody(Json.toJson(declaration))

    val result = route(application, variationRequest).get
    status(result) mustBe OK

  }

}
