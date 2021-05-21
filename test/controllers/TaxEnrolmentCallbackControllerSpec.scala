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
import models.tax_enrolments.OrchestratorToTaxableSuccess
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import play.api.test.Helpers.{status, _}
import services.TaxableMigrationService

import scala.concurrent.Future

class TaxEnrolmentCallbackControllerSpec extends BaseSpec with GuiceOneServerPerSuite {

  val auditConnector = mock[AuditConnector]
  val mockMigrationService = mock[TaxableMigrationService]
  val trn = "XTRN1234567"

  ".taxableSubscriptionCallback" should {

    "return 200 " when {
      "tax enrolment callback for subscription id enrolment  " in {
        val SUT = new TaxEnrolmentCallbackController(mockMigrationService, Helpers.stubControllerComponents())

        val result = SUT.taxableSubscriptionCallback(trn).apply(postRequestWithPayload(Json.parse({"""{ "url" : "http//","state" : "SUCCESS"}"""})))
        status(result) mustBe OK
      }
    }
  }

  ".nonTaxableSubscriptionCallback" should {

    "return 200 " when {
      "tax enrolment callback for subscription id enrolment  " in {
        val SUT = new TaxEnrolmentCallbackController(mockMigrationService, Helpers.stubControllerComponents())

        val result = SUT.nonTaxableSubscriptionCallback(trn).apply(postRequestWithPayload(Json.parse({"""{ "url" : "http//","state" : "SUCCESS"}"""})))
        status(result) mustBe OK
      }
    }
  }

  ".migrationSubscriptionCallback" should {
    val urn = "NTTRUST00000001"
    val subscriptionId = "testSubId"
    "return 200 " when {
      "tax enrolment callback for subscription id enrolment " in {
        when(mockMigrationService.completeMigration(eqTo(subscriptionId), eqTo(urn))(any())).thenReturn(Future.successful(OrchestratorToTaxableSuccess))

        val SUT = new TaxEnrolmentCallbackController(mockMigrationService, Helpers.stubControllerComponents())

        val url = s"/tax-enrolment/migration-to-taxable/urn/$urn/subscriptionId/$subscriptionId"
        val request = FakeRequest("POST", url).withHeaders(CONTENT_TYPE -> "application/json")
        val result = SUT.migrationSubscriptionCallback(subscriptionId, urn).apply(request)
        status(result) mustBe OK
      }
    }
  }
}
