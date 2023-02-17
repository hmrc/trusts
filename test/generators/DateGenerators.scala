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

package generators

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.time.TaxYear

import java.time.{LocalDate, MonthDay}
import scala.language.implicitConversions

trait DateGenerators {

  val arbitraryFinishYear: Int = 2021
  val arbitraryStartYear: Int = arbitraryFinishYear - 1
  val TAX_YEAR_START_MONTH: Int = TaxYear(arbitraryStartYear).starts.getMonthValue
  val TAX_YEAR_START_DAY: Int = TaxYear(arbitraryStartYear).starts.getDayOfMonth

  implicit lazy val arbitraryDateInTaxYear: Arbitrary[LocalDate] = {
    arbitraryDateInTaxYearNTaxYearsAgo(0)
  }

  implicit lazy val arbitraryDateInTaxYearOnOrBeforeOctober5th: Arbitrary[LocalDate] = {
    Arbitrary {
      for {
        month <- Gen.choose(TAX_YEAR_START_MONTH, 10)
        day <- Gen.choose(
          min = month match {
            case TAX_YEAR_START_MONTH => TAX_YEAR_START_DAY
            case _ => 1
          },
          max = month match {
            case 4 | 6 | 9 | 11 => 30
            case 10 => 5
            case _ => 31
          }
        )
      } yield {
        LocalDate.of(arbitraryStartYear, month, day)
      }
    }
  }

  implicit lazy val arbitraryDateInTaxYearAfterOctober5th: Arbitrary[LocalDate] = {
    Arbitrary {
      for {
        month <- Gen.oneOf((1 to TAX_YEAR_START_MONTH) ++ (10 to 12))
        day <- Gen.choose(
          min = month match {
            case 10 => 6
            case _ => 1
          },
          max = month match {
            case 2 => 28
            case TAX_YEAR_START_MONTH => TAX_YEAR_START_DAY - 1
            case 4 | 6 | 9 | 11 => 30
            case _ => 31
          }
        )
      } yield {
        val year = if (MonthDay.of(month, day).isBefore(MonthDay.of(TAX_YEAR_START_MONTH, TAX_YEAR_START_DAY))) {
          arbitraryFinishYear
        } else {
          arbitraryFinishYear - 1
        }
        LocalDate.of(year, month, day)
      }
    }
  }

  implicit lazy val arbitraryDateInTaxYearOnOrBeforeDecember22nd: Arbitrary[LocalDate] = {
    Arbitrary {
      for {
        month <- Gen.choose(TAX_YEAR_START_MONTH, 12)
        day <- Gen.choose(
          min = month match {
            case TAX_YEAR_START_MONTH => TAX_YEAR_START_DAY
            case _ => 1
          },
          max = month match {
            case 4 | 6 | 9 | 11 => 30
            case 12 => 22
            case _ => 31
          }
        )
      } yield {
        LocalDate.of(arbitraryStartYear, month, day)
      }
    }
  }

  implicit lazy val arbitraryDateInTaxYearAfterDecember22nd: Arbitrary[LocalDate] = {
    Arbitrary {
      for {
        month <- Gen.oneOf((1 to TAX_YEAR_START_MONTH) :+ 12)
        day <- Gen.choose(
          min = month match {
            case 12 => 23
            case _ => 1
          },
          max = month match {
            case 2 => 28
            case TAX_YEAR_START_MONTH => TAX_YEAR_START_DAY - 1
            case 4 | 6 | 9 | 11 => 30
            case _ => 31
          }
        )
      } yield {
        val year = if (MonthDay.of(month, day).isBefore(MonthDay.of(TAX_YEAR_START_MONTH, TAX_YEAR_START_DAY))) {
          arbitraryFinishYear
        } else {
          arbitraryFinishYear - 1
        }
        LocalDate.of(year, month, day)
      }
    }
  }

  implicit def arbitraryDateInTaxYearNTaxYearsAgo(n: Int): Arbitrary[LocalDate] = {
    Arbitrary {
      for {
        month <- Gen.choose(1, 12)
        day <- Gen.choose(
          min = 1,
          max = month match {
            case 2 => 28
            case 4 | 6 | 9 | 11 => 30
            case _ => 31
          }
        )
      } yield {
        val year = if (MonthDay.of(month, day).isBefore(MonthDay.of(TAX_YEAR_START_MONTH, TAX_YEAR_START_DAY))) {
          arbitraryFinishYear - n
        } else {
          arbitraryFinishYear - n - 1
        }
        LocalDate.of(year, month, day)
      }
    }
  }

}
