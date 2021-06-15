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

import implicits.Implicits.LocalDateImplicits
import models.FirstTaxYearAvailable
import uk.gov.hmrc.time.TaxYear

import java.time.{LocalDate, MonthDay}

class TaxYearService {

  def currentDate: LocalDate = LocalDate.now

  def currentTaxYear: TaxYear = TaxYear.taxYearFor(currentDate)

  def firstTaxYearAvailable(startDate: LocalDate): FirstTaxYearAvailable = {
    val startYearOfStartDateTaxYear = TaxYear.taxYearFor(startDate).startYear

    val startYearOfOldestTaxYearToShow: Int = {
      val deadline = currentTaxYear.starts.next(MonthDay.of(12, 22)) // December 22nd

      if (!currentDate.isAfter(deadline)) {
        currentTaxYear.back(4).startYear
      } else {
        currentTaxYear.back(3).startYear
      }
    }

    if (startYearOfStartDateTaxYear < startYearOfOldestTaxYearToShow) {
      FirstTaxYearAvailable(
        yearsAgo = currentTaxYear.startYear - startYearOfOldestTaxYearToShow,
        earlierYearsToDeclare = true
      )
    } else {
      FirstTaxYearAvailable(
        yearsAgo = currentTaxYear.startYear - startYearOfStartDateTaxYear,
        earlierYearsToDeclare = false
      )
    }
  }
}
