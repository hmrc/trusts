/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.itbase

import config.AppConfig
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.MongoDateTimeFormats
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.stubControllerComponents
import repositories.{CacheRepositoryImpl, RegistrationSubmissionRepositoryImpl, TaxableMigrationRepositoryImpl, TransformationRepositoryImpl}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class IntegrationTestBase extends AnyWordSpec
  with GuiceOneServerPerSuite
  with ScalaFutures
  with MongoDateTimeFormats
  with MockitoSugar
  with BeforeAndAfterEach
  with EitherValues {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(15, Millis))

  val connectionString = "mongodb://localhost:27017/trusts-integration"

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  private val cc = stubControllerComponents()

  lazy val createApplication = applicationBuilder
    .overrides(
      bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Agent))
    ).build()

  def injector = createApplication.injector

  def appConfig: AppConfig = injector.instanceOf[AppConfig]

  def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(Seq(
        "mongodb.uri" -> connectionString,
        "metrics.enabled" -> false,
        "auditing.enabled" -> false,
//        "features.mongo.dropIndexes" -> true
      ): _*)

  def cleanDatabase(application: Application): Unit = {
    val dbs = Seq(
      application.injector.instanceOf[CacheRepositoryImpl],
      application.injector.instanceOf[RegistrationSubmissionRepositoryImpl],
      application.injector.instanceOf[TaxableMigrationRepositoryImpl],
      application.injector.instanceOf[TransformationRepositoryImpl]
    )

    val cleanDbs = dbs.forall(db =>
      Await.result(db.collection.deleteMany(BsonDocument()).toFuture(), 10.seconds).wasAcknowledged()
    )


    assert(cleanDbs, "Mongo DB was not cleaned properly or something went wrong!")
  }

  def assertMongoTest(application: Application)(block: Application => Assertion): Assertion = {
      cleanDatabase(application)
      block(application)
  }

}
