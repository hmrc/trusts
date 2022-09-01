//package uk.gov.hmrc.transformations.trustdetails
//
//import connector.TrustsConnector
//import controllers.actions.{FakeIdentifierAction, IdentifierAction}
//import models.get_trust.GetTrustSuccessResponse
//import org.mockito.ArgumentMatchers._
//import org.mockito.Mockito._
//import org.scalatest.freespec.AsyncFreeSpec
//import org.scalatest.matchers.must.Matchers._
//import org.scalatestplus.mockito.MockitoSugar
//import play.api.inject.bind
//import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
//import play.api.test.Helpers._
//import play.api.test.{FakeRequest, Helpers}
//import repositories.TransformationRepository
//import transformers.trustdetails.SetTrustDetailTransform
//import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
//import uk.gov.hmrc.itbase.IntegrationTestBase
//import utils.{JsonUtils, Session}
//
//import scala.concurrent.Future
//
//class SetTrustDetailsSpec extends AsyncFreeSpec with MockitoSugar with IntegrationTestBase {
//
//  "a set trust details call" - {
//
//    val getTrustResponse: JsValue = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
//
//    val stubbedTrustsConnector = mock[TrustsConnector]
//
//    when(stubbedTrustsConnector.getTrustInfo(any()))
//      .thenReturn(Future.successful(getTrustResponse.as[GetTrustSuccessResponse]))
//
//    def application = applicationBuilder
//      .overrides(
//        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
//        bind[TrustsConnector].toInstance(stubbedTrustsConnector)
//      ).build()
//
//    val sessionId: String = Session.id(hc)
//
//    "must add series of transforms" - {
//
//      "when migrating" in assertMongoTest(application) { app =>
//
//        val repository = app.injector.instanceOf[TransformationRepository]
//
//        val identifier: String = "NTTRUST00000001"
//
//        val body = Json.parse(
//          """
//            |{
//            |  "lawCountry": "FR",
//            |  "administrationCountry": "GB",
//            |  "residentialStatus": {
//            |    "nonUK": {
//            |      "sch5atcgga92": true
//            |    }
//            |  },
//            |  "trustUKProperty": true,
//            |  "trustRecorded": true,
//            |  "trustUKRelation": false,
//            |  "trustUKResident": false,
//            |  "typeOfTrust": "Inter vivos Settlement",
//            |  "interVivos": true
//            |}
//            |""".stripMargin
//        )
//
//        val setValueRequest = FakeRequest(PUT, s"/trusts/trust-details/$identifier/migrating-trust-details")
//          .withBody(body)
//          .withHeaders(CONTENT_TYPE -> "application/json")
//
//        val setValueResponse = route(app, setValueRequest).get
//        status(setValueResponse) mustBe OK
//
//        whenReady(repository.get(identifier, "id", sessionId)) { transforms =>
//          transforms.get.deltaTransforms mustBe Seq(
//            SetTrustDetailTransform(JsString("FR"), "lawCountry"),
//            SetTrustDetailTransform(JsString("GB"), "administrationCountry"),
//            SetTrustDetailTransform(Json.parse("""{"nonUK":{"sch5atcgga92":true}}"""), "residentialStatus"),
//            SetTrustDetailTransform(JsBoolean(true), "trustUKProperty"),
//            SetTrustDetailTransform(JsBoolean(true), "trustRecorded"),
//            SetTrustDetailTransform(JsBoolean(false), "trustUKRelation"),
//            SetTrustDetailTransform(JsBoolean(false), "trustUKResident"),
//            SetTrustDetailTransform(JsString("Inter vivos Settlement"), "typeOfTrust"),
//            SetTrustDetailTransform(JsBoolean(true), "interVivos")
//          )
//        }
//      }
//
//      "when not migrating" in assertMongoTest(application) { app =>
//
//        val repository = app.injector.instanceOf[TransformationRepository]
//
//        val identifier: String = "0123456789"
//
//        val body = Json.parse(
//          """
//            |{
//            |  "trustUKProperty": true,
//            |  "trustRecorded": true,
//            |  "trustUKResident": true
//            |}
//            |""".stripMargin
//        )
//
//        val setValueRequest = FakeRequest(PUT, s"/trusts/trust-details/$identifier/non-migrating-trust-details")
//          .withBody(body)
//          .withHeaders(CONTENT_TYPE -> "application/json")
//
//        val setValueResponse = route(app, setValueRequest).get
//        status(setValueResponse) mustBe OK
//
//        whenReady(repository.get(identifier, "id", sessionId)) { transforms =>
//          transforms.get.deltaTransforms mustBe Seq(
//            SetTrustDetailTransform(JsBoolean(true), "trustUKProperty"),
//            SetTrustDetailTransform(JsBoolean(true), "trustRecorded"),
//            SetTrustDetailTransform(JsBoolean(true), "trustUKResident")
//          )
//        }
//      }
//    }
//  }
//
//}
