package uk.gov.hmrc.repositories

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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, GetTrustSuccessResponse}
import uk.gov.hmrc.trusts.models.{AddressType, NameType}
import uk.gov.hmrc.trusts.repositories.TrustsMongoDriver
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PromoteLeadTrusteeSpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "a promote lead trustee call" - {
    "must return amended data in a subsequent 'get' call" in {

      val newTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("John", Some("William"), "O'Connor"),
        dateOfBirth = new DateTime(1965, 2, 10, 0, 0),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(
          None,
          Some("ST123456"),
          None,
          Some(AddressType(
            "221B Baker Street",
            "Suite 16",
            Some("Newcastle upon Tyne"),
            None,
            Some("NE1 2LA"),
            "GB"
          ))),
          None
      )

      val expectedGetAfterPromoteTrusteeJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-after-promote-trustee.json")

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

          val promoteRequest = FakeRequest(POST, "/trusts/promote-trustee/5174384721/0")
            .withBody(Json.toJson(newTrusteeIndInfo))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val promoteResult = route(application, promoteRequest).get
          status(promoteResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe expectedGetAfterPromoteTrusteeJson

          dropTheDatabase(connection)
        }.get
      }
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
