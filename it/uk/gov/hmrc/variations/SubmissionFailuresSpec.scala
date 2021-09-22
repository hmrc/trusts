package uk.gov.hmrc.variations

import connectors.ConnectorSpecHelper
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.auditing.TrustAuditing
import models.variation.DeclarationForApi
import models.{DeclarationName, NameType}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers._
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.CacheRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

import scala.concurrent.Future

class SubmissionFailuresSpec extends ConnectorSpecHelper  {

  val utr = "5174384721"
  val internalId = "internalId"

  "submit a successful variation" in {

    val stubbedCacheRepository = mock[CacheRepository]
    val stubbedAuditService = mock[AuditService]

    val declaration = DeclarationForApi(
      DeclarationName(NameType("First", None, "Last")),
      None,
      None
    )

    lazy val get5MLDTrustNonTaxableResponse: String = getJsonFromFile("5MLD/NonTaxable/des/valid-get-trust-5mld-non-taxable-des-response.json")

    when(stubbedCacheRepository.get(eqTo(utr), any()))
      .thenReturn(Future.successful(Some(Json.parse(get5MLDTrustNonTaxableResponse))))

    stubForGet(server, s"/trusts/registration/UTR/$utr", INTERNAL_SERVER_ERROR, "")

    lazy val application = applicationBuilder()
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[AuditService].toInstance(stubbedAuditService),
        bind[CacheRepository].toInstance(stubbedCacheRepository)
      )
      .build()

    val variationRequest = FakeRequest(POST, s"/trusts/declare/$utr")
      .withHeaders(CONTENT_TYPE -> "application/json")
      .withBody(Json.toJson(declaration))

    val result = route(application, variationRequest).get
    status(result) mustBe INTERNAL_SERVER_ERROR

    verify(stubbedAuditService)
      .auditErrorResponse(eqTo(TrustAuditing.TRUST_VARIATION_SUBMISSION_FAILED), any(), any(), any())(any())

  }


}
