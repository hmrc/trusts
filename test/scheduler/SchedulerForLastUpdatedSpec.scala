package scheduler

import base.BaseSpec
import models.UpdatedCounterValues
import org.apache.pekko.stream.Materializer
import org.bson.types.ObjectId
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.Mockito.{times, verify, when}
import org.mongodb.scala.Observable
import play.api.Configuration
import repositories.{CacheRepositoryImpl, RepositoryHelper, TaxableMigrationRepositoryImpl, TransformationRepositoryImpl}

import java.util
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class SchedulerForLastUpdatedSpec extends BaseSpec {


  val mockCacheRepositoryImpl: CacheRepositoryImpl = mock[CacheRepositoryImpl]
  val mockTaxableMigrationRepositoryImpl: TaxableMigrationRepositoryImpl = mock[TaxableMigrationRepositoryImpl]
  val mockTransformationRepositoryImpl: TransformationRepositoryImpl = mock[TransformationRepositoryImpl]

  val repositoriesJava: java.util.Set[RepositoryHelper[_]] = new util.HashSet()
  repositoriesJava.add(mockCacheRepositoryImpl)
  repositoriesJava.add(mockTaxableMigrationRepositoryImpl)
  repositoriesJava.add(mockTransformationRepositoryImpl)

  val config: Configuration = app.injector.instanceOf[Configuration]
  implicit val materializer: Materializer = app.injector.instanceOf[Materializer]


  trait Setup {
    lazy val schedulerForLastUpdated: SchedulerForLastUpdated = new SchedulerForLastUpdated(
      repositoriesJava,
      config = config
    )
  }

  "SchedulerForLastUpdated" when {
    "check for each repo with the ids" in new Setup {
      val limit = 3
      val ids: Seq[ObjectId] = Seq.fill(limit)(new ObjectId())
      val objectData: Observable[ObjectId] = Observable.apply(ids)

      repositoriesJava.forEach {
        ele =>
          when(ele.getAllInvalidDateDocuments(anyInt())).thenReturn(objectData)

          when(ele.updateAllInvalidDateDocuments(any[Seq[ObjectId]]()))
            .thenReturn(Future.successful(UpdatedCounterValues(1, 2, 3)))
      }

      Await.result(schedulerForLastUpdated.tap.pull(), 2.seconds)

      repositoriesJava.forEach {
        ele =>
          verify(ele, times(1)).getAllInvalidDateDocuments(anyInt())

          val captor: ArgumentCaptor[Seq[ObjectId]] =
            ArgumentCaptor.forClass(classOf[Seq[ObjectId]])

          verify(ele).updateAllInvalidDateDocuments(captor.capture())

          captor.getValue must contain allElementsOf (ids)

          objectData mustBe ele.getAllInvalidDateDocuments(limit)
      }

    }

//    "skip update when no invalid docs" in new Setup {
//      val emptyObjectIds: Observable[ObjectId] = Observable.apply(Nil)
//
//      repositoriesJava.forEach { ele =>
//        when(ele.getAllInvalidDateDocuments(anyInt())).thenReturn(emptyObjectIds)
//      }
//
//      Await.result(schedulerForLastUpdated.tap.pull(), 5.seconds)
//
//      repositoriesJava.forEach { ele =>
//        verify(ele).getAllInvalidDateDocuments(anyInt())
////        verify(ele, never()).updateAllInvalidDateDocuments(any[Seq[ObjectId]]())
//      }
//
//    }
  }


}
