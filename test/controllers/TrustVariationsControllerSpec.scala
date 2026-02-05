/*
 * Copyright 2024 HM Revenue & Customs
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
import cats.data.EitherT
import controllers.actions.FakeIdentifierAction
import errors.{
  EtmpCacheDataStaleErrorResponse, ServerError, ServiceNotAvailableErrorResponse, TrustErrors, VariationFailureForAudit
}
import models.auditing.TrustAuditing
import models.nonRepudiation.NRSResponse
import models.variation.{DeclarationForApi, VariationContext, VariationSuccessResponse}
import models.{DeclarationName, NameType}
import org.mockito.ArgumentMatchers.{eq => Meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.must.Matchers._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services._
import services.auditing.AuditService
import services.nonRepudiation.NonRepudiationService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrustVariationsControllerSpec
    extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach with IntegrationPatience {

  private lazy val bodyParsers = Helpers.stubControllerComponents().parsers.default

  private lazy val mockAuditService: AuditService = mock[AuditService]

  private val mockVariationService = mock[VariationService]

  private val mockNonRepudiationService = mock[NonRepudiationService]

  private val responseHandler = new VariationsResponseHandler(mockAuditService)

  override def beforeEach(): Unit =
    reset(mockAuditService)

  private def trustVariationsController =
    new TrustVariationsController(
      new FakeIdentifierAction(bodyParsers, Organisation),
      mockAuditService,
      mockVariationService,
      responseHandler,
      mockNonRepudiationService,
      Helpers.stubControllerComponents(),
      appConfig
    )

  ".declare" should {

    "submit a trust closure" in {
      val SUT         = trustVariationsController
      val declaration = DeclarationName(
        NameType("firstname", None, "Surname")
      )

      val declarationForApi =
        DeclarationForApi(declaration, None, endDate = Some(LocalDate.of(2021, 2, 5)))

      when(mockNonRepudiationService.maintain(any(), any())(any(), any()))
        .thenReturn(Future.successful(NRSResponse.Success("uuid")))

      when(mockVariationService.submitDeclaration(any(), any(), any(), any())(any()))
        .thenReturn(
          EitherT[Future, TrustErrors, VariationContext](
            Future.successful(
              Right(VariationContext(Json.obj(), VariationSuccessResponse("TVN123")))
            )
          )
        )

      val result = SUT.declare("aUTR")(
        FakeRequest("POST", "/no-change/aUTR").withBody(Json.toJson(declarationForApi))
      )

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.obj(
        "tvn" -> "TVN123"
      )
    }

    "return Bad Request when declaring no change and there is a form bundle number mismatch" in {
      val SUT         = trustVariationsController
      val declaration = DeclarationName(
        NameType("firstname", None, "Surname")
      )

      val declarationForApi = DeclarationForApi(declaration, None, None)

      when(mockVariationService.submitDeclaration(any(), any(), any(), any())(any()))
        .thenReturn(
          EitherT[Future, TrustErrors, VariationContext](
            Future.successful(
              Left(VariationFailureForAudit(EtmpCacheDataStaleErrorResponse, "Etmp data is stale"))
            )
          )
        )

      val result = SUT.declare("aUTR")(
        FakeRequest("POST", "/no-change/aUTR").withBody(Json.toJson(declarationForApi))
      )

      status(result)        mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "code"    -> "ETMP_DATA_STALE",
        "message" -> "ETMP returned a changed form bundle number for the trust."
      )

      verify(mockAuditService).auditErrorResponse(
        Meq(TrustAuditing.TRUST_VARIATION_SUBMISSION_FAILED),
        any(),
        Meq("id"),
        Meq("Cached ETMP data stale.")
      )(any())
    }

    "return Bad Request when JSON request body is invalid" in {
      val SUT = trustVariationsController

      val invalidJson =
        """
          |{
          |  "declaration": {
          |    "name": {
          |      "firstName": "firstname",
          |      "thisJsonIsIncorrect": "Surname"
          |    }
          |  }
          |}
          |""".stripMargin

      val result = SUT.declare("aUTR")(
        FakeRequest("POST", "/no-change/aUTR").withBody(Json.parse(invalidJson))
      )

      status(result) mustBe BAD_REQUEST
    }

    "return Service Unavailable when des dependent service is down" in {
      val SUT         = trustVariationsController
      val declaration = DeclarationName(
        NameType("firstname", None, "Surname")
      )

      val declarationForApi =
        DeclarationForApi(declaration, None, endDate = Some(LocalDate.of(2021, 2, 5)))

      when(mockVariationService.submitDeclaration(any(), any(), any(), any())(any()))
        .thenReturn(
          EitherT[Future, TrustErrors, VariationContext](
            Future.successful(
              Left(VariationFailureForAudit(ServiceNotAvailableErrorResponse, "Service unavailable."))
            )
          )
        )

      val result = SUT.declare("aUTR")(
        FakeRequest("POST", "/no-change/aUTR").withBody(Json.toJson(declarationForApi))
      )

      status(result)        mustBe SERVICE_UNAVAILABLE
      contentAsJson(result) mustBe Json.obj(
        "code"    -> "SERVICE_UNAVAILABLE",
        "message" -> "Service unavailable."
      )

      verify(mockAuditService).auditErrorResponse(
        Meq(TrustAuditing.TRUST_VARIATION_SUBMISSION_FAILED),
        any(),
        Meq("id"),
        Meq("Service unavailable.")
      )(any())
    }

    "return an Internal Server Error when submitDeclaration returns Left(ServerError()) " in {
      val SUT         = trustVariationsController
      val declaration = DeclarationName(
        NameType("firstname", None, "Surname")
      )

      val declarationForApi =
        DeclarationForApi(declaration, None, endDate = Some(LocalDate.of(2021, 2, 5)))

      when(mockNonRepudiationService.maintain(any(), any())(any(), any()))
        .thenReturn(Future.successful(NRSResponse.Success("uuid")))

      when(mockVariationService.submitDeclaration(any(), any(), any(), any())(any()))
        .thenReturn(EitherT[Future, TrustErrors, VariationContext](Future.successful(Left(ServerError()))))

      val result = SUT.declare("aUTR")(
        FakeRequest("POST", "/no-change/aUTR").withBody(Json.toJson(declarationForApi))
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

}
