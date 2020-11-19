package uk.gov.hmrc.variations

import connector.{DesConnector, TrustsStoreConnector}
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.{GetTrustSuccessResponse, ResponseHeader}
import models.variation.VariationResponse
import models.{DeclarationForApi, DeclarationName, FeatureResponse, NameType}
import org.scalatest.{AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers.{any, eq => eqTo}
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import repositories.CacheRepository
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.NonTaxable5MLDFixtures.getJsonFromFile

import scala.concurrent.Future

class SuccessfulSubmissionSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  val utr = "5174384721"
  val internalId = "internalId"

  "submit a successful variation" in {

    val stubbedDesConnector = mock[DesConnector]
    val stubbedCacheRepository = mock[CacheRepository]
    val stubbedTrustStoreConnector = mock[TrustsStoreConnector]

    val declaration = DeclarationForApi(
      DeclarationName(NameType("First", None, "Last")),
      None,
      None
    )

    val trustResponse: GetTrustSuccessResponse = new GetTrustSuccessResponse{
      val responseHeader = ResponseHeader("Processed", "123456789012")
    }

    lazy val get5MLDTrustNonTaxableResponse: String = getJsonFromFile("5MLD/NonTaxable/des/valid-get-trust-5mld-non-taxable-des-response.json")

    when(stubbedCacheRepository.get(eqTo(utr), any()))
      .thenReturn(Future.successful(Some(Json.parse(get5MLDTrustNonTaxableResponse))))

    when(stubbedTrustStoreConnector.getFeature(any())(any(), any()))
      .thenReturn(Future.successful(FeatureResponse("5mld", true)))

    when(stubbedDesConnector.getTrustInfo(eqTo(utr)))
      .thenReturn(Future.successful(trustResponse))

    when(stubbedDesConnector.trustVariation(any()))
      .thenReturn(Future.successful(VariationResponse("tvn")))

    lazy val application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[DesConnector].toInstance(stubbedDesConnector),
        bind[TrustsStoreConnector].toInstance(stubbedTrustStoreConnector),
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
