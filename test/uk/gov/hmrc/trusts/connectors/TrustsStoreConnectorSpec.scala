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

package uk.gov.hmrc.trusts.connectors

import org.scalatest.RecoverMethods
import play.api.http.Status
import uk.gov.hmrc.http.{BadRequestException, Upstream5xxResponse}
import uk.gov.hmrc.trusts.connector.TrustsStoreConnector

import scala.concurrent.ExecutionContext.Implicits.global

class TrustsStoreConnectorSpec extends ConnectorSpecHelper with RecoverMethods {

  private lazy val url: String = "/trusts-store/features/5mld"

  private lazy val connector: TrustsStoreConnector = injector.instanceOf[TrustsStoreConnector]

  private def wiremock(expectedStatus: Int, expectedResponse: String) =
    stubForGet(server, url, expectedStatus, expectedResponse)

  "TrustsStoreConnector" must {

    "call GET /feature/5mld" which {

      "returns 200 with value" in {

        val response =
          """
            |{
            |   "name": "5mld",
            |   "isEnabled": true
            |}
            |""".stripMargin

        wiremock(
          expectedStatus = Status.CREATED,
          expectedResponse = response
        )

        connector.getFeature("5mld") map { response =>
          app.stop()
          response.name mustBe "5mld"
          response.isEnabled mustBe true
        }
      }

      "returns 400 BAD_REQUEST" in {

        wiremock(
          expectedStatus = Status.BAD_REQUEST,
          expectedResponse = ""
        )

        recoverToSucceededIf[BadRequestException](connector.getFeature("5mld"))
      }

      "returns 500 INTERNAL_SERVER_ERROR" in {

        wiremock(
          expectedStatus = Status.INTERNAL_SERVER_ERROR,
          expectedResponse = ""
        )

        recoverToSucceededIf[Upstream5xxResponse](connector.getFeature("5mld"))

      }
    }
  }
}
