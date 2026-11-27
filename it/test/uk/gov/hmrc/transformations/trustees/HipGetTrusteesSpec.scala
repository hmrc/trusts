/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.transformations.trustees

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import connector.HipTrustsConnector
import connectors.ConnectorSpecHelper
import controllers.GetTrustController
import controllers.actions.{FakeIdentifierAction, ValidateIdentifierActionProvider}
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import services.auditing.AuditService
import services.{TaxYearService, TransformationService, TrustsService}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import utils.RequiredEntityDetailsForMigration

import scala.concurrent.ExecutionContext.Implicits.global

class HipGetTrusteesSpec extends ConnectorSpecHelper with DefaultAwaitTimeout {

  override def applicationBuilder(): GuiceApplicationBuilder =
    super
      .applicationBuilder()
      .configure(
        Seq(
          "microservice.services.hip.registration.port" -> server.port(),
          "microservice.services.hip.variation.port"    -> server.port(),
          "microservice.services.hip.playback.port"     -> server.port(),
          "features.hip.trusts"                         -> true
        ): _*
      )

  private lazy val trustsService            = injector.instanceOf[TrustsService]
  private lazy val validateIdentifierAction = injector.instanceOf[ValidateIdentifierActionProvider]
  private lazy val transformationService    = injector.instanceOf[TransformationService]
  private lazy val bodyParsers              = injector.instanceOf[BodyParsers.Default]

  private val mockedAuditService      = mock[AuditService]
  private val taxYearService          = mock[TaxYearService]
  private val mockRequiredDetailsUtil = mock[RequiredEntityDetailsForMigration]

  lazy val trustController =
    new GetTrustController(
      new FakeIdentifierAction(bodyParsers, Organisation),
      mockedAuditService,
      trustsService,
      transformationService,
      taxYearService,
      validateIdentifierAction,
      mockRequiredDetailsUtil,
      Helpers.stubControllerComponents()
    )

  private def get5MLDTrustUTREndpoint(utr: String) =
    s"/etmp/RESTAdapter/trustsandestates/registration/UTR/$utr"

  private def wrapInSuccessNode(in: String) =
    Json.obj("success" -> Json.parse(in))

  def connectorStubForGet(
    server: WireMockServer,
    url: String,
    returnStatus: Int,
    responseBody: String,
    delayResponse: Int = 0
  ): StubMapping = super.stubForGet(server, url, returnStatus, responseBody, delayResponse)

  "GetTrustController.getTrustees" should {
    "return correct trustees despite malformed trustees array in the json payload from the HipTrustsConnector " in {

      val utr                                 = "1234567890"
      val getProcessedTrustResponse: JsObject = wrapInSuccessNode(get5MLDTrustResponseJsonWithMalformedTrustees)
      stubForGet(server, get5MLDTrustUTREndpoint(utr), OK, getProcessedTrustResponse.toString)

      val expected = Json.parse(
        """
          |{
          |  "trustees": [
          |    {
          |      "trusteeInd": {
          |        "lineNo": "1",
          |        "bpMatchStatus": "01",
          |        "name": {
          |          "firstName": "Tamara",
          |          "middleName": "Hingis",
          |          "lastName": "Jones"
          |        },
          |        "dateOfBirth": "1965-02-28",
          |        "phoneNumber": "+447456788112",
          |        "identification": {
          |          "safeId": "2222200000000"
          |        },
          |        "entityStart": "2017-02-28",
          |        "provisional": false
          |      }
          |    },
          |    {
          |      "trusteeOrg": {
          |        "lineNo": "1",
          |        "name": "MyOrg Incorporated",
          |        "phoneNumber": "+447456788112",
          |        "email": "a",
          |        "identification": {
          |          "safeId": "2222200000000"
          |        },
          |        "entityStart": "2017-02-28",
          |        "provisional": false
          |      }
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val result =
        trustController.getTrustees("1234567890").apply(FakeRequest(GET, s"/trusts/trustees/$utr/transformed"))

      assert(trustsService.trustsConnector.isInstanceOf[HipTrustsConnector])
      assert(contentAsJson(result) === expected)

    }

  }

}
