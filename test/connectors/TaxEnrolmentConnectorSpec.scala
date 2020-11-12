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

package connectors

import config.AppConfig
import connector.TaxEnrolmentConnector
import exceptions.{BadRequestException, InternalServerErrorException}
import models.tax_enrolments.{TaxEnrolmentSubscription, TaxEnrolmentSuccess, TaxEnrolmentSuscriberResponse}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import services.TrustsStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnectorSpec extends ConnectorSpecHelper {

  lazy val connector: TaxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]

  private val mockTrustsStoreService = mock[TrustsStoreService]
  private val mockConfig = mock[AppConfig]

  override def applicationBuilder(): GuiceApplicationBuilder = {
    super.applicationBuilder()
      .overrides(
//        bind[AppConfig].toInstance(mockConfig),
        bind[TrustsStoreService].toInstance(mockTrustsStoreService)
      )
  }

  ".enrolSubscriber 4MLD" should {

    val taxable: Boolean = true
    val trn = "XTRN1234567"

    "return Success " when {
      "tax enrolments succesfully subscribed to provided subscription id" in {

        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(false))

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", 204)

        val futureResult = connector.enrolSubscriber("123456789", taxable, trn)

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    "return BadRequestException " when {
      "tax enrolments returns bad request " in {

        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(false))

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 400)

        val futureResult = connector.enrolSubscriber("987654321", taxable, trn)

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

    "return InternalServerErrorException " when {
      "tax enrolments returns internal server error " in {

        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(false))

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 500)

        val futureResult = connector.enrolSubscriber("987654321", taxable, trn)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".enrolSubscriber 5MLD" should {

    val taxable: Boolean = false
    val trn = "XTRN1234567"


    "return correct TaxEnrolmentSubscription" when {
      "calling getTaxEnrolmentSubscription" in {
        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(true))
        when(mockConfig.taxEnrolmentsPayloadBodyCallbackNonTaxable(trn)).thenReturn("/foo")
        when(mockConfig.taxEnrolmentsPayloadBodyServiceNameNonTaxable).thenReturn("serviceNameNonTaxable")
        val result = connector.getTaxEnrolmentSubscription("123456789", true, false, trn)
        result mustBe TaxEnrolmentSubscription("serviceNameNonTaxable", "/foo", "123456789")
      }
    }

    "return Success " when {
      "tax enrolments successfully subscribed to provided subscription id" in {

        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(true))

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", 204)

        val futureResult = connector.enrolSubscriber("123456789", taxable, trn)

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    "return BadRequestException " when {
      "tax enrolments returns bad request " in {

        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(true))

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 400)

        val futureResult = connector.enrolSubscriber("987654321", taxable, trn)

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

    "return InternalServerErrorException " when {
      "tax enrolments returns internal server error " in {

        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(true))

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 500)

        val futureResult = connector.enrolSubscriber("987654321", taxable, trn)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

}
