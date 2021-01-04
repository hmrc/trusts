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
import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import controllers.actions.FakeIdentifierAction
import exceptions._
import models.auditing.TrustAuditing
import models.{DeclarationForApi, DeclarationName, NameType}
import services._

import scala.concurrent.Future

class TrustVariationsControllerSpec extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach  with IntegrationPatience {

  private lazy val bodyParsers = Helpers.stubControllerComponents().parsers.default

  private lazy val mockAuditService: AuditService = mock[AuditService]

  private val mockVariationService = mock[VariationService]

  private val responseHandler = new VariationsResponseHandler(mockAuditService)

  override def beforeEach(): Unit = {
    reset(mockAuditService)
  }

  private def trustVariationsController = {
    new TrustVariationsController(
      new FakeIdentifierAction(bodyParsers, Organisation),
      mockAuditService,
      mockVariationService,
      responseHandler,
      Helpers.stubControllerComponents()
    )
  }

  ".declare" should {
    "Return bad request when declaring No change and there is a form bundle number mismatch" in {
      val SUT = trustVariationsController
      val declaration = DeclarationName(
        NameType("firstname", None, "Surname")
      )

      val declarationForApi = DeclarationForApi(declaration, None, None)

      when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
        .thenReturn(Future.failed(EtmpCacheDataStaleException))

      val result = SUT.declare("aUTR")(
        FakeRequest("POST", "/no-change/aUTR").withBody(Json.toJson(declarationForApi))
      )

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "code" -> "ETMP_DATA_STALE",
        "message" -> "ETMP returned a changed form bundle number for the trust."
      )

      verify(mockAuditService).auditErrorResponse(
        Meq(TrustAuditing.TRUST_VARIATION_SUBMISSION_FAILED),
        any(),
        Meq("id"),
        Meq("Cached ETMP data stale.")
      )(any())
    }
  }
}

