/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.controllers

import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.EstateFoundResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustFoundResponse
import uk.gov.hmrc.trusts.services.{AuditService, DesService, FakeAuditService}

import scala.concurrent.Future

class GetEstateControllerSpec extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach{

  lazy val desService: DesService = mock[DesService]
  lazy val mockedAuditService: AuditService = mock[AuditService]

  val mockAuditConnector = mock[AuditConnector]
  val mockConfig = mock[AppConfig]

  val auditService = new AuditService(mockAuditConnector, mockConfig)

  override def afterEach() =  {
    reset(mockedAuditService, mockAuditConnector, mockConfig)
  }

  private def getEstatesController = {
    val SUT = new GetEstateController(new FakeIdentifierAction(Organisation), mockedAuditService, desService)
    SUT
  }


  val invalidUTR = "1234567"
  val utr = "1234567890"

  ".get" should {

    "not perform auditing" when {
      "the feature toggle is set to false" in {

        when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(EstateFoundResponse(None, ResponseHeader("Parked", "1"))))

        when(mockConfig.auditingEnabled).thenReturn(false)

        val SUT = new GetEstateController(new FakeIdentifierAction(Organisation), auditService, desService)

        val result = SUT.get(utr).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditConnector, times(0)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "perform auditing" when {
      "the feature toggle is set to true" in {

        when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(EstateFoundResponse(None, ResponseHeader("Parked", "1"))))

        when(mockConfig.auditingEnabled).thenReturn(true)

        val SUT = new GetEstateController(new FakeIdentifierAction(Organisation), auditService, desService)

        val result = SUT.get(utr).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditConnector, times(1)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "return 200 - Ok" in {

      when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(EstateFoundResponse(None, ResponseHeader("Parked", "1"))))

      val result = getEstatesController.get(utr).apply(FakeRequest(GET, s"/estates/$utr"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetEstate"), any[JsValue], any[String], any[JsValue])(any())
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
      }
    }

    "return 400 - BadRequest" when {
      "the UTR given is invalid" in {

        val result = getEstatesController.get(invalidUTR).apply(FakeRequest(GET, s"/estates/$invalidUTR"))

        whenReady(result) { _ =>
          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "return 500 - InternalServerError" when {
      "the get endpoint returns a InvalidUTRResponse" in {


        when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(InvalidUTRResponse))

        val utr = "1234567890"
        val result = getEstatesController.get(utr).apply(FakeRequest(GET, s"/estates/$invalidUTR"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetEstate"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a InvalidRegimeResponse" in {


        when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(InvalidRegimeResponse))

        val utr = "1234567890"
        val result = getEstatesController.get(utr).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetEstate"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a BadRequestResponse" in {


        when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(BadRequestResponse))

        val utr = "1234567890"
        val result = getEstatesController.get(utr).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetEstate"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a ResourceNotFoundResponse" in {


        when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val utr = "1234567890"
        val result = getEstatesController.get(utr).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetEstate"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe NOT_FOUND
        }
      }

      "the get endpoint returns a InternalServerErrorResponse" in {


        when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val utr = "1234567890"
        val result = getEstatesController.get(utr).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetEstate"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a ServiceUnavailableResponse" in {


        when(desService.getEstateInfo(any())(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val utr = "1234567890"
        val result = getEstatesController.get(utr).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetEstate"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
