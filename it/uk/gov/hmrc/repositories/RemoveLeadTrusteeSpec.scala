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
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeType, GetTrustSuccessResponse}
import uk.gov.hmrc.trusts.models.{NameType, RemoveTrustee}
import uk.gov.hmrc.trusts.repositories.TrustsMongoDriver
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class RemoveLeadTrusteeSpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "a remove trustee call" - {

    "must return amended data in a subsequent 'get' call" in {

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
          "auditing.enabled" -> false
        ): _*)
        .build()

      running(application) {

        getConnection(application).map { connection =>
          dropTheDatabase(connection)

          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedInitialGetJson

          val trusteeToRemove = RemoveTrustee(
            trustee = DisplayTrustTrusteeType(
              trusteeInd = Some(
                DisplayTrustTrusteeIndividualType(
                  lineNo = "1",
                  bpMatchStatus = None,
                  name = NameType(
                    firstName = "John",
                    middleName = Some("William"),
                    lastName = "O'Connor"
                  ),
                  dateOfBirth = Some(DateTime.parse("1956-02-12")),
                  phoneNumber = Some("0121546546"),
                  identification = Some(DisplayTrustIdentificationType(
                    nino = Some("ST123456"),
                    safeId = None,
                    passport = None,
                    address = None
                  )),
                  entityStart = DateTime.parse("1998-02-12")
                )
              ),
              trusteeOrg = None
            ),
            endDate = LocalDate.parse("2010-10-10")
          )

          val amendRequest = FakeRequest(DELETE, "/trusts/5174384721/trustee")
            .withBody(Json.toJson(trusteeToRemove))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val amendResult = route(application, amendRequest).get
          status(amendResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK

          val trustees: List[JsValue] = (contentAsJson(newResult) \ "getTrust" \ "trust" \ "entities" \ "trustees").as[List[JsValue]]
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
