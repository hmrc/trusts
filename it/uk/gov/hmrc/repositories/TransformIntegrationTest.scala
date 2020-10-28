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

package uk.gov.hmrc.repositories

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.api.{DefaultDB, MongoConnection}
import repositories.TrustsMongoDriver

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

trait TransformIntegrationTest extends ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(15, Millis))

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

  lazy val applicationBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(Seq(
    "mongodb.uri" -> connectionString,
    "metrics.enabled" -> false,
    "auditing.enabled" -> false,
    "mongo-async-driver.akka.log-dead-letters" -> 0
  ): _*)

}
