package scheduler

import models.UpdatedCounterValues
import org.apache.pekko.stream.Materializer
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.{verify, when}
import org.mongodb.scala.Observable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import repositories.RegistrationSubmissionRepositoryImpl

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class SchedulerForRegistrationSubmissionRepoSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val registrationSubmissionRepositoryImpl: RegistrationSubmissionRepositoryImpl = Mockito.mock(classOf[RegistrationSubmissionRepositoryImpl])

  val config: Configuration = app.injector.instanceOf[Configuration]
  implicit val materializer: Materializer = app.injector.instanceOf[Materializer]


  trait Setup {
    lazy val schedulerForRegistrationSubmissionRepo: SchedulerForRegistrationSubmissionRepo = new SchedulerForRegistrationSubmissionRepo(
      registrationSubmissionRepositoryImpl,
      config = config
    )
  }


  "SchedulerForRegistrationSubmissionRepo" when {

      "set the totals for metrics from a queue of UpdatedCounterValues and returns the current totals as a UpdatedCounterValues" in new Setup {


        val ids = Seq.fill(3)(new ObjectId())
        val objectData: Observable[ObjectId] = Observable.apply(ids)

        when(registrationSubmissionRepositoryImpl.getAllInvalidDateDocuments(anyInt())).thenReturn(objectData)

        when(registrationSubmissionRepositoryImpl.updateAllInvalidDateDocuments(any[Seq[ObjectId]]()))
          .thenReturn(Future.successful(UpdatedCounterValues(1, 2, 3)))

        Await.result(schedulerForRegistrationSubmissionRepo.tap.pull(), 2.seconds)

        verify(registrationSubmissionRepositoryImpl).getAllInvalidDateDocuments(anyInt())

        val captor: ArgumentCaptor[Seq[ObjectId]] =
          ArgumentCaptor.forClass(classOf[Seq[ObjectId]])

        verify(registrationSubmissionRepositoryImpl).updateAllInvalidDateDocuments(captor.capture())

        captor.getValue contains (ids)

        assert(objectData eq registrationSubmissionRepositoryImpl.getAllInvalidDateDocuments(3))



      }
  }


}
