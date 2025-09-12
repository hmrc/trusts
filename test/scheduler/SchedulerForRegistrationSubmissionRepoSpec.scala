package scheduler

import base.BaseSpec
import models.UpdatedCounterValues
import org.apache.pekko.stream.Materializer
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.mongodb.scala.Observable
import play.api.Configuration
import repositories.RegistrationSubmissionRepositoryImpl

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class SchedulerForRegistrationSubmissionRepoSpec extends BaseSpec {

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
    "check for each repo with the ids" in new Setup {

      val limit = 3
      val ids: Seq[ObjectId] = Seq.fill(limit)(new ObjectId())
      val objectData: Observable[ObjectId] = Observable.apply(ids)

      when(registrationSubmissionRepositoryImpl.getAllInvalidDateDocuments(anyInt())).thenReturn(objectData)

      when(registrationSubmissionRepositoryImpl.updateAllInvalidDateDocuments(any[Seq[ObjectId]]()))
        .thenReturn(Future.successful(UpdatedCounterValues(1, 2, 3)))

      Await.result(schedulerForRegistrationSubmissionRepo.tap.pull(), 2.seconds)

      verify(registrationSubmissionRepositoryImpl, times(1)).getAllInvalidDateDocuments(anyInt())

      val captor: ArgumentCaptor[Seq[ObjectId]] =
        ArgumentCaptor.forClass(classOf[Seq[ObjectId]])

      verify(registrationSubmissionRepositoryImpl).updateAllInvalidDateDocuments(captor.capture())

      captor.getValue must contain allElementsOf (ids)

      objectData mustBe registrationSubmissionRepositoryImpl.getAllInvalidDateDocuments(limit)

    }

  }

}
