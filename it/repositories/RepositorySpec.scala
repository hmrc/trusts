//package repositories
//
//import org.scalatest._
//import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.test.Helpers._
//
//import scala.language.implicitConversions
//
//class RepositorySpec extends FreeSpec with MustMatchers with FailOnUnindexedQueries with IntegrationPatience
//  with ScalaFutures with OptionValues with Inside with EitherValues with Eventually {
//
//  private lazy val appBuilder: GuiceApplicationBuilder =
//    new GuiceApplicationBuilder()
//
//  val userAnswers = new UserAnswers("test")
//
//  "a playback repository" - {
//    "must be able to store a playback payload for a processed trust" in {
//
//      database.map(_.drop()).futureValue
//
//      val application = appBuilder.build()
//
//      running(application) {
//
//        val repository = application.injector.instanceOf[PlaybackRepository]
//
//        started(application).futureValue
//
//        val storedOk = repository.set(userAnswers)
//
//        storedOk.futureValue mustBe true
//
//      }
//    }
//
//    "must be able to refresh the session upon retrieval of user answers" in {
//
//      database.map(_.drop()).futureValue
//
//      val application = appBuilder.build()
//
//      running(application) {
//
//        val repository = application.injector.instanceOf[PlaybackRepository]
//
//        started(application).futureValue
//
//        repository.set(userAnswers)
//
//        val firstGet = repository.get(userAnswers.internalAuthId).map(_.get.updatedAt).futureValue
//
//        val secondGet = repository.get(userAnswers.internalAuthId).map(_.get.updatedAt).futureValue
//
//        secondGet isAfter firstGet mustBe true
//      }
//    }
//  }
//}
