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

package uk.gov.hmrc.transformations.trustdetails

import cats.data.EitherT
import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, GetTrustSuccessResponse}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mongodb.scala.Document
import org.scalatest.matchers.must.Matchers._
import play.api.inject.bind
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.TransformationRepositoryImpl
import transformers.trustdetails.SetTrustDetailTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.{JsonUtils, Session}

import scala.concurrent.Future

class SetTrustDetailsSpec extends IntegrationTestBase {

  "a set trust details call" should {

    val getTrustResponse: JsValue = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")

    val stubbedTrustsConnector = mock[TrustsConnector]

    when(stubbedTrustsConnector.getTrustInfo(any()))
      .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(getTrustResponse.as[GetTrustSuccessResponse]))))

    def application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(stubbedTrustsConnector)
      ).build()

    val sessionId: String = Session.id(hc)

    "add series of transforms" should {

      "when migrating" in assertMongoTest(application) { app =>

        def repository = application.injector.instanceOf[TransformationRepositoryImpl]

        await(repository.collection.deleteMany(filter = Document()).toFuture())
        await(repository.ensureIndexes())

        val identifier: String = "NTTRUST00000001"

        val body = Json.parse(
          """
            |{
            |  "lawCountry": "FR",
            |  "administrationCountry": "GB",
            |  "residentialStatus": {
            |    "nonUK": {
            |      "sch5atcgga92": true
            |    }
            |  },
            |  "trustUKProperty": true,
            |  "trustRecorded": true,
            |  "trustUKRelation": false,
            |  "trustUKResident": false,
            |  "typeOfTrust": "Inter vivos Settlement",
            |  "interVivos": true
            |}
            |""".stripMargin
        )

        val setValueRequest = FakeRequest(PUT, s"/trusts/trust-details/$identifier/migrating-trust-details")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val setValueResponse = route(application, setValueRequest).get
        status(setValueResponse) mustBe OK

        whenReady(repository.get(identifier, "id", sessionId).value) { transforms =>
          transforms.value.get.deltaTransforms mustBe Seq(
            SetTrustDetailTransform(JsString("FR"), "lawCountry"),
            SetTrustDetailTransform(JsString("GB"), "administrationCountry"),
            SetTrustDetailTransform(Json.parse("""{"nonUK":{"sch5atcgga92":true}}"""), "residentialStatus"),
            SetTrustDetailTransform(JsBoolean(true), "trustUKProperty"),
            SetTrustDetailTransform(JsBoolean(true), "trustRecorded"),
            SetTrustDetailTransform(JsBoolean(false), "trustUKRelation"),
            SetTrustDetailTransform(JsBoolean(false), "trustUKResident"),
            SetTrustDetailTransform(JsString("Inter vivos Settlement"), "typeOfTrust"),
            SetTrustDetailTransform(JsBoolean(true), "interVivos")
          )
        }
      }

      "when not migrating" in assertMongoTest(application) { app =>

        def repository = application.injector.instanceOf[TransformationRepositoryImpl]

        await(repository.collection.deleteMany(filter = Document()).toFuture())
        await(repository.ensureIndexes())

        val identifier: String = "0123456789"

        val body = Json.parse(
          """
            |{
            |  "trustUKProperty": true,
            |  "trustRecorded": true,
            |  "trustUKResident": true
            |}
            |""".stripMargin
        )

        val setValueRequest = FakeRequest(PUT, s"/trusts/trust-details/$identifier/non-migrating-trust-details")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val setValueResponse = route(application, setValueRequest).get
        status(setValueResponse) mustBe OK

        whenReady(repository.get(identifier, "id", sessionId).value) { transforms =>
          transforms.value.get.deltaTransforms mustBe Seq(
            SetTrustDetailTransform(JsBoolean(true), "trustUKProperty"),
            SetTrustDetailTransform(JsBoolean(true), "trustRecorded"),
            SetTrustDetailTransform(JsBoolean(true), "trustUKResident")
          )
        }
      }
    }
  }

}
