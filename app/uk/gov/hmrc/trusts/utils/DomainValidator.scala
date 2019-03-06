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
import uk.gov.hmrc.trusts.utils.TypeOfTrust._

class DomainValidator(registration : Registration) extends ValidationUtil {

  val EFRBS_VALIDAION_MESSAGE = "Trusts efrbs start date can be provided for Employment Related trust only."

  def trustStartDateIsNotFutureDate : Option[TrustsValidationError] = {
    isNotFutureDate(registration.details.trust.get.details.startDate,
      "/details/trust/details/startDate", "Trusts start date")
  }

  def validateEfrbsDate : Option[TrustsValidationError] = {
    val isEfrbsDateDefined = registration.details.trust.get.details.efrbsStartDate.isDefined
    val isEmploymentRelatedTrust = registration.details.trust.get.details.typeOfTrust==EMPLOYMENT_RELATED_TRUST.toString
    if (isEfrbsDateDefined && !isEmploymentRelatedTrust){
      Some(TrustsValidationError(EFRBS_VALIDAION_MESSAGE, "/details/trust/details/efrbsStartDate"))
    } else None
  }

  def trustEfrbsDateIsNotFutureDate : Option[TrustsValidationError] = {
    isNotFutureDate(registration.details.trust.get.details.efrbsStartDate,
      "/details/trust/details/efrbsStartDate", "Trusts efrbs start date")
  }

}


object BusinessValidation {

 def  check(registration : Registration)  = {
  val domainValidator =  new DomainValidator(registration)
   val errors = List (
     domainValidator.trustStartDateIsNotFutureDate,
     domainValidator.validateEfrbsDate,
     domainValidator.trustEfrbsDateIsNotFutureDate
   ).flatMap(x=> x)

   errors
  }
}
