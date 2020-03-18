package uk.gov.hmrc.repositories

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Application
import reactivemongo.api.{DefaultDB, MongoConnection}
import uk.gov.hmrc.trusts.repositories.TrustsMongoDriver
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

trait TransformIntegrationTest extends ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  val connectionString = "mongodb://localhost:27017/trusts-integration"

  def getDatabase(connection: MongoConnection): Future[DefaultDB] = {
    connection.database("trusts-integration")
  }

  def getConnection(application: Application): Try[MongoConnection] = {
    val mongoDriver = application.injector.instanceOf[TrustsMongoDriver]
    for {
      uri <- MongoConnection.parseURI(connectionString)
      connection <- mongoDriver.api.driver.connection(uri, strictUri = true)
    } yield connection
  }

  def dropTheDatabase(connection: MongoConnection): Unit = {
    Await.result(getDatabase(connection).flatMap(_.drop()), Duration.Inf)
  }

}
