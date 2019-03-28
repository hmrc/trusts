/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.utils

import uk.gov.hmrc.trusts.models.Registration
import uk.gov.hmrc.trusts.services.TrustsValidationError


class AssetsDomainValidation (registration: Registration) extends ValidationUtil {


  def valueFullIsNotMoreThanValuePrevious:List[Option[TrustsValidationError]]= {
    registration.trust.assets.propertyOrLand.map {
      properties =>
        properties.zipWithIndex.map {
          case (property, index) =>
            val isValid = property.valueFull.getOrElse(0L) >= property.valuePrevious.getOrElse(0L)
            if (!isValid) {
              Some(TrustsValidationError(s"Value full must be equal or more than value previous.",
                s"/trust/assets/propertyOrLand/${index}/valueFull"))
            } else {
              None
            }
        }
    }.toList.flatten
  }


}


object AssetsDomainValidation {

  def check(registration: Registration): List[TrustsValidationError] = {
    val aValidator = new AssetsDomainValidation(registration)
    aValidator.valueFullIsNotMoreThanValuePrevious.flatten
  }
}
