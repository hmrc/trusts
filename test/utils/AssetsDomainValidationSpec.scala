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

package utils

import base.BaseSpec
import models.Registration
import org.scalatest.matchers.must.Matchers._

class AssetsDomainValidationSpec  extends BaseSpec with DataExamples {
  def SUT(registration: Registration) = new AssetsDomainValidation(registration)

  "valueFullIsNotMoreThanValueValuePrevious" should {
    "return validation error when value full is more than value previous in property and land asset " in {
      val heritageFundTrust = heritageFundWithValues(valueFull = "999999999998")
      val response = SUT(heritageFundTrust).valueFullIsNotMoreThanValuePrevious
      response.flatten.size mustBe 1
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "Value full must be equal or more than value previous."
          error.location mustBe s"/trust/assets/propertyOrLand/${index}/valueFull"
      }

      BusinessValidation.check(heritageFundTrust).size mustBe 1
    }

    "return none when value full is same as value previous in property and land asset " in {
      val heritageFundTrust = heritageFundWithValues(valueFull = "999999999999")
      val response = SUT(heritageFundTrust).valueFullIsNotMoreThanValuePrevious
      response.flatten mustBe empty
      BusinessValidation.check(heritageFundTrust) mustBe empty
    }
  }

}
