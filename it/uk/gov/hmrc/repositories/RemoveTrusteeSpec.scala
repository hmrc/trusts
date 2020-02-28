package uk.gov.hmrc.repositories

import java.time.LocalDate

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.MongoConnection
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.repositories.TrustsMongoDriver
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class RemoveTrusteeSpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar {

  trait TrusteeIndividualFixture {
    val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

    val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")
  }

  trait TrusteeOrgFixture {
    val getTrustWithOrgTrustees : GetTrustSuccessResponse =
      JsonUtils.getJsonValueFromFile("trusts-etmp-received-trustees-org.json").as[GetTrustSuccessResponse]

    val expectedInitialGetTrusteeOrgJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-initial-trustee-org.json")
  }

  "a remove trustee call" - {

    "must return amended data in a subsequent 'get' call for trustee individual" in new TrusteeIndividualFixture {

      val stubbedDesConnector = mock[DesConnector]
      when(stubbedDesConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(getTrustResponseFromDES))

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .configure(Seq(
          "mongodb.uri" -> connectionString,
          "metrics.enabled" -> false,
          "auditing.enabled" -> false,
          "mongo-async-driver.akka.log-dead-letters" -> 0
        ): _*)
        .build()

      running(application) {

        getConnection(application).map { connection =>
          dropTheDatabase(connection)

          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedInitialGetJson

          val trusteeToRemove = Json.parse(
            """
              |{
              |	"trustee": {
              |		"trusteeInd": {
              |			"lineNo": "1",
              |			"entityStart": "1998-02-12",
              |			"name": {
              |       "firstName": "John",
              |       "middleName": "William",
              |       "lastName": "O'Connor"
              |     },
              |     "dateOfBirth": "1956-02-12",
              |			"identification": {
              |				"nino": "ST123456"
              |			},
              |     "phoneNumber":"0121546546"
              |		}
              |	},
              |	"endDate": "2010-10-10"
              |}
              |""".stripMargin)

          val amendRequest = FakeRequest(POST, "/trusts/remove-trustee/5174384721")
            .withBody(Json.toJson(trusteeToRemove))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val amendResult = route(application, amendRequest).get
          status(amendResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/trustees")).get
          status(newResult) mustBe OK

          val trustees: List[JsValue] = (contentAsJson(newResult) \ "trustees").as[List[JsValue]]
          trustees mustBe empty

          dropTheDatabase(connection)
        }
      }.get
    }

    "must return amended data in a subsequent 'get' call for trustee business" in new TrusteeOrgFixture {

      val stubbedDesConnector = mock[DesConnector]
      when(stubbedDesConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(getTrustWithOrgTrustees))

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .configure(Seq(
          "mongodb.uri" -> connectionString,
          "metrics.enabled" -> false,
          "auditing.enabled" -> false,
          "mongo-async-driver.akka.log-dead-letters" -> 0
        ): _*)
        .build()

      running(application) {

        getConnection(application).map { connection =>
          dropTheDatabase(connection)

          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedInitialGetTrusteeOrgJson

          val trusteeToRemove = Json.parse(
            """
              |{
              |	"trustee": {
              |		"trusteeOrg": {
              |			"lineNo": "1",
              |			"bpMatchStatus": "01",
              |			"entityStart": "1998-02-12",
              |			"name": "Amazon",
              |			"identification": {
              |				"utr": "1234567890"
              |			},
              |     "phoneNumber":"0121546546"
              |		}
              |	},
              |	"endDate": "2010-10-10"
              |}
              |""".stripMargin)

          val amendRequest = FakeRequest(POST, "/trusts/remove-trustee/5174384721")
            .withBody(Json.toJson(trusteeToRemove))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val amendResult = route(application, amendRequest).get
          status(amendResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/trustees")).get
          status(newResult) mustBe OK

          val trustees: List[JsValue] = (contentAsJson(newResult) \ "trustees").as[List[JsValue]]
          trustees mustBe empty

          dropTheDatabase(connection)
        }
      }.get
    }
  }

  // We must be quite patient.
  implicit val defaultPatience = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  // Database boilerplate
  private val connectionString = "mongodb://localhost:27017/trusts-integration"

  private def getDatabase(connection: MongoConnection) = {
    connection.database("trusts-integration")
  }

  private def getConnection(application: Application) = {
    val mongoDriver = application.injector.instanceOf[TrustsMongoDriver]
    lazy val connection = for {
      uri <- MongoConnection.parseURI(connectionString)
      connection <- mongoDriver.api.driver.connection(uri, true)
    } yield connection
    connection
  }

  def dropTheDatabase(connection: MongoConnection): Unit = {
    Await.result(getDatabase(connection).flatMap(_.drop()), Duration.Inf)
  }
}
