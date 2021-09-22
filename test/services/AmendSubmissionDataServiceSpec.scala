/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import base.BaseSpec
import org.mockito.Mockito.when
import play.api.libs.json.Json
import org.scalatest.matchers.must.Matchers._

import java.time.LocalDate

class AmendSubmissionDataServiceSpec extends BaseSpec {

  private val mockLocalDateService = mock[LocalDateService]
  private val service = new AmendSubmissionDataService(mockLocalDateService)

  "AmendSubmissionDataService" when {

    ".applyRulesAndAddSubmissionDate" must {

      val inputJson = Json.obj()

        "add submission date" in {

          val date = "2000-01-01"

          when(mockLocalDateService.now).thenReturn(LocalDate.parse(date))

          val result = service.applyRulesAndAddSubmissionDate(inputJson)

          result mustBe Json.parse(
            s"""
              |{
              |  "submissionDate": "$date"
              |}
              |""".stripMargin
          )
      }
    }
  }
}
