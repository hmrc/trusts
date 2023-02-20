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

package services

import base.BaseSpec
import generators.DateGenerators
import models.FirstTaxYearAvailable
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate

class TaxYearServiceSpec extends BaseSpec with ScalaCheckPropertyChecks with MockitoSugar with DateGenerators with BeforeAndAfterEach {

  val taxYearService: TaxYearService = mock[TaxYearService]

  override def beforeEach(): Unit = {
    reset(taxYearService)
    when(taxYearService.currentTaxYear).thenReturn(TaxYear(arbitraryStartYear))
    when(taxYearService.firstTaxYearAvailable(any())).thenCallRealMethod()
  }

  "TaxYearService" when {

    "firstTaxYearAvailable" must {

      "return first tax year available for start date" when {

        "before deadline" when {

          "start date is more than 4 tax years ago" in {

            forAll(
              arbitrary[LocalDate](arbitraryDateInTaxYearOnOrBeforeDecember22nd),
              Gen.choose(5, 10) // any number of years ago over 4 (10 is arbitrary)
            ) {
              (currentDate, yearsAgo) =>

                forAll(arbitrary[LocalDate](arbitraryDateInTaxYearNTaxYearsAgo(yearsAgo))) { startDate =>
                  beforeEach()

                  when(taxYearService.currentDate).thenReturn(currentDate)

                  val result = taxYearService.firstTaxYearAvailable(startDate)

                  result mustBe FirstTaxYearAvailable(yearsAgo = 4, earlierYearsToDeclare = true)
                }
            }
          }

          "start date is 4 or less years ago" in {

            forAll(
              arbitrary[LocalDate](arbitraryDateInTaxYearOnOrBeforeDecember22nd),
              Gen.choose(0, 4) // 4 or less years ago
            ) {
              (currentDate, yearsAgo) =>

                forAll(arbitrary[LocalDate](arbitraryDateInTaxYearNTaxYearsAgo(yearsAgo))) { startDate =>
                  beforeEach()

                  when(taxYearService.currentDate).thenReturn(currentDate)

                  val result = taxYearService.firstTaxYearAvailable(startDate)

                  result mustBe FirstTaxYearAvailable(yearsAgo = yearsAgo, earlierYearsToDeclare = false)
                }
            }
          }
        }

        "after deadline" when {

          "start date is more than 3 tax years ago" in {

            forAll(
              arbitrary[LocalDate](arbitraryDateInTaxYearAfterDecember22nd),
              Gen.choose(4, 10) // any number of years ago over 3 (10 is arbitrary)
            ) {
              (currentDate, yearsAgo) =>

                forAll(arbitrary[LocalDate](arbitraryDateInTaxYearNTaxYearsAgo(yearsAgo))) { startDate =>
                  beforeEach()

                  when(taxYearService.currentDate).thenReturn(currentDate)

                  val result = taxYearService.firstTaxYearAvailable(startDate)

                  result mustBe FirstTaxYearAvailable(yearsAgo = 3, earlierYearsToDeclare = true)
                }
            }
          }

          "start date is 3 or less years ago" in {

            forAll(
              arbitrary[LocalDate](arbitraryDateInTaxYearAfterDecember22nd),
              Gen.choose(0, 3) // 3 or less years ago
            ) {
              (currentDate, yearsAgo) =>

                forAll(arbitrary[LocalDate](arbitraryDateInTaxYearNTaxYearsAgo(yearsAgo))) { startDate =>
                  beforeEach()

                  when(taxYearService.currentDate).thenReturn(currentDate)

                  val result = taxYearService.firstTaxYearAvailable(startDate)

                  result mustBe FirstTaxYearAvailable(yearsAgo = yearsAgo, earlierYearsToDeclare = false)
                }
            }
          }
        }
      }
    }
  }

}
