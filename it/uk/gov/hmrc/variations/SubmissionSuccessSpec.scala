package uk.gov.hmrc.variations

import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.{GetTrustSuccessResponse, ResponseHeader}
import models.variation.VariationResponse
import models.{DeclarationName, NameType}
import models.variation.DeclarationForApi
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.must.Matchers._
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import repositories.CacheRepository
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.NonTaxable5MLDFixtures.getJsonFromFile

import scala.concurrent.Future

class SubmissionSuccessSpec extends AsyncWordSpec with MockitoSugar with IntegrationTestBase {

  val utr = "5174384721"
  val internalId = "internalId"

  "submit a successful variation" in {

    val stubbedTrustsConnector = mock[TrustsConnector]
    val stubbedCacheRepository = mock[CacheRepository]

    val declaration = DeclarationForApi(
      DeclarationName(NameType("First", None, "Last")),
      None,
      None
    )

    val trustResponse: GetTrustSuccessResponse = new GetTrustSuccessResponse{
      val responseHeader: ResponseHeader = ResponseHeader("Processed", "123456789012")
    }

    lazy val get5MLDTrustNonTaxableResponse: String = getJsonFromFile("5MLD/NonTaxable/des/valid-get-trust-5mld-non-taxable-des-response.json")

    when(stubbedCacheRepository.get(eqTo(utr), any(), any()))
      .thenReturn(Future.successful(Some(Json.parse(get5MLDTrustNonTaxableResponse))))

    when(stubbedTrustsConnector.getTrustInfo(eqTo(utr)))
      .thenReturn(Future.successful(trustResponse))

    when(stubbedTrustsConnector.trustVariation(any()))
      .thenReturn(Future.successful(VariationResponse("tvn")))

    lazy val application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
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
