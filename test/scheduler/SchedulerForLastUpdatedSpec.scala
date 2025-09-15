/*
 * Copyright 2025 HM Revenue & Customs
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
  }


}
