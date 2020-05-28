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

import java.time.LocalDateTime

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json._
import play.api.test.Helpers.running
import uk.gov.hmrc.trusts.models.FrontEndUiState
import uk.gov.hmrc.trusts.repositories.UiStateRepository

class UiStateRepositorySpec extends FreeSpec with MustMatchers with TransformIntegrationTest {

  private val data1 = Json.obj(
    "field1" -> "value1",
    "field2" -> "value2",
    "theAnswer" -> 42
  )
  private val data2 = Json.obj(
    "field1" -> "valueX",
    "field2" -> "valueY",
    "theAnswer" -> 3.14
  )
  private val data3 = Json.obj(
    "field1" -> "valueA",
    "field2" -> "valueB",
    "theAnswer" -> 6.28
  )

  "the ui state repository" - {

    "must be able to store and retrieve data" in {

      val application = applicationBuilder.build()

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)

          val repository = application.injector.instanceOf[UiStateRepository]

          val state1 = FrontEndUiState(
            "draftId1",
            "InternalId",
            LocalDateTime.of(2012, 12, 8, 11, 34),
            data1
          )

          repository.set(state1).futureValue mustBe true

          val state2 = FrontEndUiState(
            "draftId2",
            "InternalId",
            LocalDateTime.of(2016, 10, 24, 17, 2),
            data2
          )

          repository.set(state2).futureValue mustBe true

          val state3 = FrontEndUiState(
            "draftId1",
            "InternalId2",
            LocalDateTime.of(2019, 2, 1, 23, 59),
            data3
          )

          repository.set(state3).futureValue mustBe true

          repository.get("draftId1", "InternalId").futureValue mustBe Some(state1)
          repository.get("draftId2", "InternalId").futureValue mustBe Some(state2)
          repository.get("draftId1", "InternalId2").futureValue mustBe Some(state3)

          repository.getAll("InternalId").futureValue mustBe Seq(state2, state1)

          dropTheDatabase(connection)
        }.get
      }
    }
    "must be able to remove drafts no longer being used" in {
      val application = applicationBuilder.build()

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)

          val repository = application.injector.instanceOf[UiStateRepository]

          repository.remove("draftId1", "InternalId").futureValue mustBe true

          val state1 = FrontEndUiState(
            "draftId1",
            "InternalId",
            LocalDateTime.of(2012, 12, 8, 11, 34),
            data1
          )

          repository.set(state1).futureValue mustBe true

          repository.get("draftId1", "InternalId").futureValue mustBe Some(state1)

          repository.remove("draftId1", "InternalId").futureValue mustBe true

          repository.get("draftId1", "InternalId").futureValue mustBe None

          dropTheDatabase(connection)
        }.get
      }
    }
  }
}
