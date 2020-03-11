package uk.gov.hmrc.repositories

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.MongoConnection
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.repositories.TrustsMongoDriver
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class RemoveTrusteeSpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar {

  trait JsonFixtures {

    val getTrustResponseFromDES : JsValue = JsonUtils
      .getJsonValueFromFile("trusts-etmp-received-multiple-trustees.json")
  }

  "a remove trustee call" - {

    "must return amended data in a subsequent 'get' call" in new JsonFixtures {

      val stubbedDesConnector = mock[DesConnector]

      when(stubbedDesConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(getTrustResponseFromDES.as[GetTrustSuccessResponse]))

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

          val removeAtIndex = Json.parse(
            """
              |{
              |	"index": 0,
              |	"endDate": "2010-10-10"
              |}
              |""".stripMargin)

          val amendRequest = FakeRequest(PUT, "/trusts/5174384721/trustees/remove")
            .withBody(Json.toJson(removeAtIndex))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val firstRemoveResult = route(application, amendRequest).get
          status(firstRemoveResult) mustBe OK

          val secondRemoveResult = route(application, amendRequest).get
          status(secondRemoveResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/trustees")).get
          status(newResult) mustBe OK

          val trustees = (contentAsJson(newResult) \ "trustees").as[JsArray]
          trustees mustBe Json.parse(
            """
              |[
              |            {
              |              "trusteeOrg": {
              |                "name": "Trustee Org 2",
              |                "phoneNumber": "0121546546",
              |                "identification": {
              |                  "utr": "5465416546"
              |                },
              |                "entityStart": "1998-02-12",
              |                "provisional": true
              |              }
              |            }
              |]
              |""".stripMargin)

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
