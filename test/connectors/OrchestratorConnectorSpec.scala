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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{badRequest, post, serverError, urlEqualTo}
import connector.OrchestratorConnector
import errors.ServerError
import models.orchestrator.OrchestratorMigrationRequest
import models.tax_enrolments.OrchestratorToTaxableSuccessResponse
import org.scalatest.matchers.must.Matchers._
import play.api.http.Status._
import play.api.libs.json.Json

class OrchestratorConnectorSpec extends ConnectorSpecHelper {

  private lazy val connector: OrchestratorConnector = injector.instanceOf[OrchestratorConnector]

  ".migrateToTaxable" should {

    val urn = "NTTRUST00000001"
    val utr = "123456789"

    val request: OrchestratorMigrationRequest = OrchestratorMigrationRequest(urn, utr)
    val requestBody                           = Json.stringify(Json.toJson(request))

    "return OrchestratorToTaxableSuccessResponse" when
      Seq(OK, NO_CONTENT, ACCEPTED).foreach { status =>
        s"tax enrolments successfully subscribed to provided subscription id (status: $status)" in {
          val responseBody = """{"success": "true"}"""
          stubForHeaderlessPost(
            server,
            "/trusts-enrolment-orchestrator/orchestration-process",
            requestBody,
            status,
            responseBody
          )

          val futureResult = connector.migrateToTaxable(urn, utr).value

          whenReady(futureResult) { result =>
            result mustBe Right(OrchestratorToTaxableSuccessResponse())
          }
        }
      }

    "return ServerError(message)" when {
      "tax enrolments returns BadRequest " in {
        server.stubFor(
          post(urlEqualTo("/trusts-enrolment-orchestrator/orchestration-process"))
            .willReturn(badRequest)
        )

        val futureResult = connector.migrateToTaxable(urn, utr).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Bad request"))
        }
      }

      "tax enrolments returns internal server error " in {
        server.stubFor(
          post(urlEqualTo("/trusts-enrolment-orchestrator/orchestration-process"))
            .willReturn(serverError)
        )

        val futureResult = connector.migrateToTaxable(urn, utr).value

        whenReady(futureResult) { result =>
          result mustBe Left(ServerError("Error response from orchestrator: 500"))
        }
      }
    }
  }

}
